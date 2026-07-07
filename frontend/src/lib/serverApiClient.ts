// src/lib/serverApiClient.ts
import { cookies } from 'next/headers';

const getBaseUrl = () => {
  // Point crucial : communication directe via le réseau Docker "app-network"
  return process.env.API_INTERNAL_URL || 'http://localhost:8080';
};

async function handleResponse(response: Response) {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error || errorData.message || `Erreur API: ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}

export const serverApiClient = {
  get: async <T>(endpoint: string): Promise<T> => {
    const headers: Record<string, string> = { 
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    const cookieStore = await cookies();
    
    // 1. Propagation manuelle du token d'authentification
    const token = cookieStore.get('AUTH_TOKEN')?.value;
    if (token) {
      headers['Cookie'] = `AUTH_TOKEN=${token}`;
    }

    // 2. Propagation manuelle du profil actif (créé dans le ProfileContext)
    const profileId = cookieStore.get('profileId')?.value;
    if (profileId) {
      headers['X-Profile-Id'] = profileId;
    }

    const response = await fetch(`${getBaseUrl()}/api/v1${endpoint}`, {
      method: 'GET',
      headers,
      cache: 'no-store', // Désactive le cache agressif de Next.js pour les requêtes API
    });
    return handleResponse(response) as Promise<T>;
  },
};