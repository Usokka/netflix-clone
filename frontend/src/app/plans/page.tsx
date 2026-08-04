'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Check, ShieldCheck } from 'lucide-react';
import { apiClient } from '@/lib/apiClient';
import Modal from '@/components/ui/modal';

const PLANS = [
  {
    id: 'BASIC',
    name: 'Essentiel',
    price: '8,99€',
    resolution: '720p',
    color: 'bg-zinc-800 border-zinc-700',
  },
  {
    id: 'STANDARD',
    name: 'Standard',
    price: '13,49€',
    resolution: '1080p',
    color: 'bg-zinc-800 border-zinc-700',
  },
  {
    id: 'PREMIUM',
    name: 'Premium',
    price: '17,99€',
    resolution: '4K + HDR',
    color: 'bg-gradient-to-br from-red-900/40 to-red-600/10 border-red-600',
    popular: true,
  }
] as const;

export default function PlansPage() {
  const router = useRouter();
  const [selectedPlan, setSelectedPlan] = useState<(typeof PLANS)[number]['id']>('PREMIUM');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  const handleSubscribe = async () => {
    setLoading(true);
    setError(null);

    try {
      await apiClient.post('/subscriptions', { plan: selectedPlan });
      setShowSuccessModal(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "L'accès de démonstration n'a pas pu être activé.");
    } finally {
      setLoading(false);
    }
  };

  const handleCloseModal = () => {
    setShowSuccessModal(false);
    router.push('/');
  };

  return (
    <div className="min-h-screen bg-[#141414] text-white flex flex-col items-center pt-24 px-4 pb-12 select-none">
      <div className="max-w-5xl w-full relative z-10">
        <h1 className="text-4xl md:text-5xl font-bold mb-4 text-center">
          Choisissez le forfait qui vous convient
        </h1>
        <p className="text-xl text-gray-400 mb-12 text-center">
          Comparez les forfaits disponibles pour cette démonstration.
        </p>

        <div className="bg-amber-500/10 border border-amber-500/60 text-amber-100 px-4 py-3 rounded-lg mb-8 text-center max-w-2xl mx-auto">
          Aucun paiement réel n’est effectué. Les différences entre forfaits sont uniquement illustratives et
          l’activation de démonstration est désactivée par défaut côté serveur.
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-3 rounded-lg mb-8 text-center max-w-2xl mx-auto font-medium">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          {PLANS.map((plan) => (
            <label
              key={plan.id}
              className={`relative flex flex-col p-6 rounded-xl border-2 cursor-pointer transition-all duration-300 focus-within:ring-2 focus-within:ring-white ${
                selectedPlan === plan.id 
                  ? 'border-red-600 scale-105 shadow-2xl shadow-red-600/20' 
                  : `${plan.color} hover:border-gray-500`
              }`}
            >
              <input
                type="radio"
                name="subscription-plan"
                value={plan.id}
                checked={selectedPlan === plan.id}
                onChange={() => setSelectedPlan(plan.id)}
                className="sr-only"
              />
              {'popular' in plan && plan.popular && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-red-600 text-white text-xs font-bold px-3 py-1 rounded-full tracking-wider uppercase shadow-lg">
                  Le plus populaire
                </span>
              )}
              <h2 className="text-2xl font-bold mb-2">{plan.name}</h2>
              <p className="text-gray-400 mb-6 font-medium">Qualité vidéo : {plan.resolution}</p>
              
              <div className="text-4xl font-black mb-8">
                {plan.price} <span className="text-lg text-gray-500 font-normal">/mois</span>
              </div>

              <ul className="space-y-4 mb-8 flex-1">
                <li className="flex items-center gap-3 text-sm font-medium">
                  <Check className="w-5 h-5 text-red-600 shrink-0" /> Catalogue de démonstration
                </li>
                <li className="flex items-center gap-3 text-sm font-medium">
                  <Check className="w-5 h-5 text-red-600 shrink-0" /> Lecture HLS locale
                </li>
                <li className="flex items-center gap-3 text-sm font-medium">
                  <Check className="w-5 h-5 text-red-600 shrink-0" /> Historique et liste personnalisée
                </li>
              </ul>
            </label>
          ))}
        </div>

        <div className="flex justify-center">
          <button 
            onClick={handleSubscribe}
            disabled={loading}
            className="bg-red-600 hover:bg-red-700 text-white text-xl font-bold py-4 px-16 rounded transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-xl shadow-red-600/20 w-full md:w-auto"
          >
            {loading ? "Activation en cours..." : "Activer l'accès démo"}
          </button>
        </div>
      </div>

      <Modal
        isOpen={showSuccessModal}
        onClose={handleCloseModal}
        title="Accès de démonstration activé"
      >
        <div className="flex flex-col items-center text-center space-y-4 mb-8">
          <ShieldCheck className="w-16 h-16 text-green-500 mb-2" />
          <p className="text-gray-300 font-medium text-lg">
            Votre abonnement <span className="text-white font-bold">{PLANS.find(p => p.id === selectedPlan)?.name}</span> est désormais actif.
          </p>
          <p className="text-sm text-zinc-400">
            Cette activation ne correspond pas à un paiement et ne doit pas être utilisée en production.
          </p>
        </div>

        <button
          onClick={handleCloseModal}
          className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3.5 rounded transition shadow-lg shadow-red-600/20 text-lg"
        >
          Commencer à regarder
        </button>
      </Modal>
    </div>
  );
}
