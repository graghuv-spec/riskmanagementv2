# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first — better layer caching
# (npm ci only re-runs when package-lock.json changes)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build for production
COPY . .
RUN npm run build:prod

# ── Stage 2: Serve with nginx ─────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Custom nginx config (Angular routing + API proxy to backend service)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy Angular production build
# Angular 17 outputs to dist/<project-name>/browser/ (changed from dist/<project-name>/ in v17)
COPY --from=build /app/dist/frontend/browser/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
