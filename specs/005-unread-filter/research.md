# Research: 안 읽은 것만 보기 필터

## R1. 안 읽음 조건을 받는 타입과 검증

- **Decision**: `unread`를 `@RequestParam(required = false) @Pattern(regexp = "true|false", message = "true 또는 false여야 합니다") String unread`로 받는다. 값이 없으면(null) 거르지 않는다. `defaultValue`는 쓰지 않는다. 컨트롤러에서 `"true".equals(unread)`로 boolean으로 바꿔 서비스에 넘긴다.
- **Rationale**:
  - `Boolean`/`boolean`으로 바인딩하면 Spring 변환기가 `yes`·`on`·`1`도 참으로 바꾼다(사용자 보충, grilling Q8). R-LINK-24("다른 참·거짓 표기는 거부")를 지키지 못한다.
  - 타입 변환 실패는 `MethodArgumentTypeMismatchException`이라 다른 쿼리 오류와 함께 알리지 못한다(spec 004 research R1과 같은 원인).
  - 문자열 + `@Pattern`이면 page·size·tag·order와 같은 메서드 검증으로 모여 기존 400 `errors` 형식(`field=unread`)으로 나간다. `ApiExceptionHandler`는 바꾸지 않는다.
  - 빈 값(`unread=`)은 `""`로 들어와 `@Pattern`에 걸린다. 두 번 보내면(`unread=true&unread=false`) 값이 `"true,false"`로 합쳐져 걸린다(order와 같은 경로).
- **확인 방법**: `unread=yes`·`on`·`1`·`TRUE`·` true`·빈 값·두 번 보냄이 400인지 Red 테스트로 먼저 고정한다.
- **Alternatives considered**: `Boolean` + 사용자 정의 변환기 — 전역 변환을 바꾸거나 오류를 따로 모아야 해 비용이 크다. `read=false` — 세 상태처럼 보여 범위를 넘는다(grilling Q7).

## R2. 저장소 쿼리

- **Decision**: 기존 `findByTag`·`countByTag` 옆에 안 읽음 조건을 받는 쿼리 두 쌍을 더하지 않고, 태그(null 허용)와 안 읽음을 함께 받는 쿼리 한 쌍 `findFiltered(tag, unreadOnly, pageable)`·`countFiltered(tag, unreadOnly)`로 태그 쿼리를 바꾼다. 조건 없는 요청은 기존 `findPage(Sort, …)`(`findAll`) 경로를 그대로 쓴다.
  - JPQL: `where (:tag is null or exists (select 1 from l.tags t where lower(t) = :tag)) and (:unreadOnly = false or l.read = false)`
  - 저장소 기본 메서드 `findPageFiltered(String tag, boolean unreadOnly, Sort, page, size)`는 기존 `sortedPage`(큰 쪽 번호 → 빈 쪽)를 재사용한다.
- **Rationale**: 조합이 4가지(없음·태그·안 읽음·둘 다)라 쿼리를 따로 두면 4벌이 된다. 조건 없는 경로를 `findAll`로 남겨 SC-002(기존 응답 그대로)를 지킨다. 정렬은 기존 `Sort` 인자 그대로(R-LINK-23).
- **확인 방법**: H2에서 `:tag is null` 비교가 동작하는지는 `@DataJpaTest` Red 테스트(태그 없음 + 안 읽음, 태그 + 안 읽음)로 확인한다. 동작하지 않으면 태그 있는/없는 두 쿼리로 나눈다(대안 A).
- **Alternatives considered**: (A) 조합마다 쿼리 — 중복. (B) JPA Specification(`JpaSpecificationExecutor`) — 새 패턴이 들어오고 기존 큰 쪽 번호 처리와 따로 맞춰야 한다.

## R3. 인덱스

- **Decision**: 추가하지 않는다. 스키마 변경 없음. 사용자 1명, 수천 건(spec Assumptions). `is_read` 컬럼은 spec 002에서 이미 있다.

## R4. 화면 상태와 경쟁(R-UI-24)

- **Decision**: `App`에 `unread` 상태(기본 `false`)를 더한다. 체크박스는 사용자가 고른 값을 바로 보인다(정렬과 같음).
  - 요청을 만드는 모든 경로(체크박스, 태그 적용·해제, 정렬 변경, 쪽 넘김, 저장)는 "마지막으로 고른 값"을 담은 요청 조건 묶음(T003 prefactor에서 `requestedTag`와 정렬을 `requested` ref 하나로 모으고, T025에서 `unread`를 더함)에서 바뀌지 않는 조건을 읽는다. 화면 렌더 시점의 state나 "보인 값"(`shownTag`)을 읽지 않는다.
  - `load(page, tag, order, unread, fromFilter)`가 세 ref를 요청 직전에 갱신한다.
  - 저장 성공: 태그 해제, `newest`, `unread=false`로 다시 불러온다(R-UI-23).
  - 태그 400 경로(`requestedTag.current = shownTag.current`)는 바꾸지 않는다. 그 경로와 토글이 겹치는 경우는 범위 밖(#46).
- **Rationale**: 정렬 기능의 K1(정렬이 진행 중인 tag 요청과 경쟁)은 "보인 값"을 읽어서 생겼다(PR #44 리뷰 1·2). 세 조건을 같은 규칙으로 읽으면 조합이 늘어도 경쟁 규칙이 하나다.
- **고장 내기**: `requested*` 대신 직전에 성공한 값(`shown*`)을 읽게 바꾸면 R-UI-24 E2E 3개가 FAIL해야 한다(SC-003, 사용자 보충).

## R5. 경쟁 E2E를 만드는 방법

- **Decision**: `sort.spec.ts`의 `delayListRequests`처럼 `page.route`로 특정 목록 요청만 800ms 늦춘 뒤, 기다리지 않고 다음 조작을 한다. 마지막 요청의 쿼리(`tag`, `order`, `unread`)와 화면 결과를 함께 단언한다. 대기(`waitForResponse`)를 넣어 경쟁을 피하지 않는다(registry 2026-10-22 "통과시키려고 대기를 넣은 E2E는 그 대기가 가린 경쟁을 따로 테스트한다").
- **Rationale**: 같은 지연 방식이 이미 R-UI-19 경쟁 테스트에 있다.

## R6. 쪽 밀림 테스트(R-UI-25, clarify A)

- **Decision**: 쪽 크기를 줄일 수 없으므로(화면은 기본 20) 고유 태그로 안 읽은 링크 21개를 저장해 태그 + 안 읽음으로 2쪽을 만든다. 1쪽 항목 하나를 읽음으로 바꾸고 다음 쪽으로 가면, 2쪽에는 원래 2쪽의 첫 항목(21번째)이 아니라 빈 쪽이 보인다(원래 21번째가 1쪽으로 당겨짐) → 빈 목록 안내, 쪽 넘김 숨김. 체크박스를 껐다 켜면 그 항목이 1쪽에 보인다.
- **Rationale**: 21개면 밀림과 clarify A(빈 쪽에서 쪽 넘김 숨김)를 한 테스트로 함께 고정한다. 데이터 격리(features/README)는 고유 태그로 지킨다.

## R7. Visual QA 기준 이미지

- **Decision**: 체크박스 때문에 바뀌는 기준 이미지 3장은 CI `update_snapshots`로 다시 만들어 같은 PR에 커밋한다(spec 004 research R5와 같음).
