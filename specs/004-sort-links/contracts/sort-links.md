# Contract: 목록 정렬 (browse-links 개정)

> **상태: 승인됨, 반영됨 (2026-10-21)**. 사용자 답 "1) 승인"(이유 미기재). 계약 본문은 [browse-links](../../002-browse-links/contracts/browse-links.md) 한 곳에 둔다(`GET /api/links`의 `order` 행, 정렬·400 설명, 상태 코드 표). 이 파일은 승인 기록과 링크만 남긴다.

| 바뀐 것 | R-ID |
|---|---|
| `order` 쿼리(`newest`·`oldest`, 생략 = `newest`) | R-LINK-19 |
| `tag`·쪽 나누기와 함께 동작 | R-LINK-20 |
| 잘못된 값(빈 값 포함) 400 `field=order`, 다른 오류와 함께 | R-LINK-21 |
