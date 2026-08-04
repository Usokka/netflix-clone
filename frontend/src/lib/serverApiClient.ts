import { cookies } from 'next/headers';

export class ServerApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
    this.name = 'ServerApiError';
  }
}

const getBaseUrl = () => process.env.API_INTERNAL_URL || 'http://localhost:8080';

async function handleResponse<T>(response: Response): Promise<T> {
  const payload = await response.json().catch(() => null) as {
    error?: string;
    message?: string;
  } | null;

  if (!response.ok) {
    throw new ServerApiError(
      payload?.error ?? payload?.message ?? `Erreur API: ${response.status}`,
      response.status,
    );
  }

  return payload as T;
}

export const serverApiClient = {
  get: async <T>(endpoint: string): Promise<T> => {
    const headers: Record<string, string> = { Accept: 'application/json' };
    const cookieStore = await cookies();

    const token = cookieStore.get('AUTH_TOKEN')?.value;
    if (token) headers.Cookie = `AUTH_TOKEN=${token}`;

    const profileId = cookieStore.get('profileId')?.value;
    if (profileId) headers['X-Profile-Id'] = profileId;

    const response = await fetch(`${getBaseUrl()}/api/v1${endpoint}`, {
      method: 'GET',
      headers,
      cache: 'no-store',
      signal: AbortSignal.timeout(10_000),
    });
    return handleResponse<T>(response);
  },
};
