/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'api.dicebear.com' },
      { protocol: 'https', hostname: 'upload.wikimedia.org' },
      { protocol: 'https', hostname: 'assets.nflxext.com' },
    ],
  },
  async rewrites() {
    const backendUrl = process.env.BACKEND_URL ?? 'http://localhost:8080';
    const streamingUrl = process.env.STREAMING_URL ?? 'http://localhost:8081';

    return {
      beforeFiles: [
        {
          source: '/api/v1/:path*',
          destination: `${backendUrl}/api/v1/:path*`,
        },
        {
          source: '/video/:path*',
          destination: `${streamingUrl}/video/:path*`,
        },
      ],
    };
  },
};

export default nextConfig;
