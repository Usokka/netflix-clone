'use client';

import { useEffect } from 'react';

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
    <div className="min-h-screen w-full bg-[#141414] flex flex-col items-center justify-center px-4">
      <div className="text-center space-y-4">
        <h1 className="text-4xl md:text-6xl font-bold text-red-600">Oups! 😕</h1>
        <p className="text-lg md:text-xl text-gray-300">
          Une erreur est survenue lors du chargement du catalogue.
        </p>
        <p className="text-sm md:text-base text-gray-400 max-w-md">
          {error.message || 'Veuillez vérifier votre connexion ou réessayer plus tard.'}
        </p>
        <button
          onClick={() => reset()}
          className="mt-8 px-6 py-3 bg-red-600 text-white rounded font-bold hover:bg-red-700 transition"
        >
          Réessayer
        </button>
      </div>
    </div>
  );
}
