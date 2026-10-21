#!/usr/bin/env bash
set -uo pipefail
. "$(dirname "$0")/common.sh"
current_run

feature="${1:?사용법: drive.sh <save-link|browse-links|tag-filter|mark-read>}"
spec="$SKILL_DIR/drive/$feature.verify.ts"
[ -f "$spec" ] || { echo "drive: $spec 없음" >&2; exit 2; }

out="$EVIDENCE_DIR/$feature"
mkdir -p "$out"
if ! "$SKILL_DIR/scripts/doctor.sh" > "$out/doctor.log" 2>&1; then
  cat "$out/doctor.log"
  echo 3 > "$out/exit-code"
  echo "drive $feature: doctor FAIL, 조작하지 않음 (증거 $out/doctor.log)"
  exit 3
fi
# NODE_PATH lets the spec outside frontend/ resolve the same @playwright/test install the E2E suite uses.
(cd "$REPO/frontend" && VERIFY_EVIDENCE="$out" NODE_PATH="$REPO/frontend/node_modules" \
  npx playwright test -c "$SKILL_DIR/drive/playwright.verify.config.ts" "$feature.verify.ts") \
  2>&1 | tee "$out/playwright.log"
code="${PIPESTATUS[0]}"
echo "$code" > "$out/exit-code"
if [ "$code" -eq 0 ]; then echo "drive $feature: PASS (증거 $out)"; else echo "drive $feature: FAIL exit=$code (증거 $out)"; fi
exit "$code"
