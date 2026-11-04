---
inclusion: always
---

# DB 규칙 (PostgreSQL, Flyway)

## Flyway
- 마이그레이션 파일은 src/main/resources/db/migration/ 에 V{번호}__{설명}.sql 형식으로 둔다. (예: V1__create_task.sql)
- 이미 적용된 파일은 절대 수정하지 않는다. 변경은 항상 새 번호의 파일로 추가한다.
- 한 파일에는 하나의 목적만 담는다. (테이블 생성, 컬럼 추가, 인덱스 추가 등)
- SQL은 사람이 리뷰하기 쉽게 작성한다. 제약조건과 인덱스에는 이름을 붙이고, 비직관적인 부분에는 주석을 단다.

## 네이밍과 타입
- 테이블과 컬럼은 소문자 snake_case, 테이블명은 단수형이다. (task, event)
- PK는 id bigint generated always as identity 로 한다.
- 문자열은 길이를 명시한 varchar(n)을 쓴다. (title은 varchar(200))
- 시각은 모두 timestamptz로 저장한다. (start_at, end_at, done_at, created_at, updated_at)
- created_at, updated_at은 not null이며 기본값은 now()다.
- 날짜 없이 시각만 필요한 값(반복 규칙 등)은 time 타입을 쓴다.

## task 테이블
- status는 varchar(10) not null이며 check 제약으로 WISH/TODO/DOING/DONE만 허용한다. (PostgreSQL enum 타입은 쓰지 않는다)
- DOING은 최대 1개다. 서비스에서 보장하고, DB에는 partial unique index로 방어한다.
  - 예: create unique index uq_task_single_doing on task (status) where status = 'DOING';
- done_at은 status가 DONE일 때만 값이 있다.
- 마감 컬럼은 요구사항에서 확정한 뒤 만든다. (날짜만 / 시각까지)
- 조회 패턴(상태별 목록, 오늘 완료한 일)에 맞춰 status, done_at에 인덱스를 둔다.

## event 테이블
- end_at은 start_at보다 뒤여야 한다. (check 제약)
- 날짜 범위 조회(오늘, 내일, 이번 주)가 많으므로 start_at에 인덱스를 둔다.

## 운영
- 로컬 DB를 삭제하는 명령(docker compose down -v 등)은 실행하기 전에 반드시 확인을 받는다.
- 백업은 pg_dump로 주 1회 한다.
