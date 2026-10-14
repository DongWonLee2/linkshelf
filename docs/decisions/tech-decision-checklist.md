# 기술·관례 결정 체크리스트 — linkshelf

> 템플릿: `AI_개발_가이드/templates/tech-decision-checklist.md` · 작성 2026-10-14
> 2026-10-14 사용자 승인. 판단 이유는 "위임" → 사용자 위임으로 기록.

| 항목 | 후보·현 상태 | 승인 필요? | 선택 이유·포기한 것 | 기록 위치 |
|---|---|---|---|---|
| 언어/런타임/프레임워크 버전 | Java 21 (로컬 OpenJDK 21.0.12), Spring Boot 4.1.1, Gradle 9.8.1 (Groovy DSL, 사용자 지시) | 예 | 사용자 위임 | ADR-0001 |
| 모듈·패키지 구조 | 모노레포 `backend/`, 패키지 `dev.linkshelf.<기능>` (기능별) | 예 | 사용자 위임 | ADR-0002 / AGENTS.md |
| DB·ORM·마이그레이션 | H2(파일·메모리) + Spring Data JPA, 스키마는 JPA 자동 생성(MVP만) | 예 | 사용자 위임 | ADR-0001 |
| 코드 생성·Lombok·DTO | Lombok 미사용, DTO는 Java `record` | 예 | 사용자 위임 | ADR-0001 |
| 입력 검증·오류 응답 계약 | Jakarta Bean Validation + RFC 9457 `ProblemDetail` | 예 | 사용자 위임 | ADR-0001 |
| 인증/인가, CSRF/CORS/비밀 | 인증 없음(로컬 1인). Spring Security 미도입. 비밀값 없음 | 예 | 사용자 위임 | PRD §6 |
| API 문서 도구 | 도입 안 함(MVP-1). 계약은 spec의 표로 기록 | 예 | 사용자 위임 | ADR-0001 |
| 시간대·정렬·페이지네이션 | 시각은 UTC `Instant` 저장. 정렬·페이지네이션은 목록 기능 spec에서 | 아니오(후속) | 사용자 위임 | Spec |
| 린트·서식 | 도입 안 함(MVP-1). 컴파일 경고만 | 예 | 사용자 위임 | — |
| 테스트·테스트 DB | JUnit 5 + Spring Boot Test, H2 메모리 | 예 | 사용자 위임 | ADR-0003 |
| 프론트 상태·디자인 토큰 | 해당 없음(MVP-3) | — | — | — |
| 브랜치/PR/병합·복구 | `feat/<issue>-<slug>`, PR 필수 + CI 필수, 사람이 병합, 병합 후 `git revert` | 예 | 사용자 위임 | ADR-0002 |
