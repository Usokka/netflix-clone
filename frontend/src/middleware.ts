import { NextRequest, NextResponse } from 'next/server';

// Routes accessibles SANS être connecté
const PUBLIC_ROUTES = ['/login', '/register'];

// Routes qui nécessitent d'être connecté
const PROTECTED_ROUTES = ['/', '/watch'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const authToken = request.cookies.get('AUTH_TOKEN')?.value; // ← nom exact du cookie Spring
  const isAuthenticated = Boolean(authToken);

  // Vérifie si la route courante est protégée
  const isProtectedRoute = PROTECTED_ROUTES.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );

  // Vérifie si la route courante est publique (login/register)
  const isPublicRoute = PUBLIC_ROUTES.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );

  // Non authentifié sur une route protégée → redirect /login
  if (isProtectedRoute && !isAuthenticated) {
    const loginUrl = new URL('/login', request.url);
    return NextResponse.redirect(loginUrl);
  }

  // Déjà authentifié sur login/register → redirect /
  if (isPublicRoute && isAuthenticated) {
    const homeUrl = new URL('/', request.url);
    return NextResponse.redirect(homeUrl);
  }

  return NextResponse.next();
}

export const config = {
  // Le middleware tourne sur toutes les routes SAUF :
  // - les fichiers statiques Next.js (_next/static, _next/image)
  // - les assets publics (favicon, images, etc.)
  // - les routes API internes
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};