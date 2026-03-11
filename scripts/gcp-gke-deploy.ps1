param(
  [string]$ProjectId = "",
  [string]$Region = "",
  [string]$ClusterName = "riskmanagement-gke",
  [string]$Namespace = "riskmanagement",
  [string]$ReleaseName = "riskmanagement-cloud",
  [switch]$CreateCluster,
  [switch]$Autopilot,
  [switch]$InstallIngressNginx,
  [Parameter(Mandatory = $true)]
  [string]$Domain,
  [Parameter(Mandatory = $true)]
  [string]$BackendRepository,
  [string]$BackendTag = "latest",
  [Parameter(Mandatory = $true)]
  [string]$FrontendRepository,
  [string]$FrontendTag = "latest",
  [Parameter(Mandatory = $true)]
  [string]$DbHost,
  [int]$DbPort = 5432,
  [string]$DbName = "postgres",
  [Parameter(Mandatory = $true)]
  [string]$DbUser,
  [Parameter(Mandatory = $true)]
  [string]$DbPassword,
  [switch]$EnableTls
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

if (-not (Get-Command gcloud.cmd -ErrorAction SilentlyContinue)) {
  Fail "Google Cloud SDK (gcloud.cmd) is not installed or not in PATH."
}
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
  Fail "kubectl is not installed or not in PATH."
}
if (-not (Get-Command helm -ErrorAction SilentlyContinue)) {
  Fail "helm is not installed or not in PATH."
}

if (-not $ProjectId) {
  $ProjectId = (gcloud.cmd config get-value project 2>$null).Trim()
}
if (-not $ProjectId -or $ProjectId -eq "(unset)") {
  Fail "Project ID is required. Set -ProjectId or run: gcloud config set project <project-id>"
}

if (-not $Region) {
  $Region = (gcloud.cmd config get-value compute/region 2>$null).Trim()
}
if (-not $Region -or $Region -eq "(unset)") {
  Fail "Region is required. Set -Region or run: gcloud config set compute/region <region>"
}

Info "Using project '$ProjectId' and region '$Region'"
gcloud.cmd config set project $ProjectId | Out-Null

if ($CreateCluster) {
  Info "Checking if cluster '$ClusterName' exists"
  $exists = gcloud.cmd container clusters list --region $Region --format="value(name)" | Select-String -SimpleMatch $ClusterName

  if (-not $exists) {
    if ($Autopilot) {
      Info "Creating GKE Autopilot cluster '$ClusterName' in region '$Region'"
      gcloud.cmd container clusters create-auto $ClusterName --region $Region
    }
    else {
      Info "Creating GKE Standard cluster '$ClusterName' in region '$Region'"
      gcloud.cmd container clusters create $ClusterName --region $Region --num-nodes 2
    }
  }
  else {
    Info "Cluster '$ClusterName' already exists"
  }
}

Info "Fetching kubeconfig credentials for cluster '$ClusterName'"
gcloud.cmd container clusters get-credentials $ClusterName --region $Region --project $ProjectId

if ($InstallIngressNginx) {
  Info "Installing or upgrading ingress-nginx controller"
  helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx | Out-Null
  helm repo update | Out-Null
  helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace
}

Info "Deploying app with managed DB flow"
$deployArgs = @{
  Domain = $Domain
  BackendRepository = $BackendRepository
  BackendTag = $BackendTag
  FrontendRepository = $FrontendRepository
  FrontendTag = $FrontendTag
  DbHost = $DbHost
  DbPort = $DbPort
  DbName = $DbName
  DbUser = $DbUser
  DbPassword = $DbPassword
  Namespace = $Namespace
  ReleaseName = $ReleaseName
}

if ($EnableTls) {
  $deployArgs.EnableTls = $true
}

& .\scripts\helm-deploy-cloud-managed.ps1 @deployArgs

Info "Deployment complete"
Info "Check resources: kubectl -n $Namespace get pods,svc,ingress"
