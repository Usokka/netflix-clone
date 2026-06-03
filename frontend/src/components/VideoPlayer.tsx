'use client';
import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';

interface VideoPlayerProps {
  movieId: string;
}

export default function VideoPlayer({ movieId }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let hls: Hls | null = null;
    let isMounted = true;

    const initializeVideo = async () => {
      try {
        const ticketRes = await fetch(`/api/v1/stream/${movieId}/ticket`, {
          credentials: 'include', // ← envoie le cookie AUTH_TOKEN
        });
        if (!ticketRes.ok) throw new Error('Impossible de récupérer le ticket de streaming');
        const { ticket } = await ticketRes.json();

        if (!isMounted) return;

        // ticket dans l'URL dès le départ — fonctionne avec XHR et fetch
        const manifestUrl = `/video/${movieId}/playlist.m3u8?ticket=${ticket}`;

        if (Hls.isSupported()) {
          hls = new Hls({
            // xhrSetup : appelé sur CHAQUE requête XHR (manifeste + segments .ts)
            // Réinjecte le ticket sur les segments aussi, car leurs URLs
            // dans le .m3u8 sont relatives et ne contiennent pas le ticket
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
              video.play().catch(() =>
                console.log('Lecture automatique bloquée par le navigateur')
              );
            }
          });

          hls.on(Hls.Events.ERROR, (_, data) => {
            if (data.fatal && isMounted) {
              console.error('Erreur HLS fatale:', data);
              setError(`Erreur de streaming : ${data.type}`);
              setLoading(false);
            }
          });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          // Safari — HLS natif
          video.src = manifestUrl;
          video.addEventListener('loadedmetadata', () => {
            if (isMounted) {
              setLoading(false);
              video.play();
            }
          });
        } else {
          setError('Votre navigateur ne supporte pas le streaming HLS.');
          setLoading(false);
        }
      } catch (err) {
        if (isMounted) {
          console.error(err);
          setError("Échec de l'authentification au flux vidéo.");
          setLoading(false);
        }
      }
    };

    initializeVideo();

    return () => {
      isMounted = false;
      if (hls) hls.destroy();
    };
  }, [movieId]);

  if (error) {
    return (
      <div className="w-full aspect-video bg-neutral-900 flex items-center justify-center text-red-500 border border-red-800 rounded-lg">
        <p className="text-center px-4">{error}</p>
      </div>
    );
  }

  return (
    <div className="relative w-full aspect-video bg-black rounded-lg overflow-hidden group">
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-neutral-950 z-10">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-red-600" />
        </div>
      )}
      <video
        ref={videoRef}
        className="w-full h-full"
        controls
        preload="auto"
        playsInline
        muted
        autoPlay
      />
    </div>
  );
}