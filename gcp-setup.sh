#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# gcp-setup.sh  –  One-time GCP infrastructure setup for RiskManagement Pro
#
# Run this ONCE from your local machine before the first Cloud Build deploy.
# Prerequisites: gcloud CLI installed and authenticated (gcloud auth login)
#
# Usage:
#   chmod +x gcp-setup.sh
#   ./gcp-setup.sh
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Configuration — edit these before running ─────────────────────────────────
PROJECT_ID="project-23ff0507-3fba-4c58-b06"
REGION="us-central1"
REGISTRY_REPO="riskmanagement"
SQL_INSTANCE="riskmanagement-db"
SQL_DB="postgres"
SQL_USER="postgres"
SQL_PASSWORD="changeme123!"          # Change this!
BACKEND_SERVICE="rm-backend"
FRONTEND_SERVICE="rm-frontend"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
section() { echo ""; echo -e "${CYAN}══ $* ══${NC}"; }

echo ""
echo "══════════════════════════════════════════════════════"
echo "  RiskManagement Pro — GCP Setup"
echo "  Project : $PROJECT_ID"
echo "  Region  : $REGION"
echo "══════════════════════════════════════════════════════"
echo ""
warn "SQL_PASSWORD is set to '$SQL_PASSWORD' — change it in this script first!"
echo ""
read -rp "Continue? (y/N) " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || exit 0

# ── 1. Enable required APIs ───────────────────────────────────────────────────
section "Enabling GCP APIs"
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  --project="$PROJECT_ID"
info "APIs enabled."

# ── 2. Artifact Registry repository ──────────────────────────────────────────
section "Creating Artifact Registry repository"
if gcloud artifacts repositories describe "$REGISTRY_REPO" \
     --location="$REGION" --project="$PROJECT_ID" >/dev/null 2>&1; then
  info "Repository '$REGISTRY_REPO' already exists — skipping."
else
  gcloud artifacts repositories create "$REGISTRY_REPO" \
    --repository-format=docker \
    --location="$REGION" \
    --project="$PROJECT_ID"
  info "Repository '$REGISTRY_REPO' created."
fi

# Configure Docker to authenticate with Artifact Registry
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet
info "Docker configured for Artifact Registry."

# ── 3. Cloud SQL (PostgreSQL 15) ──────────────────────────────────────────────
section "Creating Cloud SQL instance (this takes ~5 minutes)"
if gcloud sql instances describe "$SQL_INSTANCE" --project="$PROJECT_ID" >/dev/null 2>&1; then
  info "SQL instance '$SQL_INSTANCE' already exists — skipping."
else
  gcloud sql instances create "$SQL_INSTANCE" \
    --database-version=POSTGRES_15 \
    --tier=db-f1-micro \
    --region="$REGION" \
    --project="$PROJECT_ID" \
    --storage-size=10GB \
    --storage-type=SSD \
    --no-backup
  info "SQL instance created."
fi

# Set postgres user password
gcloud sql users set-password "$SQL_USER" \
  --instance="$SQL_INSTANCE" \
  --password="$SQL_PASSWORD" \
  --project="$PROJECT_ID"
info "DB password set."

# Create riskmanagement schema
gcloud sql databases create "$SQL_DB" \
  --instance="$SQL_INSTANCE" \
  --project="$PROJECT_ID" 2>/dev/null || info "Database '$SQL_DB' already exists."

# ── 4. Service account for Cloud Run ─────────────────────────────────────────
section "Creating service account"
SA_NAME="rm-cloudrun-sa"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

if gcloud iam service-accounts describe "$SA_EMAIL" --project="$PROJECT_ID" >/dev/null 2>&1; then
  info "Service account already exists — skipping."
else
  gcloud iam service-accounts create "$SA_NAME" \
    --display-name="RiskManagement Cloud Run SA" \
    --project="$PROJECT_ID"
  info "Service account created."
fi

# Grant Cloud SQL Client role
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudsql.client" \
  --condition=None --quiet
info "Cloud SQL Client role granted."

# Grant Cloud Run invoker role (service-to-service calls)
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/run.invoker" \
  --condition=None --quiet
info "Cloud Run Invoker role granted."

# Grant Cloud Build service account permissions
CLOUDBUILD_SA="${PROJECT_ID}@cloudbuild.gserviceaccount.com"
for role in roles/run.admin roles/iam.serviceAccountUser roles/artifactregistry.writer; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${CLOUDBUILD_SA}" \
    --role="$role" \
    --condition=None --quiet
done
info "Cloud Build service account permissions granted."

# ── 5. Summary ────────────────────────────────────────────────────────────────
INSTANCE_CONNECTION="${PROJECT_ID}:${REGION}:${SQL_INSTANCE}"
DB_URL="jdbc:postgresql:///${SQL_DB}?cloudSqlInstance=${INSTANCE_CONNECTION}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&currentSchema=riskmanagement"

section "Setup complete — add these secrets to GitHub Actions"
echo ""
echo "  Secret name             Value"
echo "  ──────────────────────  ──────────────────────────────────────────────"
echo "  GCP_PROJECT_ID          ${PROJECT_ID}"
echo "  GCP_REGION              ${REGION}"
echo "  GCP_SA_KEY              (see below)"
echo "  CLOUD_SQL_INSTANCE      ${INSTANCE_CONNECTION}"
echo "  DB_URL                  ${DB_URL}"
echo "  DB_PASSWORD             ${SQL_PASSWORD}"
echo "  SERVICE_ACCOUNT         ${SA_EMAIL}"
echo ""
echo "── Generate GCP_SA_KEY (run this, then copy the output to GitHub secret):"
echo ""
echo "  gcloud iam service-accounts keys create /tmp/sa-key.json \\"
echo "    --iam-account=${SA_EMAIL}"
echo "  cat /tmp/sa-key.json | base64 -w 0"
echo "  rm /tmp/sa-key.json"
echo ""
echo "── Cloud Build trigger command (manual deploy):"
echo ""
echo "  gcloud builds submit . \\"
echo "    --config=cloudbuild.yaml \\"
echo "    --project=${PROJECT_ID} \\"
echo "    --substitutions=_CLOUD_SQL_INSTANCE=${INSTANCE_CONNECTION},_DB_URL=${DB_URL},_DB_PASSWORD=${SQL_PASSWORD},_SERVICE_ACCOUNT=${SA_EMAIL}"
echo ""
