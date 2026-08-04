'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { CreditCard, CheckCircle2, XCircle, ArrowLeft } from 'lucide-react';
import { apiClient } from '@/lib/apiClient';
import { Subscription } from '@/types';
import Navbar from '@/components/Navbar';

export default function AccountPage() {
  const router = useRouter();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSubscriptions = async () => {
      try {
        const data = await apiClient.get<Subscription[]>('/subscriptions');
        setSubscriptions(data);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Impossible de charger vos abonnements.');
      } finally {
        setLoading(false);
      }
    };

    fetchSubscriptions();
  }, []);

  const formatDate = (dateString: string) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  };

  const getPlanName = (planCode: string) => {
    switch (planCode) {
      case 'BASIC': return 'Essentiel';
      case 'STANDARD': return 'Standard';
      case 'PREMIUM': return 'Premium';
      default: return planCode;
    }
  };

  return (
    <div className="min-h-screen bg-[#141414] text-white">
      <Navbar />

      <main className="pt-28 px-4 md:px-12 max-w-6xl mx-auto pb-12">
        <button 
          onClick={() => router.push('/')}
          className="flex items-center gap-2 text-gray-400 hover:text-white transition mb-8 text-sm font-medium"
        >
          <ArrowLeft className="w-4 h-4" /> Retour au catalogue
        </button>

        <h1 className="text-3xl md:text-4xl font-bold mb-8 flex items-center gap-3">
          <CreditCard className="w-8 h-8 text-red-600" />
          Mon Compte
        </h1>

        <div className="bg-zinc-900 border border-zinc-800 rounded-lg overflow-hidden shadow-2xl">
          <div className="p-6 border-b border-zinc-800 bg-zinc-900/50 flex items-center justify-between">
            <h2 className="text-xl font-semibold">Abonnement</h2>
            <button 
              onClick={() => router.push('/plans')}
              className="bg-zinc-800 hover:bg-zinc-700 text-white text-sm font-medium py-2 px-4 rounded transition"
            >
              Voir les forfaits
            </button>
          </div>

          {loading ? (
            <div className="p-12 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-red-600"></div>
            </div>
          ) : error ? (
            <div className="p-8 text-center text-red-500 bg-red-500/10">
              {error}
            </div>
          ) : subscriptions.length === 0 ? (
            <div className="p-12 text-center text-gray-400">
              Vous n&apos;avez aucun abonnement actif.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-zinc-800/50 text-gray-400 uppercase text-xs tracking-wider">
                  <tr>
                    <th className="px-6 py-4 font-medium">Forfait</th>
                    <th className="px-6 py-4 font-medium">Début</th>
                    <th className="px-6 py-4 font-medium">Expiration</th>
                    <th className="px-6 py-4 font-medium text-right">Statut</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {subscriptions.map((sub) => (
                    <tr key={sub.id} className="hover:bg-zinc-800/30 transition">
                      <td className="px-6 py-5 font-bold text-base">
                        {getPlanName(sub.plan)}
                      </td>
                      <td className="px-6 py-5 text-gray-300">
                        {formatDate(sub.startedAt)}
                      </td>
                      <td className="px-6 py-5 text-gray-300">
                        {formatDate(sub.expiresAt)}
                      </td>
                      <td className="px-6 py-5 text-right">
                        {sub.active ? (
                          <span className="inline-flex items-center gap-1.5 bg-green-500/10 text-green-500 px-3 py-1 rounded-full font-medium text-xs border border-green-500/20">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Actif
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 bg-zinc-800 text-gray-400 px-3 py-1 rounded-full font-medium text-xs border border-zinc-700">
                            <XCircle className="w-3.5 h-3.5" /> Expiré
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
