# 읽음 표시

사용자가 항목의 체크박스를 누르면 읽음으로 바뀌고, 새로고침해도 유지된다. 실패하면 체크가 되돌아가고 오류가 보인다.

## 하위 기능

- `read-check`: 체크박스를 누르면 `PUT /api/links/{id}/read`가 200이고 체크된다.
- `read-persist`: 새로고침 뒤에도 체크되어 있고 API의 `read`가 `true`다.
- `read-revert`: 실패하면 체크가 되돌아가고 `link-item-error`가 보인다(E2E `E2E_R_UI_10_reverts_checkbox_when_marking_fails`가 다룬다. 실패를 만들려면 요청 가로채기가 필요해서 검증 여정에는 넣지 않았다).

## 들어가는 법(사용자 시점)

- 목록 각 항목 왼쪽의 체크박스(`<URL> 읽음`).

## Playwright로 조작하기

전제:

- `doctor.sh` OK. 읽지 않은 링크 하나를 공개 API로 저장한다.

- **체크.** 그 항목(`data-link-id`)의 `link-item-read-toggle`을 누른다. PUT 응답 200, 체크됨. 증거 `1-checked.png`.
- **새로고침.** 다시 불러와도 체크됨. 증거 `2-after-reload.png`.
- **다른 경로로 확인.** `GET /api/links?page=0&size=100`에서 그 id의 `read`가 `true`. 증거 `3-api-list.json`.
- 실행: `$S/drive.sh mark-read`.

## 함정

- 항목은 위치가 아니라 `data-link-id`로 고른다. 다른 여정이 링크를 더하면 순서가 바뀐다.
- 체크 표시는 응답 전에 바뀔 수 있다. PUT 응답을 기다린 뒤 상태를 단언한다.
