'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { PlusCircle, Pencil } from 'lucide-react';
import { Profile } from '@/types';
import { useProfile } from '@/context/ProfileContext';
import { apiClient } from "@/lib/apiClient";

export default function ProfilesPage() {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false); // <-- Nouvel état de gestion
  
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

  const handleProfileClick = async (profile: Profile) => {
    if (isEditing) {
      // Mode Édition : on demande l'action à l'utilisateur
      const action = window.prompt(
        `Gérer le profil "${profile.name}"\nTapez 'M' pour modifier le nom, ou 'S' pour supprimer :`
      );

      if (action?.toUpperCase() === 'M') {
        const newName = window.prompt("Nouveau nom :", profile.name);
        if (newName && newName.trim() !== "" && newName !== profile.name) {
          try {
            const updatedProfile = await apiClient.post<Profile>(`/profiles/${profile.id}`, {
              name: newName.trim(),
              avatarUrl: profile.avatarUrl
            }); // Note: Si ton backend utilise PUT pour la modification, remplace .post par .put
            setProfiles(profiles.map(p => p.id === profile.id ? updatedProfile : p));
          } catch (err) {
            alert("Erreur lors de la modification.");
          }
        }
      } else if (action?.toUpperCase() === 'S') {
        const confirmDelete = window.confirm(`Voulez-vous vraiment supprimer le profil ${profile.name} ?`);
        if (confirmDelete) {
          try {
            await apiClient.delete(`/profiles/${profile.id}`);
            setProfiles(profiles.filter(p => p.id !== profile.id));
          } catch (err) {
            alert("Erreur lors de la suppression.");
          }
        }
      }
    } else {
      // Mode normal : Sélection du profil et redirection
      setActiveProfile(profile);
      router.push('/');
    }
  };

  const handleCreateProfile = async () => {
    const name = window.prompt('Entrez le nom du nouveau profil :');
    if (!name || name.trim() === '') return;

    try {
      const newProfile = await apiClient.post<Profile>('/profiles', {
        name: name.trim(),
        avatarUrl: `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(name)}`
      });
      setProfiles([...profiles, newProfile]);
    } catch (err: any) {
      alert(`Erreur : ${err.message}`);
    }
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
        <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-2 rounded mb-8">
          {error}
        </div>
      )}

      <div className="flex flex-wrap justify-center gap-4 md:gap-8 max-w-4xl px-4">
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
              {/* Overlay d'édition */}
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
            onClick={handleCreateProfile}
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
        className={`mt-16 border uppercase tracking-widest text-sm px-6 py-2 transition-colors duration-300 ${
          isEditing 
            ? "bg-white text-black font-bold border-white hover:bg-neutral-200" 
            : "border-gray-500 text-gray-400 hover:text-white hover:border-white"
        }`}
        onClick={() => setIsEditing(!isEditing)}
      >
        {isEditing ? "Terminé" : "Gérer les profils"}
      </button>
    </div>
  );
}