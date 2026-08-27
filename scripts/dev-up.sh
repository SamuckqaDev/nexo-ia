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

container_exists() {
  local container_name="$1"

  if [[ "${CONTAINER_ENGINE[*]}" == *podman* ]]; then
    "${CONTAINER_ENGINE[@]}" container exists "$container_name"
  else
    "${CONTAINER_ENGINE[@]}" container inspect "$container_name" >/dev/null 2>&1
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
    if container_exists "$container_name"; then
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
    printf 'NEXO_POSTGRES_DEV_PORT=5432\n'
    printf 'NEXO_MAILPIT_SMTP_PORT=1025\n'
    printf 'NEXO_MAILPIT_HTTP_PORT=8025\n'
    printf 'NEXO_SECURE_COOKIE=false\n'
    printf 'NEXO_CONTAINER_SMTP_HOST=mailpit\n'
    printf 'NEXO_CONTAINER_SMTP_PORT=1025\n'
  } > "$environment_file"
}

compose() {
  "${COMPOSE[@]}" "${COMPOSE_FILES[@]}" --env-file "$PROJECT_ROOT/.env" "$@"
}

read_environment_value() {
  local name="$1"
  local default_value="$2"
  local value

  value="$(awk -F= -v key="$name" '$1 == key { value = substr($0, index($0, "=") + 1) } END { print value }' "$PROJECT_ROOT/.env")"
  printf '%s' "${value:-$default_value}"
}

set_environment_value() {
  local name="$1"
  local value="$2"
  local temporary_file

  temporary_file="$(mktemp "$PROJECT_ROOT/.env.XXXXXX")"
  awk -v key="$name" -v replacement="$value" '
    BEGIN { updated = 0 }
    index($0, key "=") == 1 { print key "=" replacement; updated = 1; next }
    { print }
    END { if (!updated) print key "=" replacement }
  ' "$PROJECT_ROOT/.env" > "$temporary_file"
  chmod 600 "$temporary_file"
  mv "$temporary_file" "$PROJECT_ROOT/.env"
}

host_port_is_in_use() {
  local port="$1"

  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$port" >/dev/null 2>&1
    return
  fi

  (exec 3<>"/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1
}

service_owns_host_port() {
  local service="$1"
  local container_port="$2"
  local host_port="$3"
  local container_id
  local published_ports

  container_id="$(compose ps --quiet "$service" 2>/dev/null | head -n 1)"
  [[ -n "$container_id" ]] || return 1
  published_ports="$("${CONTAINER_ENGINE[@]}" port "$container_id" "$container_port/tcp" 2>/dev/null || true)"
  [[ "$published_ports" =~ :$host_port$ ]]
}

find_available_port() {
  local preferred_port="$1"
  local candidate

  for ((candidate = preferred_port; candidate < preferred_port + 50; candidate += 1)); do
    if ! host_port_is_in_use "$candidate"; then
      printf '%s' "$candidate"
      return
    fi
  done

  return 1
}

ensure_available_host_port() {
  local environment_name="$1"
  local default_port="$2"
  local service="$3"
  local container_port="$4"
  local configured_port
  local available_port

  configured_port="$(read_environment_value "$environment_name" "$default_port")"
  [[ "$configured_port" =~ ^[0-9]+$ ]] || fail "$environment_name must be a numeric TCP port."

  if service_owns_host_port "$service" "$container_port" "$configured_port"; then
    return
  fi
  if ! host_port_is_in_use "$configured_port"; then
    return
  fi

  available_port="$(find_available_port "$((configured_port + 1))")" \
    || fail "No free port was found for $environment_name near $configured_port."
  log "Port $configured_port is already in use outside Nexo; using $available_port for $environment_name"
  set_environment_value "$environment_name" "$available_port"
}

ensure_development_ports() {
  ensure_available_host_port NEXO_SERVER_PORT 8080 backend 8080
  ensure_available_host_port NEXO_FRONTEND_DEV_PORT 5173 frontend-dev 5173
  ensure_available_host_port NEXO_POSTGRES_DEV_PORT 5432 postgres 5432
  ensure_available_host_port NEXO_MAILPIT_SMTP_PORT 1025 mailpit 1025
  ensure_available_host_port NEXO_MAILPIT_HTTP_PORT 8025 mailpit 8025
}

service_state() {
  local service="$1"
  local container_id

  container_id="$(compose ps --quiet "$service" 2>/dev/null | head -n 1)"
  if [[ -z "$container_id" ]]; then
    printf 'missing'
    return
  fi

  "${CONTAINER_ENGINE[@]}" inspect \
    --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
    "$container_id" 2>/dev/null || printf 'unknown'
}

wait_for_service_health() {
  local service="$1"
  local attempts="$2"
  local state

  for ((attempt = 1; attempt <= attempts; attempt += 1)); do
    state="$(service_state "$service")"
    if [[ "$state" == "healthy" ]]; then
      return 0
    fi
    sleep 1
  done

  return 1
}

show_startup_diagnostics() {
  log "Current container state"
  compose ps --all || true
  log "Recent startup logs"
  compose logs --tail=60 backend mcp-fetch mcp-duckduckgo || true
  if [[ "$(service_state postgres)" != "healthy" ]]; then
    compose logs --tail=30 postgres || true
  fi
}

recover_optional_service() {
  local service="$1"
  local max_attempts=3
  local attempt
  local state

  if wait_for_service_health "$service" 1; then
    log "$service is healthy"
    return
  fi

  for ((attempt = 1; attempt <= max_attempts; attempt += 1)); do
    state="$(service_state "$service")"
    log "Recovering optional $service ($attempt/$max_attempts, current state: $state)"
    compose up --detach --force-recreate "$service" || true

    if wait_for_service_health "$service" 15; then
      log "$service recovered and is healthy"
      return
    fi

    compose logs --tail=40 "$service" || true
    sleep $((attempt * 2))
  done

  state="$(service_state "$service")"
  log "Warning: $service remains $state after recovery attempts"
  log "Nexo will continue without this optional MCP server; retry later with ./scripts/dev-up.sh"
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
      return 0
    fi
    if [[ "$(service_state backend)" =~ ^(dead|exited)$ ]]; then
      compose logs --tail=120 backend
      return 1
    fi
    sleep 2
  done

  compose logs --tail=120 backend
  return 1
}

start_core_stack() {
  local max_attempts=3
  local attempt

  for ((attempt = 1; attempt <= max_attempts; attempt += 1)); do
    log "Starting the core development stack ($attempt/$max_attempts)"
    if compose up --detach --build postgres mailpit backend frontend-dev; then
      if wait_for_backend; then
        return
      fi
    fi

    show_startup_diagnostics
    if ((attempt < max_attempts)); then
      log "Core stack is not ready; retrying without deleting named volumes"
      if [[ "$(service_state postgres)" != "healthy" ]]; then
        compose up --detach --force-recreate postgres || true
        wait_for_service_health postgres 30 || true
      fi
      compose up --detach --build --force-recreate --no-deps backend frontend-dev || true
      sleep $((attempt * 3))
    fi
  done

  fail "The core Nexo stack could not recover after $max_attempts attempts. Review the diagnostics above."
}

main() {
  local backend_port
  local bind_address
  local frontend_port

  cd "$PROJECT_ROOT"
  resolve_compose
  prepare_container_runtime
  create_environment
  ensure_development_ports

  log "Checking existing Nexo services while preserving named volumes"
  remove_legacy_containers

  start_core_stack
  recover_optional_service mcp-fetch
  recover_optional_service mcp-duckduckgo

  backend_port="$(read_environment_value NEXO_SERVER_PORT 8080)"
  bind_address="$(read_environment_value NEXO_BIND_ADDRESS 127.0.0.1)"
  frontend_port="$(read_environment_value NEXO_FRONTEND_DEV_PORT 5173)"

  log "Nexo IA is ready"
  if [[ "${bind_address:-127.0.0.1}" == "0.0.0.0" ]]; then
    printf 'Frontend: http://<server-ip>:%s (all interfaces)\n' "${frontend_port:-5173}"
    printf 'Backend:  http://<server-ip>:%s (all interfaces)\n' "${backend_port:-8080}"
  else
    printf 'Frontend: http://%s:%s\n' "${bind_address:-127.0.0.1}" "${frontend_port:-5173}"
    printf 'Backend:  http://%s:%s\n' "${bind_address:-127.0.0.1}" "${backend_port:-8080}"
  fi
  printf 'Mailpit:  http://127.0.0.1:%s\n' "$(read_environment_value NEXO_MAILPIT_HTTP_PORT 8025)"
  printf 'Logs:     %s\n' "${COMPOSE[*]} ${COMPOSE_FILES[*]} logs -f"
}

main "$@"
