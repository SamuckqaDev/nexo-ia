#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESKTOP_ROOT="$PROJECT_ROOT/desktop"
runtime_config="$DESKTOP_ROOT/dist/nexo-runtime-config.json"
server_url="${NEXO_REMOTE_SERVER_URL:-}"

fail() {
  printf '\n[Nexo Desktop] Error: %s\n' "$1" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: ./scripts/build-electron-remote.sh --server-url URL [--arch arm64|x64]

Builds the macOS Electron shell. The Linux Nexo server keeps the frontend, backend,
database, Ollama, and MCP processing; this package only opens the remote renderer and
keeps the optional local Workspace Companion capability.

Examples:
  ./scripts/build-electron-remote.sh --server-url http://100.118.193.77:5173
  NEXO_REMOTE_SERVER_URL=http://100.118.193.77:5173 ./scripts/build-electron-remote.sh
EOF
}

architecture=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --server-url)
      [[ $# -ge 2 ]] || fail "--server-url requires a URL."
      server_url="$2"
      shift 2
      ;;
    --arch)
      [[ $# -ge 2 ]] || fail "--arch requires arm64 or x64."
      architecture="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

[[ "$(uname -s)" == "Darwin" ]] || fail "Run this macOS package builder on macOS; Electron cannot produce a signed macOS app from this Linux host."
[[ -n "$server_url" ]] || fail "Provide --server-url or NEXO_REMOTE_SERVER_URL."
[[ "$server_url" =~ ^https?://[^[:space:]]+$ ]] || fail "Server URL must use HTTP or HTTPS."
command -v node >/dev/null 2>&1 || fail "Node.js 24 is required."
command -v npm >/dev/null 2>&1 || fail "npm is required."

node_major="$(node -p 'process.versions.node.split(".")[0]')"
[[ "$node_major" == "24" ]] || fail "Node.js 24 is required; found $(node --version)."

if [[ ! -x "$DESKTOP_ROOT/node_modules/.bin/electron-builder" ]]; then
  (cd "$DESKTOP_ROOT" && npm ci)
fi

printf '[Nexo Desktop] Building remote macOS shell for %s\n' "$server_url"
(cd "$DESKTOP_ROOT" && npm run build)
printf '{"rendererUrl":"%s"}\n' "$server_url" > "$runtime_config"
trap 'rm -f "$runtime_config"' EXIT

build_args=(--mac)
if [[ -n "$architecture" ]]; then
  [[ "$architecture" == "arm64" || "$architecture" == "x64" ]] || fail "Architecture must be arm64 or x64."
  build_args+=(--arm64)
  [[ "$architecture" == "x64" ]] && build_args=(--mac --x64)
fi

(cd "$DESKTOP_ROOT" && npx electron-builder "${build_args[@]}")
printf '\n[Nexo Desktop] macOS artifacts are in %s/release\n' "$DESKTOP_ROOT"
