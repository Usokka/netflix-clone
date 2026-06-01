'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="h-screen w-screen bg-black flex flex-col items-center justify-center relative px-4">
      <div className="absolute top-6 left-6 z-50">
        <Link href="/" className="text-white flex items-center gap-2 font-semibold hover:text-gray-300 transition">
          <ArrowLeft className="w-6 h-6" />
          <span>Retour</span>
        </Link>
      </div>

      <div className="text-center space-y-6 max-w-md">
        <h1 className="text-5xl md:text-6xl font-bold text-red-600">Erreur 😞</h1>
        <p className="text-lg md:text-xl text-gray-300">
          Impossible de charger le film demandé.
        </p>
        <p className="text-sm md:text-base text-gray-400">
          {error.message || 'Cet ID de film n\'existe pas ou une erreur réseau s\'est produite.'}
        </p>
        <div className="flex gap-4 justify-center pt-4">
          <button
            onClick={() => reset()}
            className="px-6 py-3 bg-red-600 text-white rounded font-bold hover:bg-red-700 transition"
          >
            Réessayer
          </button>
          <Link href="/">
            <button className="px-6 py-3 bg-zinc-700 text-white rounded font-bold hover:bg-zinc-600 transition">
              Retour à l'accueil
            </button>
          </Link>
        </div>
      </div>
    </main>
  );
}
