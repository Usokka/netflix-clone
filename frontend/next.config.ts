/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  async rewrites() {
    return {
      beforeFiles: [
        {
          source: '/api/v1/:path*',
          destination: 'http://backend-api:8080/api/v1/:path*',
        },
        {
          source: '/video/:path*',
          destination: 'http://streaming-service:8081/video/:path*',
        },
      ],
    };
  },
};

export default nextConfig;