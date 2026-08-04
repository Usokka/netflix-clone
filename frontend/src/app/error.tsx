'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Modal from '@/components/ui/modal';
import { AlertOctagon } from 'lucide-react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();

  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-zinc-800 via-[#141414] to-black flex items-center justify-center">
      <Modal 
        isOpen={true} 
        onClose={() => router.push('/')} 
        title="Oups! 😕"
      >
        <div className="flex flex-col items-center text-center space-y-4 mb-8">
          <AlertOctagon className="w-16 h-16 text-red-600 mb-2" />
          <p className="text-gray-300 font-medium">
            Une erreur est survenue lors du chargement du catalogue.
          </p>
          <p className="text-sm text-zinc-500">Veuillez vérifier votre connexion ou réessayer plus tard.</p>
        </div>

        <button
          onClick={() => reset()}
          className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3 rounded transition shadow-lg shadow-red-600/20"
        >
          Réessayer
        </button>
      </Modal>
    </div>
  );
}
