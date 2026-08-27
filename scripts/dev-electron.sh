#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESKTOP_ROOT="$PROJECT_ROOT/desktop"
ENVIRONMENT_FILE="$PROJECT_ROOT/.env"

renderer_url="${NEXO_RENDERER_URL:-}"
skip_stack=false
force_install=false
dry_run=false

log() {
  printf '\n[Nexo Desktop] %s\n' "$1"
}

fail() {
  printf '\n[Nexo Desktop] Error: %s\n' "$1" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: ./scripts/dev-electron.sh [options]

Starts the Nexo development stack and opens the Electron desktop application.

Options:
  --renderer-url URL  Load an existing Nexo frontend, for example http://192.168.1.20:5173.
                      Supplying this option skips the local Compose startup.
  --skip-stack        Do not start Compose; use the local frontend port from .env (default 5173).
  --install           Run npm ci in desktop/ even when node_modules already exists.
  --dry-run           Validate configuration and print what would run without starting anything.
  -h, --help          Show this help.

Examples:
  ./scripts/dev-electron.sh
  ./scripts/dev-electron.sh --skip-stack
  ./scripts/dev-electron.sh --renderer-url http://192.168.1.20:5173
EOF
}

read_environment_value() {
  local name="$1"
  local default_value="$2"
  local value=""

  if [[ -f "$ENVIRONMENT_FILE" ]]; then
    value="$(awk -F= -v key="$name" '$1 == key { value = substr($0, index($0, "=") + 1) } END { print value }' "$ENVIRONMENT_FILE")"
  fi

  printf '%s' "${value:-$default_value}"
}

validate_renderer_url() {
  if [[ ! "$renderer_url" =~ ^https?://[^[:space:]]+$ ]]; then
    fail "Renderer URL must be an HTTP or HTTPS address: $renderer_url"
  fi
  renderer_url="${renderer_url%/}"
}

validate_node() {
  command -v node >/dev/null 2>&1 || fail "Node.js 24 is required. Run the platform setup script first."
  command -v npm >/dev/null 2>&1 || fail "npm is required. Run the platform setup script first."

  local node_major
  node_major="$(node -p 'process.versions.node.split(".")[0]')"
  if [[ "$node_major" != "24" ]]; then
    fail "Node.js 24 is required by the desktop package; found $(node --version)."
  fi
}

wait_for_renderer() {
  if ! command -v curl >/dev/null 2>&1; then
    log "curl is unavailable; renderer readiness probe skipped"
    return
  fi

  log "Waiting for the renderer at $renderer_url"
  for _ in $(seq 1 60); do
    if curl --fail --silent --max-time 3 "$renderer_url" >/dev/null; then
      log "Renderer is ready"
      return
    fi
    sleep 1
  done

  fail "Renderer did not become reachable within 60 seconds: $renderer_url"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --renderer-url)
      [[ $# -ge 2 ]] || fail "--renderer-url requires a URL."
      renderer_url="$2"
      skip_stack=true
      shift 2
      ;;
    --skip-stack)
      skip_stack=true
      shift
      ;;
    --install)
      force_install=true
      shift
      ;;
    --dry-run)
      dry_run=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1. Use --help for usage."
      ;;
  esac
done

validate_node

if [[ -z "$renderer_url" ]]; then
  frontend_port="$(read_environment_value NEXO_FRONTEND_DEV_PORT 5173)"
  renderer_url="http://127.0.0.1:$frontend_port"
fi
validate_renderer_url

if [[ "$dry_run" == true ]]; then
  log "Configuration is valid"
  printf 'Project:  %s\n' "$PROJECT_ROOT"
  printf 'Renderer: %s\n' "$renderer_url"
  printf 'Stack:    %s\n' "$([[ "$skip_stack" == true ]] && printf 'existing' || printf 'start locally')"
  printf 'Install:  %s\n' "$([[ "$force_install" == true ]] && printf 'npm ci forced' || printf 'when missing')"
  exit 0
fi

if [[ "$skip_stack" == false ]]; then
  log "Starting the local Nexo stack"
  "$PROJECT_ROOT/scripts/dev-up.sh"
fi

wait_for_renderer

if [[ "$force_install" == true || ! -x "$DESKTOP_ROOT/node_modules/.bin/electron" ]]; then
  log "Installing deterministic Electron dependencies"
  (cd "$DESKTOP_ROOT" && npm ci)
fi

log "Opening Nexo IA in Electron"
printf 'Renderer: %s\n' "$renderer_url"
printf 'Close the Electron window or press Ctrl+C to stop the desktop process.\n'
printf 'The development containers remain running so browser and desktop sessions keep working.\n'

cd "$DESKTOP_ROOT"
NEXO_RENDERER_URL="$renderer_url" npm run dev
