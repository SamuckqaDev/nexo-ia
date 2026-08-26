#!/usr/bin/env bash

set -Eeuo pipefail

NEXO_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$NEXO_SCRIPT_DIR/lib/bootstrap-runtime.sh"

command -v brew >/dev/null 2>&1 \
  || nexo_fail "Homebrew is required. Install it from https://brew.sh and run this script again."

nexo_log "Installing macOS prerequisites"
brew list git >/dev/null 2>&1 || brew install git
brew list python@3.12 >/dev/null 2>&1 || brew install python@3.12
brew list ollama >/dev/null 2>&1 || brew install ollama
if ! command -v docker >/dev/null 2>&1; then
  brew install --cask docker
fi

if ! docker info >/dev/null 2>&1; then
  nexo_log "Starting Docker Desktop"
  open -a Docker
  for _ in $(seq 1 90); do
    docker info >/dev/null 2>&1 && break
    sleep 2
  done
fi
docker info >/dev/null 2>&1 \
  || nexo_fail "Docker Desktop did not become ready. Open it once, accept its setup, and rerun."

nexo_prepare_ollama
nexo_prepare_comfyui
nexo_start_stack
