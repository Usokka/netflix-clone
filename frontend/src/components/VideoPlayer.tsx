'use client';
import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/apiClient';
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

  const saveProgress = async (time: number) => {
    if (!activeProfile || time === lastSavedTimeRef.current) return;
    try {
      await apiClient.post('/watch-history', {
        movieId: movieId, // Ici, c'est le vrai UUID qui part vers PostgreSQL
        timestamp: Math.floor(time),
      }, activeProfile.id);
      lastSavedTimeRef.current = time;
    } catch (err) {
      console.error("Erreur sauvegarde progression :", err);
    }
  };

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !activeProfile) return;

    let hls: Hls | null = null;
    let isMounted = true;

    const initializeVideo = async () => {
      try {
        // CORRECTION : On utilise videoFolderUrl pour demander le ticket HLS
        const { ticket } = await apiClient.get<{ ticket: string }>(
          `/stream/${videoFolderUrl}/ticket`, 
          activeProfile.id
        );

        if (!isMounted) return;

        // CORRECTION : On utilise videoFolderUrl pour le chemin du fichier m3u8
        const manifestUrl = `/video/${videoFolderUrl}/playlist.m3u8?ticket=${ticket}`;



        if (Hls.isSupported()) {
          hls = new Hls({
            xhrSetup: (xhr, url) => {
              if (!url.includes('ticket=')) {
                xhr.open('GET', `${url}?ticket=${ticket}`, true);
              }
            },
            startPosition: timestamp > 0 ? timestamp : -1, // NOUVEAU : Démarrer au bon moment (HLS)
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
          video.addEventListener('loadedmetadata', () => {
            if (isMounted) {
              // NOUVEAU : Démarrer au bon moment (Safari / iOS natif)
              if (timestamp > 0) video.currentTime = timestamp;
              setLoading(false);
              video.play();
            }
          });
        }
      } catch (err: any) {
        if (isMounted) {
          setLoading(false);
          if (err.status === 403 || err.message?.includes('inactif') || err.message?.includes('403')) {
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
      // NOUVEAU : Sauvegarde finale à la fermeture du composant
      if (currentTimeRef.current > 0) {
        saveProgress(currentTimeRef.current);
      }
      if (hls) hls.destroy();
    };
  }, [movieId, activeProfile]);

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
            onTimeUpdate={handleTimeUpdate} // NOUVEAU : On relie l'événement
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
            Oups ! Il semble que vous n'ayez pas d'abonnement actif.
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
            S'abonner maintenant
          </button>
        </div>
      </Modal>
    </>
  );
}