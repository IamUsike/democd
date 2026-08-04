# Build stage — production Vite bundle
FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Bake API base URL at build time (browser calls this host, not Docker DNS).
# On the VM set e.g. http://<VM_PUBLIC_IP>:8081 — not localhost.
ARG VITE_API_BASE_URL=http://localhost:8081
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# Runtime — serve static files with nginx
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
