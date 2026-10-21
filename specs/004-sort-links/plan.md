# Implementation Plan: 링크 목록 정렬 옵션

**Branch**: `feat/43-sort-links` (Issue #43, PR 1개) | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-sort-links/spec.md` · 기준 SHA `0006c38`

**상태**: 승인됨 (2026-10-21). 사용자 답 "1) 승인"(이유 미기재). 아래 [승인 기록](#승인-기록)을 본다.

## Summary

- **백엔드**: `GET /api/links`에 `order=newest|oldest`(생략 = newest)를 더한다. 문자열 + `@Pattern`으로 검증해 잘못된 값(빈 값 포함)은 기존 400 `errors` 형식(`field=order`)으로, 다른 쿼리 오류와 함께 알린다. 저장소는 정렬을 인자로 받는다.
- **화면**: 목록 위 정렬 선택 상자 `link-sort-select`. 정렬 변경은 필터 유지·첫 쪽, 쪽 넘김·필터는 정렬 유지, 저장 성공은 최신순으로 되돌림.

## Technical Context

**Language/Version**: Java 21 (ADR-0001), TypeScript + React + Vite (ADR-0005)

**Primary Dependencies**: 기존 그대로. **새 의존성 없음** (`@Pattern`은 이미 쓰는 Jakarta Validation)

**Storage**: H2. **스키마 변경 없음**, 인덱스 추가 없음([research R3](research.md))

**Testing**: JUnit 5, MockMvc, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` (ADR-0003). 화면은 Playwright E2E·Visual, 실제 앱은 verify-linkshelf

**Target Platform**: 로컬, CI GitHub Actions ubuntu (`backend-test`, `frontend-e2e`)

**Project Type**: web (backend + frontend)

**Performance Goals**: 기존과 같음(수천 건, 별도 성능 테스트 없음)

**Constraints**: 정렬 생략 응답은 기존과 같아야 한다(SC-002). 화면은 주소에 상태를 남기지 않는다

**Scale/Scope**: 쿼리 파라미터 1개, 화면 요소 1개(testid 1개)

## Constitution Check

*GATE: Phase 0 전 / Phase 1 후 재확인*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 승인 없는 계약 변경 금지 | PASS | browse-links `GET /api/links`에 `order` 추가, 400 `field`에 `order` 추가. 대안(enum, `sort=`)은 [research R1](research.md). 2026-10-21 사용자 승인 |
| II. 테스트 먼저 | PASS(계획) | R-ID마다 Red 커밋 후 구현. 고장 내기는 quickstart §5 |
| III. 네 가지 결과 | PASS(계획) | quickstart 보고 방식 |
| IV. 2회 실패 중단 | 해당 시 적용 | — |
| V. 작은 PR | PASS | Issue #43 PR 1개(사용자 승인). 백엔드·화면 검증 줄은 PR 표에서 나눈다 |
| VI. ID 추적 | PASS(계획) | R-LINK-19~21, R-UI-18~20 → tasks → #43 → `R_LINK_NN_…`/`E2E_R_UI_NN_…` |
| VII. 우회 금지 | PASS | — |

**Phase 1 후 재확인**: 새 위반 없음. I은 2026-10-21 사용자 승인으로 PASS.

## 핵심 설계 결정

| # | 결정 | 근거 |
|---|---|---|
| 1 | `order`는 문자열 + `@Pattern("newest\|oldest")`, `defaultValue` 없음 | 다른 쿼리 오류와 함께 알림(R-LINK-21), 빈 값 400(clarify Q3). [R1](research.md) |
| 2 | 저장소 메서드가 `Sort`를 받고, 서비스는 내부 enum `LinkOrder`로 넘긴다 | 큰 쪽 번호 처리·태그 쿼리 재사용, 동률 id 같은 방향. [R2](research.md) |
| 3 | 인덱스·스키마 변경 없음 | 규모. [R3](research.md) |
| 4 | 화면 `order` 상태는 `tag`와 같은 패턴, 저장 성공 시 `newest` | R-UI-19·20. [R4](research.md) |
| 5 | Visual 기준 이미지는 CI `update_snapshots`로 갱신 | 같은 환경에서 만든다(R-UI-13). [R5](research.md) |

## 계약 문서

계약 본문은 [browse-links](../002-browse-links/contracts/browse-links.md)에 반영했다(개정 표기). [contracts/sort-links.md](contracts/sort-links.md)는 승인 기록과 링크만 둔다. `features/README.md` 표에 `link-sort-select` 줄을 먼저 넣는다(testid 계약).

## Project Structure

```text
specs/004-sort-links/          spec, plan, research, data-model, quickstart, contracts/sort-links.md, tasks
specs/002-browse-links/contracts/browse-links.md   개정 표기·order 행 추가
backend/src/main/java/dev/linkshelf/link/
  LinkController.java          order 파라미터
  LinkService.java             list(page, size, tag, order)
  LinkRepository.java          OLDEST_FIRST, 정렬을 받는 조회
  LinkOrder.java               (새) NEWEST, OLDEST
backend/src/test/java/dev/linkshelf/link/   R_LINK_19~21 테스트
frontend/src/api.ts, App.tsx, (새) SortSelect.tsx
frontend/e2e/sort.spec.ts      (새) E2E_R_UI_18~20, visual 기준 이미지 갱신
.claude/skills/verify-linkshelf/features/README.md, features/sort-links.md, drive/sort-links.verify.ts
```

## 병렬 실행

계약 승인과 커밋 뒤, 백엔드(`backend/`)와 화면(`frontend/`, `features/`, `drive/`)은 고칠 파일이 겹치지 않는다. 이때 implementer 두 개를 worktree로 띄우고 결과를 `feat/43-sort-links` 하나로 합친다. 런북 §8-6의 "각각 독립된 PR" 조건과 다르다. 이 차이는 측정 표의 하네스 문제로 적는다(advisor 검토 2026-10-21).

## 승인 기록

| 항목 | 결정 | 사람의 말 |
|---|---|---|
| 정렬 기준·API·화면(grilling) | 추천안 | 추천안 선택(이유 미기재) |
| 저장 뒤 정렬, 빈 `order` | 최신순으로 되돌림, 400 | advisor 추천 선택(이유 미기재) |
| Issue·PR | #43 하나, PR 하나 | "1) Issue 1개, PR 1개" |
| browse-links 계약 개정(constitution I) | 승인, browse-links에 직접 반영 | "1) 승인"(이유 미기재) |
