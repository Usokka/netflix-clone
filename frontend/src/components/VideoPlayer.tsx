'use client';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { useRouter } from 'next/navigation';
import { ApiError, apiClient } from '@/lib/apiClient';
import { useProfile } from '@/context/ProfileContext';
import Modal from '@/components/ui/modal';
import { Lock } from 'lucide-react';

interface VideoPlayerProps {
  movieId: string; // NOUVEAU : Le vrai UUID pour la base de données
  videoFolderUrl: string; // NOUVEAU : Le nom du dossier pour le flux HLS
  timestamp?: number;
}

export default function VideoPlayer({ movieId, videoFolderUrl, timestamp = 0 }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [showSubscriptionModal, setShowSubscriptionModal] = useState(false);
  
  const { activeProfile } = useProfile();

  const lastSavedTimeRef = useRef<number>(timestamp);
  const currentTimeRef = useRef<number>(timestamp);

  const saveProgress = useCallback(async (time: number) => {
    if (!activeProfile || time === lastSavedTimeRef.current) return;
    const stoppedAtSeconds = Math.floor(time);

    try {
      await apiClient.post('/watch-history', {
        movieId,
        timestamp: stoppedAtSeconds,
      }, activeProfile.id);
      lastSavedTimeRef.current = stoppedAtSeconds;
    } catch (err) {
      console.error("Erreur sauvegarde progression :", err);
    }
  }, [activeProfile, movieId]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !activeProfile) return;

    let hls: Hls | null = null;
    let isMounted = true;
    let nativeLoadedMetadataHandler: (() => void) | null = null;

    const initializeVideo = async () => {
      try {
        const { ticket } = await apiClient.get<{ ticket: string }>(
          `/stream/${videoFolderUrl}/ticket`, 
          activeProfile.id
        );

        if (!isMounted) return;

        const manifestUrl = `/video/${videoFolderUrl}/playlist.m3u8?ticket=${ticket}`;

        if (Hls.isSupported()) {
          hls = new Hls({
            xhrSetup: (xhr, url) => {
              const requestUrl = new URL(url, window.location.href);
              if (requestUrl.origin === window.location.origin && !requestUrl.searchParams.has('ticket')) {
                const separator = url.includes('?') ? '&' : '?';
                xhr.open('GET', `${url}${separator}ticket=${ticket}`, true);
              }
            },
            startPosition: timestamp > 0 ? timestamp : -1,
          });

          hls.loadSource(manifestUrl);
          hls.attachMedia(video);

          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            if (isMounted) {
              setLoading(false);
              video.play().catch(() => console.log('Autoplay bloqué'));
            }
          });

          hls.on(Hls.Events.ERROR, (_, data) => {
            if (data.fatal && isMounted) {
              setError(`Erreur de streaming : ${data.type}`);
              setLoading(false);
            }
          });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          video.src = manifestUrl;
          nativeLoadedMetadataHandler = () => {
            if (isMounted) {
              if (timestamp > 0) video.currentTime = timestamp;
              setLoading(false);
              video.play().catch(() => console.log('Autoplay bloqué'));
            }
          };
          video.addEventListener('loadedmetadata', nativeLoadedMetadataHandler);
        }
      } catch (err: unknown) {
        if (isMounted) {
          setLoading(false);
          if (err instanceof ApiError && err.status === 403) {
            setShowSubscriptionModal(true);
          } else {
            setError("Échec de la connexion au flux vidéo.");
          }
        }
      }
    };

    initializeVideo();

    return () => {
      isMounted = false;
      if (currentTimeRef.current > 0) {
        void saveProgress(currentTimeRef.current);
      }
      if (nativeLoadedMetadataHandler) {
        video.removeEventListener('loadedmetadata', nativeLoadedMetadataHandler);
      }
      if (hls) hls.destroy();
    };
  }, [activeProfile, saveProgress, timestamp, videoFolderUrl]);

  // NOUVEAU : Écouteur pour la sauvegarde régulière (toutes les 15 secondes)
  const handleTimeUpdate = (e: React.SyntheticEvent<HTMLVideoElement>) => {
    const currentTime = e.currentTarget.currentTime;
    currentTimeRef.current = currentTime;

    if (currentTime - lastSavedTimeRef.current >= 15) {
      saveProgress(currentTime);
    }
  };

  return (
    <>
      <div className="relative w-full aspect-video bg-black rounded-lg overflow-hidden group border border-zinc-800 shadow-2xl">
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center bg-neutral-950 z-10">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-red-600" />
          </div>
        )}
        
        {error ? (
          <div className="absolute inset-0 flex items-center justify-center text-red-500 bg-neutral-950 z-10">
            <p className="text-center px-4 font-medium">{error}</p>
          </div>
        ) : (
          <video
            ref={videoRef}
            className="w-full h-full"
            controls
            preload="auto"
            playsInline
            muted={false}
            autoPlay
            onTimeUpdate={handleTimeUpdate}
          />
        )}
      </div>

      <Modal 
        isOpen={showSubscriptionModal} 
        onClose={() => router.push('/')} 
        title="Abonnement Requis"
      >
        <div className="flex flex-col items-center text-center space-y-4 mb-8">
          <div className="w-16 h-16 bg-red-600/20 rounded-full flex items-center justify-center mb-2 border border-red-600/30">
            <Lock className="w-8 h-8 text-red-500" />
          </div>
          <p className="text-gray-300 font-medium">
            Oups ! Il semble que vous n&apos;ayez pas d&apos;abonnement actif.
          </p>
          <p className="text-sm text-zinc-400">
            Pour visionner ce film en haute qualité et sans interruption, débloquez un accès Premium.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => router.push('/')}
            className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-white font-semibold py-3 rounded transition"
          >
            Plus tard
          </button>
          <button
            onClick={() => router.push('/plans')}
            className="flex-1 bg-red-600 hover:bg-red-700 text-white font-semibold py-3 rounded transition shadow-lg shadow-red-600/20"
          >
            Voir les forfaits
          </button>
        </div>
      </Modal>
    </>
  );
}
