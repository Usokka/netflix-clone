interface ApiErrorPayload {
  error?: string;
  message?: string;
  [key: string]: unknown;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly payload: ApiErrorPayload,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

let refreshPromise: Promise<boolean> | null = null;
let csrfPromise: Promise<string> | null = null;

async function parsePayload(response: Response): Promise<ApiErrorPayload | null> {
  if (response.status === 204) return null;

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) return null;

  return response.json().catch(() => null) as Promise<ApiErrorPayload | null>;
}

async function handleResponse<T>(response: Response): Promise<T> {
  const payload = await parsePayload(response);

  if (!response.ok) {
    const message = payload?.error ?? payload?.message ?? `Erreur API: ${response.status}`;
    throw new ApiError(message, response.status, payload ?? {});
  }

  return payload as T;
}

function isRefreshableRequest(url: string) {
  return !['/auth/login', '/auth/register', '/auth/refresh'].some((route) => url.includes(route));
}

function readCookie(name: string) {
  if (typeof document === 'undefined') return null;

  const prefix = `${name}=`;
  const cookie = document.cookie
    .split('; ')
    .find((entry) => entry.startsWith(prefix));

  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}

async function getCsrfToken(): Promise<string> {
  const cookieToken = readCookie('XSRF-TOKEN');
  if (cookieToken) return cookieToken;

  if (!csrfPromise) {
    csrfPromise = fetch('/api/v1/auth/csrf', {
      method: 'GET',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new ApiError('Impossible d’initialiser la protection CSRF', response.status, {});
        }

        const payload = await response.json() as { token: string };
        return payload.token;
      })
      .finally(() => {
        csrfPromise = null;
      });
  }

  return csrfPromise;
}

async function refreshSession(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = getCsrfToken()
      .then((csrfToken) => fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': csrfToken,
        },
        credentials: 'include',
      }))
      .then((response) => response.ok)
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function fetchWithAuth(url: string, options: RequestInit): Promise<Response> {
  let response = await fetch(url, options);

  if (response.status !== 401 || !isRefreshableRequest(url)) {
    return response;
  }

  if (await refreshSession()) {
    response = await fetch(url, options);
  } else if (typeof window !== 'undefined') {
    window.location.replace('/login');
  }

  return response;
}

async function request<T>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  endpoint: string,
  body?: unknown,
  profileId?: string,
): Promise<T> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  };

  if (profileId) headers['X-Profile-Id'] = profileId;
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = await getCsrfToken();

  const response = await fetchWithAuth(`/api/v1${endpoint}`, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  return handleResponse<T>(response);
}

export const apiClient = {
  get: <T>(endpoint: string, profileId?: string) =>
    request<T>('GET', endpoint, undefined, profileId),
  post: <T>(endpoint: string, body?: unknown, profileId?: string) =>
    request<T>('POST', endpoint, body, profileId),
  put: <T>(endpoint: string, body?: unknown, profileId?: string) =>
    request<T>('PUT', endpoint, body, profileId),
  delete: <T>(endpoint: string, profileId?: string) =>
    request<T>('DELETE', endpoint, undefined, profileId),
};
