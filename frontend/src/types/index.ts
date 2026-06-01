// src/types/index.ts
export interface Movie {
  id: string; // UUID as string from backend
  title: string;
  thumbnailUrl: string;
  videoFolderUrl: string;
  durationSeconds: number;
  description?: string;
  releaseYear?: number;
  maturityRating?: string;
  language?: string;
}