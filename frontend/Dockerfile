FROM node:22-alpine3.23 AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:22-alpine3.23 AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_TELEMETRY_DISABLED=1
ENV NEXT_FONT_GOOGLE_MOCKED=1

ARG BACKEND_URL=http://backend-api:8080
ARG STREAMING_URL=http://streaming-service:8081
ENV BACKEND_URL=$BACKEND_URL
ENV STREAMING_URL=$STREAMING_URL

RUN npm run build

FROM node:22-alpine3.23 AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 nodejs \
    && adduser --system --uid 1001 --ingroup nodejs nextjs

COPY --from=builder --chown=nextjs:nodejs /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
CMD ["node", "server.js"]
