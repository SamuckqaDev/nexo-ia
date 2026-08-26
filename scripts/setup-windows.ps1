param(
  [switch]$SkipImageRuntime
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$RuntimeRoot = Join-Path $ProjectRoot ".nexo-runtime"
$ComfyRoot = Join-Path $RuntimeRoot "comfyui"
$ComfyEnvironment = Join-Path $RuntimeRoot "comfy-venv"
$Checkpoint = Join-Path $ComfyRoot "models\checkpoints\v1-5-pruned-emaonly-fp16.safetensors"
$CheckpointUrl = "https://huggingface.co/Comfy-Org/stable-diffusion-v1-5-archive/resolve/main/v1-5-pruned-emaonly-fp16.safetensors?download=true"

function Write-Nexo([string]$Message) {
  Write-Host "`n[Nexo IA] $Message" -ForegroundColor Cyan
}

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
  throw "winget is required to install the Windows prerequisites."
}

function Install-NexoPackage([string]$Id) {
  winget list --id $Id --exact --accept-source-agreements | Out-Null
  if ($LASTEXITCODE -ne 0) {
    winget install --id $Id --exact --accept-package-agreements --accept-source-agreements
  }
}

Write-Nexo "Installing Windows prerequisites"
Install-NexoPackage "Docker.DockerDesktop"
Install-NexoPackage "Ollama.Ollama"
Install-NexoPackage "Git.Git"
Install-NexoPackage "Python.Python.3.12"

# winget updates the machine PATH, but the current PowerShell process does not receive it.
$env:Path = @(
  $env:Path
  (Join-Path $env:ProgramFiles "Docker\Docker\resources\bin")
  (Join-Path $env:ProgramFiles "Git\cmd")
  (Join-Path $env:LOCALAPPDATA "Programs\Ollama")
  (Join-Path $env:LOCALAPPDATA "Programs\Python\Python312")
  (Join-Path $env:LOCALAPPDATA "Programs\Python\Python312\Scripts")
) -join ";"

$DockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
if (-not (docker info 2>$null)) {
  if (Test-Path $DockerDesktop) { Start-Process $DockerDesktop }
  for ($Index = 0; $Index -lt 90; $Index += 1) {
    if (docker info 2>$null) { break }
    Start-Sleep -Seconds 2
  }
}
if (-not (docker info 2>$null)) {
  throw "Docker Desktop did not become ready. Open it once, accept WSL 2 setup, and rerun."
}

New-Item -ItemType Directory -Force -Path $RuntimeRoot | Out-Null
try {
  Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 http://127.0.0.1:11434/api/tags | Out-Null
} catch {
  Write-Nexo "Starting Ollama"
  Start-Process ollama -ArgumentList "serve" -WindowStyle Hidden -RedirectStandardOutput (Join-Path $RuntimeRoot "ollama.log")
  $OllamaReady = $false
  for ($Index = 0; $Index -lt 45; $Index += 1) {
    try {
      Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 http://127.0.0.1:11434/api/tags | Out-Null
      $OllamaReady = $true
      break
    } catch {
      Start-Sleep -Seconds 2
    }
  }
  if (-not $OllamaReady) { throw "Ollama did not become ready. See .nexo-runtime/ollama.log." }
}
ollama pull $(if ($env:NEXO_CHAT_MODEL) { $env:NEXO_CHAT_MODEL } else { "qwen3:8b" })
ollama pull $(if ($env:NEXO_EMBEDDING_MODEL) { $env:NEXO_EMBEDDING_MODEL } else { "nomic-embed-text" })

if (-not $SkipImageRuntime) {
  Write-Nexo "Installing the official ComfyUI runtime"
  if (-not (Test-Path (Join-Path $ComfyRoot ".git"))) {
    git clone --depth 1 https://github.com/Comfy-Org/ComfyUI.git $ComfyRoot
  }
  if (-not (Test-Path (Join-Path $ComfyEnvironment "Scripts\python.exe"))) {
    $PythonExecutable = Join-Path $env:LOCALAPPDATA "Programs\Python\Python312\python.exe"
    if (-not (Test-Path $PythonExecutable)) {
      $PythonExecutable = (Get-Command python -ErrorAction Stop).Source
    }
    & $PythonExecutable -m venv $ComfyEnvironment
  }
  $ComfyPython = Join-Path $ComfyEnvironment "Scripts\python.exe"
  & $ComfyPython -m pip install --upgrade pip
  & $ComfyPython -m pip install -r (Join-Path $ComfyRoot "requirements.txt")
  if (-not (Test-Path $Checkpoint)) {
    Write-Nexo "Downloading the official SD 1.5 checkpoint (about 2 GB)"
    Invoke-WebRequest -UseBasicParsing -Uri $CheckpointUrl -OutFile "$Checkpoint.part"
    Move-Item "$Checkpoint.part" $Checkpoint
  }
  try {
    Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 http://127.0.0.1:8188/system_stats | Out-Null
  } catch {
    $ComfyArguments = @((Join-Path $ComfyRoot "main.py"), "--listen", "0.0.0.0", "--port", "8188")
    $ComfyProcess = @{
      FilePath = $ComfyPython
      ArgumentList = $ComfyArguments
      WorkingDirectory = $ComfyRoot
      WindowStyle = "Hidden"
      RedirectStandardOutput = (Join-Path $RuntimeRoot "comfyui.log")
      RedirectStandardError = (Join-Path $RuntimeRoot "comfyui-error.log")
    }
    Start-Process @ComfyProcess
    $ComfyReady = $false
    for ($Index = 0; $Index -lt 90; $Index += 1) {
      try {
        Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 http://127.0.0.1:8188/system_stats | Out-Null
        $ComfyReady = $true
        break
      } catch {
        Start-Sleep -Seconds 2
      }
    }
    if (-not $ComfyReady) { throw "ComfyUI did not become ready. See .nexo-runtime/comfyui-error.log." }
  }
}

& (Join-Path $PSScriptRoot "dev-up-windows.ps1")
