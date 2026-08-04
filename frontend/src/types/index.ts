// src/types/index.ts

export interface Movie {
  id: string; // UUID venant du backend Java
  title:string;
  thumbnailUrl: string;
  videoFolderUrl: string;
  durationSeconds: number;
  description?: string;
  releaseYear?: number;
  maturityRating?: string;
  language?: string;
  genres?: string[];
  timestamp?: number; 
  progressPercentage?: number;
}

export interface Genre {
  id: number;
  name: string;
}

export interface User {
  id: string; // UUID
  email: string;
  role: string;
  createdAt: string; 
  profiles?: Profile[];
  subscriptions?: Subscription[];
}

export interface Profile {
  id: string; // UUID
  name: string;
  avatarUrl?: string;
}

// MISE À JOUR ICI
export interface Subscription {
  id: string; // UUID
  plan: string;
  startedAt: string; 
  expiresAt: string;
  active: boolean; // Corrigé pour correspondre au DTO Java
}

export interface AuthResponse {
  message: string;
  error?: string;
}
