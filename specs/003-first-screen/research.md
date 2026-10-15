# Research: 첫 화면

기준 SHA `f1609fa` · 2026-10-15. 출처는 advisor 검토(2026-10-15)가 인용한 공식 문서다.

## R1. Visual QA 기준 이미지

- **Decision**: CI(`ubuntu-24.04`)가 만든 이미지를 아티팩트로 받아 커밋한다. 첫 실행은 기준 이미지가 없어 실패한다(예상). 이후 갱신은 `workflow_dispatch` + `--update-snapshots=changed`.
- **Rationale**: Playwright는 기준 이미지가 없으면 실제 이미지를 기준 위치에 쓰고 테스트를 실패시킨다. 이미지 이름에 플랫폼 접미사가 붙고, 같은 환경에서 만들고 비교하라고 권한다([Visual comparisons](https://playwright.dev/docs/test-snapshots)).
- **Alternatives**: 로컬 Docker(설치 승인 필요, 글꼴 일치 미확인), 갱신 PR 자동 생성(`contents: write` 필요, GITHUB_TOKEN이 만든 PR은 CI가 바로 돌지 않음 — [GitHub 문서](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow)). 이미지가 흔들리면 job을 Playwright 컨테이너(`mcr.microsoft.com/playwright:v1.63.0-noble`)로 옮기는 안을 다시 본다(컨테이너 안 bootRun 가능 여부 미확인).

## R2. 스크린샷 안정화

- **Decision**: `visual.spec.ts`만 `page.route`로 고정 JSON 응답을 쓴다. `timezoneId`·`locale`·viewport 고정, 캡처 전 `page.mouse.move(-1, -1)`. 허용 오차 기본값. Linux가 아니면 SKIP.
- **Rationale**: 메모리 DB는 모든 E2E가 공유하므로 실제 백엔드로 "빈 목록"을 재현할 수 없고, URL·시각이 매번 바뀐다. 고정 JSON은 spec 001·002 계약의 응답 예시에서 가져오고, 실제 API와의 일치는 기능 E2E가 검증한다.

## R3. CI job 구성

- **Decision**: Node 22, Java 21, `npx playwright install --with-deps chromium`, `./gradlew classes`를 먼저, `globalTimeout` 10분, job 20분, 실패 시 아티팩트 7일.
- **Rationale**: Playwright CI 문서는 브라우저 캐시를 권하지 않는다(복원이 다운로드만큼 걸리고 시스템 의존성은 캐시되지 않음, [CI](https://playwright.dev/docs/ci)). 컴파일을 먼저 해 두면 webServer 시간 제한(180초)이 서버 기동에만 쓰인다.

## R4. 경로별 CI 분리(ADR-0002 재검토 트리거)

- **Decision**: 보류.
- **Rationale**: 경로 필터로 workflow가 건너뛰어지면 필수 검사가 Pending으로 남아 병합이 막힌다([GitHub 문서](https://docs.github.com/en/enterprise-server@3.8/pull-requests/collaborating-with-pull-requests/collaborating-on-repositories-with-code-quality-features/troubleshooting-required-status-checks)). E2E는 백엔드를 띄우므로 백엔드 변경에도 돌아야 한다.

## R5. PR 분할과 병렬

- **Decision**: B1(CI, smoke만) → A(화면) ∥ B2(기능 E2E + visual). B2는 Red를 로컬에서 확인·커밋하고 A 병합 후 rebase해서 PR을 연다.
- **Rationale**: 하나의 PR에서 CI가 실패하면 "화면이 없어서"인지 "설정이 틀려서"인지 구분되지 않는다. 기준 이미지는 리뷰까지 끝난 A의 최종 화면으로 찍는다.
