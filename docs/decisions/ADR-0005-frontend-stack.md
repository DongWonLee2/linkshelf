# ADR-0005: 프론트엔드 스택

- 상태: Accepted
- 작성일 / 사람 승인일: 2026-10-15 / 2026-10-15
- 결정 책임자: 저장소 소유자
- 관련 Spec/R-ID: PRD-01~04 화면(MVP-3, spec 003 예정)
- 사용한 외부 근거: npm registry 조회(2026-10-15). 로컬 `node -v` = v25.8.1, `npm -v` = 11.11.0. Vite 8 요구 Node `^20.19.0 || >=22.12.0`

## 맥락

PRD-01~04 백엔드 API(spec 001·002)가 끝났다. Feature Map, E2E, Visual QA를 시험하려면 화면이 필요하다(런북 부록 MVP-3).

## 평가 기준

- 로컬 1인용, 서버 렌더링이 필요 없음
- E2E와 스크린샷 비교를 CI(ubuntu)에서 돌릴 수 있음
- 백엔드를 고치지 않음(API 계약은 spec 001·002 그대로)
- 의존성 최소

## 대안

1. React + Vite + TypeScript, 같은 저장소 `frontend/` (런북 후보)
2. 프레임워크 없이 정적 HTML + JS를 Spring이 서빙 — 백엔드 변경 필요, 화면이 커지면 관리가 어렵다
3. Next.js — 서버 렌더링·라우팅이 이 범위에 필요 없고 의존성이 크다

## Agent의 추천과 근거

1. 런북 후보이고, 백엔드를 건드리지 않으며(Vite 개발 프록시), Playwright와 함께 쓰는 예가 가장 많다.

## 사람의 최종 결정과 실제 판단 이유

사용자: "React+Vite+TS ADR, 최소 npm 패키지와 Playwright 브라우저 설치, CI 프론트 E2E job 추가는 묻지 말고 진행해" (2026-10-15, 조언 00:40 1번 승인 묶음).

## 결정 내용

- 위치: `frontend/`. 백엔드와 같은 저장소.
- 개발 중 `/api`는 Vite 프록시로 `http://localhost:8080`에 붙인다. 백엔드에 CORS 설정을 더하지 않는다.
- 패키지(정확한 버전 고정, `package-lock.json` 커밋). 공급망 위험을 줄이려고 2주 이상 지난 버전 중 최신을 골랐다(2026-09-30 이전 공개).
  | 패키지 | 버전 | 용도 |
  |---|---|---|
  | react, react-dom | 19.3.0 | 화면 |
  | vite | 8.3.1 | 개발 서버·빌드 |
  | @vitejs/plugin-react | 6.1.1 | Vite에서 React(JSX) 변환 |
  | typescript | 7.0.2 | 타입 검사 |
  | @types/react, @types/react-dom | 19.3.0 | React 타입 |
  | @playwright/test | 1.63.0 | E2E·스크린샷 비교 |
  - 조언 노트의 최소 목록(react, vite, typescript, @playwright/test)에 React를 Vite로 쓰는 데 필요한 짝(react-dom, @vitejs/plugin-react, 타입 2개)을 더했다. 상태 관리·라우터·CSS 프레임워크·테스트 러너(Vitest 등)는 넣지 않는다.
- Playwright 브라우저는 Chromium만 받는다(`npx playwright install chromium`).
- E2E는 Playwright `webServer`로 백엔드(메모리 H2, `SPRING_DATASOURCE_URL`)와 Vite 개발 서버를 함께 띄운다. 로컬 파일 DB를 건드리지 않는다.
- 화면과 E2E 사이의 계약은 `docs/feature-map.md`의 `data-testid` 목록이다.
- CI에 `frontend-e2e` job을 더한다(spec 003 구현 때). 필수 검사로 넣는 일(보호 규칙 변경)은 첫 PASS 뒤 따로 묻는다.

## 포기하는 것·운영 영향

- Node가 필요하다. 로컬 v25(현재 버전), CI는 LTS를 쓴다. 버전 차이로 생기는 문제는 CI 결과를 기준으로 본다.
- 운영 배포·정적 파일 서빙은 범위 밖(PRD 제외: 배포).
- 단위 테스트 러너 없이 E2E로 화면을 검증한다. 화면 로직이 복잡해지면 다시 검토한다.
- 되돌리기: `frontend/`와 이 ADR을 지우는 PR.

## 결정 검증

- `npm ci && npm run typecheck && npm run build` 성공
- `npx playwright test` 연기 테스트(앱 껍데기가 뜨고, 프록시로 `/api/links`가 200) PASS
- 재검토 트리거: 화면이 여러 페이지로 늘어날 때(라우터), 서버 렌더링이나 배포가 필요해질 때
