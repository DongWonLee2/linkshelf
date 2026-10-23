# 안 읽은 것만 보기

사용자가 목록 위의 "안 읽은 것만" 체크박스를 켜면 목록이 첫 쪽부터 안 읽은 링크만 다시 보인다. 태그 필터와 정렬은 그대로 남는다. 링크를 새로 저장하면 체크박스가 꺼지고 새 링크가 맨 위에 보인다.

## 하위 기능

- `unread-only`: `link-unread-only-toggle`을 켜면 첫 쪽부터 읽음으로 표시한 링크가 빠진다. 처음 열거나 새로 고치면 꺼져 있다(E2E `E2E_R_UI_21_…`, `E2E_R_UI_22_…`).
- `unread-keeps-tag-and-order`: 거른 상태·오래된순에서 켜면 `tag-filter-active`와 `link-sort-select`가 그대로이고 거른 결과 중 안 읽은 링크만 그 순서로 보인다(E2E `E2E_R_UI_22_…`).
- `unread-off-on-save`: 켠 상태에서 링크를 저장하면 체크박스가 꺼지고 새 링크가 첫 항목이다(E2E `E2E_R_UI_23_…`).

## 들어가는 법(사용자 시점)

- "저장한 링크" 영역에서 "정렬" 선택 상자 옆의 "안 읽은 것만" 체크박스. 기본은 꺼짐이다.

## Playwright로 조작하기

전제:

- `doctor.sh` OK. 백엔드가 `GET /api/links?unread=`를 안다(spec 005 R-LINK-22). 고유 태그 `un<id>`가 붙은 링크 3개(A, B, C 순)를 공개 API로 저장하고 B를 읽음으로 표시한다. 태그 없는 안 읽은 링크도 하나 저장한다.

- **기본값.** 화면을 열면 `link-unread-only-toggle`이 꺼져 있다.
- **안 읽은 것만.** 태그 `un<id>`로 거르고 `link-sort-select`에서 `oldest`를 고른 뒤 체크박스를 켠다. `link-item`이 2개이고 순서가 A, C다. B와 태그 없는 링크는 보이지 않는다. 증거 `1-unread-oldest.png`, `1-unread-list.aria.txt`.
- **조건 유지.** 같은 화면에서 `tag-filter-active`와 `link-sort-select`(`oldest`)가 그대로다. 화면과 다른 경로로 `GET /api/links?unread=true&order=oldest&tag=un<id>` 응답이 A, C 순인지 확인한다. 증거 `2-api-unread.json`.
- **저장하면 꺼짐.** 폼으로 새 링크 D를 저장한다. `link-form-saved`가 보이고 체크박스가 꺼지며 첫 항목이 D다. 새로 고친 뒤에도 꺼져 있다. 증거 `3-saved-off.png`, `4-after-reload.png`.
- 실행: `$S/drive.sh unread-filter`.

## 함정

- 공유 DB라서 안 읽은 링크 전체는 다른 여정이 저장한 링크를 포함한다. 순서·개수는 반드시 고유 태그로 거른 목록에서 본다.
- 체크박스 상태는 주소에 남지 않는다. 새로 고침이나 `page.goto('/')`는 끈 상태로 되돌린다.
- 켠 상태에서 항목을 읽음으로 바꿔도 그 항목은 다시 불러올 때까지 남는다(R-UI-25). 바로 사라지기를 기대하지 않는다.
