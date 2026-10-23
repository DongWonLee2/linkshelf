# Implementation Plan: 안 읽은 것만 보기 필터

**Branch**: `feat/49-unread-filter` (Issue 생성 뒤, PR 1개) | **Date**: 2026-10-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-unread-filter/spec.md` · 기준 SHA `b15718f`

**상태**: 승인됨(2026-10-22). Issue #49, browse-links 계약 개정([contracts/unread-filter.md](contracts/unread-filter.md)) 모두 사용자 승인 "승인".

## Summary

- **백엔드**: `GET /api/links`에 `unread=true|false`(생략 = `false`)를 더한다. 문자열 + `@Pattern`으로 검증해 `yes`·`on`·`1`·빈 값·두 번 보냄은 기존 400 `errors` 형식(`field=unread`)으로, 다른 쿼리 오류와 함께 알린다. 저장소는 태그(null 허용)와 안 읽음을 함께 받는 쿼리 한 쌍으로 거른다.
- **화면**: 정렬 선택 상자 옆 체크박스 `link-unread-only-toggle`. 켜고 끄면 태그·정렬 유지·첫 쪽, 쪽 넘김·태그·정렬은 안 읽음 유지, 저장 성공은 끔. 새 요청은 마지막으로 고른 태그·정렬·안 읽음 값(T003 prefactor의 요청 조건 묶음)을 읽는다. 실패해도 정렬·안 읽음은 되돌리지 않는다(R-UI-24).

## Technical Context

**Language/Version**: Java 21 (ADR-0001), TypeScript + React + Vite (ADR-0005)

**Primary Dependencies**: 기존 그대로. **새 의존성 없음**

**Storage**: H2. **스키마 변경 없음**(`is_read`는 spec 002에서 있음), 인덱스 추가 없음([research R3](research.md))

**Testing**: ADR-0003 경계 그대로 — 단위(순수 로직이 있을 때만), `@WebMvcTest`(요청 검증·400 형식), `@DataJpaTest`(거르기·정렬·쪽 나누기 쿼리), `@SpringBootTest` 통합 1~2개(요청 → 응답 전체). 화면은 Playwright E2E·Visual, 실제 앱은 verify-linkshelf

**Target Platform**: 로컬, CI GitHub Actions ubuntu (`backend-test`, `frontend-e2e`)

**Project Type**: web (backend + frontend)

**Performance Goals**: 기존과 같음(수천 건, 별도 성능 테스트 없음)

**Constraints**: `unread` 생략 응답은 기존과 같아야 한다(SC-002). 화면은 주소에 상태를 남기지 않는다. 경쟁 E2E는 대기로 경쟁을 피하지 않는다([research R5](research.md))

**Scale/Scope**: 쿼리 파라미터 1개, 화면 요소 1개(testid 1개), 저장소 쿼리 1쌍 교체

## Constitution Check

*GATE: Phase 0 전 / Phase 1 후 재확인*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 승인 없는 계약 변경 금지 | 통과(2026-10-22 승인) | browse-links `GET /api/links`에 `unread` 추가, 400 `field`에 `unread` 추가. 개정안은 [contracts/unread-filter.md](contracts/unread-filter.md). 승인 전에는 browse-links와 코드를 바꾸지 않는다(tasks T001) |
| II. 테스트 먼저 | PASS(계획) | R-ID마다 Red 커밋 후 구현. 고장 내기 2가지는 quickstart §5 |
| III. 네 가지 결과 | PASS(계획) | quickstart 보고 방식 |
| IV. 2회 실패 중단 | 해당 시 적용 | — |
| V. 작은 PR | PASS(계획) | Issue 1개 PR 1개(spec 004와 같음). 백엔드·화면 검증 줄은 PR 표에서 나눈다 |
| VI. ID 추적 | PASS(계획) | R-LINK-22~24, R-UI-21~25 → tasks → Issue → `R_LINK_NN_…`/`E2E_R_UI_NN_…` |
| VII. 우회 금지 | PASS | — |

**Phase 1 후 재확인**: 새 위반 없음. I은 사용자 승인 전까지 게이트로 남는다.

## 핵심 설계 결정

| # | 결정 | 근거 |
|---|---|---|
| 1 | `unread`는 문자열 + `@Pattern("true\|false")`, `defaultValue` 없음 | `Boolean` 바인딩은 `yes`·`on`·`1`을 참으로 바꿈(사용자 보충), 다른 오류와 함께 알림. [R1](research.md) |
| 2 | 태그 쿼리를 태그(null 허용)+안 읽음 쿼리 한 쌍으로 바꾸고, 조건 없는 경로는 `findAll` 유지 | 조합 4가지를 쿼리 하나로, SC-002. [R2](research.md) |
| 3 | 인덱스·스키마 변경 없음 | 규모. [R3](research.md) |
| 4 | 화면 조건 세 개를 `requested*` ref에서 읽는다 | R-UI-24, 정렬 경쟁(K1) 원인 제거. [R4](research.md) |
| 5 | 경쟁 E2E는 `page.route` 지연으로 실제로 겹친다 | registry 원칙. [R5](research.md) |
| 6 | 쪽 밀림은 안 읽은 링크 21개로 1개 테스트 | clarify A. [R6](research.md) |
| 7 | Visual 기준 이미지는 CI `update_snapshots`로 갱신 | R-UI-13. [R7](research.md) |

## 테스트 경계 (ADR-0003)

| 경계 | 무엇 | R-ID |
|---|---|---|
| `@WebMvcTest` | `unread` 값 검증(허용 2개, 거부 7종), 다른 항목과 함께 400, 메시지 문구, 서비스에 넘기는 boolean | R-LINK-22, 24 |
| `@DataJpaTest` | 안 읽음만, 태그+안 읽음, 오래된순+안 읽음, 쪽 사이 겹침·빠짐 없음, 전체 개수 | R-LINK-22, 23 |
| `@SpringBootTest` 통합 2개 | 생략 응답 = `unread=false` 응답(문자열 비교), 저장 → 읽음 지정 → `unread=true` 목록 | R-LINK-22, 23 |
| 단위 | 없음(새 순수 로직 없음) | — |
| Playwright E2E | 화면 동작, 경쟁 3개, 쪽 밀림, 실패·빈 결과 | R-UI-21~25 |

## 계약 문서

계약 본문은 승인 뒤 [browse-links](../002-browse-links/contracts/browse-links.md)에 반영한다(개정 표기). [contracts/unread-filter.md](contracts/unread-filter.md)는 개정안과 승인 기록을 둔다. `features/README.md` 표에 `link-unread-only-toggle` 줄을 먼저 넣는다(testid 계약).

## Project Structure

```text
specs/005-unread-filter/       spec, plan, research, data-model, quickstart, contracts/unread-filter.md, tasks, diagrams/
specs/002-browse-links/contracts/browse-links.md   (승인 뒤) 개정 표기·unread 행 추가
backend/src/main/java/dev/linkshelf/link/
  LinkController.java          unread 파라미터
  LinkService.java             list(page, size, tag, order, unreadOnly)
  LinkRepository.java          findFiltered/countFiltered, findPageFiltered (findByTag/countByTag 대체)
backend/src/test/java/dev/linkshelf/link/   R_LINK_22~24 테스트, 기존 시그니처 추종
frontend/src/api.ts, App.tsx, (새) UnreadToggle.tsx
frontend/e2e/unread.spec.ts    (새) E2E_R_UI_21~25, visual 기준 이미지 갱신
.claude/skills/verify-linkshelf/features/README.md, features/unread-filter.md, drive/unread-filter.verify.ts
```

## 병렬 실행

계약 승인과 커밋 뒤, 백엔드(`backend/`)와 화면(`frontend/`, `features/`, `drive/`)은 고칠 파일이 겹치지 않는다. implementer 두 개를 worktree로 띄우고 결과를 브랜치 하나로 합칠 수 있다(spec 004와 같음, registry "dw-mode 병렬 지시 정리" 보류 항목에 해당).

## 승인 기록

| 항목 | 결정 | 사람의 말 |
|---|---|---|
| 범위·API·화면·경쟁·증거(공통 grilling Q1~Q12) | 추천안 + 덧붙임 3개 | "Q1~Q6 모두 추천대로", "Q7~Q12 모두 추천대로. 세 가지 덧붙임" |
| 쪽 밀림으로 빈 쪽(clarify Q1) | R-UI-03 그대로 | "A" |
| 명세 흐름 비교 판정 | 이 산출물로 구현 + prefactor(T003)·R-UI-24 실패 규칙·고장 내기 4종 | "판정: A로 구현한다. 제안한 두 가지(…)를 넣어." |
| Issue·PR | Issue #49, PR 1개 | "승인" |
| browse-links 계약 개정(constitution I) | 승인 | "승인" |
