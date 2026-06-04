'use client'; // Obligatoire : les error boundaries sont toujours côté client

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Modal from '@/components/ui/modal';
import { AlertCircle } from 'lucide-react';

export default function WatchError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();

  useEffect(() => {
    // Optionnel : Tu peux envoyer l'erreur à un service de monitoring ici
    console.error("Erreur interceptée par error.tsx :", error);
  }, [error]);

  return (
    <div className="h-screen w-screen bg-black flex items-center justify-center">
      {/* Notre composant réutilisable entre en scène */}
      <Modal 
        isOpen={true} 
        onClose={() => router.push('/')} 
        title="Impossible de lire la vidéo"
      >
        <div className="flex flex-col items-center text-center space-y-4 mb-8">
          <AlertCircle className="w-16 h-16 text-red-600 mb-2" />
          <p className="text-gray-300">
            Nous avons rencontré un problème lors du chargement de ce film. Il a peut-être été retiré de notre catalogue ou un problème réseau est survenu.
          </p>
          <p className="text-sm text-zinc-500 bg-zinc-800/50 p-2 rounded w-full border border-zinc-700/50">
            Détail : {error.message}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => router.push('/')}
            className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-white font-semibold py-2.5 rounded transition"
          >
            Retour à l'accueil
          </button>
          <button
            onClick={() => reset()} // reset() tente de re-rendre la page serveur
            className="flex-1 bg-white text-black hover:bg-neutral-200 font-semibold py-2.5 rounded transition"
          >
            Réessayer
          </button>
        </div>
      </Modal>
    </div>
  );
}