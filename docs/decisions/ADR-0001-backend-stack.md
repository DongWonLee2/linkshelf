# ADR-0001: 백엔드 스택

- 상태: Accepted
- 작성일 / 사람 승인일: 2026-10-14 / 2026-10-14
- 결정 책임자: 저장소 소유자
- 관련 Spec/R-ID: PRD-01~04
- 사용한 외부 근거: Maven Central `spring-boot` 최신 4.1.1, Gradle 최신 9.8.1 (2026-10-14 조회), 로컬 `java -version` = OpenJDK 21.0.12

## 맥락

링크를 저장·조회하는 로컬 1인용 REST API가 필요하다. 런북이 Spring Boot + Java LTS + Gradle + JPA + H2를 후보로 지정했다.
조용한 기본값을 피하려고 버전, DTO, 검증, 오류 형식을 여기서 명시한다.

## 평가 기준

1. 테스트 자동화가 쉬운가 2. 로컬에 이미 있는 도구로 돌아가는가 3. 설정이 적은가 4. 이후 프론트·배포로 확장 가능한가

## 대안

| 대안 | 장점 | 단점·위험 | 근거·실험 |
|---|---|---|---|
| A. Spring Boot 4.1.1 + Java 21 + Gradle + JPA + H2 | 로컬 JDK 21 그대로 사용, 슬라이스 테스트 지원, 런북 지정 | 의존성·기동 시간이 무거움, Boot 4는 최신이라 자료가 적을 수 있음 | 로컬 JDK 확인 |
| B. Spring Boot 3.5.x + Java 21 | 자료·예제가 많음 | 이전 세대, 곧 지원 종료 | — |
| C. Java 25 + Spring Boot 4.1.1 | 최신 LTS | 로컬에 JDK 25 없음 → 설치 필요(런북 범위 밖) | `java_home -V`에 25 없음 |

## Agent의 추천과 근거

A를 추천한다. 추가 설치 없이 돌아가고 런북의 "최신 안정판" 조건을 맞춘다.
세부 관례:
- 빌드: Gradle Wrapper(9.8.1), **Groovy DSL**(`build.gradle`), Java toolchain 21
- 영속성: Spring Data JPA + H2. 개발은 파일 DB(`./data/`, Git 제외), 테스트는 메모리 DB. MVP-1은 JPA 스키마 자동 생성, 마이그레이션 도구(Flyway)는 실제 DB 도입 때 별도 ADR
- DTO: Java `record`. Lombok 쓰지 않음
- 입력 검증: Jakarta Bean Validation(`spring-boot-starter-validation`)
- 오류 응답: RFC 9457 `ProblemDetail`(`application/problem+json`). 400 검증 실패, 409 중복. 필드 오류는 `errors` 확장 필드에 `{field, message}` 목록
- 시각: UTC `Instant`
- API 문서 도구·Spring Security·린트는 MVP-1에서 쓰지 않음
- 패키지: `dev.linkshelf`, 기능별 하위 패키지(`dev.linkshelf.link`)

## 사람의 최종 결정과 실제 판단 이유

Agent 추천안 채택, 단 Gradle 빌드 스크립트는 Groovy DSL로 변경(사용자 지시 "Gradle은 Groovy로 변경"). 판단 이유: 사용자 위임

## 포기하는 것·운영 영향

- 트레이드오프: 가벼운 프레임워크(Javalin 등)보다 무겁다. H2는 운영 DB와 SQL 방언이 다를 수 있다.
- 마이그레이션: 스키마 자동 생성은 데이터 보존을 보장하지 않는다. 실제 DB 도입 전 Flyway로 바꿔야 한다.
- 되돌리기: 코드가 적은 지금은 쉽다.

## 결정 검증

- 실행할 실험: 4단계 걷는 골격에서 `./gradlew test` 로컬·CI PASS
- 성공 판정: 헬스 체크 테스트 PASS, 실패 주입 시 CI FAIL
- 재검토 트리거: 배포 대상 결정, 다중 사용자 요구, H2 호환 문제
