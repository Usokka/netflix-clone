'use client';
import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/apiClient';
import { useProfile } from '@/context/ProfileContext';
import Modal from '@/components/ui/modal';
import { Lock } from 'lucide-react';

interface VideoPlayerProps {
  movieId: string;
}

export default function VideoPlayer({ movieId }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  
  // Nouvel état pour notre modal d'abonnement
  const [showSubscriptionModal, setShowSubscriptionModal] = useState(false);
  
  const { activeProfile } = useProfile();

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !activeProfile) return;

    let hls: Hls | null = null;
    let isMounted = true;

    const initializeVideo = async () => {
      try {
        const { ticket } = await apiClient.get<{ ticket: string }>(
          `/stream/${movieId}/ticket`, 
          activeProfile.id
        );

        if (!isMounted) return;

        const manifestUrl = `/video/${movieId}/playlist.m3u8?ticket=${ticket}`;

        // ... (Le reste de ta configuration HLS.js reste identique)
        if (Hls.isSupported()) {
          hls = new Hls({
            xhrSetup: (xhr, url) => {
              if (!url.includes('ticket=')) {
                xhr.open('GET', `${url}?ticket=${ticket}`, true);
              }
            },
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
              setLoading(false);
              video.play();
            }
          });
        }
      } catch (err: any) {
        if (isMounted) {
          setLoading(false);
          // On vérifie si l'erreur vient du statut d'abonnement (403 Forbidden)
          // Selon la façon dont apiClient formate les erreurs, on vérifie le status ou le message
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
      if (hls) hls.destroy();
    };
  }, [movieId, activeProfile]);

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
            muted={false} // Désactiver le mute par défaut si possible
            autoPlay
          />
        )}
      </div>

      {/* --- MODAL D'ABONNEMENT --- */}
      <Modal 
        isOpen={showSubscriptionModal} 
        onClose={() => router.push('/')} // On renvoie à l'accueil si on ferme
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