'use client';
import React, { createContext, useContext, useState, useEffect } from 'react';
import { Profile } from '@/types';

interface ProfileContextType {
  activeProfile: Profile | null;
  setActiveProfile: (profile: Profile | null) => void;
}

const ProfileContext = createContext<ProfileContextType | undefined>(undefined);

export function ProfileProvider({ children }: { children: React.ReactNode }) {
  const [activeProfile, setActiveProfile] = useState<Profile | null>(null);

  // 1. Récupération du profil au chargement de l'application
  useEffect(() => {
    const storedProfile = localStorage.getItem('activeProfile');
    if (storedProfile) {
      try {
        setActiveProfile(JSON.parse(storedProfile));
      } catch (e) {
        console.error("Erreur de parsing du profil local", e);
      }
    }
  }, []);

  // 2. Fonction pour mettre à jour l'état, le localStorage et le cookie simultanément
  const handleSetProfile = (profile: Profile | null) => {
    setActiveProfile(profile);
    
    if (profile) {
      localStorage.setItem('activeProfile', JSON.stringify(profile));
      // On sauvegarde l'ID dans un cookie (SameSite=Lax) pour l'envoyer au backend si besoin
      document.cookie = `profileId=${profile.id}; path=/; max-age=31536000; SameSite=Lax`;
    } else {
      localStorage.removeItem('activeProfile');
      document.cookie = 'profileId=; path=/; max-age=0; SameSite=Lax';
    }
  };

  return (
    <ProfileContext.Provider value={{ activeProfile, setActiveProfile: handleSetProfile }}>
      {children}
    </ProfileContext.Provider>
  );
}

// Hook personnalisé pour consommer le contexte facilement
export function useProfile() {
  const context = useContext(ProfileContext);
  if (context === undefined) {
    throw new Error("useProfile doit être utilisé à l'intérieur d'un ProfileProvider");
  }
  return context;
}