<!-- Claude Code는 v2.1.277부터 CLAUDE.md가 없으면 AGENTS.md를 직접 읽는다.
     이 파일은 AGENTS.md 직접 읽기가 안 되는 환경을 위한 import용이다. 규칙은 AGENTS.md에만 쓴다. -->
@AGENTS.md

## Agent skills

### Issue tracker

GitHub Issues(`gh` CLI). See `docs/agents/issue-tracker.md`.

### Domain docs

single-context(루트 `GLOSSARY.md` + `adr/`). ADR은 `adr/`에 둔다. `docs/adr/`는 만들지 않는다. See `docs/agents/domain.md`.

`docs/agents/*.md`는 로컬 전용이다(규칙 23, 커밋하지 않음).
