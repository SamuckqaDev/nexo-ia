#!/usr/bin/env bash

set -Eeuo pipefail

NEXO_PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
NEXO_RUNTIME_ROOT="$NEXO_PROJECT_ROOT/.nexo-runtime"
NEXO_COMFY_ROOT="$NEXO_RUNTIME_ROOT/comfyui"
NEXO_COMFY_ENV="$NEXO_RUNTIME_ROOT/comfy-venv"
NEXO_COMFY_MODEL_URL="https://huggingface.co/Comfy-Org/stable-diffusion-v1-5-archive/resolve/main/v1-5-pruned-emaonly-fp16.safetensors?download=true"
NEXO_COMFY_MODEL_NAME="v1-5-pruned-emaonly-fp16.safetensors"

nexo_log() {
  printf '\n[Nexo IA] %s\n' "$1"
}

nexo_fail() {
  printf '\n[Nexo IA] Error: %s\n' "$1" >&2
  exit 1
}

nexo_wait_http() {
  local url="$1"
  local attempts="${2:-60}"
  local index
  for ((index = 0; index < attempts; index += 1)); do
    if curl --fail --silent --max-time 3 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

nexo_prepare_ollama() {
  mkdir -p "$NEXO_RUNTIME_ROOT"
  if ! curl --fail --silent --max-time 3 http://127.0.0.1:11434/api/tags >/dev/null 2>&1; then
    nexo_log "Starting Ollama"
    nohup ollama serve >"$NEXO_RUNTIME_ROOT/ollama.log" 2>&1 &
    nexo_wait_http http://127.0.0.1:11434/api/tags 45 \
      || nexo_fail "Ollama did not become ready. See .nexo-runtime/ollama.log."
  fi
  nexo_log "Installing the default chat and embedding models"
  ollama pull "${NEXO_CHAT_MODEL:-qwen3:8b}"
  ollama pull "${NEXO_EMBEDDING_MODEL:-nomic-embed-text}"
}

nexo_prepare_comfyui() {
  if [[ "${NEXO_SKIP_IMAGE_RUNTIME:-0}" == "1" ]]; then
    nexo_log "Skipping ComfyUI because NEXO_SKIP_IMAGE_RUNTIME=1"
    return
  fi
  mkdir -p "$NEXO_RUNTIME_ROOT"
  if [[ ! -d "$NEXO_COMFY_ROOT/.git" ]]; then
    nexo_log "Installing the official ComfyUI runtime"
    git clone --depth 1 https://github.com/Comfy-Org/ComfyUI.git "$NEXO_COMFY_ROOT"
  fi
  if [[ ! -x "$NEXO_COMFY_ENV/bin/python" ]]; then
    python3 -m venv "$NEXO_COMFY_ENV"
  fi
  "$NEXO_COMFY_ENV/bin/python" -m pip install --upgrade pip
  "$NEXO_COMFY_ENV/bin/python" -m pip install -r "$NEXO_COMFY_ROOT/requirements.txt"

  local checkpoint="$NEXO_COMFY_ROOT/models/checkpoints/$NEXO_COMFY_MODEL_NAME"
  if [[ ! -f "$checkpoint" ]]; then
    nexo_log "Downloading the official SD 1.5 checkpoint (about 2 GB)"
    curl --fail --location --retry 3 --output "$checkpoint.part" "$NEXO_COMFY_MODEL_URL"
    mv "$checkpoint.part" "$checkpoint"
  fi

  if curl --fail --silent --max-time 3 http://127.0.0.1:8188/system_stats >/dev/null 2>&1; then
    return
  fi
  nexo_log "Starting ComfyUI for the backend container"
  nohup "$NEXO_COMFY_ENV/bin/python" "$NEXO_COMFY_ROOT/main.py" \
    --listen 0.0.0.0 --port 8188 \
    >"$NEXO_RUNTIME_ROOT/comfyui.log" 2>&1 &
  printf '%s\n' "$!" >"$NEXO_RUNTIME_ROOT/comfyui.pid"
  nexo_wait_http http://127.0.0.1:8188/system_stats 90 \
    || nexo_fail "ComfyUI did not become ready. See .nexo-runtime/comfyui.log."
}

nexo_start_stack() {
  "$NEXO_PROJECT_ROOT/scripts/dev-up.sh"
}
