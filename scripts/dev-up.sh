#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILES=(-f "$PROJECT_ROOT/compose.yaml" -f "$PROJECT_ROOT/compose.dev.yaml")

log() {
  printf '\n[Nexo IA] %s\n' "$1"
}

fail() {
  printf '\n[Nexo IA] Error: %s\n' "$1" >&2
  exit 1
}

resolve_compose() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
    CONTAINER_ENGINE=(docker)
  elif command -v podman >/dev/null 2>&1 && podman compose version >/dev/null 2>&1; then
    COMPOSE=(podman compose)
    CONTAINER_ENGINE=(podman)
  elif command -v podman-compose >/dev/null 2>&1; then
    COMPOSE=(podman-compose)
    CONTAINER_ENGINE=(podman)
  elif command -v flatpak-spawn >/dev/null 2>&1 \
      && flatpak-spawn --host podman compose version >/dev/null 2>&1; then
    COMPOSE=(flatpak-spawn --host podman compose)
    CONTAINER_ENGINE=(flatpak-spawn --host podman)
  else
    fail "Docker Compose or Podman Compose is required."
  fi
}

remove_legacy_containers() {
  local legacy_names=(
    nexo-ia-postgres-dev
    nexo-ia-backend-dev
    nexo-ia-frontend-dev
    nexo-ia-mailpit-dev
  )
  local container_name

  for container_name in "${legacy_names[@]}"; do
    if "${CONTAINER_ENGINE[@]}" container exists "$container_name"; then
      log "Replacing legacy Nexo container: $container_name"
      "${CONTAINER_ENGINE[@]}" rm --force "$container_name"
    fi
  done
}

prepare_container_runtime() {
  if [[ "${COMPOSE[*]}" != *podman* ]]; then
    return
  fi

  log "Ensuring the rootless Podman socket is running"
  if [[ "${COMPOSE[0]}" == "flatpak-spawn" ]]; then
    flatpak-spawn --host systemctl --user start podman.socket \
      || fail "The rootless Podman socket could not be started."
  else
    systemctl --user start podman.socket \
      || fail "The rootless Podman socket could not be started."
  fi
}

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 "$1" | tr -d '\n'
  else
    head -c "$1" /dev/urandom | base64 | tr -d '\n'
  fi
}

create_environment() {
  local environment_file="$PROJECT_ROOT/.env"

  if [[ -f "$environment_file" ]]; then
    return
  fi

  log "Creating a private development .env file"
  umask 077
  {
    printf 'NEXO_DATABASE_NAME=nexo\n'
    printf 'NEXO_DATABASE_USER=nexo\n'
    printf 'NEXO_DATABASE_PASSWORD=%s\n' "$(random_secret 32)"
    printf 'NEXO_JWT_SECRET=%s\n' "$(random_secret 48)"
    printf 'NEXO_CONTAINER_OLLAMA_BASE_URL=http://host.containers.internal:11434\n'
    printf 'NEXO_CONTAINER_COMFYUI_BASE_URL=http://host.containers.internal:8188\n'
    printf 'NEXO_SERVER_PORT=8080\n'
    printf 'NEXO_BIND_ADDRESS=127.0.0.1\n'
    printf 'NEXO_FRONTEND_DEV_PORT=5173\n'
    printf 'NEXO_SECURE_COOKIE=false\n'
    printf 'NEXO_CONTAINER_SMTP_HOST=mailpit\n'
    printf 'NEXO_CONTAINER_SMTP_PORT=1025\n'
  } > "$environment_file"
}

compose() {
  "${COMPOSE[@]}" "${COMPOSE_FILES[@]}" --env-file "$PROJECT_ROOT/.env" "$@"
}

wait_for_backend() {
  local port
  local curl_command
  port="$(awk -F= '$1 == "NEXO_SERVER_PORT" { print $2 }' "$PROJECT_ROOT/.env" | tail -1)"
  port="${port:-8080}"

  if [[ "${COMPOSE[0]}" == "flatpak-spawn" ]]; then
    curl_command=(flatpak-spawn --host curl)
  elif command -v curl >/dev/null 2>&1; then
    curl_command=(curl)
  else
    log "Backend started; curl is unavailable, so the health probe was skipped"
    return
  fi

  log "Waiting for the backend health endpoint"
  for _ in $(seq 1 60); do
    if "${curl_command[@]}" --fail --silent --max-time 5 \
        "http://127.0.0.1:${port}/api/v1/system" >/dev/null; then
      log "Backend is healthy"
      return
    fi
    sleep 2
  done

  compose logs --tail=120 backend
  fail "Backend did not become healthy within 120 seconds."
}

main() {
  local backend_port
  local bind_address
  local frontend_port

  cd "$PROJECT_ROOT"
  resolve_compose
  prepare_container_runtime
  create_environment

  log "Removing previous Nexo containers while preserving named volumes"
  compose down --remove-orphans
  remove_legacy_containers

  log "Building the backend image and starting the development stack"
  compose up --detach --build --force-recreate postgres mailpit backend frontend-dev

  wait_for_backend

  backend_port="$(awk -F= '$1 == "NEXO_SERVER_PORT" { print $2 }' "$PROJECT_ROOT/.env" | tail -1)"
  bind_address="$(awk -F= '$1 == "NEXO_BIND_ADDRESS" { print $2 }' "$PROJECT_ROOT/.env" | tail -1)"
  frontend_port="$(awk -F= '$1 == "NEXO_FRONTEND_DEV_PORT" { print $2 }' "$PROJECT_ROOT/.env" | tail -1)"

  log "Nexo IA is ready"
  if [[ "${bind_address:-127.0.0.1}" == "0.0.0.0" ]]; then
    printf 'Frontend: http://<server-ip>:%s (all interfaces)\n' "${frontend_port:-5173}"
    printf 'Backend:  http://<server-ip>:%s (all interfaces)\n' "${backend_port:-8080}"
  else
    printf 'Frontend: http://%s:%s\n' "${bind_address:-127.0.0.1}" "${frontend_port:-5173}"
    printf 'Backend:  http://%s:%s\n' "${bind_address:-127.0.0.1}" "${backend_port:-8080}"
  fi
  printf 'Mailpit:  http://127.0.0.1:8025\n'
  printf 'Logs:     %s\n' "${COMPOSE[*]} ${COMPOSE_FILES[*]} logs -f"
}

main "$@"
