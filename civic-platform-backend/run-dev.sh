#!/usr/bin/env bash
# Free port 8081 (common cause of spring-boot:run failure) then start the API.
set -euo pipefail
cd "$(dirname "$0")"
if command -v fuser >/dev/null 2>&1; then
  fuser -k 8081/tcp 2>/dev/null || true
  sleep 0.5
fi
exec mvn spring-boot:run "$@"
