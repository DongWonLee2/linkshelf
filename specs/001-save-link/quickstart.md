# Quickstart: 링크 저장 검증 (001-save-link)

API 형식은 [contracts/save-link.md](contracts/save-link.md), 테이블은 [data-model.md](data-model.md)를 본다(둘 다 승인 전 초안).

## 준비

- JDK 21, 저장소 루트에서 실행

## 1. 자동 테스트 (필수 검증)

```bash
cd backend && ./gradlew test
```

- 기대: 전체 PASS. 요구사항별 테스트 이름은 [plan.md 테스트 계획](plan.md#테스트-계획)을 본다.
- 결과는 `build/test-results/test/*.xml`의 tests/failures/errors/skipped 값으로 보고한다.

## 2. 수동 확인 (선택)

```bash
cd backend && ./gradlew bootRun     # 다른 터미널에서 아래 실행
```

| # | 요청 본문 | 기대 |
|---|---|---|
| 1 | `{"url":"https://example.com/a","memo":"한글 메모 😀","tags":["Java"," java ","스프링"]}` | 201, tags `["Java","스프링"]` |
| 2 | `{"url":"HTTPS://Example.COM:443/a"}` | 409, `url` = `https://example.com/a` |
| 3 | `{"url":"https://example.com/a/"}` | 201 (다른 URL로 봄) |
| 4 | `{"url":"ftp://example.com"}` | 400, errors[0].field = `url` |

```bash
curl -s -i -X POST localhost:8080/api/links -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/a","memo":"한글 메모 😀","tags":["Java"," java ","스프링"]}'
```

- 개발 DB 파일은 `backend/data/`에 생긴다(Git 제외). 처음부터 다시 하려면 bootRun을 멈추고 이 폴더를 지운다(사람이 직접).

## 3. 오류 주입 (런북 5-8, 커밋하지 않음)

- 테스트 하나의 기대값을 일부러 바꾸고(예: 409 → 400) `./gradlew test` FAIL을 확인한 뒤 원복하고 PASS를 다시 확인한다. 로그는 PR에 남긴다.
