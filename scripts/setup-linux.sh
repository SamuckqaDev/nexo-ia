#!/usr/bin/env bash

set -Eeuo pipefail

NEXO_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$NEXO_SCRIPT_DIR/lib/bootstrap-runtime.sh"

install_packages() {
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose-v2 git curl python3 python3-venv
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y moby-engine docker-compose-plugin git curl python3
  elif command -v pacman >/dev/null 2>&1; then
    sudo pacman -Syu --needed --noconfirm docker docker-compose git curl python
  else
    nexo_fail "Supported package manager not found (apt, dnf, or pacman)."
  fi
}

nexo_log "Installing Linux development prerequisites"
install_packages

if command -v docker >/dev/null 2>&1; then
  sudo systemctl enable --now docker >/dev/null 2>&1 || true
  if ! docker info >/dev/null 2>&1; then
    sudo usermod -aG docker "${USER:?Current user is unavailable}"
    nexo_fail "Docker was installed and your user was added to its group. Sign out/in, then rerun this script."
  fi
fi

if ! command -v ollama >/dev/null 2>&1; then
  nexo_log "Installing Ollama from its official installer"
  NEXO_OLLAMA_INSTALLER="$(mktemp)"
  curl --fail --location --output "$NEXO_OLLAMA_INSTALLER" https://ollama.com/install.sh
  sh "$NEXO_OLLAMA_INSTALLER"
  rm -f "$NEXO_OLLAMA_INSTALLER"
fi

command -v git >/dev/null 2>&1 || nexo_fail "Git is required."
command -v curl >/dev/null 2>&1 || nexo_fail "curl is required."
command -v python3 >/dev/null 2>&1 || nexo_fail "Python 3 is required for ComfyUI."

nexo_prepare_ollama
nexo_prepare_comfyui
nexo_start_stack
