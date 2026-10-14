# Agent Handoff — MVP-1 4단계 완료 → 5단계 대기

- 상태: paused (사용자가 bootstrap PR 병합 후 세션 재시작 예정)
- 작성: 2026-10-14 11:35 KST · Claude Code (Opus 5.5)
- Task: MVP-1 런북 `~/Dev/AI_개발_가이드/plans/mvp1-runbook.md` 0~4단계
- 작업 브랜치: `chore/bootstrap` → GitHub PR #1 (OPEN, CI PASS, 사용자 병합 대기)
- Base SHA (`main`): `ba05ea0` / Head SHA: 이 handoff 커밋 (직전 `e6fe6ca`)
- Spec/ADR: constitution v1.0.0, PRD Approved, ADR-0001~0003 Accepted
- 환경 기록: `~/Dev/AI_개발_가이드/plans/mvp0-inventory.md`

## 지금까지 완료한 것

| 단계 | 결과 | 증거 |
|---|---|---|
| 0 환경 점검 | PASS | inventory §1~6 |
| 1 도구 | PASS | uv 0.13.0, specify-cli 1.2.0. diagram-design은 기존 스킬 유지(플러그인 설치 안 함) |
| 2 저장소 | PASS | `DongWonLee2/linkshelf` public, `.claude/settings.json` 권한. `.env` 차단은 guardrails mod와 함께만 확인 |
| 3 기초 문서 | PASS | constitution, AGENTS.md, PRD, ADR 3개, `docs/diagrams/architecture.html`/`.svg` |
| 4 골격·CI | PASS | 로컬 `./gradlew test` 2 tests PASS. PR #1 CI `backend-test` PASS (run 38499278914) |
| 4 main 보호 (A7) | PASS | 필수 검사 이름(backend-test)이 실제 CI 검사 이름과 일치, 실패 PR이 BLOCKED. draft 영향은 분리 못 함 |
| 4 검증기 검증 (A1 일부) | PASS | draft PR #2 CI FAIL (run 38500830240), 닫고 원격·로컬 `tmp/ci-should-fail` 삭제(사용자 승인) |
| 전역: Orca 제거 | PASS | `~/.claude/settings.json`에서 Orca 훅 13개 이벤트, 빈 `hooks`, `statusLine` 삭제. 백업 `~/ai-platform-backups/2026-10-14/settings.json.before-orca-removal` |

## 실제 GitHub 번호 ↔ 런북 이름

| 런북 이름 | GitHub |
|---|---|
| PR #1 (bootstrap) | #1 |
| CI 실패 감지 시험 | #2 (closed, 병합 안 함) |
| PR #2 (링크 저장 API) | 아직 없음 → 다음 번호(#3 이상) |

## 현재 막힌 지점

없음. 사용자 작업 대기: PR #1 병합, 세션 재시작.

## 수정된 파일과 미커밋 상태

- 저장소: 이 파일 커밋 후 깨끗한 상태
- 저장소 밖: `~/.claude/settings.json`(Orca 제거, 승인됨), inventory 파일

## 다음 첫 행동

`git switch main && git pull` 후 기준 SHA를 기록하고 5단계 `/speckit-specify` 실행.

## 5단계에서 지킬 사용자 지시

- 각 FR에 출처 PRD 번호(PRD-01)를 함께 적는다.
- 결정 필요 항목(메모 최대 길이, URL 정규화 범위, 태그 개수 제한)은 사용자에게 묻고 spec에 기록한다.
- 실제 GitHub PR 번호는 결과 보고에 런북 이름과 따로 적는다.

## 사람이 결정하거나 승인할 사항

- PR #1 병합
- 재시작 후 `/hooks`에서 Orca 훅이 사라졌는지 확인
- 할 일: Codex `~/.codex/hooks.json`의 Orca 훅 정리(cmux 훅은 유지, config.toml 신뢰 기록 포함)
- 런북 파일 4단계가 아직 이전 순서로 저장돼 있음(사용자 메시지 순서로 진행함) → 필요하면 런북 수정

## 재개 조건

- [ ] `main`에 PR #1 병합 확인, 기준 SHA 기록
- [ ] `/hooks`에서 Orca 훅 없음 확인
- [ ] `cd backend && ./gradlew test` 재실행 PASS
