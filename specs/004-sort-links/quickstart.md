# Quickstart: 링크 목록 정렬 옵션 검증

계약은 [contracts/sort-links.md](contracts/sort-links.md), 상태는 [data-model.md](data-model.md)를 본다. 결과는 `PASS / FAIL / SKIP / NOT_RUN`으로만 적는다.

## 1. 백엔드 테스트 (R-LINK-19~21)

```sh
cd backend && ./gradlew test
```

기대: 전체 PASS. 새 테스트 이름은 `R_LINK_19_…`·`R_LINK_20_…`·`R_LINK_21_…`. 기존 R-LINK-09~14 테스트는 바꾸지 않고 PASS(SC-002).

## 2. 실제 요청/응답

`../../.claude/skills/verify-linkshelf/scripts/launch.sh`로 띄운 뒤(메모리 H2), 링크 A·B·C를 차례로 저장하고:

| 요청 | 기대 |
|---|---|
| `GET /api/links` | 200, C·B·A |
| `GET /api/links?order=oldest` | 200, A·B·C |
| `GET /api/links?order=oldest&tag=java` | 200, 태그가 `java`인 것만 오래된순 |
| `GET /api/links?order=` | 400, `errors[0].field = "order"` |
| `GET /api/links?order=bad&page=-1` | 400, `errors`에 `order`와 `page` 둘 다 |

## 3. 화면 E2E·Visual (R-UI-18~20)

```sh
cd frontend && npx playwright test
```

기대: 새 `E2E_R_UI_18_…`~`E2E_R_UI_20_…`와 기존 E2E 전부 PASS. Visual 기준 이미지는 CI `update_snapshots`로 갱신한 것과 일치.

## 4. verify-linkshelf 정렬 여정

`features/sort-links.md` 여정을 `drive/sort-links.verify.ts`로 한 바퀴 돈다. 증거 경로를 PR에 적는다.

## 5. 고장 내기 (SC-003)

| 고장 | 잡아야 할 테스트 |
|---|---|
| `OLDEST_FIRST`의 `createdAt`을 `desc`로 | `R_LINK_19_oldest_orders_by_created_at_asc_then_id_asc` 등 FAIL |
| `OLDEST_FIRST`의 `id`를 `desc`로(동률) | `R_LINK_19_oldest_orders_by_created_at_asc_then_id_asc`, `R_LINK_20_oldest_pages_without_overlap_or_gap` FAIL |
| 화면에서 정렬 변경 시 `order`를 보내지 않기 | `E2E_R_UI_19_…` FAIL |
| 저장 뒤 `order`를 되돌리지 않기 | `E2E_R_UI_20_…` FAIL |

각 고장을 넣고 FAIL을 확인한 뒤 되돌려 PASS를 확인한다. 고장은 커밋하지 않는다.
