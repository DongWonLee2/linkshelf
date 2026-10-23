# Quickstart: 안 읽은 것만 보기 필터 검증

계약은 [contracts/unread-filter.md](contracts/unread-filter.md), 상태는 [data-model.md](data-model.md)를 본다. 결과는 `PASS / FAIL / SKIP / NOT_RUN`으로만 적는다.

## 1. 백엔드 테스트 (R-LINK-22~24)

```sh
cd backend && ./gradlew test
```

기대: 전체 PASS. 새 테스트 이름은 `R_LINK_22_…`·`R_LINK_23_…`·`R_LINK_24_…`. 기존 R-LINK-09~21 테스트의 기대값은 바꾸지 않고 PASS(SC-002). 저장소 메서드 이름 변경에 따른 호출 수정은 PR에 이유를 적는다.

## 2. 실제 요청/응답

`../../.claude/skills/verify-linkshelf/scripts/launch.sh`로 띄운 뒤(메모리 H2), 링크 A(태그 `java`)·B(태그 `java`)·C를 차례로 저장하고 B를 읽음으로 지정한 뒤:

| 요청 | 기대 |
|---|---|
| `GET /api/links` | 200, C·B·A (`unread=false`와 같은 응답) |
| `GET /api/links?unread=true` | 200, C·A, `totalElements` 2 |
| `GET /api/links?unread=true&tag=java&order=oldest` | 200, A만 |
| `GET /api/links?unread=yes` | 400, `errors[0].field = "unread"`, 메시지 `true 또는 false여야 합니다` |
| `GET /api/links?unread=` | 400, `field = "unread"` |
| `GET /api/links?unread=1&order=bad&page=-1` | 400, `errors`에 `unread`·`order`·`page` 셋 다 |

## 3. 화면 E2E·Visual (R-UI-21~25)

```sh
cd frontend && npx playwright test
```

기대: 새 `E2E_R_UI_21_…`~`E2E_R_UI_25_…`와 기존 E2E 전부 PASS. 경쟁 E2E 3개(`E2E_R_UI_24_…`)는 지연으로 실제로 겹친 상태에서 PASS. Visual 기준 이미지는 CI `update_snapshots`로 갱신한 것과 일치.

## 4. verify-linkshelf 안 읽음 여정

`features/unread-filter.md` 여정을 `drive/unread-filter.verify.ts`로 한 바퀴 돈다. 증거 경로를 PR에 적는다.

## 5. 고장 내기 (SC-003)

| 고장 | 잡아야 할 테스트 |
|---|---|
| 저장소 조건 `l.read = false`를 `l.read = true`로(읽은 것만) | `R_LINK_22_…`, `R_LINK_23_…` 저장소·통합 테스트 FAIL |
| 컨트롤러가 `unread`를 `Boolean`으로 받기 | `R_LINK_24_rejects_other_boolean_spellings_400` FAIL(`yes`·`on`·`1`이 200) |
| 화면의 새 요청이 `requested*` 대신 직전에 성공한 값(`shown*`)을 읽기 | `E2E_R_UI_24_` 3개 모두 FAIL |
| 저장 뒤 `unread`를 끄지 않기 | `E2E_R_UI_23_…` FAIL |

각 고장을 넣고 FAIL을 확인한 뒤 되돌려 PASS를 확인한다. 고장은 커밋하지 않는다.
