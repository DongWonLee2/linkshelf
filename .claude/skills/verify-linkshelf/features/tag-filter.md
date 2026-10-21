# 태그 필터

사용자가 태그를 입력해 적용하면 대소문자를 무시하고 그 태그가 붙은 링크만 보인다. 해제하면 전체 목록 첫 쪽으로 돌아간다. 잘못된 태그는 오류를 보이고 목록은 그대로다.

## 하위 기능

- `filter-match`: 앞뒤 공백과 대소문자를 무시하고 일치하는 링크만 보인다. `tag-filter-active`에 현재 필터가 보인다.
- `filter-clear`: `tag-filter-clear`로 해제하면 필터 표시가 사라지고 다른 링크도 다시 보인다.
- `filter-error`: 31자 이상 태그는 `tag-filter-error`를 보이고, 목록과 현재 필터는 그대로다.
- `filter-blank`: 빈 입력으로 적용하면 해제와 같다(E2E가 다룬다).

## 들어가는 법(사용자 시점)

- "저장한 링크" 영역의 "태그로 거르기" 입력과 `적용`·해제 버튼.

## Playwright로 조작하기

전제:

- `doctor.sh` OK. 고유 태그 `Vf<id>`가 붙은 링크 하나, `etc<id>`가 붙은 링크 하나를 공개 API로 저장한다.

- **일치.** ` VF<id> `(공백·대문자)를 넣고 적용한다. `link-item`이 정확히 1개이고 그 항목이 `Vf<id>` 링크다. `tag-filter-active`에 태그가 보인다. 증거 `1-filtered.png`.
- **오류.** 거른 상태에서 `x` 31개를 넣고 적용한다. `tag-filter-error`가 보이고, 목록은 여전히 `Vf<id>` 링크 1개이며 `tag-filter-active`도 그대로다. 증거 `2-error-keeps-list.png`.
- **해제.** `tag-filter-clear`를 누른다. `tag-filter-active`가 숨고 `etc<id>` 링크가 보인다. 증거 `3-cleared.png`.
- 실행: `$S/drive.sh tag-filter`.

## 함정

- 태그는 고유값으로 만든다. `Java` 같은 흔한 태그는 다른 여정의 데이터와 섞여 개수 단언이 깨진다.
- 서버는 태그를 대소문자 무시로 비교하지만 표시는 저장한 표기 그대로다. 표시 문자열을 비교할 때는 대소문자 무시 정규식을 쓴다.
