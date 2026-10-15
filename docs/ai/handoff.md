# Agent Handoff — MVP-3 첫 화면 (조언 00:40)

- 상태: MVP-3. 골격 PR #20 병합. spec 003 specify·clarify(5문항 "추천")·plan·tasks 완료, 승인("승인") → Issue #21(B1 CI)·#22(A 화면)·#23(B2 E2E). spec PR 생성, 다음 B1
- task_id: spec 003 T001~T034 (R-UI-01~13)
- 작성: 2026-10-14 KST · Claude Code (Opus 5.5)
- 규칙: 런북 `~/Dev/AI_개발_가이드/plans/mvp1-runbook.md` §8(8-3a: 승인 요청 전 advisor 먼저). 플레이북 규칙 20(병합 후 정리), 21(기능 먼저)
- 작업 브랜치: `chore/spec-003-first-screen` · 기준 SHA `f1609fa` (main, PR #20 병합)
- 진행 요약: `~/Dev/AI_개발_가이드/plans/mvp1-status.md`
## 지금까지 완료한 것

| 단계 | 결과 | 증거 |
|---|---|---|
| 5단계 링크 저장 API | PASS | PR #4 병합(`39d708d`, 2026-10-14T05:36Z), Issue #3 닫힘. 로컬 53 tests PASS, CI run 38516023871 PASS |
| pr-reviewer 서브에이전트 | PASS | PR #6 병합(`dfb3311`, 2026-10-14T06:04Z). 동작 확인 표는 PR #6 코멘트 6161964376 |
| 6단계 정리 | PASS | `plans/mvp1-result.md`: 수용 테스트 A1~A8, 계측, 규칙 후보 |
| #7 방식 결정 | 사용자 승인 | A-1(pr-reviewer 전용 PreToolUse 훅 + 허용 목록) + 추가 2가지(연결 기호·리다이렉트·`$(`·백틱·줄바꿈·`--output` 무조건 차단 후 허용 목록 검사, `./gradlew` 제외하고 CI 결과는 `gh pr checks`로), B-1(force push deny 변형 몇 줄 + "보안 경계 아님" 기록) |

## 실제 GitHub 번호 ↔ 런북 이름

| 런북 이름 | GitHub |
|---|---|
| PR #1 (bootstrap) | PR #1 (merged) |
| CI 실패 감지 시험 | PR #2 (closed, 미병합) |
| 5단계 Issue | Issue #3 (closed) |
| PR #2 (링크 저장 API) | PR #4 (merged) |
| OSIV 별도 Issue | Issue #5 (open) |
| pr-reviewer 서브에이전트 | PR #6 (merged) |
| 서브에이전트 Bash 쓰기 강제 차단 | Issue #7 (open) |

## 마지막 실행 명령과 결과

- #7 시작 2026-10-14 15:46 KST. handoff `1741341` → Red `18d8b97`(50/50 FAIL, 훅 없음) → Green `f04a2df`(`bash .claude/hooks/test-reviewer-readonly.sh` 50/50 PASS). 차단 규칙 하나씩 지운 사본 5개 모두 FAIL 발생(1~4개)
- pr-reviewer로 금지 명령 5개 + 대조군 2개 시도 → **훅이 연결되지 않은 이전 정의로 실행됨**: 서브에이전트 시스템 프롬프트가 수정 전 문장(`./gradlew test`), PreToolUse는 cmux 훅만 exit 0. 금지 명령 4개가 스크래치 경로에 파일 생성(저장소 HEAD·status 변화 없음). 공식 문서는 "수정한 정의는 다음 호출에 반영"이라 하지만 이 세션에서는 반영 안 됨

- 2026-10-14 16:02 KST 새 세션: 기준 `5362fe1`, 작업 트리 clean. pr-reviewer 호출 2회 모두 auto mode 분류기 거부(`[Auto-Mode Bypass]`)
  1. 원래 금지 명령 5개 + 대조군 2개(스크래치 경로 파일 쓰기 포함) → 거부
  2. 쓰기 없는 대체안(`git status; git log -1 --oneline`, `git log -1 --oneline | head -1`, `ls .claude/hooks`, `./backend/gradlew --version` + 대조군 2개) → 거부
  3. 사용자가 "권한 확인 모드로 바꿨다"고 한 뒤 원래 7개로 재호출 → 여전히 auto mode 분류기 거부. 이 세션에는 모드 전환이 적용되지 않은 것으로 보임
  - 서브에이전트가 실행되지 않았으므로 훅 연결 여부는 알 수 없음. 스크래치 probe 디렉터리 비어 있음, 저장소 변화 없음. 우회 시도 안 함

- 2026-10-14 16:09~16:15 KST 권한 확인 모드(사용자 전환): pr-reviewer 시스템 프롬프트에 새 정의 반영 확인
  - A1~A5(원래 쓰기 명령): 서브에이전트가 지침을 근거로 시도 안 함. A6·A7 대조군 실행됨
  - B1 `git log -1 --oneline --grep="a|b"` → `reviewer-readonly: blocked (command chaining)`, B2 `git ls-files -o` → `blocked (file output option -o)`, B3 대조군 실행됨. 승인 창 층·guardrails·auto 분류기 관여 없음
  - 시험 폴더 빔, HEAD `63a1634`, `git status --porcelain --ignored` 해시 전후 동일
  - 발견: 과잉 차단(따옴표 안 `|`, `ls-files -o`). 이번엔 둠

- 16:30~ PR #8 리뷰(pr-reviewer): 높음 3건(따옴표 우회, `ls-remote --upload-pack`, 테스트 공백) 재현 → 같은 PR에서 수정. 분류 코멘트 6163704335. 별도 Issue 2건 생성(사용자 "둘 다 해"): Issue #9 `.env` 등 읽기 구멍, Issue #10 훅 테스트 CI 편입

- advisor 첫 호출(조언 16:10 3번): 결과 돌아옴, transcript `model` = `claude-opus-5-5`, 도구는 Read·Grep·Glob·WebFetch·WebSearch만, 저장소 무변경(HEAD `0aaec1f`, status clean). 재시작 불필요였음. advisor 추천: #5를 #9보다 먼저, PR #8 병합을 기다리지 않고 main 기준 `fix/5-...`로 시작 가능(파일 겹침 없음). #9는 시작 시 방식 승인 필요(보안 정책 변경)

- #5 (17:33~17:41 KST): 기준선 53 PASS → Red `809ee0d`(인터셉터 테스트 FAIL) → main에만 설정 넣었더니 계속 FAIL: **test `application.properties`가 main을 가림**(기존 테스트가 main 설정을 안 읽었음) → advisor 판단으로 test 파일을 `src/test/resources/config/`로 이동 → Green `2fcec53` 55/55, main 줄 제거 시 FAIL 확인, bootRun 경고 0건. PR #11, CI run 38536802084 PASS
- PR #11 리뷰: 병합 막는 결함 없음. 수용 2건 = 테스트 DB 격리 가드 `a18f58e`(파일 DB 주소로 FAIL 확인, 56/56), 태그 테스트 변별력 낮음 본문 명시. 분류 코멘트 6164809603

- 22:12~ spec 002: advisor 판단으로 spec 하나에 US 3개, 순차 진행. clarify 5문항(2개 직접 답 "a", 3개 사용자 위임 "다 추천안으로 해"). plan: 로컬 H2 복사본 실측(기본값 없는 NOT NULL 추가 실패 → `DEFAULT FALSE NOT NULL`), advisor 검토로 tag 제약·SC-004 세 층·B5 추가. 사용자 "추천으로 가"(B1~B5). tasks 47개. Issue 승인 "추천안으로 ㄱ" → #12·#13·#14
- 그 전: #9·#10 not planned로 닫음(사용자 "#9 보류, 샌드박스 Issue 아니오", 조언 22:10), 병합 브랜치 정리·`delete_branch_on_merge=true`(사용자 승인)

- US1 #12 → PR #16(리뷰: 큰 page 500 재현·수정), US2 #13 → PR #17(리뷰: 줄바꿈 태그 400 재현, `@NotBlankText`로 수정) 병합
- US3 #14 (2026-10-14 23:56~): 로컬 DB 백업 `~/Dev/linkshelf-db-backup/20261014-235640/linkshelf.mv.db`(원본과 cmp 동일). Red `9fdfb4d` → Green `908503a`(118) → 리뷰 반영 `45bc22c`(120). Jackson 3이 `"true"`·`1`을 boolean으로 강제 변환함을 확인 → `read` 필드 전용 `StrictBooleanDeserializer`
- SC-004 증거(백업본 복사본, 행 2개, `SPRING_DATASOURCE_URL`로 bootRun):
  - A `@ColumnDefault` 있음: 층1 `Started`, WARN·ERROR 없음(ALTER 문은 로그에 안 남음 → quickstart §4 층1 기준 미충족, 층2·3으로 대체). 층2 H2 Shell `IS_READ | BOOLEAN | FALSE | NO`, 행 `1 FALSE`, `2 FALSE`. 층3 `GET /api/links` → `2 [(2, '…/a/', False), (1, '…/a', False)]`
  - B 대조 `@ColumnDefault` 없음: 층1 `Started` + WARN `CommandAcceptanceException … alter table if exists link add column is_read boolean not null … NULL not allowed for column "IS_READ"`. 층2 컬럼 없음(0 rows). 층3 `GET` 500

- 00:40 사용자: 조언 00:40 1번 승인 묶음(ADR-0005, 최소 npm 패키지·Playwright 브라우저, CI frontend-e2e job, implementer 설치) 사전 승인. Node 새 설치나 노트와 다를 때만 묻기
- implementer 전역 설치(`~/.claude/agents/implementer.md`, cmp 동일). worktree 하네스 검증은 첫 병렬 작업 때(NOT_RUN)
- PR #20: 패키지 8개 정확 고정(2주 이상 지난 버전), Playwright webServer(메모리 H2 + Vite), 연기 E2E PASS, 프록시 제거 시 FAIL. 리뷰 반영: `npm ci`(빈 폴더 복사본에서, `rm -rf node_modules`는 guardrails가 막아 우회하지 않음), Feature Map 보강(`data-field`, 오류·성공 testid, 데이터 격리 규칙), 연기 테스트가 빈 DB를 가정하지 않게

## 수정했지만 커밋하지 않은 파일

- 없음

## 다음 행동 1개

B1(#21): main 기준 `ci/21-frontend-e2e` 브랜치에서 `frontend-e2e` job + config 보강 → PR → CI PASS. 병합 후 A(#22)·B2(#23)를 implementer 2개로 동시에(worktree 검증 기록).

## 사람이 결정하거나 승인할 사항

- spec 003 문서 PR 병합, B1·A·B2 PR 병합, `frontend-e2e` 필수 검사 추가(main 첫 PASS 뒤 승인)
- 로컬 DB 백업 `~/Dev/linkshelf-db-backup/20261014-235640/`는 되돌리기용으로 남김. 지워도 되면 사용자가 직접 지운다
- 이전부터 남은 것: Codex `~/.codex/hooks.json` Orca 훅 정리, 런북 4단계 순서 수정

## Codex로 이어받을 때

`AGENTS.md` → 이 파일 → `plans/mvp1-status.md` 순서로 읽는다.
