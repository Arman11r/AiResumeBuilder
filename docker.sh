#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
#  ResumeAI Docker helper
#  Usage: ./docker.sh [command]
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

COMPOSE="docker compose"
ENV_FILE=".env"

# Colours
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

header() { echo -e "\n${CYAN}══ $* ══${NC}\n"; }
ok()     { echo -e "${GREEN}✔ $*${NC}"; }
warn()   { echo -e "${YELLOW}⚠ $*${NC}"; }
err()    { echo -e "${RED}✖ $*${NC}"; exit 1; }

# ── Ensure .env exists ────────────────────────────────────────────────────────
check_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    warn ".env not found — copying from .env.example"
    cp .env.example "$ENV_FILE"
    warn "Edit .env with your real secrets before running again."
  fi
}

# ── Commands ──────────────────────────────────────────────────────────────────
cmd_up() {
  header "Starting all ResumeAI services"
  check_env
  $COMPOSE up -d --build
  ok "All containers started.  Frontend → http://localhost:3000  |  Gateway → http://localhost:8080  |  Eureka → http://localhost:8761"
}

cmd_down() {
  header "Stopping all services"
  $COMPOSE down
  ok "Done."
}

cmd_restart() {
  local svc="${2:-}"
  if [[ -z "$svc" ]]; then
    header "Restarting all services"
    $COMPOSE restart
  else
    header "Restarting $svc"
    $COMPOSE restart "$svc"
  fi
  ok "Done."
}

cmd_logs() {
  local svc="${2:-}"
  if [[ -z "$svc" ]]; then
    $COMPOSE logs -f --tail=100
  else
    $COMPOSE logs -f --tail=200 "$svc"
  fi
}

cmd_build() {
  local svc="${2:-}"
  header "Building ${svc:-all services}"
  check_env
  if [[ -z "$svc" ]]; then
    $COMPOSE build --no-cache
  else
    $COMPOSE build --no-cache "$svc"
  fi
  ok "Build complete."
}

cmd_ps() {
  $COMPOSE ps
}

cmd_clean() {
  header "Removing containers, networks and volumes (DATA WILL BE LOST)"
  read -r -p "Are you sure? [y/N] " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { warn "Aborted."; exit 0; }
  $COMPOSE down -v --remove-orphans
  ok "Cleaned."
}

cmd_mysql() {
  header "Opening MySQL shell"
  source "$ENV_FILE" 2>/dev/null || true
  docker exec -it resumeai-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD:-admin123}"
}

cmd_help() {
  cat <<EOF
Usage: ./docker.sh <command> [service]

Commands:
  up              Build (if needed) and start all containers
  down            Stop and remove containers
  restart [svc]   Restart all or a single service
  logs    [svc]   Tail logs for all or a single service
  build   [svc]   Rebuild images (no-cache)
  ps              Show container status
  clean           Remove containers, networks AND volumes (destructive)
  mysql           Open MySQL interactive shell
  help            Show this message

Examples:
  ./docker.sh up
  ./docker.sh logs auth-service
  ./docker.sh restart frontend
  ./docker.sh build ai-service
EOF
}

# ── Dispatch ──────────────────────────────────────────────────────────────────
CMD="${1:-help}"
case "$CMD" in
  up)      cmd_up      "$@" ;;
  down)    cmd_down    "$@" ;;
  restart) cmd_restart "$@" ;;
  logs)    cmd_logs    "$@" ;;
  build)   cmd_build   "$@" ;;
  ps)      cmd_ps      "$@" ;;
  clean)   cmd_clean   "$@" ;;
  mysql)   cmd_mysql   "$@" ;;
  *)       cmd_help       ;;
esac
