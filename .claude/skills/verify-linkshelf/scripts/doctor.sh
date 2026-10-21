#!/usr/bin/env bash
# Read-only: is the current verification instance ours and worth driving?
set -uo pipefail
. "$(dirname "$0")/common.sh"
current_run

fail=0
check() {
  if eval "$2"; then echo "OK   $1"; else echo "FAIL $1"; fail=1; fi
}
backend_pid="$(cat "$RUN_DIR/backend.pid" 2>/dev/null)"
frontend_pid="$(cat "$RUN_DIR/frontend.pid" 2>/dev/null)"

echo "run=$RUN_ID revision=$(cut -c1-7 "$EVIDENCE_DIR/revision") HEAD=$(git -C "$REPO" rev-parse --short HEAD)"
check "backend 프로세스 살아 있음(pid $backend_pid)" 'kill -0 "$backend_pid" 2>/dev/null'
check "frontend 프로세스 살아 있음(pid $frontend_pid)" 'kill -0 "$frontend_pid" 2>/dev/null'
check "포트 $BACKEND_PORT 주인이 이번 backend" '[ "$(listener_pid "$BACKEND_PORT")" = "$backend_pid" ]'
check "포트 $FRONTEND_PORT 주인이 이번 frontend" '[ "$(listener_pid "$FRONTEND_PORT")" = "$frontend_pid" ]'
check "backend가 메모리 DB(verify-$RUN_ID) 사용" 'ps -o command= -p "$backend_pid" | grep -q "jdbc:h2:mem:verify-$RUN_ID"'
check "빌드한 revision이 지금 HEAD와 같음" '[ "$(cat "$EVIDENCE_DIR/revision")" = "$(git -C "$REPO" rev-parse HEAD)" ]'
check "프록시 GET /api/links 200 + items 필드" 'curl -sf "http://localhost:$FRONTEND_PORT/api/links" | grep -q "\"items\""'

if [ "$fail" -ne 0 ]; then echo "doctor: FAIL"; exit 1; fi
echo "doctor: OK"
