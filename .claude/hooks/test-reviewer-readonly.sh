#!/usr/bin/env bash
# Tests for reviewer-readonly.sh (Issue #7). Run: bash .claude/hooks/test-reviewer-readonly.sh
# Feeds PreToolUse JSON to the hook and checks the exit code: 0 = allowed, 2 = blocked.
set -u
HOOK="$(cd "$(dirname "$0")" && pwd)/reviewer-readonly.sh"
pass=0
fail=0

check() {
  local expected=$1 cmd=$2 actual
  jq -n --arg c "$cmd" '{tool_name: "Bash", tool_input: {command: $c}}' | bash "$HOOK" >/dev/null 2>&1
  actual=$?
  if [ "$actual" -eq "$expected" ]; then
    pass=$((pass + 1))
  else
    fail=$((fail + 1))
    printf 'FAIL expected=%s actual=%s: %q\n' "$expected" "$actual" "$cmd"
  fi
}

# Allowed: read-only git and gh commands the reviewer needs.
check 0 'git diff main...HEAD'
check 0 'git log --oneline -5'
check 0 'git show --stat HEAD'
check 0 'git status --short'
check 0 'git rev-parse HEAD'
check 0 'git ls-files .claude'
check 0 'gh pr view 7 --json title,body,files'
check 0 'gh pr diff 7'
check 0 'gh pr checks 7'
check 0 'gh pr list --state all'
check 0 'gh issue view 7'
check 0 'gh run view 123 --log-failed'

# Blocked unconditionally: chaining, redirection, substitution, newline.
check 2 'echo hi > /tmp/x'
check 2 'git log -1 > out.txt'
check 2 'git log -1 >> out.txt'
check 2 'cat < x'
check 2 'git status; touch x'
check 2 'git log | tee x'
check 2 'git status && touch x'
check 2 'git status || touch x'
check 2 'git diff &'
check 2 'git show $(touch x)'
check 2 'git show `touch x`'
check 2 $'git status\ntouch x'

# Blocked unconditionally: file output options.
check 2 'git log --output=out.txt'
check 2 'git diff --output out.txt'
check 2 'gh pr view 7 -o out.txt'

# Blocked: gradle (CI results come from gh pr checks instead).
check 2 './gradlew test'
check 2 './gradlew --version'
check 2 'cd backend && ./gradlew test'

# Blocked: not on the allow list.
check 2 'touch x'
check 2 "sed -i '' s/a/b/ f"
check 2 'rm build/x'
check 2 'git commit -m x'
check 2 'git add .'
check 2 'git push origin x'
check 2 'git branch x'
check 2 'git -C . push origin x'
check 2 'git -c core.pager=x log'
check 2 "'git' push"
check 2 'FOO=1 git log'
check 2 "bash -c 'git status'"
check 2 'gh pr comment 7 --body x'
check 2 'gh pr edit 7 --title x'
check 2 'gh pr review 7 --approve'
check 2 'gh issue comment 7 --body x'
check 2 'gh api repos/x/y'
check 2 'gh pr view 7 --web'
check 2 'gh run download 123'
check 2 ''

echo "pass=$pass fail=$fail"
[ "$fail" -eq 0 ]
