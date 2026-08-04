'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/apiClient';
import { AuthResponse } from '@/types';
import Modal from '@/components/ui/modal';
import { CheckCircle } from 'lucide-react';
import Link from 'next/link';

interface AuthFormProps {
  mode: 'login' | 'register';
}

export default function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const isLogin = mode === 'login';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const endpoint = isLogin ? '/auth/login' : '/auth/register';

    try {
      await apiClient.post<AuthResponse>(endpoint, { email, password });

      if (isLogin) {
        router.push('/profiles');
      } else {
        setShowSuccessModal(true);
        setLoading(false);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Une erreur inattendue est survenue.');
      setLoading(false);
    }
  };

  const handleCloseSuccessModal = () => {
    setShowSuccessModal(false);
    router.push('/login');
  };

  return (
    <div
      className="relative min-h-screen w-full bg-cover bg-center flex items-center justify-center text-white"
      style={{
        backgroundImage: 'radial-gradient(circle at top, #3f3f46 0%, #18181b 40%, #09090b 100%)',
      }}
    >
      {/* Logo */}
      <div className="absolute top-4 left-8 md:left-16">
        <h1 className="text-red-600 text-4xl font-extrabold tracking-tighter uppercase select-none">
          Netflix
        </h1>
      </div>

      {/* Formulaire */}
      <div className="bg-black/75 p-16 rounded-md w-full max-w-[450px] min-h-[550px] flex flex-col backdrop-blur-sm">
        <h2 className="text-3xl font-bold mb-8">
          {isLogin ? 'Se connecter' : "S'inscrire"}
        </h2>

        {error && (
          <div role="alert" className="bg-[#e87c03] text-sm rounded px-4 py-3 mb-4 font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 flex-1">
          <label htmlFor={`${mode}-email`} className="sr-only">Adresse e-mail</label>
          <input
            id={`${mode}-email`}
            type="email"
            placeholder="Adresse e-mail"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            maxLength={254}
            required
            className="w-full h-12 px-5 bg-[#333] border-0 rounded text-white placeholder-gray-400 focus:bg-[#454545] focus:outline-none transition-colors"
          />

          <label htmlFor={`${mode}-password`} className="sr-only">Mot de passe</label>
          <input
            id={`${mode}-password`}
            type="password"
            placeholder="Mot de passe"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isLogin ? 'current-password' : 'new-password'}
            minLength={isLogin ? undefined : 12}
            maxLength={72}
            required
            className="w-full h-12 px-5 bg-[#333] border-0 rounded text-white placeholder-gray-400 focus:bg-[#454545] focus:outline-none transition-colors"
          />

          <button
            type="submit"
            disabled={loading}
            className="w-full h-12 bg-red-600 hover:bg-red-700 font-bold rounded mt-6 transition-colors flex items-center justify-center disabled:opacity-50"
          >
            {loading ? 'Patientez...' : isLogin ? 'Se connecter' : "S'inscrire"}
          </button>
        </form>

        <div className="text-gray-400 mt-12 text-sm">
          {isLogin ? (
            <>
              Nouveau sur Netflix ?{' '}
              <Link href="/register" className="text-white hover:underline font-medium">
                Inscrivez-vous maintenant.
              </Link>
            </>
          ) : (
            <>
              Déjà membre ?{' '}
              <Link href="/login" className="text-white hover:underline font-medium">
                Connectez-vous.
              </Link>
            </>
          )}
        </div>
      </div>

      <Modal
        isOpen={showSuccessModal}
        onClose={handleCloseSuccessModal}
        title="Bienvenue !"
      >
        <div className="flex flex-col items-center text-center space-y-4 mb-6">
          <CheckCircle className="w-16 h-16 text-green-500 mb-2" />
          <p className="text-gray-300 font-medium">
            Votre compte a été créé avec succès.
          </p>
          <p className="text-sm text-zinc-400">
            Vous pouvez maintenant vous connecter avec vos identifiants pour profiter de notre catalogue.
          </p>
        </div>

        <button
          onClick={handleCloseSuccessModal}
          className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3 rounded transition"
        >
          Aller à la connexion
        </button>
      </Modal>
    </div>
  );
}
