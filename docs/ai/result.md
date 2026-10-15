# MVP-3 결과 — linkshelf 첫 화면 (2026-10-15)

> 조언 00:40 항목(MVP-3)의 마무리 기록(G). 진행 요약은 `~/Dev/AI_개발_가이드/plans/mvp1-status.md`.

## 무엇이 생겼나

| 항목 | PR | 결과 |
|---|---|---|
| ADR-0005, `frontend/` 골격, Feature Map | #20 | React + Vite + TS, Playwright(메모리 H2 + Vite webServer), testid 계약 |
| spec 003 (US1~US4, R-UI-01~13) | #24 | clarify 1회, plan·Issue 승인 1회 |
| CI `frontend-e2e` (B1) | #25 | ubuntu-24.04 고정, 아티팩트, `update_snapshots` 입력 |
| 첫 화면 (A, implementer) | #26 | 목록·저장·필터·읽음, 리뷰 수용 4건(코드 수정 기준, 읽음 PUT 직렬화 등. 분류상 수용 5건 중 1건은 T016·T017 증거 보강) |
| E2E·Visual (B2, implementer) | #27 | Red 커밋 → 화면 병합 후 merge → 기준 이미지 → 리뷰 수용 6건. 최종 기능 21 + smoke 1 + visual 3 = 25개 |
| 라벨 줄바꿈 수정 + 기준 이미지 갱신 | #29 | Red(라벨 높이 42px) → Green, visual 차이로 FAIL → 새 기준 이미지 |
| 보호 규칙 | — | main 필수 검사 `backend-test` + `frontend-e2e` |

## 처음 요구사항 중 실제로 돈 것

| 요구 | 결과 | 근거 |
|---|---|---|
| Feature Map | PASS | testid 계약 하나로 화면·E2E를 따로 만들어 합쳤을 때 FAIL 0(T016: 19 PASS / 3 SKIP) |
| E2E (CI) | PASS | 필수 검사, 최종 26개(기능 22 + visual 3 + smoke 1) |
| Visual QA | PASS | 화면 변화가 CI FAIL로 잡히고 `*-diff.png`가 아티팩트로 남음(PR #29 첫 CI) |
| 병렬 implementer | PASS | A·B2 한 턴에 동시 실행, worktree 격리, 파일 충돌 0 |
| ego lite 시범 | NOT_RUN | 선택 항목. 이번에 하지 않음 |

## 계측

| 항목 | 값 | 비고 |
|---|---|---|
| 병렬 단계 시간 | A 첫 PR 약 6분, B2 Red 약 15분(동시) | 순차였다면 약 21분. 이후 T016·리뷰·기준 이미지는 포트 때문에 순차. 출처: 구현 세션의 서브에이전트 실행 시간(저장소 기록 아님) |
| PR 생성 → 병합 | #26 23분, #27 33분 | 사용자 병합 대기 포함 |
| 사용자 질문(이 단계) | 승인 1회("다 승인": 병합·필수 검사·새 Issue·정리), 병합 알림 3회 | spec 003 단계의 clarify 1·승인 1은 별도 |
| advisor 호출 | 2회 | 필수 검사 시점(묶어서 묻기), 묶음 질문 검토 |
| 리뷰가 찾은 실제 결함 | 3건 | 읽음 연타 실패 시 되돌림 값(#26), R-UI-12 가드 미검증·약한 단언(#27), 라벨 줄바꿈(#27 → #29) |
| 고장 내기 | 화면 3건 + 새 단언 4건 + R-UI-12 1건 | 모두 FAIL로 잡힘 |

## 배운 것

- **worktree는 끝나도 남는다.** 잠금은 풀리지만 디렉터리는 남아 메인 `git status`에 보이고, 안의 `.claude/skills`가 스킬 목록에 중복으로 뜬다. `.gitignore`에 넣고, 제거는 삭제 승인 대상으로 다룬다.
- **포트가 병렬의 병목이다.** 고정 포트(8080/5173)를 쓰는 E2E는 동시에 하나만 돌 수 있어, 병렬 효과는 "코드 작성"까지였다.
- **기준 이미지는 결함도 정답으로 고정한다.** 처음 찍은 이미지에 라벨 줄바꿈이 있었고, 리뷰어가 PNG를 직접 열어서야 발견했다. 기준 이미지를 커밋할 때 눈으로 확인하는 단계가 필요하다.
- **비교 실패 시 새 이미지는 `*-actual.png`에 있다.** snapshots 아티팩트는 기준이 없을 때만 새 이미지를 담는다.
- **force push 금지 아래에서는 merge로 따라간다.** Red 커밋 이력도 함께 남는다.

## 남은 것

- 로컬 브랜치 `tmp/t016`: push하지 않은 임시 merge commit 하나. 내용(화면 + Red)은 main에 모두 있다. `-d`가 거부해 남겨 둠. 지우려면 사용자가 직접 `git branch -D tmp/t016`.
- E2E 위험(리뷰 기록): R-UI-12의 3초 고정 대기, `workers: 1` 순서 가정, 공유 DB가 커질수록 R-UI-02 쪽 넘김 증가.
- `ubuntu-latest`가 2026-10-19부터 Ubuntu 26으로 바뀐다(CI 공지). `frontend-e2e`는 24.04로 고정이라 기준 이미지 영향 없음. `backend-test`는 `ubuntu-latest`라 함께 바뀐다(JDK는 setup-java로 고정, 영향은 작을 것으로 추정·미확인).
