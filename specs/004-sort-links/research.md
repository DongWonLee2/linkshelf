# Research: 링크 목록 정렬 옵션

## R1. 정렬 값을 받는 타입과 검증

- **Decision**: `order`를 `@RequestParam(required = false) @Pattern(regexp = "newest|oldest", message = "newest 또는 oldest여야 합니다") String order`로 받는다. 값이 없으면(null) 서비스에서 최신순으로 본다. `defaultValue`는 쓰지 않는다.
- **Rationale**:
  - enum 타입으로 받으면 잘못된 값이 타입 변환(`MethodArgumentTypeMismatchException`)에서 먼저 실패한다. 그러면 `order=bad&page=-1`일 때 `order` 하나만 알려 주게 되어 R-LINK-21("함께 있으면 모두 알려 준다")을 지키지 못한다(spec 002 research R4의 한계와 같은 원인).
  - 문자열 + `@Pattern`은 page·size·tag와 같은 메서드 검증(`HandlerMethodValidationException`)으로 모여 기존 400 `errors` 형식으로 나간다. `ApiExceptionHandler`는 바꾸지 않는다.
  - `defaultValue = "newest"`를 쓰면 Spring이 빈 값(`order=`)도 기본값으로 바꿔 clarify Q3(빈 값은 400)를 지킬 수 없다. `defaultValue` 없이 받으면 빈 값은 `""`로 들어와 `@Pattern`에 걸린다. `tag=`가 400인 것과 같은 경로다(browse-links 계약 "빈 문자열이나 공백만 보내면 400").
  - `@Pattern`은 이미 쓰는 Jakarta Validation에 있다. 새 의존성이 없다.
- **확인 방법**: `order=`·`order=NEWEST`·`order= newest`가 400인지 Red 테스트로 먼저 확인한다(가정을 테스트로 고정).
- **Alternatives considered**:
  - enum + `MethodArgumentTypeMismatchException` 변환: 오류를 모두 알리지 못함(위).
  - Spring Data `Sort`/`sort=` 그대로: 아무 필드로나 정렬할 수 있고 내부 필드 이름이 계약에 드러난다(grilling에서 사용자가 제외).

## R2. 정렬을 저장소까지 전달하는 방법

- **Decision**: `LinkRepository`의 `NEWEST_FIRST` 옆에 `OLDEST_FIRST = Sort.by(asc("createdAt"), asc("id"))`를 두고, 기존 `findNewestFirst`·`findNewestFirstByTag`를 정렬을 인자로 받는 `findPage(Sort, page, size)`·`findPageByTag(String, Sort, page, size)`로 바꾼다. 컨트롤러 → 서비스는 내부 enum `LinkOrder { NEWEST, OLDEST }`로 넘긴다(문자열은 컨트롤러 경계에서 끝난다).
- **Rationale**: 큰 쪽 번호 처리(`offset > Integer.MAX_VALUE` → 빈 쪽, PR #16 리뷰)와 태그 쿼리를 그대로 재사용한다. 동률 처리는 두 방향 모두 `id`가 같은 방향이라 쪽 사이 겹침·빠짐이 없다.
- **Alternatives considered**: 오래된순 전용 메서드 2개 추가 — 같은 쿼리가 4벌이 되어 중복.

## R3. 인덱스

- **Decision**: 추가하지 않는다. DB 스키마 변경 없음.
- **Rationale**: 기존 최신순도 인덱스 없이 정렬한다. 사용자 1명, 수천 건(spec Assumptions). 방향만 바뀌므로 비용이 같다.

## R4. 화면 상태와 저장 뒤 정렬

- **Decision**: `App`에 `order` 상태(기본 `newest`)를 더하고 `load(page, tag, order, …)`로 넘긴다. 선택 상자는 목록 영역(`.browse`)의 필터 옆에 둔다.
  - 정렬 변경: `load(0, tag, newOrder, false)` — 필터 유지, 첫 쪽. 쪽 넘김이 아니므로 스크롤·포커스 이동 없음(R-UI-19).
  - 쪽 넘김·필터 적용/해제: 현재 `order`를 그대로 넘긴다.
  - 저장 성공(R-UI-05): 필터 해제와 함께 `order`를 `newest`로 되돌린다(R-UI-20).
  - 선택 상자 값은 요청이 성공했을 때의 `order`가 아니라 사용자가 고른 값을 바로 보인다. 실패하면 `list-error`(spec Edge Cases).
- **Rationale**: `tag`와 같은 패턴이라 코드가 한 곳에서 읽힌다. 새 라이브러리·라우팅 없음.

## R5. Visual QA 기준 이미지

- **Decision**: 선택 상자 때문에 바뀌는 기준 이미지는 CI `workflow_dispatch`의 `update_snapshots`로 다시 만들어 같은 PR에 커밋한다(spec 003 research R1, PR #29와 같은 방법).
