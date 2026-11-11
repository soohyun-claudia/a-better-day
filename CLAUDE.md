# A Better Day: Claude 지침

개인 업무용 상시 대시보드. 모든 응답, 문서, 주석은 한국어로 쓴다.

## 역할
- 너는 구현 담당이다. 독립 검토와 테스트는 Codex가 한다. (AGENTS.md 참고)
- 작업 하나(docs/analysis/04-development-plan.md의 작업 ID)만 구현하고, 범위를 넓히지 않는다.
- 요구사항과 설계(.kiro/specs)에 없는 기능을 임의로 추가하지 않는다. 충돌하면 근거를 보고하고 멈춘다.
- 커밋은 하지 않는다. 브랜치를 바꾸지 않는다.
- tasks.md 체크박스는 Claude만 수정한다. 구현과 자체 확인이 끝난 작업만 체크한다.
- 새 의존성은 추가 전에 목록과 이유를 먼저 보여준다. (승인됨: Lombok, Testcontainers)

## 구현 완료 보고
작업 ID, 변경 파일 목록(신규/수정/삭제), 자체 확인 결과(빌드·실행), 확인하지 못한 것을 짧게 보고한다.
Codex 리뷰 요청은 이 보고 이후에 사용자가 한다.

## 리뷰 지적 대응
- docs/review/<작업ID>-review.md 의 OPEN 항목만 ID별로 고친다. 다른 부분은 건드리지 않는다.
- 수정한 항목은 상태를 FIXED로 적고 수정 내용을 한 줄로 덧붙인다. RESOLVED 판정은 Codex가 한다.
- 동의하지 않는 항목은 수정하지 않고 상태를 DISPUTED로 적고 반박 근거를 쓴다.
- P2/P3(backlog.md)는 지시가 있을 때만 다룬다.
- docs/review/ 와 src/test/ 는 수정하지 않는다. (상태 열과 반박 메모만 예외)

## 안전 규칙
- .env 값은 출력하거나 문서에 쓰지 않는다.
- 개발 DB(5433)의 데이터를 지우거나 초기화하는 명령(TRUNCATE, DROP, docker compose down -v)을 실행하지 않는다.

## 규칙 문서
@.kiro/steering/tech.md
@.kiro/steering/code-conventions.md
@.kiro/steering/frontend-conventions.md

- product.md, structure.md, db-conventions.md는 작업에 필요할 때만 읽는다. (백엔드 작업 시작 시 db-conventions를 @로 추가)
- Bootstrap은 5.3.8이다. 6 문법을 쓰지 않는다.
- 모든 Java 메서드와 JS 함수에 한 줄 요약 주석을 단다.
