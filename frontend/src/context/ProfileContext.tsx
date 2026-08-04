'use client';

import React, { createContext, useCallback, useContext, useMemo, useSyncExternalStore } from 'react';
import { Profile } from '@/types';

interface ProfileContextType {
  activeProfile: Profile | null;
  setActiveProfile: (profile: Profile | null) => void;
}

const STORAGE_KEY = 'activeProfile';
const PROFILE_CHANGE_EVENT = 'active-profile-change';
const ProfileContext = createContext<ProfileContextType | undefined>(undefined);

function subscribeToProfile(callback: () => void) {
  window.addEventListener('storage', callback);
  window.addEventListener(PROFILE_CHANGE_EVENT, callback);

  return () => {
    window.removeEventListener('storage', callback);
    window.removeEventListener(PROFILE_CHANGE_EVENT, callback);
  };
}

function getProfileSnapshot() {
  return localStorage.getItem(STORAGE_KEY);
}

function getServerProfileSnapshot() {
  return null;
}

function parseProfile(serializedProfile: string | null): Profile | null {
  if (!serializedProfile) return null;

  try {
    const profile = JSON.parse(serializedProfile) as Partial<Profile>;
    if (typeof profile.id !== 'string' || typeof profile.name !== 'string') return null;
    return profile as Profile;
  } catch {
    return null;
  }
}

export function ProfileProvider({ children }: { children: React.ReactNode }) {
  const serializedProfile = useSyncExternalStore(
    subscribeToProfile,
    getProfileSnapshot,
    getServerProfileSnapshot,
  );
  const activeProfile = useMemo(() => parseProfile(serializedProfile), [serializedProfile]);

  const setActiveProfile = useCallback((profile: Profile | null) => {
    if (profile) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
      document.cookie = `profileId=${profile.id}; path=/; max-age=31536000; SameSite=Lax`;
    } else {
      localStorage.removeItem(STORAGE_KEY);
      document.cookie = 'profileId=; path=/; max-age=0; SameSite=Lax';
    }

    window.dispatchEvent(new Event(PROFILE_CHANGE_EVENT));
  }, []);

  return (
    <ProfileContext.Provider value={{ activeProfile, setActiveProfile }}>
      {children}
    </ProfileContext.Provider>
  );
}

export function useProfile() {
  const context = useContext(ProfileContext);
  if (context === undefined) {
    throw new Error("useProfile doit être utilisé à l'intérieur d'un ProfileProvider");
  }
  return context;
}
