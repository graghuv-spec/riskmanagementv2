#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# azure-setup.sh  –  One-time Azure infrastructure setup for RiskManagement Pro
#
# Run this ONCE from your local machine before the first CI/CD deploy.
# Prerequisites: Azure CLI installed and authenticated (az login)
#
# What this script provisions:
#   - Resource Group
#   - Azure Container Registry (ACR) with admin-disabled (uses managed identity)
#   - Azure Database for PostgreSQL Flexible Server (v15)
#   - User-assigned Managed Identity with AcrPull role (for Container Apps)
#   - Azure Container Apps Environment
#   - Backend and Frontend Container Apps (placeholder image; CI updates them)
#   - Service Principal for GitHub Actions (Contributor + AcrPush)
#
# Usage:
#   chmod +x azure-setup.sh
#   ./azure-setup.sh
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Configuration — edit these before running ─────────────────────────────────
RESOURCE_GROUP="riskmanagement-rg"
LOCATION="eastus"
ACR_NAME="riskmanagementacr"          # globally unique; lowercase alphanumeric only
POSTGRES_SERVER="riskmanagement-pg"   # globally unique
POSTGRES_DB="postgres"
POSTGRES_USER="pgadmin"
POSTGRES_PASSWORD="changeme123!"      # Change this!
BACKEND_APP="rm-backend"
FRONTEND_APP="rm-frontend"
CONTAINER_ENV="riskmanagement-env"
IDENTITY_NAME="rm-containerapp-identity"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
section() { echo ""; echo -e "${CYAN}══ $* ══${NC}"; }

if ! command -v az &>/dev/null; then
  echo -e "${RED}[ERROR]${NC} Azure CLI not found. Install from: https://aka.ms/installazurecli"
  exit 1
fi

if ! az account show &>/dev/null; then
  echo -e "${RED}[ERROR]${NC} Not logged in to Azure. Run: az login"
  exit 1
fi

SUBSCRIPTION_ID=$(az account show --query id -o tsv)

echo ""
echo "══════════════════════════════════════════════════════"
echo "  RiskManagement Pro — Azure Setup"
echo "  Subscription : $SUBSCRIPTION_ID"
echo "  Location     : $LOCATION"
echo "══════════════════════════════════════════════════════"
echo ""
warn "POSTGRES_PASSWORD is set to '$POSTGRES_PASSWORD' — change it in this script first!"
echo ""
read -rp "Continue? (y/N) " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || exit 0

# ── 1. Resource Group ─────────────────────────────────────────────────────────
section "Creating resource group"
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none
info "Resource group '$RESOURCE_GROUP' ready."

# ── 2. Azure Container Registry ───────────────────────────────────────────────
section "Creating Azure Container Registry"
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --admin-enabled false \
  --output none
ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)
ACR_ID=$(az acr show --name "$ACR_NAME" --query id -o tsv)
info "ACR created. Login server: $ACR_LOGIN_SERVER"

# ── 3. Azure Database for PostgreSQL Flexible Server ──────────────────────────
section "Creating Azure Database for PostgreSQL Flexible Server (this takes ~5 minutes)"
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER" \
  --location "$LOCATION" \
  --admin-user "$POSTGRES_USER" \
  --admin-password "$POSTGRES_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 15 \
  --public-access 0.0.0.0 \
  --output none
info "PostgreSQL Flexible Server '$POSTGRES_SERVER' created."

az postgres flexible-server db create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$POSTGRES_SERVER" \
  --database-name "$POSTGRES_DB" \
  --output none 2>/dev/null || info "Database '$POSTGRES_DB' already exists."

# Allow connections from Container Apps (all Azure services via 0.0.0.0 rule)
az postgres flexible-server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER" \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0 \
  --output none
info "Firewall rule 'AllowAzureServices' created."

# ── 4. User-assigned Managed Identity (DefaultAzureCredential target) ─────────
#
# Container Apps authenticate to ACR using this managed identity, which
# implements DefaultAzureCredential in the Azure SDK — no stored credentials.
section "Creating user-assigned managed identity"
az identity create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$IDENTITY_NAME" \
  --output none
IDENTITY_ID=$(az identity show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$IDENTITY_NAME" \
  --query id -o tsv)
IDENTITY_CLIENT_ID=$(az identity show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$IDENTITY_NAME" \
  --query clientId -o tsv)
info "Managed identity created (client ID: $IDENTITY_CLIENT_ID)."

# Grant AcrPull — Container Apps pull images using DefaultAzureCredential
az role assignment create \
  --assignee "$IDENTITY_CLIENT_ID" \
  --role AcrPull \
  --scope "$ACR_ID" \
  --output none
info "AcrPull role assigned to managed identity on ACR."

# ── 5. Container Apps Environment ─────────────────────────────────────────────
section "Creating Container Apps environment"
az containerapp env create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$CONTAINER_ENV" \
  --location "$LOCATION" \
  --output none
info "Container Apps environment '$CONTAINER_ENV' created."

# ── 6. Backend Container App ──────────────────────────────────────────────────
#
# Placeholder image is used on first create; CI/CD will update it on deploy.
section "Creating backend Container App"
POSTGRES_HOST="${POSTGRES_SERVER}.postgres.database.azure.com"
DB_URL="jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB}?sslmode=require&currentSchema=riskmanagement"

az containerapp create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$BACKEND_APP" \
  --environment "$CONTAINER_ENV" \
  --image "mcr.microsoft.com/azuredocs/containerapps-helloworld:latest" \
  --user-assigned "$IDENTITY_ID" \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-identity "$IDENTITY_ID" \
  --ingress external \
  --target-port 8080 \
  --cpu 1.0 \
  --memory 2Gi \
  --min-replicas 1 \
  --max-replicas 3 \
  --env-vars \
    "DB_URL=${DB_URL}" \
    "DB_USERNAME=${POSTGRES_USER}" \
    "DB_PASSWORD=secretref:db-password" \
  --output none
info "Backend Container App '$BACKEND_APP' created."

BACKEND_FQDN=$(az containerapp show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$BACKEND_APP" \
  --query "properties.configuration.ingress.fqdn" -o tsv)

# ── 7. Frontend Container App ─────────────────────────────────────────────────
section "Creating frontend Container App"
az containerapp create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$FRONTEND_APP" \
  --environment "$CONTAINER_ENV" \
  --image "mcr.microsoft.com/azuredocs/containerapps-helloworld:latest" \
  --user-assigned "$IDENTITY_ID" \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-identity "$IDENTITY_ID" \
  --ingress external \
  --target-port 80 \
  --cpu 0.5 \
  --memory 1Gi \
  --min-replicas 0 \
  --max-replicas 3 \
  --env-vars "BACKEND_URL=https://${BACKEND_FQDN}" \
  --output none
info "Frontend Container App '$FRONTEND_APP' created."

FRONTEND_FQDN=$(az containerapp show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$FRONTEND_APP" \
  --query "properties.configuration.ingress.fqdn" -o tsv)

# ── 8. Service principal for GitHub Actions ───────────────────────────────────
section "Creating service principal for GitHub Actions CI/CD"
SP_NAME="riskmanagement-github-sp"
SP_JSON=$(az ad sp create-for-rbac \
  --name "$SP_NAME" \
  --role Contributor \
  --scopes "/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}" \
  --sdk-auth 2>/dev/null)

SP_CLIENT_ID=$(echo "$SP_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['clientId'])")

# Grant AcrPush so CI can push images
az role assignment create \
  --assignee "$SP_CLIENT_ID" \
  --role AcrPush \
  --scope "$ACR_ID" \
  --output none
info "AcrPush role assigned to service principal."

# ── 9. Summary ────────────────────────────────────────────────────────────────
section "Setup complete — add these secrets to GitHub Actions"
echo ""
echo "  Secret name         Value"
echo "  ──────────────────  ──────────────────────────────────────────────────"
echo "  AZURE_CREDENTIALS   <see JSON below>"
echo "  AZURE_ACR_NAME      ${ACR_NAME}"
echo ""
echo "── AZURE_CREDENTIALS (copy the entire JSON block to the GitHub secret):"
echo ""
echo "$SP_JSON"
echo ""
echo "── Container App URLs (placeholder — CI will deploy real images):"
echo ""
echo "  App → https://${FRONTEND_FQDN}"
echo "  API → https://${BACKEND_FQDN}"
echo ""
echo "── Manual image push example (after az login + az acr login):"
echo ""
echo "  az acr login --name ${ACR_NAME}"
echo "  docker build -t ${ACR_LOGIN_SERVER}/backend:latest ./backend"
echo "  docker push ${ACR_LOGIN_SERVER}/backend:latest"
echo ""
