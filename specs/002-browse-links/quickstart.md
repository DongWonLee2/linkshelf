# Quickstart: 링크 목록 조회·태그 필터·읽음 표시 검증

결과는 `PASS / FAIL / SKIP / NOT_RUN`으로만 적는다(constitution III).

## 1. 자동 테스트 (스토리마다)

```bash
cd backend && ./gradlew test
```

- 기대: 전체 PASS. 테스트 이름은 `R_LINK_NN_…`으로 시작한다(ADR-0003). R-LINK-09~18이 각각 하나 이상의 테스트와 연결된다(SC-006).
- 기존 R-LINK-01~08 테스트는 수정 없이 통과해야 한다. 저장 응답에 `read`가 더해지는 것만 예외이고, 기존 단언은 바꾸지 않는다(R-LINK-18).
- CI `backend-test`가 같은 명령을 돈다.

## 2. 수동 확인 (US1·US2)

```bash
cd backend && ./gradlew bootRun
```

다른 터미널에서 (계약: [contracts/browse-links.md](contracts/browse-links.md)):

```bash
curl -s -X POST localhost:8080/api/links -H 'Content-Type: application/json' -d '{"url":"https://example.com/qs-a","tags":["Java"]}'
curl -s -X POST localhost:8080/api/links -H 'Content-Type: application/json' -d '{"url":"https://example.com/qs-b","tags":["java","스프링"]}'
curl -s 'localhost:8080/api/links?size=1'          # 최신 1개, hasNext=true
curl -s 'localhost:8080/api/links?tag=%20JAVA%20'   # qs-b, qs-a 순서
curl -s -o /dev/null -w '%{http_code}\n' 'localhost:8080/api/links?size=101'   # 400
```

## 3. 수동 확인 (US3)

```bash
curl -s -X PUT localhost:8080/api/links/<id>/read -H 'Content-Type: application/json' -d '{"read":true}'   # read=true
curl -s -X PUT localhost:8080/api/links/<id>/read -H 'Content-Type: application/json' -d '{"read":true}'   # 같은 응답
curl -s -o /dev/null -w '%{http_code}\n' -X PUT localhost:8080/api/links/999999/read -H 'Content-Type: application/json' -d '{"read":true}'   # 404
```

## 4. 기존 로컬 데이터에서 기동 (SC-004, US3 PR에서 필수)

자동 테스트는 빈 메모리 DB라 이 경우를 잡지 못한다([research R1](research.md#r1-기존-로컬-db에-읽음-컬럼을-더하는-방법)). 원본을 건드리지 않게 **복사본**으로 확인한다.

0. US3 작업을 시작하기 전에 원본 `backend/data/linkshelf.mv.db`를 백업 폴더에 복사해 둔다(되돌리기용, 원본은 건드리지 않음).
1. 같은 원본을 임시 폴더에 한 번 더 복사한다(읽음 컬럼이 없고 행이 있는 상태).
2. `SPRING_DATASOURCE_URL=jdbc:h2:file:<임시 폴더>/linkshelf ./gradlew bootRun`으로 기동한다. `ddl-auto=update`는 ALTER가 실패해도 기동을 계속할 수 있으므로 "떴다"만으로는 PASS가 아니다. 아래 세 층을 따로 적는다:
   - 층 1 기동 로그: 스키마 변경 예외·경고가 없고, `alter table link add column is_read …` 문장이 보인다.
   - 층 2 DB 직접 조회(앱 종료 후 H2 Shell로 복사본 조회): `is_read` 컬럼이 있고 `DEFAULT FALSE`, `NOT NULL`이며 기존 행이 모두 false.
   - 층 3 API: `GET /api/links`에서 기존 링크가 모두 `read:false`, 다른 필드 불변.
3. 대조: 새 복사본에 대해 `@ColumnDefault`를 뺀 엔티티로 기동하면 층 1 또는 층 2에서 실패가 드러나는지(검증기 검증, constitution II).

## 5. 일부러 고장 내기 (검증기 검증)

- 정렬에서 `id` 내림차순을 빼면 같은 시각 링크 순서 테스트가 FAIL하는지.
- 태그 비교에서 소문자화를 빼면 대소문자 테스트가 FAIL하는지.
- 쪽 크기 상한 검사를 빼면 `size=101` 테스트가 FAIL하는지.
- 읽음 지정에서 저장을 빼면(값만 응답) 목록 확인 테스트가 FAIL하는지.
