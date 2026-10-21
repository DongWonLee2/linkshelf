#!/usr/bin/env bash
# Stops only the processes this run started and removes run state. Evidence stays.
set -uo pipefail
. "$(dirname "$0")/common.sh"
current_run

for name in frontend backend; do
  pid="$(cat "$RUN_DIR/$name.pid" 2>/dev/null || true)"
  [ -n "$pid" ] || continue
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    for _ in $(seq 1 15); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid"
  fi
  echo "stopped $name pid $pid"
done

rm -f "$RUN_DIR"/*.pid
rmdir "$RUN_DIR"
rm -f "$VERIFY_ROOT/current"
for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
  [ -z "$(listener_pid "$port")" ] && echo "port $port free" || echo "port $port 아직 사용 중(pid $(listener_pid "$port")): 이번 실행이 띄운 것이 아니면 건드리지 않는다"
done
echo "evidence kept: $VERIFY_ROOT/$RUN_ID/evidence"
