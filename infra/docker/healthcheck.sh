#!/usr/bin/env bash
# Container health check: passes when the service says it's ready for traffic.
# Uses bash's /dev/tcp so the image doesn't need curl or wget.
set -euo pipefail

exec 3<>"/dev/tcp/127.0.0.1/${SERVER_PORT:-8080}"
printf 'GET /actuator/health/readiness HTTP/1.0\r\nHost: localhost\r\n\r\n' >&3
grep -q '"status":"UP"' <&3
