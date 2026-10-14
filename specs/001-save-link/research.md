# Research: 링크 저장 (001-save-link)

- 작성: 2026-10-14 · 기준 SHA `8c26558`
- 실측 환경: OpenJDK 21.0.12, H2 2.4.240(Gradle 캐시의 프로젝트 의존성 jar), 실측 코드는 저장소 밖 scratchpad에서 실행

## R1. 길이 세는 단위와 DB 컬럼 크기 (R-LINK-08, 02/03/05)

- **Decision**: 앱 검증은 **코드 포인트**(`String.codePointCount`)로 센다. DB 컬럼은 **UTF-16 단위로 한도 × 2**로 잡는다.
  - 메모 `VARCHAR(1000)`, 태그 `VARCHAR(60)`, URL `VARCHAR(4096)`
- **Rationale**:
  - 사용자 결정 Q4는 "코드 포인트"다.
  - 실측: H2 2.4.240은 VARCHAR 길이를 UTF-16 단위로 센다. `varchar(3)`에 `가나다`(3단위)는 들어갔고, `😀😀`(2코드포인트, 4단위)는 SQLState 22001로 거부됐다.
  - 코드 포인트 하나는 UTF-16으로 최대 2단위다. 따라서 한도 × 2이면 앱 검증을 통과한 값은 반드시 DB에 들어간다. 앱 검증이 실제 관문이고, DB 크기는 안전망이다.
- **Alternatives considered**:
  - UTF-16 길이(`@Size` 기본, `VARCHAR(500)`): 가장 단순하지만 😀를 2자로 센다. 사용자가 고르지 않았다.
  - 자소 클러스터: 한 글자가 임의로 길 수 있어 컬럼 크기를 한도로 정할 수 없다. 사용자가 고르지 않았다.
- **주의**: Bean Validation 기본 `@Size`는 UTF-16으로 센다. 그래서 길이 검증에는 `@Size`를 쓰지 않고 코드 포인트를 세는 커스텀 제약을 쓴다(R4).

## R2. URL 검증과 정규화 (R-LINK-02, R-LINK-04)

- **Decision**:
  - 파싱은 `java.net.URI`로 한다(새 의존성 없음).
  - 앞뒤 공백을 `strip()`으로 지운 뒤 다음을 모두 만족해야 한다: 코드 포인트 2,048 이하, 파싱 성공, 스킴이 `http`/`https`(대소문자 무시), `getHost() != null`.
  - 정규화된 문자열은 **raw 구성요소를 이어 붙여** 다시 만든다.
    - `scheme.toLowerCase(ROOT) + "://" + [rawUserInfo + "@"] + host.toLowerCase(ROOT) + [":" + port(기본 포트가 아닐 때)] + rawPath + ["?" + rawQuery] + ["#" + rawFragment]`
- **Rationale**: 실측한 `java.net.URI` 결과는 다음과 같다.

  | 입력 | 결과 | 처리 |
  |---|---|---|
  | `HTTPS://Example.COM:443/a` | scheme `HTTPS`, host `Example.COM`, port 443 | → `https://example.com/a` |
  | `http://example.com:443/a` | port 443 | `http`의 기본 포트가 아니므로 유지 |
  | `https://example.com/a%20b` | rawPath `/a%20b` | 퍼센트 인코딩 그대로 |
  | `https://example.com/경로/😀` | rawPath `/경로/😀` | 허용, 그대로 |
  | `http://` | 문법 오류 "Expected authority" | 거부 |
  | `not a url` | 문법 오류 "Illegal character" | 거부 |
  | `ftp://example.com` | scheme `ftp` | 스킴 규칙으로 거부 |
  | `mailto:a@b.c`, `https:///nohost` | host null | 거부 |
  | `https://[::1]:8443/a` | host `[::1]` | 허용 |
  | **`https://한국.kr/a`** | **host null** | **거부 (A1 사용자 결정 (a), 2026-10-14). punycode는 허용** |

  - 여러 인자를 받는 `URI` 생성자는 `%`를 다시 인코딩한다(`%20` → `%2520`). 그래서 이 생성자 대신 raw 문자열을 이어 붙인다.
- **Alternatives considered**:
  - `URI.normalize()`: `.`/`..` 경로를 바꾼다. 사용자가 고른 "표준 동치만" 범위를 넘는다.
  - Apache Commons Validator `UrlValidator`: 새 의존성이라 constitution I에 걸린다. 필요하지 않다.

## R3. 중복 URL: DB 고유 제약 + 409 변환 (R-LINK-04, R-LINK-06)

- **Decision**:
  1. `link.normalized_url`에 이름 있는 고유 제약 `uk_link_normalized_url`을 둔다.
  2. 서비스는 `@Transactional` 안에서 `existsByNormalizedUrl`로 먼저 확인하고, 있으면 `DuplicateLinkException`을 던진다.
  3. 없으면 `saveAndFlush`로 바로 INSERT한다. 커밋 때가 아니라 서비스 안에서 제약 위반이 드러나게 하기 위해서다.
  4. `DataIntegrityViolationException`을 잡았을 때 원인 체인에 SQLState `23505`가 있고 메시지에 `UK_LINK_NORMALIZED_URL`(대소문자 무시)이 있으면 `DuplicateLinkException`으로 바꾼다. 다른 무결성 오류는 그대로 던진다(500).
  5. `@RestControllerAdvice`가 `DuplicateLinkException`을 409 ProblemDetail로 바꾼다.
- **Rationale**:
  - 사용자 지시: "중복 URL은 정규화된 URL 컬럼의 DB 고유 제약으로도 막고, 제약 위반을 409로 바꾸는 방식으로 설계해(동시 요청 대비)".
  - 실측: H2 위반은 SQLState `23505`이고, 메시지는 `Unique index or primary key violation: "PUBLIC.UK_LINK_NORMALIZED_URL INDEX PUBLIC.UK_LINK_NORMALIZED_URL_INDEX_2 ON PUBLIC.LINK(NORMALIZED_URL ...`이다. 이름이 대문자로 바뀌고 인덱스 접미사가 붙는다. 그래서 정확히 일치가 아니라 대소문자를 무시한 포함 여부로 판정한다.
  - 사전 확인(2번)은 흔한 경우를 빠르게 처리할 뿐이다. 정확성은 제약(4번)이 보장한다.
- **Alternatives considered**:
  - 제약만 쓰고 사전 확인 없음: 경로가 하나라 더 단순하다. 다만 "제약으로**도**"라는 지시와 런북 시퀀스(검증 → 중복 확인 → 저장)에 맞춰 사전 확인을 둔다. 둘 다 테스트한다.
  - 모든 `DataIntegrityViolationException`을 409로 변환: 다른 제약 위반까지 "중복"으로 잘못 알리게 된다.
- **테스트 방법(동시성 대체)**:
  - ADR-0003은 동시성 테스트를 하지 않는다고 정했다.
  - 대신 경쟁 상황을 결정적으로 재현한다. `@MockitoSpyBean`으로 `existsByNormalizedUrl`이 `false`를 돌려주게 해 사전 확인을 통과시키고, 제약 → 409 경로를 검증한다.
  - 제약 자체는 JPA 슬라이스에서 같은 URL을 두 번 `saveAndFlush`해 확인한다.
- **위험**: Hibernate가 꺼낸 제약 이름의 형식은 버전마다 다를 수 있다. 그래서 Hibernate의 `getConstraintName()`에 기대지 않고 SQLState와 메시지로 판정한다. 첫 테스트에서 실제 예외 체인을 확인한다.

## R4. 입력 검증 방식 (ADR-0001: Bean Validation)

- **Decision**: 요청 record에 커스텀 Bean Validation 제약 3개를 둔다. 모두 `strip()` 후 코드 포인트로 센다.
  - `url`: `@LinkUrl`. 필수, ≤ 2,048, http/https, host 필수.
  - `memo`: `@MaxCodePoints(500)`. null·공백이면 통과.
  - `tags`: `@LinkTags(maxCount = 10, maxLength = 30)`. 원소 null·공백이면 거부, 원소 ≤ 30. **대소문자 무시로 합친 뒤** 개수 ≤ 10.
  - 검증기와 서비스는 같은 순수 함수(`UrlNormalizer`, `TagNormalizer`, `Texts.codePoints`)를 쓴다.
- **Rationale**:
  - 태그 10개 한도는 "중복 합친 뒤 기준"이라 `@Size(max=10)`로는 틀린다. 예를 들어 원본 11개 중 대소문자 중복이 1개면 허용해야 한다.
  - `@NotBlank`는 `trim()` 기준이라 전각 공백(U+3000)을 공백으로 보지 않는다. 그래서 `strip()`을 쓰는 커스텀 제약을 둔다.
  - Bean Validation을 쓰므로 여러 항목 오류를 한 번에 모은다(spec 엣지 케이스).
- **Alternatives considered**:
  - 서비스 안에서 수작업으로 검증하고 오류를 모음: ADR-0001(Bean Validation)과 어긋난다.
  - 표준 `@Size`/`@NotBlank`/`@Pattern`: 위 두 문제가 생긴다.

## R5. 오류 응답 (ADR-0001: RFC 9457 ProblemDetail)

- **Decision**: `@RestControllerAdvice`가 `ResponseEntityExceptionHandler`를 상속한다.
  - `handleMethodArgumentNotValid`를 재정의해 `errors: [{field, message}]` 확장 필드를 붙인다.
  - `DuplicateLinkException`은 409 핸들러가 처리한다.
  - 깨진 JSON(`HttpMessageNotReadableException`)은 상속 기본 동작으로 400 ProblemDetail이 된다.
- **Rationale**: ADR-0001이 정한 형식이고, 새 의존성이 없다.

## R6. 태그 정규화 (R-LINK-05, Q5)

- **Decision**:
  - 각 원소에 `strip()`을 적용한다.
  - `toLowerCase(Locale.ROOT)`를 키로 `LinkedHashMap`에 넣되, 첫 값만 남긴다. 그러면 첫 표기와 첫 등장 순서가 유지된다.
- **한계(기록만)**: 단순 소문자화라 `ß`/`SS`처럼 유니코드 대소문자 접기가 필요한 경우는 같게 보지 않는다. 한글은 영향이 없다.

## R7. 저장 시각

- **Decision**: `Instant.now()`(UTC)를 쓰고, `TIMESTAMP(6) WITH TIME ZONE`에 저장한다. 테스트는 값이 있는지와 ISO-8601 `Z` 형식인지만 확인한다. `Clock` 주입은 필요해질 때 추가한다(최소 변경).
