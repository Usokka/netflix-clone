'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { PlusCircle, Pencil, AlertTriangle } from 'lucide-react';
import { Profile } from '@/types';
import { useProfile } from '@/context/ProfileContext';
import { apiClient } from "@/lib/apiClient";
import Modal from '@/components/ui/modal'; // Assure-toi que le chemin est correct

// Définition de l'état de notre Modal pour couvrir tous les cas
type ModalState = {
  isOpen: boolean;
  mode: 'create' | 'edit' | 'delete' | null;
  targetProfile: Profile | null;
  inputValue: string;
};

export default function ProfilesPage() {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  
  // État de la modal
  const [modal, setModal] = useState<ModalState>({
    isOpen: false,
    mode: null,
    targetProfile: null,
    inputValue: ''
  });
  
  const router = useRouter();
  const { setActiveProfile } = useProfile();

  useEffect(() => {
    const fetchProfiles = async () => {
      try {
        const data = await apiClient.get<Profile[]>('/profiles');
        setProfiles(data);
      } catch (err: any) {
        setError(err.message || 'Impossible de charger les profils.');
      } finally {
        setLoading(false);
      }
    };
    fetchProfiles();
  }, []);

  // Fonction utilitaire pour ouvrir la modal selon le contexte
  const openModal = (mode: ModalState['mode'], profile: Profile | null = null) => {
    setModal({
      isOpen: true,
      mode,
      targetProfile: profile,
      inputValue: mode === 'edit' && profile ? profile.name : ''
    });
  };

  const closeModal = () => {
    setModal({ isOpen: false, mode: null, targetProfile: null, inputValue: '' });
  };

  const handleProfileClick = (profile: Profile) => {
    if (isEditing) {
      // Au lieu du window.prompt, on ouvre notre belle modal en mode édition
      openModal('edit', profile);
    } else {
      setActiveProfile(profile);
      router.push('/');
    }
  };

  // Ce handler remplace tous les anciens blocs try/catch disséminés dans le code
  const handleModalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const { mode, targetProfile, inputValue } = modal;
    
    try {
      if (mode === 'create') {
        if (!inputValue.trim()) return;
        const newProfile = await apiClient.post<Profile>('/profiles', {
          name: inputValue.trim(),
          avatarUrl: `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(inputValue.trim())}`
        });
        setProfiles([...profiles, newProfile]);
      } 
      
      else if (mode === 'edit' && targetProfile) {
        if (!inputValue.trim() || inputValue === targetProfile.name) {
          closeModal();
          return;
        }
        // Note: vérifie si ton API attend un PUT ou un POST ici
        const updatedProfile = await apiClient.post<Profile>(`/profiles/${targetProfile.id}`, {
          name: inputValue.trim(),
          avatarUrl: targetProfile.avatarUrl
        }); 
        setProfiles(profiles.map(p => p.id === targetProfile.id ? updatedProfile : p));
      }
      
      else if (mode === 'delete' && targetProfile) {
        await apiClient.delete(`/profiles/${targetProfile.id}`);
        setProfiles(profiles.filter(p => p.id !== targetProfile.id));
      }

      closeModal();
    } catch (err: any) {
      setError(`Erreur lors de l'opération : ${err.message}`);
    }
  };

  // Helper pour le titre de la Modal
  const getModalTitle = () => {
    if (modal.mode === 'create') return "Créer un profil";
    if (modal.mode === 'edit') return "Modifier le profil";
    if (modal.mode === 'delete') return "Supprimer le profil";
    return "";
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#141414] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-red-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#141414] flex flex-col items-center justify-center text-white select-none">
      <h1 className="text-3xl md:text-5xl font-semibold mb-8 tracking-wide">
        {isEditing ? "Gérer les profils" : "Qui est-ce ?"}
      </h1>

      {error && (
        <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-2 rounded mb-8 z-10">
          {error}
        </div>
      )}

      {/* Grille des profils (Inchangée) */}
      <div className="flex flex-wrap justify-center gap-4 md:gap-8 max-w-4xl px-4 z-10">
        {profiles.map((profile) => (
          <div
            key={profile.id}
            onClick={() => handleProfileClick(profile)}
            className="group flex flex-col items-center cursor-pointer max-w-[120px] relative"
          >
            <div className={`w-24 h-24 md:w-32 md:h-32 rounded overflow-hidden border-2 transition-all duration-300 ${isEditing ? 'border-neutral-500 opacity-50 hover:opacity-100 hover:border-white' : 'border-transparent hover:border-white'}`}>
              <img
                src={profile.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${profile.name}`}
                alt={profile.name}
                className="w-full h-full object-cover bg-zinc-800"
              />
              {isEditing && (
                <div className="absolute inset-0 flex items-center justify-center">
                  <Pencil className="w-8 h-8 text-white drop-shadow-lg" />
                </div>
              )}
            </div>
            <span className="mt-4 text-gray-400 group-hover:text-white transition-colors duration-300 truncate w-full text-center">
              {profile.name}
            </span>
          </div>
        ))}

        {profiles.length < 4 && (
          <div
            onClick={() => openModal('create')} // On ouvre la modal au lieu du prompt
            className="group flex flex-col items-center cursor-pointer max-w-[120px]"
          >
            <div className="w-24 h-24 md:w-32 md:h-32 rounded border-2 border-transparent group-hover:bg-zinc-800 transition-all duration-300 flex items-center justify-center">
              <PlusCircle className="w-12 h-12 text-gray-400 group-hover:text-white transition-colors" />
            </div>
            <span className="mt-4 text-gray-400 group-hover:text-white transition-colors duration-300">
              Ajouter
            </span>
          </div>
        )}
      </div>

      <button 
        className={`mt-16 border uppercase tracking-widest text-sm px-6 py-2 transition-colors duration-300 z-10 ${
          isEditing 
            ? "bg-white text-black font-bold border-white hover:bg-neutral-200" 
            : "border-gray-500 text-gray-400 hover:text-white hover:border-white"
        }`}
        onClick={() => setIsEditing(!isEditing)}
      >
        {isEditing ? "Terminé" : "Gérer les profils"}
      </button>

      {/* --- INTEGRATION DE NOTRE COMPOSANT MODAL --- */}
      <Modal 
        isOpen={modal.isOpen} 
        onClose={closeModal} 
        title={getModalTitle()}
      >
        <form onSubmit={handleModalSubmit}>
          {modal.mode !== 'delete' ? (
            <div className="space-y-4">
              <input
                type="text"
                value={modal.inputValue}
                onChange={(e) => setModal({ ...modal, inputValue: e.target.value })}
                placeholder="Nom du profil"
                className="w-full bg-zinc-800 border border-zinc-600 rounded px-4 py-3 text-white placeholder-gray-400 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                autoFocus
              />
              {modal.mode === 'edit' && (
                <button
                  type="button"
                  onClick={() => setModal({ ...modal, mode: 'delete' })}
                  className="text-red-500 text-sm hover:underline flex items-center gap-1 mt-2"
                >
                  <AlertTriangle className="w-4 h-4" /> Supprimer ce profil
                </button>
              )}
            </div>
          ) : (
            <div className="text-gray-300 mb-6">
              Êtes-vous sûr de vouloir supprimer le profil <span className="text-white font-bold">{modal.targetProfile?.name}</span> ? Cette action est irréversible.
            </div>
          )}

          <div className="flex items-center gap-3 mt-8">
            <button
              type="button"
              onClick={closeModal}
              className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-white font-semibold py-2.5 rounded transition"
            >
              Annuler
            </button>
            <button
              type="submit"
              className={`flex-1 font-semibold py-2.5 rounded transition ${
                modal.mode === 'delete' 
                  ? "bg-red-600 hover:bg-red-700 text-white" 
                  : "bg-white text-black hover:bg-neutral-200"
              }`}
            >
              {modal.mode === 'delete' ? "Supprimer" : "Enregistrer"}
            </button>
          </div>
        </form>
      </Modal>

    </div>
  );
}