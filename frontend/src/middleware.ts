import { NextRequest, NextResponse } from 'next/server';

const PUBLIC_ROUTES = ['/login', '/register'];
const PROTECTED_ROUTES = ['/', '/watch', '/search']; 

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  let authToken = request.cookies.get('AUTH_TOKEN')?.value;
  const refreshToken = request.cookies.get('REFRESH_TOKEN')?.value;
  let tokenRefreshed = false;

  const isProtectedRoute = PROTECTED_ROUTES.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );

  const isPublicRoute = PUBLIC_ROUTES.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );

  // 1. TENTATIVE DE RAFRAÎCHISSEMENT SSR
  if (!authToken && refreshToken) {
    try {
      const backendUrl = process.env.API_INTERNAL_URL || 'http://localhost:8080';
      const refreshRes = await fetch(`${backendUrl}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { 'Cookie': `REFRESH_TOKEN=${refreshToken}` }
      });

      if (refreshRes.ok) {
        const setCookieHeader = refreshRes.headers.get('set-cookie');
        if (setCookieHeader) {
          const match = setCookieHeader.match(/AUTH_TOKEN=([^;]+)/);
          if (match) {
            authToken = match[1];
            tokenRefreshed = true;
          }
        }
      }
    } catch (err) {
      console.error("Erreur de rafraîchissement SSR :", err);
    }
  }

  const isAuthenticated = Boolean(authToken);

  // 2. REDIRECTIONS DE SÉCURITÉ
  if (isProtectedRoute && !isAuthenticated) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  if (isPublicRoute && isAuthenticated) {
    return NextResponse.redirect(new URL('/', request.url));
  }

  // 3. ON MODIFIE LA REQUÊTE *UNIQUEMENT* SI LE TOKEN A ÉTÉ RAFRAÎCHI
  if (tokenRefreshed && authToken) {
    const requestHeaders = new Headers(request.headers);
    
    // On récupère le cookie original brut, sans le faire décoder par Next.js
    const currentCookieHeader = request.headers.get('cookie') || '';
    
    // On remplace intelligemment l'ancien token ou on l'ajoute à la fin
    let newCookieHeader = currentCookieHeader;
    if (newCookieHeader.includes('AUTH_TOKEN=')) {
      newCookieHeader = newCookieHeader.replace(/AUTH_TOKEN=[^;]+/, `AUTH_TOKEN=${authToken}`);
    } else {
      newCookieHeader = newCookieHeader ? `${newCookieHeader}; AUTH_TOKEN=${authToken}` : `AUTH_TOKEN=${authToken}`;
    }
    
    requestHeaders.set('cookie', newCookieHeader);

    const response = NextResponse.next({
      request: { headers: requestHeaders }
    });

    // On sauvegarde le nouveau token dans le navigateur de l'utilisateur
    response.cookies.set('AUTH_TOKEN', authToken, {
      httpOnly: true,
      path: '/',
      maxAge: 15 * 60, // 15 minutes
      sameSite: 'lax'
    });

    return response;
  }

  // 4. AUCUN RAFRAÎCHISSEMENT ? ON LAISSE PASSER LA REQUÊTE INTACTE
  return NextResponse.next();
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};