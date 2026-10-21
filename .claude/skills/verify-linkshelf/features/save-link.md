# 링크 저장

사용자가 URL, 메모, 태그를 입력하고 저장하면 성공 표시가 뜨고 폼이 비며, 저장한 링크가 목록 맨 위에 나타난다. 새로고침해도 남아 있다.

## 하위 기능

- `save-form`: URL·메모·태그(쉼표 구분)를 입력하고 저장한다.
- `save-feedback`: `link-form-saved`가 보이고 세 입력이 빈다.
- `save-first`: 새 링크가 목록 첫 항목이고 메모·태그가 입력한 대로 보인다.
- `save-persist`: `GET /api/links`와 새로고침 뒤 화면에 같은 링크가 있다.

## 들어가는 법(사용자 시점)

- 첫 화면 위쪽 "링크 저장" 폼.

## Playwright로 조작하기

전제:

- `launch.sh` 뒤 `doctor.sh`가 OK.

- **입력.** URL 칸에 고유 URL, 메모 `검증 메모`, 태그 `Verify, 검증`을 넣는다. `link-url-input`, `link-memo-input`, `link-tags-input`. 증거 `1-filled.png`.
- **저장.** `link-save-button`을 누른다. `link-form-saved`가 보이고 `link-url-input` 값이 빈다.
- **목록 맨 위.** 첫 `link-item`의 `link-item-url`이 그 URL, `link-item-memo`가 `검증 메모`, `link-item-tag`가 `Verify`, `검증`. 증거 `2-saved.png`, `2-saved-list.aria.txt`.
- **다른 경로로 확인.** `GET /api/links?page=0&size=20`에 그 URL이 메모·태그와 함께 있다. 증거 `3-api-list.json`.
- **새로고침.** 다시 불러와도 첫 항목이 그 URL. 증거 `4-after-reload.png`.
- 실행: `$S/drive.sh save-link`.

## 함정

- 서버가 URL을 정규화한다(호스트 소문자, 빈 경로에 `/`). 비교할 URL은 `uniqueUrl()`처럼 이미 정규형인 소문자 경로로 만든다.
- 같은 URL을 두 번 저장하면 409(`link-form-duplicate`)라서 저장 여정이 실패한다. 매번 고유 URL을 쓴다.
- 저장 성공 표시는 다음 입력을 시작할 때까지 남는다. 사라지는 것을 기다리지 않는다.
