'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/apiClient';
import { AuthResponse } from '@/types';

interface AuthFormProps {
  mode: 'login' | 'register';
}

export default function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

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
        alert('Compte créé avec succès ! Vous pouvez maintenant vous connecter.');
        router.push('/login');
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Une erreur inattendue est survenue.');
      setLoading(false);
    }
  };

  return (
    <div
      className="relative min-h-screen w-full bg-cover bg-center flex items-center justify-center text-white"
      style={{
        backgroundImage:
          "linear-gradient(rgba(0,0,0,0.5), rgba(0,0,0,0.5)), url('https://assets.nflxext.com/ffe/siteui/vlv3/c1a7b547-581e-4521-869a-0341d01460d1/web/FR-fr-20230605-popsignuptwoweeks-perspective_alpha_website_large.jpg')",
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
          <div className="bg-[#e87c03] text-sm rounded px-4 py-3 mb-4 font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 flex-1">
          <input
            type="email"
            placeholder="Adresse e-mail"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className="w-full h-12 px-5 bg-[#333] border-0 rounded text-white placeholder-gray-400 focus:bg-[#454545] focus:outline-none transition-colors"
          />

          <input
            type="password"
            placeholder="Mot de passe"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
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
              <a href="/register" className="text-white hover:underline font-medium">
                Inscrivez-vous maintenant.
              </a>
            </>
          ) : (
            <>
              Déjà membre ?{' '}
              <a href="/login" className="text-white hover:underline font-medium">
                Connectez-vous.
              </a>
            </>
          )}
        </div>
      </div>
    </div>
  );
}