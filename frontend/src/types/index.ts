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
  genres?: Genre[];
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

export interface Subscription {
  id: string; // UUID
  plan: string;
  startedAt: string; 
  expiresAt?: string;
  isActive: boolean;
}

export interface AuthResponse {
  message: string;
  error?: string;
}