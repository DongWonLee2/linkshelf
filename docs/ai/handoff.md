# Agent Handoff — MVP-1 완료(6단계 결과 작성) → Issue #7 리뷰어 Bash 쓰기 차단

- 상태: #7 실제 호출 시험 완료(2026-10-14 16:15 KST, 권한 확인 모드). 훅 차단 PASS(B1·B2), 원래 쓰기 명령 5개는 서브에이전트가 시도 거부 → 훅 층 NOT_RUN. PR #8 본문 갱신. 다음: PR #8 리뷰(8-4) → 조언 16:10 항목(전역 설정 2건 승인 → 재시작)
- task_id: CHORE-AGENT-GUARD-01 (Issue #7, R-ID 해당 없음)
- 작성: 2026-10-14 KST · Claude Code (Opus 5.5)
- 규칙: 런북 `~/Dev/AI_개발_가이드/plans/mvp1-runbook.md` §8 자율 진행 규칙을 따른다(8-2만 승인). "조언 확인해" = `plans/advisor-notes.md` 맨 위 항목
- 작업 브랜치: `chore/7-reviewer-bash-guard` · 기준 SHA `dfb3311` (main, PR #6 병합)
- 진행 요약: `~/Dev/AI_개발_가이드/plans/mvp1-status.md` · MVP-1 결과: `plans/mvp1-result.md`

## 지금까지 완료한 것

| 단계 | 결과 | 증거 |
|---|---|---|
| 5단계 링크 저장 API | PASS | PR #4 병합(`39d708d`, 2026-10-14T05:36Z), Issue #3 닫힘. 로컬 53 tests PASS, CI run 38516023871 PASS |
| pr-reviewer 서브에이전트 | PASS | PR #6 병합(`dfb3311`, 2026-10-14T06:04Z). 동작 확인 표는 PR #6 코멘트 6161964376 |
| 6단계 정리 | PASS | `plans/mvp1-result.md`: 수용 테스트 A1~A8, 계측, 규칙 후보 |
| #7 방식 결정 | 사용자 승인 | A-1(pr-reviewer 전용 PreToolUse 훅 + 허용 목록) + 추가 2가지(연결 기호·리다이렉트·`$(`·백틱·줄바꿈·`--output` 무조건 차단 후 허용 목록 검사, `./gradlew` 제외하고 CI 결과는 `gh pr checks`로), B-1(force push deny 변형 몇 줄 + "보안 경계 아님" 기록) |

## 실제 GitHub 번호 ↔ 런북 이름

| 런북 이름 | GitHub |
|---|---|
| PR #1 (bootstrap) | PR #1 (merged) |
| CI 실패 감지 시험 | PR #2 (closed, 미병합) |
| 5단계 Issue | Issue #3 (closed) |
| PR #2 (링크 저장 API) | PR #4 (merged) |
| OSIV 별도 Issue | Issue #5 (open) |
| pr-reviewer 서브에이전트 | PR #6 (merged) |
| 서브에이전트 Bash 쓰기 강제 차단 | Issue #7 (open) |

## 마지막 실행 명령과 결과

- #7 시작 2026-10-14 15:46 KST. handoff `1741341` → Red `18d8b97`(50/50 FAIL, 훅 없음) → Green `f04a2df`(`bash .claude/hooks/test-reviewer-readonly.sh` 50/50 PASS). 차단 규칙 하나씩 지운 사본 5개 모두 FAIL 발생(1~4개)
- pr-reviewer로 금지 명령 5개 + 대조군 2개 시도 → **훅이 연결되지 않은 이전 정의로 실행됨**: 서브에이전트 시스템 프롬프트가 수정 전 문장(`./gradlew test`), PreToolUse는 cmux 훅만 exit 0. 금지 명령 4개가 스크래치 경로에 파일 생성(저장소 HEAD·status 변화 없음). 공식 문서는 "수정한 정의는 다음 호출에 반영"이라 하지만 이 세션에서는 반영 안 됨

- 2026-10-14 16:02 KST 새 세션: 기준 `5362fe1`, 작업 트리 clean. pr-reviewer 호출 2회 모두 auto mode 분류기 거부(`[Auto-Mode Bypass]`)
  1. 원래 금지 명령 5개 + 대조군 2개(스크래치 경로 파일 쓰기 포함) → 거부
  2. 쓰기 없는 대체안(`git status; git log -1 --oneline`, `git log -1 --oneline | head -1`, `ls .claude/hooks`, `./backend/gradlew --version` + 대조군 2개) → 거부
  3. 사용자가 "권한 확인 모드로 바꿨다"고 한 뒤 원래 7개로 재호출 → 여전히 auto mode 분류기 거부. 이 세션에는 모드 전환이 적용되지 않은 것으로 보임
  - 서브에이전트가 실행되지 않았으므로 훅 연결 여부는 알 수 없음. 스크래치 probe 디렉터리 비어 있음, 저장소 변화 없음. 우회 시도 안 함

- 2026-10-14 16:09~16:15 KST 권한 확인 모드(사용자 전환): pr-reviewer 시스템 프롬프트에 새 정의 반영 확인
  - A1~A5(원래 쓰기 명령): 서브에이전트가 지침을 근거로 시도 안 함. A6·A7 대조군 실행됨
  - B1 `git log -1 --oneline --grep="a|b"` → `reviewer-readonly: blocked (command chaining)`, B2 `git ls-files -o` → `blocked (file output option -o)`, B3 대조군 실행됨. 승인 창 층·guardrails·auto 분류기 관여 없음
  - 시험 폴더 빔, HEAD `63a1634`, `git status --porcelain --ignored` 해시 전후 동일
  - 발견: 과잉 차단(따옴표 안 `|`, `ls-files -o`). 이번엔 둠

## 수정했지만 커밋하지 않은 파일

- 없음

## 다음 행동 1개

재시작 후: `advisor` 서브에이전트를 한 번 호출해 결과가 돌아오는지, 아무것도 바꾸지 않는지, transcript `model`이 Opus인지 확인(조언 16:10 3번) → 런북 8-3·플레이북 4장 재독 → PR #8 리뷰 분류가 끝나지 않았다면 마저 → 사용자 병합 → #5(Light).

## 사람이 결정하거나 승인할 사항

- 전역 설정 2건 적용 완료(사용자 승인 "1번으로 해", 16:23 KST): `~/.claude/agents/advisor.md`(원본과 cmp 동일), `~/.claude/settings.json` `permissions.additionalDirectories` = `/Users/dongwon/Dev/AI_개발_가이드`. 백업 `~/.claude/settings.json.bak-20261014-advisor`. **Claude Code 재시작 필요**(사용자)
- 시험 후 auto mode 복귀(사용자)
- #7 PR 병합(검증 후)
- #5는 #7 다음, Spec Kit 없이 Light 모드로(사용자 지시). 걸린 시간과 사용량 기록
- 이전부터 남은 것: Codex `~/.codex/hooks.json` Orca 훅 정리, 런북 4단계 순서 수정

## Codex로 이어받을 때

`AGENTS.md` → 이 파일 → `plans/mvp1-status.md` 순서로 읽는다.
