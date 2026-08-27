param()

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$EnvironmentFile = Join-Path $ProjectRoot ".env"
$ComposeFiles = @("-f", (Join-Path $ProjectRoot "compose.yaml"), "-f", (Join-Path $ProjectRoot "compose.dev.yaml"))

function Write-Nexo([string]$Message) {
  Write-Host "`n[Nexo IA] $Message" -ForegroundColor Cyan
}

function New-NexoSecret([int]$Length) {
  $Bytes = New-Object byte[] $Length
  $Generator = [Security.Cryptography.RandomNumberGenerator]::Create()
  try {
    $Generator.GetBytes($Bytes)
    return [Convert]::ToBase64String($Bytes)
  } finally {
    $Generator.Dispose()
  }
}

function Get-NexoEnvironmentValue([string]$Name, [string]$DefaultValue) {
  $Match = Get-Content $EnvironmentFile | Where-Object { $_ -match "^$([Regex]::Escape($Name))=" } | Select-Object -Last 1
  if (-not $Match) {
    return $DefaultValue
  }
  return ($Match -split "=", 2)[1]
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker Desktop with Compose is required. Run scripts/setup-windows.ps1 first."
}
docker compose version | Out-Null

if (-not (Test-Path $EnvironmentFile)) {
  Write-Nexo "Creating a private development .env file"
  $EnvironmentLines = @(
    "NEXO_DATABASE_NAME=nexo"
    "NEXO_DATABASE_USER=nexo"
    "NEXO_DATABASE_PASSWORD=$(New-NexoSecret 32)"
    "NEXO_JWT_SECRET=$(New-NexoSecret 48)"
    "NEXO_CONTAINER_OLLAMA_BASE_URL=http://host.containers.internal:11434"
    "NEXO_CONTAINER_COMFYUI_BASE_URL=http://host.containers.internal:8188"
    "NEXO_SERVER_PORT=8080"
    "NEXO_BIND_ADDRESS=127.0.0.1"
    "NEXO_FRONTEND_DEV_PORT=5173"
    "NEXO_SECURE_COOKIE=false"
    "NEXO_CONTAINER_SMTP_HOST=mailpit"
    "NEXO_CONTAINER_SMTP_PORT=1025"
  )
  [IO.File]::WriteAllLines(
    $EnvironmentFile,
    $EnvironmentLines,
    (New-Object Text.UTF8Encoding($false))
  )
}

Push-Location $ProjectRoot
try {
  Write-Nexo "Recreating the development stack while preserving named volumes"
  & docker compose @ComposeFiles --env-file $EnvironmentFile down --remove-orphans
  & docker compose @ComposeFiles --env-file $EnvironmentFile up --detach --build --force-recreate postgres mailpit backend frontend-dev

  Write-Nexo "Waiting for the backend health endpoint"
  $ServerPort = Get-NexoEnvironmentValue "NEXO_SERVER_PORT" "8080"
  $FrontendPort = Get-NexoEnvironmentValue "NEXO_FRONTEND_DEV_PORT" "5173"
  $BindAddress = Get-NexoEnvironmentValue "NEXO_BIND_ADDRESS" "127.0.0.1"
  $Ready = $false
  for ($Index = 0; $Index -lt 60; $Index += 1) {
    try {
      Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri "http://127.0.0.1:$ServerPort/api/v1/system" | Out-Null
      $Ready = $true
      break
    } catch {
      Start-Sleep -Seconds 2
    }
  }
  if (-not $Ready) {
    & docker compose @ComposeFiles --env-file $EnvironmentFile logs --tail=120 backend
    throw "Backend did not become healthy within 120 seconds."
  }

  Write-Nexo "Nexo IA is ready"
  if ($BindAddress -eq "0.0.0.0") {
    Write-Host "Frontend: http://<server-ip>:$FrontendPort (all interfaces)"
    Write-Host "Backend:  http://<server-ip>:$ServerPort (all interfaces)"
  } else {
    Write-Host "Frontend: http://${BindAddress}:$FrontendPort"
    Write-Host "Backend:  http://${BindAddress}:$ServerPort"
  }
  Write-Host "Mailpit:  http://127.0.0.1:8025"
} finally {
  Pop-Location
}
