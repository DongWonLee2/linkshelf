# Feature Map — linkshelf 첫 화면 (MVP-3)

> 화면 코드(`frontend/src/`)와 E2E 코드(`frontend/e2e/`) 사이의 **계약**이다. 둘은 이 표의 `data-testid`만으로 서로를 찾는다. testid를 바꾸거나 지우려면 이 문서를 먼저 고치는 PR을 낸다. 화면 범위·문구의 세부는 spec 003(`specs/003-first-screen/`)에서 정한다.
>
> 기준: PRD-01~04(백엔드 완료, spec 001·002), API 계약 [save-link](../specs/001-save-link/contracts/save-link.md), [browse-links](../specs/002-browse-links/contracts/browse-links.md). 화면 때문에 API를 바꾸지 않는다.

## 화면 요소 ↔ API ↔ E2E

| PRD | 화면 요소 | `data-testid` | API | E2E 테스트 이름 |
|---|---|---|---|---|
| — | 앱 껍데기 | `app-root` | — | `E2E_SMOKE` (있음) |
| PRD-01 저장 | 저장 폼 | `link-form` | `POST /api/links` | `E2E_R_UI_05_saves_link_and_shows_it_first` |
| | URL 입력 | `link-url-input` | | |
| | 메모 입력 | `link-memo-input` | | |
| | 태그 입력(쉼표로 구분) | `link-tags-input` | | |
| | 저장 버튼 | `link-save-button` | | |
| | 입력 오류 표시(항목마다 하나, 속성 `data-field`=`url`·`memo`·`tags`) | `link-form-error` | 400 `errors[].field` | `E2E_R_UI_06_shows_field_errors`, `E2E_R_UI_06_shows_tags_error` |
| | 저장 성공 표시(저장 후 폼은 비운다) | `link-form-saved` | 201 | `E2E_R_UI_05_saves_link_and_shows_it_first` |
| | 중복 안내 | `link-form-duplicate` | 409 | `E2E_R_UI_06_shows_duplicate_notice` |
| PRD-02 목록 | 목록 | `link-list` | `GET /api/links?page&size` | `E2E_R_UI_01_lists_newest_first_with_fields` |
| | 목록 항목(속성 `data-link-id`) | `link-item` | | |
| | 항목 URL(링크) | `link-item-url` | | |
| | 항목 메모 | `link-item-memo` | | |
| | 항목 태그 하나 | `link-item-tag` | | |
| | 항목 저장 시각 | `link-item-created-at` | `createdAt` | |
| | 빈 목록 안내 | `link-list-empty` | `items: []` | `E2E_R_UI_03_shows_empty_state` |
| | 이전 쪽 / 다음 쪽 | `link-list-prev` / `link-list-next` | `page`, `hasNext` | `E2E_R_UI_02_pages_through_links` |
| | 쪽 정보(예: 1 / 3). 빈 목록(`totalPages` 0)이면 표시하지 않는다 | `link-list-page-info` | `page`, `totalPages` | |
| | 목록 불러오기 실패(400·네트워크) | `list-error` | `GET` 실패 | `E2E_R_UI_03_shows_list_error_when_api_fails`, `E2E_R_UI_03_shows_list_error_when_api_returns_400` |
| PRD-03 태그 필터 | 필터 입력 | `tag-filter-input` | `GET /api/links?tag=` | `E2E_R_UI_07_filters_by_tag_ignoring_case` |
| | 적용 / 해제 버튼 | `tag-filter-apply` / `tag-filter-clear` | | |
| | 현재 필터 표시 | `tag-filter-active` | | |
| | 필터 입력 오류(31자 이상 등 400) | `tag-filter-error` | 400 `field=tag` | `E2E_R_UI_08_shows_filter_error_and_keeps_list` |
| PRD-04 읽음 | 항목 읽음 체크박스 | `link-item-read-toggle` | `PUT /api/links/{id}/read` | `E2E_R_UI_09_marks_read_and_keeps_after_reload` |
| | 읽음 변경 실패(404·400·네트워크, 체크 상태는 되돌림) | `link-item-error` | `PUT` 실패 | `E2E_R_UI_10_reverts_checkbox_when_marking_fails` |

## 규칙

- testid는 위 표에 있는 것만 쓴다. E2E는 텍스트·CSS 클래스가 아니라 testid와 역할(role)로 요소를 찾는다.
- 목록 항목은 하나의 `link-item` 안에 `link-item-*`를 둔다. 항목을 고를 때는 `data-link-id` 또는 `link-item-url` 텍스트를 쓴다.
- 읽음 체크박스는 `checked` 상태가 API의 `read`와 같아야 한다.
- 필터 입력이 비었거나 공백뿐일 때 적용을 누르면 필터 해제와 같게 처리하고 요청에 `tag`를 보내지 않는다(빈 tag는 API에서 400).
- **E2E 데이터 격리:** E2E는 하나의 메모리 DB를 공유한다. 각 테스트는 자기만 쓰는 고유 URL(예: 테스트 이름 + 시각)로 저장하고, DB가 비어 있다고 가정하지 않는다. 빈 목록 상태(`link-list-empty`)는 그 상태를 만들 수 있는 조건(예: 없는 태그로 거르기)에서 확인한다.
- 이 표는 요소마다 대표 E2E만 적는다. R-UI ↔ E2E 전체 연결은 [spec 003 plan](../specs/003-first-screen/plan.md#e2e-테스트-이름--r-ui-sc-002)이 기준이다.
- Visual QA(스크린샷 비교)는 같은 testid로 화면 상태를 맞춘 뒤 찍는다. 기준 이미지 방법은 spec 003 plan에서 정한다.
