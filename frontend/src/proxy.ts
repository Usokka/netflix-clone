import { NextRequest, NextResponse } from 'next/server';

const PUBLIC_ROUTES = ['/login', '/register'];
const PROTECTED_ROUTES = ['/', '/account', '/plans', '/profiles', '/search', '/watch'];

interface RefreshedSession {
  accessToken: string;
  setCookieHeaders: string[];
}

function matchesRoute(pathname: string, route: string) {
  if (route === '/') return pathname === '/';
  return pathname === route || pathname.startsWith(`${route}/`);
}

function isAccessTokenUsable(token: string | undefined) {
  if (!token) return false;

  try {
    const payloadPart = token.split('.')[1];
    if (!payloadPart) return false;
    const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
    const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const payload = JSON.parse(atob(paddedBase64)) as { exp?: number; token_type?: string };
    return payload.token_type === 'access'
      && typeof payload.exp === 'number'
      && payload.exp * 1000 > Date.now() + 30_000;
  } catch {
    return false;
  }
}

function updateAccessCookieHeader(cookieHeader: string, accessToken: string) {
  if (cookieHeader.includes('AUTH_TOKEN=')) {
    return cookieHeader.replace(/AUTH_TOKEN=[^;]*/, `AUTH_TOKEN=${accessToken}`);
  }
  return cookieHeader
    ? `${cookieHeader}; AUTH_TOKEN=${accessToken}`
    : `AUTH_TOKEN=${accessToken}`;
}

async function refreshSession(request: NextRequest): Promise<RefreshedSession | null> {
  const refreshToken = request.cookies.get('REFRESH_TOKEN')?.value;
  if (!refreshToken) return null;

  try {
    const backendUrl = process.env.API_INTERNAL_URL || 'http://localhost:8080';
    let csrfToken = request.cookies.get('XSRF-TOKEN')?.value;
    let csrfCookieHeaders: string[] = [];

    if (!csrfToken) {
      const csrfResponse = await fetch(`${backendUrl}/api/v1/auth/csrf`, {
        method: 'GET',
        headers: { Accept: 'application/json' },
        cache: 'no-store',
      });
      if (!csrfResponse.ok) return null;

      const payload = await csrfResponse.json() as { token?: string };
      csrfToken = payload.token;
      csrfCookieHeaders = csrfResponse.headers.getSetCookie();
    }

    if (!csrfToken) return null;

    const response = await fetch(`${backendUrl}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: {
        Cookie: `REFRESH_TOKEN=${refreshToken}; XSRF-TOKEN=${csrfToken}`,
        'X-XSRF-TOKEN': csrfToken,
      },
      cache: 'no-store',
    });
    if (!response.ok) return null;

    const setCookieHeaders = [
      ...csrfCookieHeaders,
      ...response.headers.getSetCookie(),
    ];
    const accessCookie = setCookieHeaders.find((header) => header.startsWith('AUTH_TOKEN='));
    const accessToken = accessCookie?.match(/^AUTH_TOKEN=([^;]+)/)?.[1];
    return accessToken ? { accessToken, setCookieHeaders } : null;
  } catch {
    return null;
  }
}

function attachSessionCookies(response: NextResponse, setCookieHeaders: string[]) {
  setCookieHeaders.forEach((header) => response.headers.append('Set-Cookie', header));
  return response;
}

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname.startsWith('/video/')) {
    const requestHeaders = new Headers(request.headers);
    requestHeaders.delete('cookie');
    return NextResponse.next({ request: { headers: requestHeaders } });
  }

  const isPublicRoute = PUBLIC_ROUTES.some((route) => matchesRoute(pathname, route));
  const isProtectedRoute = PROTECTED_ROUTES.some((route) => matchesRoute(pathname, route));
  const currentAccessToken = request.cookies.get('AUTH_TOKEN')?.value;

  let usableAccessToken = isAccessTokenUsable(currentAccessToken) ? currentAccessToken : undefined;
  let refreshedSession: RefreshedSession | null = null;
  if (!usableAccessToken && (isProtectedRoute || isPublicRoute)) {
    refreshedSession = await refreshSession(request);
    usableAccessToken = refreshedSession?.accessToken;
  }

  if (isProtectedRoute && !usableAccessToken) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('next', pathname);
    return NextResponse.redirect(loginUrl);
  }

  if (isPublicRoute && usableAccessToken) {
    const response = NextResponse.redirect(new URL('/', request.url));
    return refreshedSession
      ? attachSessionCookies(response, refreshedSession.setCookieHeaders)
      : response;
  }

  if (refreshedSession) {
    const requestHeaders = new Headers(request.headers);
    requestHeaders.set(
      'cookie',
      updateAccessCookieHeader(request.headers.get('cookie') ?? '', refreshedSession.accessToken),
    );
    const response = NextResponse.next({ request: { headers: requestHeaders } });
    return attachSessionCookies(response, refreshedSession.setCookieHeaders);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/((?!api|_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};
