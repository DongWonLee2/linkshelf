# 목록 보기

사용자는 저장한 링크를 최신순으로 보고, 각 항목에서 URL·메모·태그·저장 시각을 읽고, 쪽을 넘긴다. 결과가 없으면 빈 목록 안내를 본다.

## 하위 기능

- `list-order`: 최신 저장이 위에 온다.
- `list-fields`: 메모, 태그, 저장 시각(서울 시간 `YYYY-MM-DD HH:mm`)이 보인다.
- `list-paging`: `link-list-page-info`가 보이고 `link-list-prev`·`link-list-next`로 넘긴다(E2E `E2E_R_UI_02_pages_through_links`가 다룬다).
- `list-empty`: 결과가 0건이면 `link-list-empty`가 보이고 쪽 정보는 숨는다.

## 들어가는 법(사용자 시점)

- 첫 화면 아래쪽 "저장한 링크" 영역. 페이지를 열면 바로 보인다.

## Playwright로 조작하기

전제:

- `doctor.sh` OK. 링크 두 개를 공개 API로 저장한다(오래된 것은 태그 `Java`, 새 것은 메모 `최신`).

- **순서.** 첫 `link-item`의 `data-link-id`가 새 링크, 두 번째가 오래된 링크.
- **필드.** 첫 항목 `link-item-memo`가 `최신`, `link-item-created-at`이 서울 시간 분 단위. 두 번째 항목 `link-item-tag`가 `Java`. 증거 `1-list.png`, `1-list.aria.txt`.
- **빈 목록.** 없는 태그로 거른다(`tag-filter-input` 입력 → `tag-filter-apply`). `link-list-empty`가 보이고 `link-list-page-info`는 숨는다. 증거 `2-empty.png`.
- 실행: `$S/drive.sh browse-links`.

## 함정

- DB를 공유하므로 "목록이 비었다"는 가정을 하지 않는다. 빈 상태는 없는 태그로 거를 때만 만든다.
- 두 링크를 너무 빨리 저장해도 순서는 id 역순이라 안정적이다. 대신 다른 여정이 사이에 저장하지 않도록 drive는 한 번에 하나씩 돈다(`workers: 1`).
