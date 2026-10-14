# Quickstart: 첫 화면 검증

결과는 `PASS / FAIL / SKIP / NOT_RUN`으로만 적는다.

## 1. 로컬 실행

```bash
cd backend && ./gradlew bootRun            # 터미널 1 (로컬 파일 DB를 써도 되는 수동 확인일 때만)
cd frontend && npm run dev                  # 터미널 2 → http://localhost:5173
```

자동 테스트는 이 서버들을 쓰지 않는다(아래 2).

## 2. E2E (로컬·CI 공통)

```bash
cd frontend && npm ci && npx playwright install chromium
npx playwright test                         # 백엔드(메모리 H2)와 Vite를 직접 띄운다
```

- 기대: 기능 E2E 전부 PASS. macOS에서는 `visual.spec.ts`가 SKIP(Linux 전용).
- 실행 전후로 `backend/data/linkshelf.mv.db` 수정 시각이 같아야 한다(SC-005). `E2E_R_UI_12_…` 가드도 확인한다.
- 8080·5173 포트가 이미 쓰이고 있으면 시작 단계에서 실패한다(`reuseExistingServer: false`). 이것은 의도된 동작이다.

## 3. CI

- `frontend-e2e` job(ubuntu-24.04). 실패하면 `playwright-report`·`test-results` 아티팩트에서 diff 이미지를 본다.

## 4. 기준 이미지 만들기·바꾸기 (R-UI-13)

1. 첫 실행: visual 테스트가 "기준 이미지 없음"으로 실패한다(PR 본문에 예상 FAIL로 미리 적는다).
2. `gh run download <run-id> -n <스냅샷 아티팩트> -D frontend/e2e/`로 받는다. 받은 파일이 `*-chromium-linux.png`뿐인지 확인한다.
3. 커밋하고 CI를 다시 돌려 PASS를 확인한다.
4. 나중에 화면을 일부러 바꿀 때: `gh workflow run ci.yml --ref <branch> -f update_snapshots=true` → 아티팩트를 받아 커밋한다. **사람이** PR의 Files changed에서 이미지 차이(2-up·swipe)를 보고 병합한다.

## 5. 일부러 고장 내기 (SC-003)

- 목록 정렬 뒤집기 → `E2E_R_UI_01_…` FAIL
- 입력 오류 표시 제거 → `E2E_R_UI_06_shows_field_errors` FAIL
- 읽음 실패 시 되돌림 제거 → `E2E_R_UI_10_…` FAIL
- 스타일 한 줄 바꾸기 → `VISUAL_R_UI_13_…` FAIL(CI)
