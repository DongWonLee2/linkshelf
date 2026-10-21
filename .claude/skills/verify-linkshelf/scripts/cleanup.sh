#!/usr/bin/env bash
set -uo pipefail
. "$(dirname "$0")/common.sh"
current_run

for name in frontend backend; do
  pid="$(cat "$RUN_DIR/$name.pid" 2>/dev/null || true)"
  [ -n "$pid" ] || continue
  case "$name" in
    backend) expected="jdbc:h2:mem:verify-$RUN_ID" ;;
    frontend) expected="vite/bin/vite.js" ;;
  esac
  if kill -0 "$pid" 2>/dev/null && ! ps -o command= -p "$pid" | grep -qF "$expected"; then
    echo "skip $name pid $pid: 명령줄에 $expected 없음(이번 실행 프로세스 아님)"
    continue
  fi
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid"
    for _ in $(seq 1 15); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid"
    echo "stopped $name pid $pid"
  else
    echo "$name pid $pid 이미 종료됨"
  fi
done

rm -f "$RUN_DIR"/*.pid
rmdir "$RUN_DIR"
rm -f "$VERIFY_ROOT/current"
for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
  [ -z "$(listener_pid "$port")" ] && echo "port $port free" || echo "port $port 아직 사용 중(pid $(listener_pid "$port")): 이번 실행이 띄운 것이 아니면 건드리지 않는다"
done
echo "evidence kept: $VERIFY_ROOT/$RUN_ID/evidence"
