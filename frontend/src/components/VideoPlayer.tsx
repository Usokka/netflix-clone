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
        // 1. Récupérer le ticket de streaming AVANT d'initialiser HLS
        // Cela garantit que le ticket est disponible pour le fetchSetup
        const ticketRes = await fetch(`/api/v1/stream/${movieId}/ticket`);
        if (!ticketRes.ok) throw new Error('Impossible de récupérer le ticket de streaming');
        const ticketData = await ticketRes.json();
        const ticket = ticketData.ticket;

        if (!isMounted) return;

        // L'URL du manifeste cible le rewrite Next.js /video/*
        const manifestUrl = `/video/${movieId}/playlist.m3u8`;

        // 2. Vérifier si Hls.js est supporté par le navigateur (Cas général : Chrome, Firefox, Arch Chromium...)
        if (Hls.isSupported()) {
          hls = new Hls({
            // Config cruciale : On intercepte chaque appel réseau de Hls.js pour y injecter notre ticket
            fetchSetup: (context, init) => {
              const url = new URL(context.url, window.location.href);
              url.searchParams.set('ticket', ticket); // Ajoute dynamiquement ?ticket=eyJhbGci...
              return new Request(url.toString(), init);
            },
          });

          hls.loadSource(manifestUrl);
          hls.attachMedia(video);

          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            if (isMounted) {
              setLoading(false);
              video.play().catch(() => console.log("Lecture automatique bloquée par le navigateur"));
            }
          });

          hls.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal && isMounted) {
              console.error('Erreur HLS fatale:', data);
              setError(`Erreur de streaming : ${data.type}`);
              setLoading(false);
            }
          });
        }
        // 3. Cas particulier (Safari / iOS) qui gère le HLS nativement sans bibliothèque
        else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          video.src = `${manifestUrl}?ticket=${ticket}`;
          video.addEventListener('loadedmetadata', () => {
            if (isMounted) {
              setLoading(false);
              video.play();
            }
          });
        } else {
          setError("Votre navigateur ne supporte pas le streaming HLS.");
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

    // Nettoyage au démontage du composant
    return () => {
      isMounted = false;
      if (hls) {
        hls.destroy();
      }
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
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-red-600"></div>
        </div>
      )}
      <video
        ref={videoRef}
        className="w-full h-full"
        controls
        preload="auto"
        playsInline
      />
    </div>
  );
}