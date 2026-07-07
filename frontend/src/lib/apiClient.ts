// src/lib/apiClient.ts

async function handleResponse(response: Response) {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.error || errorData.message || `Erreur API: ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}

// Intercepteur HTTP maison
async function fetchWithAuth(url: string, options: RequestInit): Promise<Response> {
  let response = await fetch(url, options);

// ... (début de la fonction inchangé)

  if (response.status === 401 && !url.includes('/auth/refresh') && !url.includes('/auth/login')) {
    try {
      // 1. On tente de rafraîchir le token silencieusement
      const refreshRes = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // <--- LA LIGNE MAGIQUE EST ICI !
      });

      if (refreshRes.ok) {
        // 2. Succès ! Le backend a posé un nouveau cookie AUTH_TOKEN. On rejoue la requête initiale.
        response = await fetch(url, options); // options contient déjà credentials: 'include'
      } else {
        // 3. Échec.
        if (typeof window !== 'undefined') window.location.href = '/login';
      }
    } catch (error) {
      if (typeof window !== 'undefined') window.location.href = '/login';
    }
  }

  return response;
}

// ... (suite du fichier inchangée)

export const apiClient = {
  get: async <T>(endpoint: string, profileId?: string): Promise<T> => {
    const headers: HeadersInit = { 'Content-Type': 'application/json' };
    if (profileId) headers['X-Profile-Id'] = profileId;

    // NOUVEAU : On utilise fetchWithAuth au lieu de fetch
    const response = await fetchWithAuth(`/api/v1${endpoint}`, {
      method: 'GET',
      headers,
      credentials: 'include', 
    });
    return handleResponse(response) as Promise<T>;
  },

  post: async <T>(endpoint: string, body?: any, profileId?: string): Promise<T> => {
    const headers: HeadersInit = { 'Content-Type': 'application/json' };
    if (profileId) headers['X-Profile-Id'] = profileId;

    // NOUVEAU : On utilise fetchWithAuth
    const response = await fetchWithAuth(`/api/v1${endpoint}`, {
      method: 'POST',
      headers,
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
    });
    return handleResponse(response) as Promise<T>;
  },

  delete: async <T>(endpoint: string, profileId?: string): Promise<T> => {
    const headers: HeadersInit = { 'Content-Type': 'application/json' };
    if (profileId) headers['X-Profile-Id'] = profileId;

    // NOUVEAU : On utilise fetchWithAuth
    const response = await fetchWithAuth(`/api/v1${endpoint}`, {
      method: 'DELETE',
      headers,
      credentials: 'include',
    });
    return handleResponse(response) as Promise<T>;
  },
};