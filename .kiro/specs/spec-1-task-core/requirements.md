# Spec 1 요구사항: 프로젝트 기반 + task 핵심 기능

## 개요

개인 업무용 상시 대시보드의 첫 번째 단계다.
로컬 개발 환경(PostgreSQL + Spring Boot)을 구성하고, task의 핵심 상태 전환 기능과
오늘 할 일 · 한 일 · 하고 싶은 일 세 패널을 포함한 화면을 만든다.

## 확정된 전제

- 마감 저장: `due_date date` + `due_time time` 컬럼 2개. "오늘 중"은 오늘 날짜, "이번 주"는 이번 주 일요일(서울 기준)로 변환해 저장.
- 우선순위: HIGH / MEDIUM / LOW 3단계 (화면 표시: 높음 / 보통 / 낮음).
- "오늘 할 일" 범위: 마감일 무관하게 TODO · DOING 상태 전부.
- 초기 카테고리: 학습 / 운동 / 사이드 프로젝트.
- 삭제: 논리 삭제(`deleted_yn`). 이번 spec에서는 컬럼·인덱스·엔티티 구조만 갖추고, DELETE API는 Spec 2(편집·삭제)에서 추가한다.

---

## 요구사항 목록

### REQ-1. 로컬 개발 환경

**REQ-1-1. Docker Compose로 PostgreSQL 실행**
WHEN 개발자가 `docker compose up -d`를 실행하면,
THE SYSTEM SHALL PostgreSQL 17 컨테이너를 시작하고 named volume에 데이터를 유지한다.
THE SYSTEM SHALL `restart: unless-stopped`를 설정해 시스템 재시작 후에도 자동 기동한다.

**REQ-1-2. 접속 정보 분리**
THE SYSTEM SHALL DB 접속 정보(호스트, 포트, DB명, 사용자, 비밀번호)를 `.env` 파일에 담고, docker-compose.yml이 이를 참조한다.
THE SYSTEM SHALL `.env`는 `.gitignore`에 포함해 git에 올라가지 않는다.
THE SYSTEM SHALL `.env.example` 파일을 저장소에 포함해 다른 환경에서 참고할 수 있게 한다.

**REQ-1-3. Spring Boot DB 연결 및 Flyway 마이그레이션**
WHEN 애플리케이션이 시작되면,
THE SYSTEM SHALL Spring Boot가 `.env`(또는 `application.yml`의 환경 변수 참조)로 PostgreSQL에 접속한다.
THE SYSTEM SHALL Flyway가 `V1__create_task.sql` 마이그레이션을 자동 적용한다.
THE SYSTEM SHALL `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`로 설정한다.

---

### REQ-2. task 데이터 모델

**REQ-2-1. task 테이블 컬럼**
THE SYSTEM SHALL task 테이블은 다음 컬럼을 갖는다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | bigint | generated always as identity, PK | |
| title | varchar(200) | not null | |
| status | varchar(10) | not null, check(WISH/TODO/DOING/DONE) | |
| priority | varchar(10) | not null, check(HIGH/MEDIUM/LOW), default MEDIUM | |
| category | varchar(50) | nullable | WISH 상태에서만 사용 |
| due_date | date | nullable | 마감 날짜 |
| due_time | time | nullable | 마감 시각 (due_date가 있을 때만) |
| done_at | timestamptz | nullable | DONE으로 전환될 때 기록 |
| deleted_yn | char(1) | not null, default 'N', check('Y'/'N') | 논리 삭제 표시 |
| created_at | timestamptz | not null, default now() | |
| updated_at | timestamptz | not null, default now() | |

THE SYSTEM SHALL `due_time`은 `due_date`가 있을 때만 존재할 수 있다 (check 제약).
THE SYSTEM SHALL `done_at`은 status가 DONE일 때만 존재한다 (check 제약).

**REQ-2-2. DOING 단일 보장**
THE SYSTEM SHALL DOING 상태인 task가 동시에 2개 이상 존재하지 않도록 partial unique index로 DB 수준에서 방어한다.
THE SYSTEM SHALL 해당 인덱스는 삭제되지 않은 행(`deleted_yn = 'N'`)만 대상으로 한다.
THE SYSTEM SHALL 서비스 계층에서도 트랜잭션 안에 DOING 단일 규칙을 보장한다.

**REQ-2-3. 논리 삭제 구조**
THE SYSTEM SHALL task는 물리 삭제하지 않고 `deleted_yn`으로 표시한다.
THE SYSTEM SHALL 모든 조회는 `deleted_yn = 'N'`인 행만 대상으로 하며, Entity의 `@SQLRestriction` 하나로 처리한다.
THE SYSTEM SHALL `deleted_yn`의 'Y'/'N'과 boolean 변환은 변환 클래스(AttributeConverter) 한 곳에서만 한다.
(DELETE API 자체는 Spec 2에서 추가한다.)

**REQ-2-4. done_at 기록**
THE SYSTEM SHALL task가 DONE으로 전환될 때 서울 시간 기준 현재 시각을 `done_at`에 기록한다.
THE SYSTEM SHALL DONE이 아닌 상태로 돌아가면 `done_at`을 null로 초기화한다 (이번 spec 범위에서는 DONE → 다른 상태 전환은 없으나 방어 처리).

---

### REQ-3. task API

**REQ-3-1. task 생성**
WHEN 클라이언트가 `POST /api/tasks`를 호출하면,
THE SYSTEM SHALL title, status, priority, category, due_date, due_time을 받아 task를 생성하고 201로 응답한다.
THE SYSTEM SHALL title은 앞뒤 공백을 제거한 뒤 비어 있으면 400, 최대 200자를 넘기면 400을 반환한다. (공백 제거는 요청 record 생성자에서 한다.)
THE SYSTEM SHALL category는 최대 50자, `due_time`은 `due_date`가 있을 때만 허용한다.
THE SYSTEM SHALL 생성 시 status는 WISH, TODO만 허용한다. DOING/DONE으로 생성하면 400을 반환한다.
THE SYSTEM SHALL status가 WISH/TODO/DOING/DONE 외의 값(잘못된 enum)이면 400을 반환한다.
THE SYSTEM SHALL `due_time`만 있고 `due_date`가 없으면 400을 반환한다.

**REQ-3-2. task 목록 조회**
WHEN 클라이언트가 `GET /api/tasks`를 호출하면,
THE SYSTEM SHALL 전체 task 목록을 반환한다.
WHEN 클라이언트가 `GET /api/tasks?status=TODO`처럼 status 파라미터를 전달하면,
THE SYSTEM SHALL 해당 상태의 task만 필터링해 반환한다.
WHEN 클라이언트가 `GET /api/tasks?status=TODO,DOING`처럼 여러 status를 전달하면,
THE SYSTEM SHALL 해당 상태들의 task를 모두 반환한다.
> 화면 렌더링은 REQ-3-3의 `GET /api/dashboard`를 쓴다. `GET /api/tasks`는 범용 자원 조회용으로 둔다.

**REQ-3-3. 화면 전체 조회 (대시보드)**
WHEN 클라이언트가 `GET /api/dashboard`를 호출하면,
THE SYSTEM SHALL 상단 바(오늘 날짜·요일, 지금 집중할 일, 오늘 진행률), 오늘 할 일(TODO·DOING을 정렬·최대 7개·숨김 건수), 한 일(오늘 완료·최대 4개·완료 건수), 하고 싶은 일(WISH·최대 4개·총 개수·숨김 건수) 데이터를 한 번에 반환한다.
THE SYSTEM SHALL 각 task 항목에 마감 표기 문구(dueLabel)와 긴급 여부(urgent)를 백엔드에서 계산해 포함한다.
THE SYSTEM SHALL 정렬·표시 제한·숨김 건수·진행률·집계를 백엔드에서 계산한다.
THE SYSTEM SHALL 일정 패널과 하단 바는 이번 spec 범위가 아니므로 빈 값 또는 자리 표시용 데이터로 둔다.
THE SYSTEM SHALL 상단 바에 서울 기준 오늘 날짜(`today`, ISO)를 포함해 빠른 추가가 이 값을 사용하게 한다.

**REQ-3-4. 상태 전환 - DONE (체크)**
WHEN 클라이언트가 `PATCH /api/tasks/{id}`에 `{"status": "DONE"}`을 보내면,
THE SYSTEM SHALL 해당 task의 status를 DONE으로 바꾸고 done_at을 현재 시각(서울 기준)으로 기록한다.
THE SYSTEM SHALL TODO 또는 DOING 상태에서만 DONE으로 전환할 수 있다. 그 외는 409를 반환한다.
THE SYSTEM SHALL 변경된 task를 200으로 반환한다.

**REQ-3-5. 상태 전환 - DOING (▶ 버튼)**
WHEN 클라이언트가 `PATCH /api/tasks/{id}`에 `{"status": "DOING"}`을 보내면,
THE SYSTEM SHALL 기존에 DOING인 task가 있으면 해당 task의 status를 TODO로 되돌린다.
THE SYSTEM SHALL 요청 대상 task의 status를 DOING으로 바꾼다.
THE SYSTEM SHALL 두 변경을 하나의 트랜잭션으로 처리하되, 기존 DOING을 먼저 TODO로 저장·flush한 뒤 대상을 DOING으로 저장한다 (partial unique index 위반 방지).
THE SYSTEM SHALL TODO 상태에서만 DOING으로 전환할 수 있다. 그 외는 409를 반환한다.
THE SYSTEM SHALL 변경된 task를 200으로 반환한다.

**REQ-3-6. 상태 전환 - TODO 승격 (이번 주 버튼)**
WHEN 클라이언트가 `PATCH /api/tasks/{id}`에 `{"status": "TODO"}`를 보내고 대상이 WISH 상태이면,
THE SYSTEM SHALL status를 TODO로, priority를 MEDIUM으로, due_date를 이번 주 일요일(서울 기준)로 설정하고 due_time은 null로 둔다.
THE SYSTEM SHALL WISH가 아닌 task에 TODO 전환을 요구하면 409를 반환한다.
> WISH에서 DOING/DONE으로의 직접 전환은 허용하지 않고 409를 반환한다.

**REQ-3-7. 오류 응답 형식**
THE SYSTEM SHALL 모든 오류 응답은 `{"code": "...", "message": "..."}` 형식으로 반환하며, message는 화면에 바로 보여줄 수 있는 한국어로 한다.
THE SYSTEM SHALL 입력 검증 실패(Bean Validation)·잘못된 JSON 본문·잘못된 enum 값·쿼리 파라미터 타입 불일치는 400을 반환한다.
THE SYSTEM SHALL 비즈니스 검증 오류(예: 생성 시 status 위반, due_time만 있고 due_date가 없음)도 400을 반환한다.
THE SYSTEM SHALL 없는 리소스는 404, 비즈니스 규칙 위반(예: 상태 전환 불가)은 409, 그 외는 500을 사용한다.

---

### REQ-4. 화면 구성

**REQ-4-0. 역할 분리**
THE SYSTEM SHALL 정렬, 표시 개수 제한, "N개는 숨김" 건수, 마감 표기 문구(dueLabel), 긴급 여부(urgent), 진행률, 집계(오늘 완료 건수)를 백엔드에서 계산해 응답에 담는다.
THE SYSTEM SHALL 프론트엔드는 서버가 내려준 값을 그리기만 하고, 정렬·필터·집계·날짜·시간 계산·마감 문구·긴급 판정을 하지 않는다.
THE SYSTEM SHALL 시계(HH:mm)만 브라우저 시간으로 표시하되, 판정에는 쓰지 않는다.

**REQ-4-1. 전체 레이아웃**
THE SYSTEM SHALL 1920×1080 해상도에서 스크롤 없이 한 화면에 모든 요소가 보여야 한다.
THE SYSTEM SHALL 전체 높이는 100vh로 고정하고 페이지 스크롤을 막는다.
THE SYSTEM SHALL 다크 테마(`data-bs-theme="dark"`)를 적용하고, 색은 CSS 변수로 관리한다.
THE SYSTEM SHALL 글꼴은 본문 Noto Sans KR, 숫자·시각은 IBM Plex Mono를 사용하며 로컬 파일로 포함한다 (글꼴 파일은 별도 task로 추가).
THE SYSTEM SHALL Bootstrap 5.3.8은 CDN이 아닌 `static/vendor/bootstrap-5.3.8/` 로컬 파일(`css/bootstrap.min.css`, `js/bootstrap.bundle.min.js`)을 사용한다.

**REQ-4-2. 상단 바**
THE SYSTEM SHALL 시계(HH:mm)를 브라우저 시간으로 표시하고 30초마다 자동 갱신한다.
THE SYSTEM SHALL 날짜(M월 D일), 요일, 연도를 서버가 내려준 값으로 표시한다.
THE SYSTEM SHALL DOING 상태 task가 있으면 파란 점과 함께 제목을 "지금 집중할 일"로 표시한다.
THE SYSTEM SHALL DOING 상태 task가 없으면 "▶를 눌러 집중할 일을 골라 보세요" 안내를 보여준다.
THE SYSTEM SHALL 오늘 진행률(완료 수 / (남은 TODO·DOING 수 + 오늘 완료 수))을 서버가 계산한 퍼센트 막대와 "N/M" 숫자로 표시한다. 분모가 0이면 0%로 표시한다.

**REQ-4-3. 오늘 할 일 패널**
THE SYSTEM SHALL 서버가 정렬한 TODO·DOING 목록(최대 7개)을 받아 그린다. 정렬 기준: DOING 먼저 → 우선순위(HIGH→MEDIUM→LOW) → 마감 빠른 순(마감 없음은 뒤) → 생성 시각 오름차순.
THE SYSTEM SHALL 초과 건수는 서버가 내려준 "N개는 숨김" 값으로 표시한다.
THE SYSTEM SHALL "남은 N개" 문구를 헤더에 표시한다.
THE SYSTEM SHALL 각 행에 체크 버튼, 제목, 우선순위, 마감 표시(서버 dueLabel), ▶ 버튼을 포함한다.
THE SYSTEM SHALL DOING 행은 파란 배경 강조와 "진행 중" 뱃지를 표시하고 ▶ 버튼을 숨긴다.
THE SYSTEM SHALL 마감 표시 규칙(서버가 계산)을 따른다:
  - due_time이 있으면 "[날짜] HH:mm 마감"으로 표시한다. 날짜는 오늘이면 생략, 내일이면 "내일 ", 그 외는 "M/D "를 붙인다(예: "15:00 마감", "내일 15:00 마감", "11/8 15:00 마감").
  - 시각 마감의 남음/지남: 지났으면 "· N분 지남" / "· N시간 M분 지남"(1시간 이상) / "· N일 지남"(하루 이상), 2시간 이내 남음이면 "· N분 남음" / "· 1시간 남음"(정확히 60분) / "· 1시간 M분 남음"을 붙인다. 2시간 이상 남음은 문구 없이 "마감"만 표시한다.
  - due_time 없이 due_date만 있으면: 오늘이면 "오늘 중", 내일이면 "내일 마감", due_date가 이번 주 일요일과 같으면 "이번 주 마감", 그 외 날짜면 "M/D 마감"으로 표시한다.
  - due_date가 없으면 "마감 없음"으로 표시한다.
THE SYSTEM SHALL 시각이 있는 마감이 2시간 이내이거나 지났으면 서버 urgent 값으로 주황(긴급) 표시를 한다. 날짜만 있는 마감은 긴급으로 표시하지 않는다.
THE SYSTEM SHALL 우선순위가 HIGH인 task는 주황색으로 표시한다.
THE SYSTEM SHALL 하단에 빠른 추가 입력창을 두고, Enter 입력 시 TODO로 생성한다(priority=MEDIUM, due_date=서버가 내려준 today).
THE SYSTEM SHALL 공백만 있는 입력은 추가하지 않는다. 추가 성공 시 입력창을 비운다.
THE SYSTEM SHALL Enter 입력 시 한글 IME 조합 중인지 확인해 이중 입력을 방지한다.

**REQ-4-4. 한 일 패널**
THE SYSTEM SHALL 서버가 정렬한 오늘 완료 task(완료 시각 내림차순, 최대 4개)를 받아 그린다.
THE SYSTEM SHALL 각 행에 체크 아이콘(초록), 제목, 완료 시각(HH:mm)을 표시한다.
THE SYSTEM SHALL "N건 완료" 문구를 헤더에 표시한다.
THE SYSTEM SHALL 완료 항목이 없으면 "아직 끝낸 일이 없어요" 안내를 보여준다.

**REQ-4-5. 하고 싶은 일 패널**
THE SYSTEM SHALL 서버가 정렬한 WISH 상태 task(생성 시각 오름차순, 최대 4개)를 받아 그린다.
THE SYSTEM SHALL "총 N개" 문구를 헤더에 표시한다.
THE SYSTEM SHALL 각 행에 제목, 카테고리 태그(#카테고리), "이번 주" 승격 버튼을 포함한다.
THE SYSTEM SHALL 4개 초과인 경우 서버가 내려준 "외 N개는 접어 두었어요"를 표시한다.

**REQ-4-6. 왼쪽 일정 패널 (자리만)**
THE SYSTEM SHALL "오늘 일정" 패널 자리를 잡아두되, "Spec 3에서 구현 예정" 안내 문구만 표시한다.

**REQ-4-7. 하단 바 (자리만)**
THE SYSTEM SHALL 하단 바 자리를 잡아두되, "Spec 4에서 구현 예정" 안내 문구만 표시한다.

---

### REQ-5. 프론트엔드 동작 규칙

**REQ-5-1. 자동 갱신**
THE SYSTEM SHALL 30초마다 `GET /api/dashboard`로 화면 전체 데이터를 다시 가져온다.
THE SYSTEM SHALL 자동 갱신 시 입력창의 포커스와 입력 중인 내용을 유지한다.

**REQ-5-2. 오류 처리**
THE SYSTEM SHALL API 호출이 실패하면 마지막으로 성공한 데이터를 화면에 유지하고, 작은 오류 표시만 보여준다.
THE SYSTEM SHALL 빈 화면이 되지 않도록 한다.

**REQ-5-3. 이벤트 처리**
THE SYSTEM SHALL 체크, ▶, "이번 주" 버튼은 이벤트 위임으로 처리한다.
THE SYSTEM SHALL 아이콘 버튼에는 `aria-label`을 붙인다.
THE SYSTEM SHALL 체크 버튼은 호버 시 초록 테두리와 연한 초록 배경, 체크 아이콘이 나타난다.
THE SYSTEM SHALL DOING 행의 파란 점은 2초 주기로 깜빡이며, `prefers-reduced-motion`이면 애니메이션을 끈다.

---

### REQ-6. 개발용 샘플 데이터

**REQ-6-1. 샘플 데이터 스크립트**
THE SYSTEM SHALL 개발용 샘플 데이터를 `scripts/dev-seed.sql`에 두고 수동으로 실행한다. (Flyway 마이그레이션이 아님)
THE SYSTEM SHALL 샘플 데이터는 WISH 상태 task 7개 이상을 포함하며, 카테고리(학습/운동/사이드 프로젝트), 우선순위가 다양하게 섞인다.
THE SYSTEM SHALL 샘플 데이터는 TODO 상태 task도 포함해 "오늘 할 일" 패널을 바로 확인할 수 있게 한다.
THE SYSTEM SHALL 앱이 자동으로 샘플 데이터를 넣는 로더는 만들지 않는다.

---

### REQ-7. 테스트

**REQ-7-1. TaskService 단위 테스트**
THE SYSTEM SHALL DOING 단일 규칙(▶ 버튼 시 기존 DOING → TODO) 검증 테스트를 작성한다.
THE SYSTEM SHALL WISH가 아닌 task에 TODO 전환(승격)을 요구할 때 409 예외가 발생함을 검증한다.
THE SYSTEM SHALL 허용되지 않은 상태 전환(예: WISH→DOING) 시 409 예외가 발생함을 검증한다.
THE SYSTEM SHALL DONE 전환 시 done_at이 기록됨을 검증한다.

**REQ-7-2. TaskRepository 통합 테스트 (Testcontainers)**
THE SYSTEM SHALL 오늘 완료 조회(리포지토리 `findDoneToday`)가 서울 기준 오늘 완료 건만 반환함을 검증한다.
THE SYSTEM SHALL 상태별 필터 조회가 올바르게 동작함을 검증한다.
THE SYSTEM SHALL DOING partial unique index(삭제 조건 포함) 위반 시 예외가 발생함을 검증한다.

**REQ-7-3. DueLabeler 단위 테스트**
THE SYSTEM SHALL 고정 Clock으로 마감 표기·긴급 여부의 경계값을 검증한다: 정확히 2시간 남음(urgent=false)·2시간 미만 남음(urgent=true)·정확히 0분(지금 마감)·지남, 자정 전후(23:59 vs 00:01), 일요일(`nextOrSame(SUNDAY)`가 오늘), 시각 없는 마감(항상 urgent=false), "마감 없음".

**REQ-7-4. DashboardService 단위 테스트**
THE SYSTEM SHALL 오늘 할 일 정렬(DOING → 우선순위 → 마감 → 생성 순), 최대 7/4개 제한과 숨김 건수, 진행률(0/50/100%, 분모 0이면 0%)을 검증한다.
