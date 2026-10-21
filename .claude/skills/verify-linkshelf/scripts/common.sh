# Sourced by the other scripts. Paths are derived from the checkout the script lives in.
SKILL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(git -C "$SKILL_DIR" rev-parse --show-toplevel)"
VERIFY_ROOT="$REPO/.verify"
BACKEND_PORT=8080
FRONTEND_PORT=5173

current_run() {
  if [ ! -f "$VERIFY_ROOT/current" ]; then
    echo "실행 중인 검증 인스턴스 없음(.verify/current 없음). 먼저 launch.sh" >&2
    exit 2
  fi
  RUN_ID="$(cat "$VERIFY_ROOT/current")"
  RUN_DIR="$VERIFY_ROOT/$RUN_ID/run"
  EVIDENCE_DIR="$VERIFY_ROOT/$RUN_ID/evidence"
}

listener_pid() {
  lsof -nP -t -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | head -1
}
