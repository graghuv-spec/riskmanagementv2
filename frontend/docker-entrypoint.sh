#!/bin/sh
# ──────────────────────────────────────────────────────────────────────────────
# Substitute BACKEND_URL into the nginx config template at container start.
# Only $BACKEND_URL is replaced — all other nginx $variables are left intact.
# ──────────────────────────────────────────────────────────────────────────────
set -e

: "${BACKEND_URL:=http://backend:8080}"

envsubst '$BACKEND_URL' \
  < /etc/nginx/nginx.conf.template \
  > /etc/nginx/conf.d/default.conf

exec "$@"
