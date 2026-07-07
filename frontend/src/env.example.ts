// frontend/src/env.example.ts
// Copy this file to .env.local and update with your backend URL

/**
 * Backend API URL for development and production
 * 
 * Development (Docker):
 * NEXT_PUBLIC_API_URL=http://backend-api:8080/api/v1
 * 
 * Development (Local):
 * NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
 * 
 * The Next.js rewrites in next.config.ts will handle routing:
 * - /api/v1/* → backend-api:8080/api/v1/*
 * - /video/* → streaming-service:8081/video/*
 */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api/v1";
