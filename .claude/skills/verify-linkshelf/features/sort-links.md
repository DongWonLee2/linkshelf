# 목록 정렬

사용자가 목록 위의 "정렬" 선택 상자에서 "오래된순"을 고르면 목록이 첫 쪽부터 오래전에 저장한 링크 순서로 다시 보인다. 태그 필터는 그대로 남는다. 링크를 새로 저장하면 정렬이 "최신순"으로 돌아가 새 링크가 맨 위에 보인다.

## 하위 기능

- `sort-oldest`: `link-sort-select`를 오래된순으로 바꾸면 첫 쪽부터 가장 먼저 저장한 링크가 첫 항목이다. 처음 열거나 새로 고치면 최신순이다(E2E `E2E_R_UI_18_…`).
- `sort-keeps-filter`: 거른 상태에서 정렬을 바꾸면 `tag-filter-active`가 그대로이고 거른 결과만 새 순서로 보인다. 쪽 넘김·필터 해제도 정렬을 유지한다(E2E `E2E_R_UI_19_…`).
- `sort-reset-on-save`: 오래된순으로 보는 중에 링크를 저장하면 선택 상자가 최신순으로 바뀌고 새 링크가 첫 항목이다(E2E `E2E_R_UI_20_…`).

## 들어가는 법(사용자 시점)

- "저장한 링크" 영역에서 "태그로 거르기" 옆의 "정렬" 선택 상자. 선택지는 "최신순"(기본)과 "오래된순"이다.

## Playwright로 조작하기

전제:

- `doctor.sh` OK. 백엔드가 `GET /api/links?order=`를 안다(spec 004 R-LINK-19). 고유 태그 `so<id>`가 붙은 링크 3개(A, B, C 순)와 태그 없는 링크 하나를 공개 API로 저장한다.

- **기본값.** 화면을 열면 `link-sort-select` 값이 `newest`다.
- **오래된순.** 태그 `so<id>`로 거른 뒤 `link-sort-select`에서 `oldest`를 고른다. `link-item`이 3개이고 순서가 A, B, C다. 쪽 정보는 1쪽이다. 증거 `1-oldest.png`, `1-oldest-list.aria.txt`.
- **필터 유지.** 같은 화면에서 `tag-filter-active`가 그대로 보이고 태그 없는 링크는 보이지 않는다. 화면과 다른 경로로 `GET /api/links?order=oldest&tag=so<id>` 응답이 A, B, C 순인지 확인한다. 증거 `2-api-oldest.json`.
- **저장하면 최신순.** 폼으로 새 링크 D를 저장한다. `link-form-saved`가 보이고 `link-sort-select` 값이 `newest`, 첫 항목이 D다. 새로 고친 뒤에도 값이 `newest`다. 증거 `3-saved-newest.png`, `4-after-reload.png`.
- 실행: `$S/drive.sh sort-links`.

## 함정

- 공유 DB라서 오래된순의 첫 항목은 다른 여정이 먼저 저장한 링크다. 순서·개수는 반드시 고유 태그로 거른 목록에서 본다.
- 정렬은 주소에 남지 않는다. 새로 고침이나 `page.goto('/')`는 최신순으로 되돌린다. 오래된순 상태를 확인하려면 화면을 다시 열지 말고 이어서 조작한다.
- 백엔드가 `order`를 모르면 값을 무시하고 최신순으로 답한다. 그래서 오래된순 단언이 실패하면 화면보다 먼저 백엔드 빌드 revision을 `doctor.sh`로 확인한다.
