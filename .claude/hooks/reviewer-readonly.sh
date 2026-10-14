#!/usr/bin/env bash
# PreToolUse hook for the pr-reviewer subagent (Issue #7).
# Reads the hook JSON on stdin and exits 2 (block) unless the Bash command is a
# single read-only git/gh command from the allow list below.
# Order: unconditional blocks first (chaining, redirection, substitution,
# newline, file output options), then the allow list.
set -u

block() {
  echo "reviewer-readonly: blocked ($1). pr-reviewer may only run single read-only git/gh commands." >&2
  exit 2
}

cmd=$(jq -r '.tool_input.command // empty' 2>/dev/null) || block "unreadable hook input"
[ -n "$cmd" ] || block "empty command"

case "$cmd" in
  *$'\n'* | *$'\r'*) block "newline" ;;
  *';'* | *'|'* | *'&'*) block "command chaining" ;;
  *'>'* | *'<'*) block "redirection" ;;
  *'$('* | *'`'*) block "command substitution" ;;
esac

read -r -a words <<< "$cmd"

for w in "${words[@]}"; do
  case "$w" in
    --output* | -o*) block "file output option $w" ;;
    --web | -w) block "opens a browser: $w" ;;
  esac
done

prog=${words[0]}
sub=${words[1]:-}
action=${words[2]:-}

case "$prog" in
  git)
    case "$sub" in
      diff | log | show | status | rev-parse | ls-files | ls-remote | merge-base | blame) exit 0 ;;
    esac
    ;;
  gh)
    case "$sub $action" in
      "pr view" | "pr diff" | "pr checks" | "pr list" | \
      "issue view" | "issue list" | \
      "run view" | "run list") exit 0 ;;
    esac
    ;;
esac

block "not on the allow list: $prog $sub"
