#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "$0")/common.sh"

for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
  if [ -n "$(listener_pid "$port")" ]; then
    echo "launch: 포트 $port 사용 중(pid $(listener_pid "$port")). 이번 실행이 띄우지 않은 인스턴스는 조작하지 않는다" >&2
    exit 2
  fi
done

RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
RUN_DIR="$VERIFY_ROOT/$RUN_ID/run"
EVIDENCE_DIR="$VERIFY_ROOT/$RUN_ID/evidence"
mkdir -p "$RUN_DIR" "$EVIDENCE_DIR"
echo "$RUN_ID" > "$VERIFY_ROOT/current"
git -C "$REPO" rev-parse HEAD > "$EVIDENCE_DIR/revision"

# Build output goes to a file: the Gradle daemon must not inherit the caller's stdout, or a piped launch never ends.
(cd "$REPO/backend" && ./gradlew bootJar --console=plain -q > "$EVIDENCE_DIR/build.log" 2>&1 < /dev/null)
JAR="$(ls "$REPO"/backend/build/libs/*.jar | grep -v -- '-plain' | head -1)"
nohup java -jar "$JAR" --server.port="$BACKEND_PORT" \
  "--spring.datasource.url=jdbc:h2:mem:verify-$RUN_ID;DB_CLOSE_DELAY=-1" \
  > "$EVIDENCE_DIR/backend.log" 2>&1 < /dev/null &
echo $! > "$RUN_DIR/backend.pid"

if [ ! -d "$REPO/frontend/node_modules" ]; then
  (cd "$REPO/frontend" && npm ci --no-audit --no-fund > "$EVIDENCE_DIR/npm-ci.log" 2>&1 < /dev/null)
fi
cd "$REPO/frontend"
nohup node node_modules/vite/bin/vite.js > "$EVIDENCE_DIR/frontend.log" 2>&1 < /dev/null &
echo $! > "$RUN_DIR/frontend.pid"
cd "$REPO"

wait_for() {
  local url="$1" limit="$2" i=0
  until curl -sf -o /dev/null "$url"; do
    i=$((i + 1))
    if [ "$i" -ge "$limit" ]; then
      echo "launch: $url 가 ${limit}초 안에 준비되지 않음. 로그: $EVIDENCE_DIR. 정리: cleanup.sh" >&2
      exit 1
    fi
    sleep 1
  done
}
wait_for "http://localhost:$BACKEND_PORT/api/links" 180
wait_for "http://localhost:$FRONTEND_PORT/" 60
wait_for "http://localhost:$FRONTEND_PORT/api/links" 30

echo "READY run=$RUN_ID evidence=$EVIDENCE_DIR"
