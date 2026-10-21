# Implementation Plan: 첫 화면 (링크 저장·목록·태그 필터·읽음)

**Branch**: spec `chore/spec-003-first-screen` → 구현은 PR 3개(B1 → A ∥ B2) | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: `specs/003-first-screen/spec.md` · 기준 SHA `f1609fa`

**상태**: 승인됨. 사용자 답 "승인"(C1·C2와 Issue 3개, 2026-10-15, 이유 미기재)

## Summary

한 페이지 React 화면(`frontend/src/`)이 spec 001·002 API를 Vite 프록시로 부른다. 화면과 E2E는 [Feature Map](../../.claude/skills/verify-linkshelf/features/README.md)의 `data-testid`로만 연결된다. 검증은 세 층으로 나눈다.

1. **기능 E2E**(실제 백엔드, 메모리 H2): R-UI-01~12
2. **Visual QA**(API 응답 고정, Linux에서만): R-UI-13, 화면 상태 3개
3. **CI** `frontend-e2e` job(ubuntu-24.04)

## Technical Context

**Language/Version**: TypeScript 7.0.2, React 19.3.0 (ADR-0005)

**Primary Dependencies**: ADR-0005의 8개 그대로. **새 의존성 없음**(상태 관리·라우터·CSS 프레임워크·날짜 라이브러리 없음. 날짜 형식은 `Intl`/`Date`로)

**Storage**: 없음(화면 상태만). 백엔드 H2는 그대로

**Testing**: Playwright 1.63.0, Chromium. 기능 E2E는 실제 백엔드, visual은 `page.route` 고정 응답

**Target Platform**: 데스크톱 브라우저(로컬 macOS 개발, CI ubuntu-24.04)

**Project Type**: web(frontend + 기존 backend)

**Performance Goals**: 없음(SC-001은 사용자 흐름 1분)

**Constraints**: 백엔드 API 변경 금지(R-UI-11), 로컬 파일 DB 사용 금지(R-UI-12, SC-005)

**Scale/Scope**: 화면 1개, 컴포넌트 몇 개

## Constitution Check

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 승인 없는 계약 변경 금지 | PASS | API·DB·의존성 변경 없음. 화면 범위(C1)와 API 불변(C2) 확인. 사용자 답 "승인"(C1·C2와 Issue 3개, 2026-10-15, 이유 미기재) |
| II. 테스트 먼저 | PASS(계획) | B2가 Feature Map 기준 E2E를 먼저 쓰고 Red를 커밋한다. A는 B2 Red에 맞춰 구현한다. 고장 내기는 SC-003 |
| III. 네 가지 결과 | PASS(계획) | macOS에서 visual은 SKIP으로 보고 |
| IV. 2회 실패 중단 | 해당 시 적용 | — |
| V. 작은 PR | PASS | B1(CI), A(화면), B2(E2E+visual) |
| VI. ID 추적 | PASS(계획) | E2E 이름을 `E2E_R_UI_NN_…`으로 시작(아래 표) |
| VII. 우회 금지 | PASS | — |

## 핵심 설계 결정 (advisor 판단, 2026-10-15)

| # | 결정 | 근거 |
|---|---|---|
| 1 | 기준 이미지는 CI(`ubuntu-24.04` 고정)가 만든 것을 받아 커밋한다. 첫 실행은 기준 이미지가 없어 예상 FAIL → 아티팩트 `gh run download` → 커밋 → 재실행 PASS. 이후 갱신은 같은 workflow의 `workflow_dispatch`(`update_snapshots` 입력) | "CI와 같은 Linux에서 만든다"를 Docker 설치나 쓰기 권한 없이 지킨다. 갱신 PR 자동 생성은 `contents: write`가 필요하고 GITHUB_TOKEN PR은 CI가 바로 돌지 않아 쓰지 않는다 |
| 2 | visual은 전용 파일 `e2e/visual.spec.ts`에서 `page.route('/api/links*')`로 고정 JSON을 쓴다. `timezoneId`·`locale`·viewport 고정, 캡처 전 마우스 치우기. 허용 오차는 기본값 | 공유 메모리 DB에서는 "빈 목록"을 실제 백엔드로 재현할 수 없고, URL·시각이 매번 바뀐다. 기능은 기능 E2E가, 그리기는 visual이 검증한다 |
| 3 | visual은 Linux에서만 실행, macOS는 SKIP | 로컬에서 `-darwin.png`가 생기는 것을 막는다 |
| 4 | CI job: Node 22(LTS, Vite 요구 충족), Java 21, `npx playwright install --with-deps chromium`(브라우저 캐시 없음), `./gradlew classes`를 별도 step으로 먼저, `globalTimeout` 10분, `timeout-minutes: 20`, 실패 시 리포트·스냅샷 아티팩트(보존 7일) | 컴파일 시간을 webServer 180초와 분리. Playwright 문서는 브라우저 캐시를 권하지 않는다 |
| 5 | 경로별 CI 분리는 하지 않는다. ADR-0002에 "재검토함, 보류"로 기록 | 경로 필터로 건너뛴 필수 검사는 Pending으로 남아 병합이 막힌다. E2E는 백엔드 변경에도 돌아야 한다 |
| 6 | PR 분할: **B1** CI job + config 보강(기존 smoke만) → **A** 화면 ∥ **B2** 기능 E2E + visual. B2는 Red를 로컬에서 확인·커밋하고, A 병합 후 rebase해서 PR을 연다 | CI 설정·화면·테스트 실패가 다른 PR에서 드러나 층별 증거가 나뉜다. 빨간 PR을 열어 두지 않는다 |
| 7 | E2E 데이터: 기능 E2E는 테스트마다 고유 URL(`https://e2e.test/<테스트 이름>/<시각>`), 빈 목록은 없는 태그로 거른 상태로 확인 | Feature Map 데이터 격리 규칙 |
| 8 | 시간대: E2E·visual 모두 `timezoneId: 'Asia/Seoul'`, `locale: 'ko-KR'` | 저장 시각 표시(clarify Q2)를 고정 |

## E2E 테스트 이름 ↔ R-UI (SC-002)

| R-UI | E2E (`frontend/e2e/`) |
|---|---|
| R-UI-01 | `E2E_R_UI_01_lists_newest_first_with_fields` |
| R-UI-02 | `E2E_R_UI_02_pages_through_links` |
| R-UI-03 | `E2E_R_UI_03_shows_empty_state`, `E2E_R_UI_03_shows_list_error_when_api_fails` |
| R-UI-04, 05 | `E2E_R_UI_05_saves_link_and_shows_it_first`, `E2E_R_UI_05_save_clears_tag_filter`, `E2E_R_UI_04_save_button_disabled_while_saving` |
| R-UI-06 | `E2E_R_UI_06_shows_field_errors`, `E2E_R_UI_06_shows_duplicate_notice` |
| R-UI-07 | `E2E_R_UI_07_filters_by_tag_ignoring_case`, `E2E_R_UI_07_blank_filter_clears` |
| R-UI-08 | `E2E_R_UI_08_shows_filter_error_and_keeps_list`, `E2E_R_UI_08_pages_within_filter` |
| R-UI-09 | `E2E_R_UI_09_marks_read_and_keeps_after_reload`, `E2E_R_UI_09_marks_unread_again` |
| R-UI-10 | `E2E_R_UI_10_reverts_checkbox_when_marking_fails` |
| R-UI-11 | 백엔드 diff 없음(PR 검증 표) + 위 기능 E2E가 실제 API로 통과 |
| R-UI-12 | `E2E_R_UI_12_backend_uses_in_memory_db`(로컬 파일 DB 가드) |
| R-UI-13 | `VISUAL_R_UI_13_first_screen`, `VISUAL_R_UI_13_empty_list`, `VISUAL_R_UI_13_form_errors` |
| SC-001 | `E2E_SC_001_full_flow` |

- 실패 상황(목록 실패, 읽음 실패)은 `page.route`로 해당 요청만 실패시킨다. 나머지는 실제 백엔드를 쓴다.
- R-UI-12 가드: webServer의 백엔드가 메모리 DB인지 확인한다. 방법은 B2에서 정한다(예: 기동 전 env 확인, 또는 로컬 `backend/data/` 파일 수정 시각 전후 비교).

## Project Structure

### Documentation

```text
specs/003-first-screen/
├── spec.md, plan.md, research.md, quickstart.md, tasks.md(다음 단계)
└── checklists/requirements.md
.claude/skills/verify-linkshelf/features/README.md  # 화면↔E2E 계약(이미 있음)
```

API 계약은 바뀌지 않으므로 `contracts/`를 새로 만들지 않는다. 화면이 쓰는 API는 [save-link](../001-save-link/contracts/save-link.md), [browse-links](../002-browse-links/contracts/browse-links.md) 그대로다. 저장하는 데이터가 없어 `data-model.md`도 만들지 않는다.

### Source Code

```text
frontend/
├── src/                      # A: 화면
│   ├── App.tsx               #   화면 상태(쪽, 필터), 목록 다시 불러오기
│   ├── api.ts                #   fetch 래퍼(POST /api/links, GET /api/links, PUT /{id}/read), problem 응답 해석
│   ├── LinkForm.tsx          #   US2
│   ├── LinkList.tsx          #   US1, US4 체크박스
│   ├── TagFilter.tsx         #   US3
│   └── styles.css            #   최소 스타일
├── e2e/                      # B2: 기능 E2E + visual
│   ├── smoke.spec.ts         #   (있음)
│   ├── list.spec.ts, save.spec.ts, filter.spec.ts, read.spec.ts, flow.spec.ts
│   ├── visual.spec.ts        #   Linux 전용, 고정 응답
│   └── visual.spec.ts-snapshots/*-chromium-linux.png  # 첫 CI에서 생성
└── playwright.config.ts      # B1: globalTimeout, timezoneId·locale, visual Linux 전용
.github/workflows/ci.yml      # B1: frontend-e2e job
adr/ADR-0002-*.md  # 오케스트레이터: 경로별 분리 재검토 기록
```

A는 `frontend/src/`만, B2는 `frontend/e2e/`만 고친다(겹치지 않음). B1은 둘보다 먼저 병합한다.

## 승인 요청 (완료)

constitution I은 API·DB·의존성 변경이 있을 때 승인을 요구한다. 이 plan에는 셋 다 없다. 조언 00:40 3번대로 다음 두 가지를 확인받는다.

| # | 항목 | 내용 |
|---|---|---|
| C1 | 화면 범위 | spec 003 US1~US4 + R-UI-01~13. 범위 밖: 로그인, 수정·삭제, 검색, 여러 태그, 안 읽은 것만, 반응형 세부, 배포 |
| C2 | API 변경 없음 | 백엔드 코드·API 계약·DB를 바꾸지 않는다. 화면이 API 때문에 막히면 멈추고 따로 승인을 받는다 |

진행 방식(B1 → A ∥ B2, 기준 이미지 방법, CI 구성)은 advisor 판단(과정 선택)으로 정했고 승인 대상이 아니다. `frontend-e2e`를 필수 검사로 넣는 일은 main에서 첫 PASS 뒤 따로 묻는다.

## Complexity Tracking

위반 없음.
