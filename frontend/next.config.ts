/** @type {import('next').NextConfig} */
const nextConfig = {
  output : 'standalone',
  async rewrites() {
    return [
      {
        // Redirection vers l'API Spring Boot
        source: '/api/v1/:path*',
        destination: 'http://localhost:8080/api/v1/:path*',
      },
      {
        // Redirection vers le moteur de streaming HLS en C++
        source: '/video/:path*',
        destination: 'http://localhost:8081/video/:path*',
      },
    ];
  },
};

export default nextConfig;