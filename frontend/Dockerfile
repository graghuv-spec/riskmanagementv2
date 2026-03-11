# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first — better layer caching
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build for production
COPY . .
RUN npm run build:prod

# ── Stage 2: Serve with nginx ─────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# nginx.conf.template uses ${BACKEND_URL} which is substituted at container
# start by docker-entrypoint.sh.
COPY nginx.conf.template /etc/nginx/nginx.conf.template

# Copy Angular production build
# Angular 17 outputs to dist/<project>/browser/
COPY --from=build /app/dist/frontend/browser/ /usr/share/nginx/html/

# Entrypoint runs envsubst on the template then starts nginx
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
