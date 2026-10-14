# Feature Map — linkshelf 첫 화면 (MVP-3)

> 화면 코드(`frontend/src/`)와 E2E 코드(`frontend/e2e/`) 사이의 **계약**이다. 둘은 이 표의 `data-testid`만으로 서로를 찾는다. testid를 바꾸거나 지우려면 이 문서를 먼저 고치는 PR을 낸다. 화면 범위·문구의 세부는 spec 003(`specs/003-first-screen/`)에서 정한다.
>
> 기준: PRD-01~04(백엔드 완료, spec 001·002), API 계약 [save-link](../specs/001-save-link/contracts/save-link.md), [browse-links](../specs/002-browse-links/contracts/browse-links.md). 화면 때문에 API를 바꾸지 않는다.

## 화면 요소 ↔ API ↔ E2E

| PRD | 화면 요소 | `data-testid` | API | E2E 테스트 이름(예정) |
|---|---|---|---|---|
| — | 앱 껍데기 | `app-root` | — | `E2E_SMOKE` (있음) |
| PRD-01 저장 | 저장 폼 | `link-form` | `POST /api/links` | `E2E_PRD01_saves_link_and_shows_it_first` |
| | URL 입력 | `link-url-input` | | |
| | 메모 입력 | `link-memo-input` | | |
| | 태그 입력(쉼표로 구분) | `link-tags-input` | | |
| | 저장 버튼 | `link-save-button` | | |
| | 입력 오류 표시(항목별) | `link-form-error` | 400 `errors[]` | `E2E_PRD01_shows_validation_errors` |
| | 중복 안내 | `link-form-duplicate` | 409 | `E2E_PRD01_duplicate_shows_conflict` |
| PRD-02 목록 | 목록 | `link-list` | `GET /api/links?page&size` | `E2E_PRD02_lists_newest_first` |
| | 목록 항목(속성 `data-link-id`) | `link-item` | | |
| | 항목 URL(링크) | `link-item-url` | | |
| | 항목 메모 | `link-item-memo` | | |
| | 항목 태그 하나 | `link-item-tag` | | |
| | 빈 목록 안내 | `link-list-empty` | `items: []` | `E2E_PRD02_shows_empty_state` |
| | 이전 쪽 / 다음 쪽 | `link-list-prev` / `link-list-next` | `page`, `hasNext` | `E2E_PRD02_pages_through_links` |
| | 쪽 정보(예: 1 / 3) | `link-list-page-info` | `page`, `totalPages` | |
| PRD-03 태그 필터 | 필터 입력 | `tag-filter-input` | `GET /api/links?tag=` | `E2E_PRD03_filters_by_tag_ignoring_case` |
| | 적용 / 해제 버튼 | `tag-filter-apply` / `tag-filter-clear` | | |
| | 현재 필터 표시 | `tag-filter-active` | | |
| PRD-04 읽음 | 항목 읽음 체크박스 | `link-item-read-toggle` | `PUT /api/links/{id}/read` | `E2E_PRD04_marks_read_and_keeps_after_reload` |

## 규칙

- testid는 위 표에 있는 것만 쓴다. E2E는 텍스트·CSS 클래스가 아니라 testid와 역할(role)로 요소를 찾는다.
- 목록 항목은 하나의 `link-item` 안에 `link-item-*`를 둔다. 항목을 고를 때는 `data-link-id` 또는 `link-item-url` 텍스트를 쓴다.
- 읽음 체크박스는 `checked` 상태가 API의 `read`와 같아야 한다.
- Visual QA(스크린샷 비교)는 같은 testid로 화면 상태를 맞춘 뒤 찍는다. 기준 이미지 방법은 spec 003 plan에서 정한다.
