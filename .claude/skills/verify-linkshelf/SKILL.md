---
name: verify-linkshelf
description: linkshelf 웹 화면(React+Vite, Spring Boot API)을 실제로 띄워 사용자처럼 조작하고 증거를 남긴다. 화면·API 변경을 완료라고 말하기 전, 또는 기능이 실제 앱에서 동작하는지 확인할 때 쓴다.
---

# verify-linkshelf

주 표면은 브라우저 한 페이지(`http://localhost:5173`)다. 그 뒤의 API(`/api/links`)는 Vite 프록시를 거쳐 Spring Boot(`:8080`)로 간다. 기능 목록과 화면↔E2E `data-testid` 계약은 [`features/README.md`](features/README.md)에 있다.

명령은 모두 저장소 루트에서 실행한다. `S=.claude/skills/verify-linkshelf/scripts`

## Launch

```bash
$S/launch.sh        # READY run=<RUN_ID> evidence=<경로> 가 나오면 준비 완료
```

- `bootJar`를 빌드하고 `java -jar`로 띄운다. DB는 실행마다 새 메모리 H2(`jdbc:h2:mem:verify-<RUN_ID>`)다. 로컬 파일 DB `backend/data/`는 건드리지 않는다. 스키마는 JPA가 만들고 시드 데이터는 없다. 각 여정이 자기 데이터를 만든다.
- 준비 완료 조건: 백엔드 `GET /api/links` 200, Vite `/` 200, 프록시 `GET :5173/api/links` 200.
- 포트 기본값은 8080·5173이다. worktree 여러 곳에서 동시에 검증할 때는 launch 전에 `VERIFY_BACKEND_PORT`, `VERIFY_FRONTEND_PORT`를 worktree마다 다르게 준다. launch가 포트를 `.verify/<RUN_ID>/run/ports`에 적고 doctor·drive·cleanup이 그 값을 읽으므로 뒤 명령에는 다시 주지 않는다. 한 worktree에서는 한 번에 한 인스턴스만 띄운다. 포트가 사용 중이면 launch는 exit 2로 멈춘다. 그 프로세스는 이번 실행이 띄운 것이 아니므로 조작하지도 죽이지도 않는다. `npm run e2e`는 8080·5173 고정이므로 기본 포트 인스턴스와 동시에 돌리지 않는다.
- `frontend/node_modules`가 없으면(새 worktree) `npm ci`를 먼저 돈다. Chromium은 `~/Library/Caches/ms-playwright`에 이미 있어야 한다.

## Doctor

```bash
$S/doctor.sh        # 마지막 줄 doctor: OK / doctor: FAIL
```

읽기 전용이다. 점검 항목은 두 프로세스가 살아 있는지, 두 포트의 주인이 이번 실행의 pid인지, 백엔드가 이번 메모리 DB를 쓰는지, 빌드한 revision이 지금 HEAD와 같은지, 프록시 응답이 오는지다. 조작 전에, 그리고 이상해 보일 때 먼저 돌린다. revision이 다르면 cleanup 뒤 다시 launch한다.

## Drive

```bash
$S/drive.sh <기능>   # save-link | browse-links | tag-filter | mark-read | sort-links | unread-filter
```

- 시작할 때 `doctor.sh`를 돌리고 FAIL이면 조작하지 않고 exit 3으로 멈춘다.
- `drive/<기능>.verify.ts`를 Playwright(`frontend/node_modules`의 같은 설치)로 실행한다. 헬퍼는 `frontend/e2e/support.ts`를 재사용한다.
- 요소는 `features/README.md` 표의 `data-testid`로만 찾는다. 문구나 CSS로 찾지 않는다.
- 데이터는 공유 DB다. 각 여정은 `uniqueUrl()`·`uniqueId()`로 자기 데이터만 만들고, DB가 비어 있다고 가정하지 않는다.
- 새 여정은 `drive/`에 `<기능>.verify.ts`로 추가하고 `features/`에 기능 파일을 만든다.

## Evidence

`.verify/<RUN_ID>/evidence/` (git 무시). 기능마다 하위 폴더가 생긴다.

- 화면: 동작 전후 스크린샷 `*.png`, 목록 ARIA 스냅숏 `*.aria.txt`, Playwright trace(`test-results/**/trace.zip`, `npx playwright show-trace`로 연다).
- 저장 확인: 화면과 다른 경로인 `GET /api/links` 응답 `*.json`과 새로고침 뒤 스크린샷.
- 판정: `exit-code`(0 = PASS), `report.json`, `playwright.log`.
- 실행 정보: `revision`, `backend.log`, `frontend.log`, `build.log`.

보고에는 기능 이름, 증거 폴더, `exit-code`를 적는다. 테스트 전용 엔드포인트나 내부 상태 주입은 쓰지 않는다. 화면 동작은 화면으로 하고, 사전 데이터만 공개 API(`POST /api/links`)로 만든다.

## Cleanup

```bash
$S/cleanup.sh       # 이번 실행의 pid만 종료, run 상태 삭제, 증거는 남김
```

pid 파일에 적힌 프로세스만, 명령줄이 이번 실행의 것(백엔드 `verify-<RUN_ID>`, 프론트 `vite.js`)일 때만 죽인다. 프로세스 이름으로 죽이지 않는다. 마지막에 두 포트가 비었는지와 증거 경로를 출력한다. 실패한 시도 뒤에도 매번 돌린다. 오래된 증거 폴더는 사람이 지운다.

## Helpers

| 파일 | 하는 일 |
| --- | --- |
| `scripts/launch.sh` | 빌드, 두 서버 실행, 준비 대기. `RUN_ID=<이름>`으로 실행 이름을 정할 수 있다 |
| `scripts/doctor.sh` | 읽기 전용 점검 |
| `scripts/drive.sh <기능>` | 여정 한 개 실행, 증거 저장, PASS/FAIL 출력 |
| `scripts/cleanup.sh` | 정리 |
| `drive/playwright.verify.config.ts` | `webServer` 없이 launch한 인스턴스를 조작하는 설정 |
| `drive/evidence.ts` | `shot`, `aria`, `json` 증거 헬퍼 |
| `drive/tsconfig.json`, `drive/node-env.d.ts` | drive 타입 검사. `cd frontend && npx tsc -p ../.claude/skills/verify-linkshelf/drive/tsconfig.json`(CI `frontend-e2e`도 돌림). `@types/node` 대신 쓰는 선언만 둔다 |
