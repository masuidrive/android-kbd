#!/usr/bin/env bash
# dev-server.sh — PDH verify / human-review local dev server entrypoint.
#
# Serves the repository's static product page and interactive mock.
# Required common options:
#   --seed          Reset local state and run scripts/seed-pdh-verify.sh before start.
#   --port PORT     Use a fixed local port. If omitted, choose an available port.
#   --persist-to D  Optional project-specific local persistence path.
#   --no-localhost  Expose a non-localhost review URL using the project's safe method.
set -euo pipefail

show_help() {
  cat <<'USAGE'
Usage: ./scripts/dev-server.sh [--seed] [--port PORT] [--persist-to DIR] [--no-localhost]

Serves site/ locally for product-page and mock verification.

Expected project behavior:
  --seed          Reset local state and run scripts/seed-pdh-verify.sh.
  --port PORT     Bind to the requested port.
  --persist-to D  Unused by this static site.
  --no-localhost  Unsupported; this script only binds localhost.

PDH agents use this script for PDH-verify and PDH-human-review when UI/API
surfaces need a running local server.
USAGE
}

seed=false
port=""
persist_to=""
no_localhost=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --seed)
      seed=true
      shift
      ;;
    --port)
      port="${2:-}"
      if [[ -z "$port" ]]; then
        echo "Missing value for --port" >&2
        exit 2
      fi
      shift 2
      ;;
    --persist-to)
      persist_to="${2:-}"
      if [[ -z "$persist_to" ]]; then
        echo "Missing value for --persist-to" >&2
        exit 2
      fi
      shift 2
      ;;
    --no-localhost)
      no_localhost=true
      shift
      ;;
    -h|--help)
      show_help
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      show_help >&2
      exit 2
      ;;
  esac
done

if [[ -z "$port" ]]; then
  port="$((20000 + (RANDOM % 20000)))"
fi

if $seed; then
  ./scripts/seed-pdh-verify.sh
fi

if $no_localhost; then
  echo "--no-localhost is not configured for this static site" >&2
  exit 2
fi

if [[ -n "$persist_to" ]]; then
  echo "--persist-to is not used by this static site" >&2
  exit 2
fi

echo "Local product page: http://127.0.0.1:$port/" >&2
echo "Stop with Ctrl-C. No seed or login is required." >&2
cd site
exec python3 -m http.server --bind 127.0.0.1 "$port"
