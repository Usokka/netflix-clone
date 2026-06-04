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
    <div className="min-h-screen w-full bg-[#141414] flex items-center justify-center bg-[url('https://assets.nflxext.com/ffe/siteui/vlv3/c1a7b547-581e-4521-869a-0341d01460d1/web/FR-fr-20230605-popsignuptwoweeks-perspective_alpha_website_large.jpg')] bg-cover bg-center bg-blend-overlay">
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
          <p className="text-sm text-zinc-500 bg-zinc-800/50 p-3 rounded w-full border border-zinc-700/50 break-words">
            {error.message || 'Veuillez vérifier votre connexion ou réessayer plus tard.'}
          </p>
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