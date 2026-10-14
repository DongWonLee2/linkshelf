# ADR-0004: 리뷰 서브에이전트 Bash 차단과 deny 규칙의 한계

- 상태: Accepted
- 작성일 / 사람 승인일: 2026-10-14 / 2026-10-14
- 결정 책임자: 저장소 소유자
- 관련 Spec/R-ID: 해당 없음 (Issue #7, Task-ID CHORE-AGENT-GUARD-01)
- 사용한 외부 근거: Claude Code 공식 문서 code.claude.com/docs/en/sub-agents(서브에이전트 frontmatter `hooks`, PreToolUse exit 2로 차단), code.claude.com/docs/en/permissions("deny or ask rule ... isn't a security boundary around the program")

## 맥락

`pr-reviewer` 서브에이전트(PR #6)는 `tools`에 Bash가 있어서, 파일 쓰기·커밋·gh 코멘트가 프롬프트 지시로만 막혀 있었다. 리뷰어는 작성자 입력(PR 본문·diff)을 읽으므로 프롬프트 인젝션이 쓰기로 이어질 수 있다(PR #6 리뷰 지적 #3). 또 `.claude/settings.json`의 force push deny는 옵션을 뒤에 쓴 변형을 놓친다(지적 #12).

## 평가 기준

1. 리뷰어의 쓰기를 프롬프트가 아니라 규칙으로 막음 2. 메인 세션의 자율 진행(런북 §8-1)을 방해하지 않음 3. 리뷰 독립성 유지 4. 새 의존성 없음

## 대안

| 대안 | 장점 | 단점·위험 | 근거·실험 |
|---|---|---|---|
| A-1. pr-reviewer 전용 PreToolUse 훅 + 허용 목록 | 서브에이전트에만 적용, 명령 문자열 전체를 직접 검사 | 허용 목록 스크립트에 구멍이 있을 수 있음 | 훅 단위 테스트 50개, 서브에이전트로 금지 명령 실제 시도 |
| A-2. Bash 제거, 메인 세션이 diff를 파일로 전달 | 가장 강함 | 메인 세션이 보여 줄 것을 고름(독립성 약화), CI 결과 조회 불가 | — |
| A-3. settings ask 목록 추가 | 설정만 바꿈 | 메인 세션에도 적용(§8-1 충돌), 문자열 변형 못 막음 | 공식 문서 |
| B-1. force push deny 변형 몇 줄 추가 + 한계 기록 | 흔한 변형을 잡음 | `-C`, 경로 호출, `sh -c` 변형은 여전히 통과 | 공식 문서 |
| B-2. 변경 없이 문서화만 | 변경 없음 | 기능 브랜치 force push 변형은 guardrails mod에만 의존 | — |

## Agent의 추천과 근거

A-1, B-1을 추천했다. 쓰기 차단은 서브에이전트 범위에서 훅으로 강제하고, deny 규칙은 보안 경계가 아니라 흔한 실수를 막는 장치로 둔다. `main`의 force push는 GitHub 보호 규칙(서버)이 이미 막는다(ADR-0002, A7).

## 사람의 최종 결정과 실제 판단 이유

A-1, B-1 채택. 사용자 지시로 A-1에 두 가지를 더했다(사용자 표현):
1. "훅 스크립트는 ; | & > < $( ` 줄바꿈이 들어간 명령과 --output 같은 파일 쓰기 옵션을 무조건 차단한 뒤에 허용 목록을 검사해."
2. "리뷰어 허용 목록에서 ./gradlew는 빼고, 테스트 결과는 gh pr checks로 CI 결과를 읽게 해."

## 결정 내용

- `.claude/hooks/reviewer-readonly.sh`: 순서대로 ① 줄바꿈 ② `;` `|` `&` ③ `>` `<` ④ `$(`, 백틱 ⑤ `--output*`, `-o*`, `--web`/`-w` 토큰을 무조건 차단 → ⑥ 첫 두(gh는 세) 단어가 허용 목록일 때만 통과. 그 외는 exit 2.
- 허용 목록: `git diff|log|show|status|rev-parse|ls-files|ls-remote|merge-base|blame`, `gh pr view|diff|checks|list`, `gh issue view|list`, `gh run view|list`. 첫 단어 앞의 환경 변수, `git -C`/`-c` 같은 전역 옵션, 따옴표로 감싼 명령 이름은 허용 목록에 맞지 않아 차단된다.
- `./gradlew`는 허용하지 않는다. 리뷰어는 CI 결과를 읽는다.
- `.claude/settings.json` deny 추가: `Bash(git push * --force*)`, `Bash(git push * -f)`, `Bash(git push * -f *)`.

## 포기하는 것·운영 영향

- 리뷰어는 `git log --format='%h|%s'`처럼 따옴표 안에 `|`가 있는 읽기 명령도 못 쓴다(무조건 차단의 대가).
- 리뷰어는 로컬 테스트로 재현하지 못한다. CI가 돌지 않은 커밋은 테스트 결과를 NOT_RUN으로 적어야 한다.
- 허용된 `git diff`·`git show`는 저장소 `.git/config`의 외부 diff·textconv 설정을 따른다. 저장소 설정은 PR로 바뀌지 않으므로 받아들인다.
- `git status`는 인덱스 stat 캐시를 갱신할 수 있다(내용 변경 아님).
- deny 규칙은 보안 경계가 아니다: `git -C . push ... -f`, `/usr/bin/git push ...`, `sh -c '...'`은 막지 못한다. 보호는 GitHub 브랜치 보호(서버), guardrails mod, 이 ADR의 훅(리뷰어 한정)에 나눠져 있다.
- 훅은 워크스페이스 신뢰를 받은 폴더에서만 동작한다(공식 문서).
- 되돌리기: 이 PR을 revert한다.

## 결정 검증

- 실행할 실험: `bash .claude/hooks/test-reviewer-readonly.sh`(허용 12 / 차단 38), 차단 규칙을 하나씩 지운 사본으로 테스트가 FAIL하는지 확인, pr-reviewer 서브에이전트에게 금지 명령(쓰기, 연결 기호, `--output`, gradlew)을 실제로 시켜 훅 메시지로 막히는지 확인
- 성공 판정: 단위 테스트 50/50, 변형마다 FAIL 발생, 서브에이전트 시도가 `reviewer-readonly: blocked`로 막히고 파일이 생기지 않음
- 재검토 트리거: 리뷰어가 허용 목록 밖 명령이 꼭 필요해질 때, Claude Code의 서브에이전트 훅·sandbox 동작이 바뀔 때
