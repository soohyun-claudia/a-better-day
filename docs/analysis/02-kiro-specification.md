# A Better Day Kiro 문서 분석

## 분석 범위와 문서 확인 현황

기존 구조 보고서 [01-project-structure.md](01-project-structure.md)를 먼저 확인한 뒤, 아래 Kiro 문서를 모두 읽었다. 이 보고서는 문서에 정의된 의도·요구사항·설계·작업 상태를 정리한 것이며, 실제 소스 코드의 구현 완료 여부를 판정하지 않는다.

| 구분 | 파일 | 확인 |
|---|---|---|
| Steering | `.kiro/steering/code-conventions.md` | 확인 |
| Steering | `.kiro/steering/db-conventions.md` | 확인 |
| Steering | `.kiro/steering/frontend-conventions.md` | 확인 |
| Steering | `.kiro/steering/product.md` | 확인 |
| Steering | `.kiro/steering/structure.md` | 확인 |
| Steering | `.kiro/steering/tech.md` | 확인 |
| Spec 1 | `.kiro/specs/spec-1-task-core/requirements.md` | 확인 |
| Spec 1 | `.kiro/specs/spec-1-task-core/design.md` | 확인 |
| Spec 1 | `.kiro/specs/spec-1-task-core/tasks.md` | 확인 |

## 1. 프로젝트 요구사항 요약

### 제품 목적과 범위

원본: `.kiro/steering/product.md`, `.kiro/specs/spec-1-task-core/requirements.md`

- 개인 1명이 Windows 11의 localhost에서 계속 띄워 두는 업무용 대시보드다.
- 핵심 흐름은 `하고 싶은 일(WISH) → 할 일(TODO) → 하는 중(DOING) → 한 일(DONE)`이다.
- Spec 1은 로컬 PostgreSQL/Spring Boot 기반, task 상태 전환, 오늘 할 일·한 일·하고 싶은 일 패널을 대상으로 한다.
- `event` 일정 도메인은 제품에는 존재하지만 Spec 1에서는 패널 자리와 안내 문구만 제공하고 이후 Spec에서 구현한다.
- MVP에는 task 편집·삭제, WISH 관리, event 추가·수정·삭제까지 포함된다고 제품 문서에 정의되어 있으나, Spec 1은 핵심 상태 전환과 조회에 집중한다.
- 이후 범위는 주간 요약 강화와 반복 일정·반복 할 일, 장기 범위는 외부 캘린더 연동과 AWS 배포·인증이다.

### 확정된 핵심 규칙

- 시간대는 `Asia/Seoul`, 한 주는 월요일 시작·일요일 종료다.
- 마감은 `due_date`와 `due_time`으로 저장하고, “이번 주”는 해당 주 일요일 날짜로 저장한다.
- 우선순위는 `HIGH`, `MEDIUM`, `LOW`다.
- 오늘 할 일은 마감일과 무관하게 TODO·DOING 전체다.
- 삭제는 물리 삭제가 아닌 `deleted_yn` 논리 삭제다.
- DOING은 삭제되지 않은 task 중 최대 1개다.
- 날짜·시간 계산과 데이터 가공은 백엔드가 담당하고, 프론트엔드는 서버 응답을 렌더링한다.

## 2. Steering 개발 규칙

### 목적·구조·아키텍처

원본: `.kiro/steering/product.md`, `.kiro/steering/structure.md`, `.kiro/steering/tech.md`

- 프로젝트 표기는 저장소·폴더 `a-better-day`, Java 패키지 `com.abetterday`, DB/볼륨 `a_better_day`, 화면명 `A Better Day`를 사용한다.
- 도메인별 패키지 구조를 사용한다: `task`, `event`, `dashboard`, `common` 아래에 계층 패키지를 둔다.
- Controller는 요청·응답 변환만 하고 비즈니스 규칙은 Service에 둔다.
- 호출 방향은 `controller → service → repository`이며 도메인 간 직접 호출은 피한다.
- Dashboard는 조회 전용 조합 계층으로 task/event Service를 사용하고 별도 repository/domain은 두지 않는다.
- 화면은 `index.html` 한 장, CSS는 `common.css` 하나를 기준으로 한다.
- Bootstrap은 로컬 vendor 파일을 사용하고 CDN은 사용하지 않는다.

### 기술 및 실행 제약

원본: `.kiro/steering/tech.md`

- Java 25, Spring Boot 4.1.2, Spring Web, Spring Data JPA/Hibernate, Bean Validation, Flyway, Lombok을 사용한다.
- DB는 PostgreSQL이며 Oracle과 H2는 사용하지 않는다.
- 프론트엔드는 HTML, Bootstrap 5.3.8, 바닐라 JavaScript다. React, 번들러, npm 빌드는 사용하지 않는다.
- Spring Boot가 정적 파일과 REST API를 함께 제공한다.
- 로그인과 Spring Security는 현재 범위에 넣지 않는다.
- DB는 Docker Compose와 named volume으로 운영하고 `restart: unless-stopped`를 사용한다.
- DB 접속 정보는 `.env`로 분리하고 Git에 올리지 않는다. 앱은 localhost에만 접근한다.
- 새 라이브러리·의존성 추가는 사유 설명과 확인이 필요하다. Lombok과 Testcontainers는 사전 승인된 예외다.

### Java·API·예외·로깅 규칙

원본: `.kiro/steering/code-conventions.md`, `.kiro/steering/tech.md`

- Java 들여쓰기 4칸, HTML/CSS/JS 2칸, 한 줄 120자 이내, 파일 끝 개행을 지킨다.
- 주석·로그는 한국어, 코드 식별자는 영어로 작성한다. 불필요한 코드·주석 처리 코드·import는 남기지 않는다.
- 모든 Java 메서드와 JS 함수에 한 줄 요약 주석을 둔다. 메서드 내부 주석은 코드로 드러나지 않는 “왜”를 적는다.
- 생성자 주입만 사용하고 Controller/Service에는 `@RequiredArgsConstructor`를 사용한다.
- Entity를 API로 직접 노출하지 않고 Java record 요청·응답 DTO를 사용한다.
- Service에 트랜잭션을 두고, 기본적으로 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`을 둔다.
- `Clock`을 주입해 날짜·시간을 계산한다. 직접적인 `LocalDate.now()` 호출은 금지한다.
- null 대신 Optional 또는 빈 컬렉션을 사용한다.
- 표시 최대 개수, 긴급 기준 2시간, 임박 기준 15분 등 매직 값은 상수나 설정으로 분리한다.
- REST 경로는 `/api/` 아래 복수형 명사이며 동사를 쓰지 않는다. 성공 상태는 200/201/204, 입력 오류 400, 없음 404, 규칙 위반 409다.
- 오류 응답은 `{ "code": "...", "message": "..." }`로 통일하고 `@RestControllerAdvice` 한 곳에서 변환한다.
- SLF4J를 사용하고 `System.out.println`은 금지한다. 상태 변경 info, 예상된 규칙 위반 warn, 예기치 않은 오류 error를 사용한다.
- 사용자 입력 전체나 내부 스택 트레이스를 로그·응답에 노출하지 않는다.

### DB 설계·운영 규칙

원본: `.kiro/steering/db-conventions.md`, `.kiro/steering/tech.md`

- Flyway 파일은 `src/main/resources/db/migration/V{번호}__{설명}.sql` 형식이다.
- 이미 적용된 migration은 수정하지 않고 새 번호를 추가한다. 하나의 파일에는 하나의 목적만 둔다.
- 테이블·컬럼은 소문자 `snake_case`, 테이블은 단수형, PK는 `bigint generated always as identity`다.
- 문자열 길이를 명시한 `varchar(n)`을 사용하며, 일반 시각은 `timestamptz`, 서울 로컬 의미의 due 값은 `date`·`time`을 사용한다.
- `ddl-auto=validate`, `open-in-view=false`를 고정한다.
- enum은 `@Enumerated(EnumType.STRING)`, 논리 삭제는 `@SQLRestriction`과 하나의 `AttributeConverter`로 처리한다.
- 모든 삭제 조회는 `deleted_yn = 'N'`만 대상으로 하고, partial/unique index에도 삭제 조건을 둔다.
- task의 DOING 단일성·DONE과 `done_at`의 일관성·due_date/due_time 관계는 Service와 DB 제약으로 함께 방어한다.
- event의 종료 시각은 시작 시각보다 늦어야 하고 `start_at` 인덱스를 둔다.
- `docker compose down -v` 등 로컬 DB 삭제 명령은 실행 전 확인이 필요하다.
- 백업은 주 1회 `pg_dump`로 `backups/`에 파일 형태로 남기고 Git에 올리지 않는다.
- 샘플 데이터는 migration이 아닌 `scripts/dev-seed.sql`에 두고 수동 실행한다.

### 프론트엔드 규칙

원본: `.kiro/steering/frontend-conventions.md`, `.kiro/steering/code-conventions.md`

- `id`는 camelCase, `class`는 underscore 기반이며 class는 `형태_의미_상태` 순서를 따른다.
- 시맨틱 HTML과 `button`을 사용하고, 클릭 가능한 요소에 div 이벤트를 붙이지 않는다.
- 100vh·스크롤 없는 다크 대시보드, 기준 해상도 1920×1080을 사용한다. 반응형 크기는 rem 또는 clamp를 우선한다.
- 색은 CSS 변수로 정의한다. 주황은 긴급/높은 우선순위, 파랑은 DOING, 초록은 완료, 빨강은 삭제·입력 오류에만 사용한다.
- JS는 ES 모듈이며 `const` 우선, `var` 금지, `===`만 사용한다.
- `fetch`는 `api.js`에서만, 렌더링은 `render.js`, 이벤트·갱신은 `app.js`, Bootstrap Modal API는 `modal.js`에서만 호출한다.
- 정렬·필터·집계·날짜 계산·마감 문구·긴급 판정은 프론트에서 하지 않는다. 단순 레이아웃 환산과 시계 표시는 예외다.
- 사용자 입력은 `textContent` 또는 이스케이프를 사용해 XSS를 막고, Enter 처리 시 한글 IME 조합을 확인한다.
- 30~60초마다 갱신하되 입력 포커스·내용을 유지하고, 실패하면 마지막 정상 화면과 작은 오류 표시를 유지한다.
- 반복 동작은 이벤트 위임, 아이콘 버튼은 `aria-label`, `focus-visible`, reduced-motion 대응을 사용한다.
- 편집 모달은 Bootstrap 5.3.8 Modal을 사용하고 삭제 전 확인 단계를 둔다.

### 테스트 정책

원본: `.kiro/steering/code-conventions.md`, `.kiro/steering/tech.md`

- JUnit 5와 Spring Boot Test를 사용한다.
- Repository·통합 테스트는 운영과 같은 PostgreSQL Testcontainers를 우선한다.
- 비즈니스 규칙이 있는 Service는 단위 테스트를 반드시 작성한다.
- given/when/then 주석, 테스트 하나당 하나의 동작을 원칙으로 한다.
- 고정 Clock으로 마감 표기·긴급 판정·주 계산을 테스트하고, 자정·일요일·정확히 2시간·15분 경계를 포함한다.

## 3. 전체 Spec 목록

현재 `.kiro/specs/`에는 다음 Spec 하나가 있다.

| Spec 이름 | 문서 |
|---|---|
| Spec 1: 프로젝트 기반 + task 핵심 기능 | `.kiro/specs/spec-1-task-core/requirements.md`, `design.md`, `tasks.md` |

별도의 추가 문서는 확인되지 않았다.

## 4. 기능별 요구사항 및 Acceptance Criteria

요구사항 원본은 `.kiro/specs/spec-1-task-core/requirements.md`다. 아래 항목의 Acceptance Criteria는 원문의 EARS 조건과 SHALL 문장을 요약한 것이다.

### REQ-1. 로컬 개발 환경

- `REQ-1-1`: `docker compose up -d` 시 PostgreSQL 17이 시작되고 named volume으로 데이터가 유지되며 `restart: unless-stopped`가 설정되어야 한다.
- `REQ-1-2`: 호스트·포트·DB명·사용자·비밀번호는 `.env`에서 관리하고 Compose가 참조해야 한다. `.env`는 Git 제외, `.env.example`은 저장소에 있어야 한다.
- `REQ-1-3`: 앱 시작 시 `.env`/환경변수로 PostgreSQL에 연결하고 Flyway의 `V1__create_task.sql`을 자동 적용해야 한다. JPA는 `validate`, open-in-view는 false다.

### REQ-2. task 데이터 모델

- `REQ-2-1`: task 테이블의 id, title, status, priority, category, due_date, due_time, done_at, deleted_yn, created_at, updated_at과 각 타입·기본값·check 제약을 갖춰야 한다. due_time은 due_date와 함께만 존재하고, DONE과 done_at은 일치해야 한다.
- `REQ-2-2`: 삭제되지 않은 DOING은 DB partial unique index와 Service 트랜잭션으로 동시에 1개만 허용해야 한다.
- `REQ-2-3`: 논리 삭제와 `@SQLRestriction`, 단일 AttributeConverter를 사용해야 한다. DELETE API 자체는 Spec 2로 연기한다.
- `REQ-2-4`: DONE 전환 시 서울 기준 현재 시각을 기록하고, DONE 외 상태로 전환 시 done_at을 null로 초기화해야 한다.

### REQ-3. task API

- `REQ-3-1`: `POST /api/tasks`가 task를 생성하고 201을 반환해야 한다. 제목 trim·빈 값·200자 제한, category 50자 제한, due 관계 검증, 생성 시 WISH/TODO만 허용해야 한다.
- `REQ-3-2`: `GET /api/tasks`가 전체 목록을 반환하고, 단일·쉼표 구분 다중 status 필터를 지원해야 한다.
- `REQ-3-3`: `GET /api/dashboard`가 상단 바, 오늘 할 일, 한 일, 하고 싶은 일을 한 번에 반환해야 한다. 정렬·제한·숨김 수·진행률·집계·dueLabel·urgent는 서버가 계산하고, 일정·하단 바는 빈 값/자리표시자다.
- `REQ-3-4`: `PATCH /api/tasks/{id}`에 DONE을 보내면 TODO/DOING만 DONE으로 전환되고 done_at이 기록되며, 그 외는 409다.
- `REQ-3-5`: DOING 전환 시 기존 DOING을 먼저 TODO로 저장·flush하고 대상 task를 DOING으로 바꾸며, TODO에서만 허용한다.
- `REQ-3-6`: WISH에 TODO를 보내면 MEDIUM, 이번 주 일요일, due_time null로 승격하고, WISH 이외는 409다.
- `REQ-3-7`: 검증·JSON·enum·파라미터 오류는 400, 없음은 404, 상태 규칙 위반은 409, 기타는 500이며 모든 오류는 공통 `{code,message}` 형식이다.

### REQ-4. 화면 구성

- `REQ-4-0`: 서버가 정렬·제한·숨김 수·dueLabel·urgent·진행률·집계를 계산하고 프론트는 그린다. 브라우저 시계는 표시 전용이다.
- `REQ-4-1`: 1920×1080에서 스크롤 없이 100vh 다크 화면, 로컬 Bootstrap 5.3.8, Noto Sans KR/IBM Plex Mono를 사용한다.
- `REQ-4-2`: 상단 시계, 서버 날짜, DOING 집중 task/안내, 진행률을 표시하고 시계는 30초마다 갱신한다.
- `REQ-4-3`: 서버 정렬 TODO·DOING 최대 7개, 숨김 수, 체크·제목·우선순위·dueLabel·▶, DOING 강조, 빠른 추가와 IME 방지를 제공한다. 긴급과 우선순위 표시 규칙도 포함한다.
- `REQ-4-4`: 오늘 완료를 최근 순 최대 4개와 전체 완료 수로 표시하고 빈 상태 문구를 제공한다.
- `REQ-4-5`: WISH를 생성 순 최대 4개, 총 수·숨김 수·카테고리·이번 주 승격으로 표시한다.
- `REQ-4-6`: 일정 패널은 자리와 “Spec 3에서 구현 예정” 안내만 표시한다.
- `REQ-4-7`: 하단 바는 자리와 “Spec 4에서 구현 예정” 안내만 표시한다.

### REQ-5. 프론트엔드 동작

- `REQ-5-1`: 30초마다 dashboard를 다시 가져오며 입력 포커스와 입력값을 보존한다.
- `REQ-5-2`: API 실패 시 마지막 성공 화면을 유지하고 작은 오류만 표시한다.
- `REQ-5-3`: 체크·▶·이번 주 버튼 이벤트 위임, aria-label, 호버 스타일, DOING pulse와 reduced-motion을 제공한다.

### REQ-6. 개발용 샘플 데이터

- `REQ-6-1`: `scripts/dev-seed.sql`에 수동 실행용 샘플을 두고 WISH 7개 이상, 다양한 카테고리·우선순위, TODO를 포함해야 한다. 자동 로더는 금지한다.

### REQ-7. 테스트

- `REQ-7-1`: TaskService 단위 테스트로 DOING 단일 전환, 잘못된 승격·상태 전환 409, DONE의 done_at 기록을 검증한다.
- `REQ-7-2`: PostgreSQL Testcontainers 통합 테스트로 서울 기준 오늘 완료, 상태 필터, DOING partial unique index를 검증한다.
- `REQ-7-3`: 고정 Clock으로 DueLabeler의 정확히 2시간·2시간 미만·0분·지남, 자정·일요일·날짜 전용·마감 없음 경계를 검증한다.
- `REQ-7-4`: DashboardService 단위 테스트로 정렬, 7/4개 제한, 숨김 수, 진행률 0/50/100% 및 분모 0을 검증한다.

## 5. 설계된 API 및 데이터 모델

원본: `.kiro/specs/spec-1-task-core/design.md` 2장~4장, `.kiro/steering/tech.md` API 규칙

### API

| Method | Endpoint | 용도 | 요청/응답 |
|---|---|---|---|
| POST | `/api/tasks` | task 생성 | `CreateTaskRequest` → 201 `TaskResponse` |
| GET | `/api/tasks` | 목록·status 필터 | `TaskResponse[]` |
| GET | `/api/dashboard` | 화면 전체 조회 | `DashboardResponse` |
| PATCH | `/api/tasks/{id}` | 상태 전환·승격 | `ChangeStatusRequest` → `TaskResponse` |

Design은 상태 전환을 동사 endpoint 없이 PATCH body의 `status`로 통합한다. Tech Steering에는 이후 범위의 `DELETE /api/tasks/{id}`, `/api/events`도 규칙 예시로 정의되어 있으나, Spec 1 API 표에는 없다.

### Task 데이터 모델

- DB: `task(id, title, status, priority, category, due_date, due_time, done_at, deleted_yn, created_at, updated_at)`
- status: WISH/TODO/DOING/DONE
- priority: HIGH/MEDIUM/LOW
- `due_time` 단독 금지, DONE과 `done_at` 일치, 삭제 여부 Y/N check
- DOING 단일 partial unique index: `status='DOING' AND deleted_yn='N'`
- 일반 시각은 `OffsetDateTime`/`timestamptz`, due는 서울 로컬 의미의 `LocalDate`/`LocalTime`/date/time
- JPA Entity는 `@SQLRestriction`, enum 문자열 매핑, `AttributeConverter`, Creation/UpdateTimestamp를 사용한다.

### DTO와 대시보드 응답

- `CreateTaskRequest`: title, status, priority, category, dueDate, dueTime
- `ChangeStatusRequest`: status
- `TaskResponse`: 기본 task 필드와 `dueLabel`, `urgent`, `doneAt`, `createdAt`, `updatedAt`
- `DashboardResponse`: `TopBar`, `TodayTodoPanel`, `DonePanel`, `WishPanel`
- `TopBar`: 서울 기준 `today`, dateLabel, doingTask, progress
- `TodayTodoPanel`: 최대 7개 items, totalCount, hiddenCount
- `DonePanel`: 최대 4개 items, doneCount
- `WishPanel`: 최대 4개 items, totalCount, hiddenCount

### 시간·마감 계산

`DueLabeler`가 단일 책임으로 dueLabel과 urgent를 계산한다. 시각 있는 마감은 오늘/내일/날짜 접두어와 마감까지 남거나 지난 시간을 표시하며, 지났거나 2시간 미만인 경우 urgent다. 설계의 의사코드는 `diff < 120`으로 urgent를 계산하므로 정확히 120분은 긴급하지 않다. 날짜만 있는 마감은 오늘 중·내일·이번 주 마감·M/D 마감으로 표시하고 항상 urgent=false다.

### 프론트엔드 설계

Spec 1 목표 파일은 `static/index.html`, `static/css/common.css`, `static/js/api.js`, `static/js/render.js`, `static/js/app.js`와 기존 Bootstrap vendor다. `modal.js`와 로컬 글꼴은 Spec 2로 연기되었다. `api.js`는 fetch, `render.js`는 계산 없는 DOM 렌더링, `app.js`는 이벤트·30초 갱신을 담당한다.

## 6. Tasks 문서상 진행 상태

원본: `.kiro/specs/spec-1-task-core/tasks.md`

체크박스는 문서에 표시된 상태일 뿐 실제 코드 완료 여부가 아니다.

### Phase별 상태

| Phase | 범위 | 문서상 상태 |
|---|---|---|
| Phase 1 | 환경 설정 T-01~T-02 | 모두 `[ ]` |
| Phase 2 | DB·샘플 T-03~T-04 | T-03, T-04 `[x]`; T-04의 자동 로더 금지 하위 항목 `[ ]` |
| Phase 3 | 백엔드 T-05~T-12 | 전체 `[x]` |
| Phase 4 | 프론트엔드 T-13~T-17 | 모두 `[ ]` |
| Phase 5 | 테스트 T-18~T-21 | 모두 `[ ]` |
| Phase 6 | 검증 T-22 | 전체 `[ ]` |

### 세부 작업 목록

- T-01: Compose, `.env.example`, `.env`, `.gitignore` 확인 — `[ ]`
- T-02: Gradle 의존성·annotation processor·application.yml·DB 연결 확인 — `[ ]`
- T-03: Flyway task migration — `[x]`
- T-04: 개발 seed — `[x]`; 자동 로더 미작성 확인 하위 항목은 `[ ]`
- T-05: 공통 예외 처리 — `[x]`
- T-06: Clock/JPA 공통 설정 — `[x]`; JpaConfig 생략 하위 항목도 문서상 `[x]`
- T-07: task 도메인 — `[x]`
- T-08: repository — `[x]`
- T-09: DueLabeler — `[x]`
- T-10: TaskService — `[x]`
- T-11: TaskController — `[x]`
- T-12: Dashboard 조회 — `[x]`
- T-13: 정적 파일·CSS — `[ ]`
- T-14: index.html 레이아웃 — `[ ]`
- T-15: api.js — `[ ]`
- T-16: render.js — `[ ]`
- T-17: app.js — `[ ]`
- T-18: TaskServiceTest — `[ ]`
- T-19: DueLabelerTest — `[ ]`
- T-20: DashboardServiceTest — `[ ]`
- T-21: TaskRepositoryTest — `[ ]`
- T-22: 통합 실행·브라우저·기능 검증 — `[ ]`

## 7. 문서 간 불일치 및 누락 사항

아래는 문서 자체의 표현·범위·상호 정합성을 기준으로 한 확인 사항이다. 실제 코드 불일치 판정은 다음 단계에서 수행해야 한다.

### 확인된 불일치

1. **애플리케이션 포트가 문서 간 다르다.**
   - `design.md` 시스템 구성은 Spring Boot `:8080`이다.
   - `tasks.md` T-22도 `localhost:8080` 검증을 요구한다.
   - 기존 구조 분석에서 확인한 `src/main/resources/application.yml`은 서버 포트 `8090`으로 기록되어 있다.
   - 원본: `.kiro/specs/spec-1-task-core/design.md` 3행, `tasks.md` 187행, `docs/analysis/01-project-structure.md` 4장.
   - 다음 단계에서 기준 포트를 결정해야 한다.

2. **이벤트 도메인의 범위 표현이 혼재한다.**
   - `structure.md`에는 `event` 패키지와 event 도메인이 기본 구조로 제시된다.
   - `product.md`는 event를 제품 핵심 개념과 MVP 편집 대상으로 포함한다.
   - 그러나 `requirements.md` REQ-3-3, REQ-4-6/7과 `design.md`는 Spec 1에서 일정·하단 바를 자리표시자로 연기한다.
   - `tasks.md`에는 event 구현 task가 없다.
   - 따라서 Spec 1에서는 event를 구현하지 않는 해석이 일관적이나, 제품 MVP와 Spec 단계 경계는 명시적으로 확정할 필요가 있다.

3. **프론트엔드 글꼴 요구와 작업 분할이 다르다.**
   - `requirements.md` REQ-4-1은 Noto Sans KR·IBM Plex Mono 로컬 포함을 SHALL로 둔다.
   - `tasks.md` T-13은 글꼴 없이 진행하고 별도 task에서 추가한다고 적었지만, 해당 별도 task가 현재 tasks 목록에는 없다.
   - `design.md` 4-1도 `vendor/fonts/`를 Spec 2로 연기한다.

4. **초기 카테고리 요구와 구현 작업의 연결이 약하다.**
   - `requirements.md` 확정 전제는 학습·운동·사이드 프로젝트를 초기 카테고리로 제시한다.
   - REQ-6-1과 T-04는 seed 데이터의 카테고리 다양성만 요구한다.
   - 카테고리 목록을 애플리케이션 상수·검증 규칙으로 고정하는 별도 task는 없다.

### Requirements와 Design의 추적상 주의점

- REQ-3-3의 일정·하단 바 자리표시자와 Design 3-4의 “이번 spec 범위 밖”은 일치한다.
- REQ-3-4~3-6의 상태 전환 규칙과 Design 3-4의 전환 순서·도메인 메서드 설계는 대체로 일치한다.
- REQ-3-7의 오류 분류와 Design 3-5의 예외 핸들러 분류는 일치한다.
- REQ-4의 프론트 역할 분리와 Design 4-4의 모듈 분리는 일치한다.
- Requirements에는 프론트 자동 갱신 실패·IME·aria 등의 세부 기준이 있고 Design은 이를 app.js 설계 주석으로 요약한다. 구현 시 양쪽을 함께 추적해야 한다.

### Design과 Tasks의 누락·불일치

- Design의 테스트 설계는 Task T-18~T-21로 모두 대응되지만, tasks 문서상 전부 미체크다.
- Design의 `JpaConfig.java` 예시는 패키지 구조에 있으나 T-06은 “필요 시”로 두고 생략 가능하게 했다. 실제 필요 여부는 코드 검증 대상이다.
- Design의 `modal.js`와 글꼴은 Spec 2로 연기되어 Phase 4 task에 없다. 이는 Spec 1 범위 축소로 볼 수 있지만 REQ-4-1의 로컬 글꼴 SHALL과는 충돌 가능성이 있다.
- Design은 `TaskResponse`와 `DashboardResponse`의 세부 필드를 정의하지만, Tasks는 T-12에 조립 항목만 요약한다. 필드 단위 검증은 테스트·API 계약 검증에서 보완해야 한다.

### Steering과 Spec에서 추가 확인할 모호점

- `product.md`의 MVP 편집·event 기능과 Spec 1의 범위 분할 사이에서 “현재 단계의 MVP” 의미를 확정해야 한다.
- `tech.md`는 DELETE `/api/tasks/{id}`와 `/api/events`를 API 규칙 예시로 제시하지만, REQ-2-3은 DELETE API를 Spec 2로 연기한다. 현재 Spec 1에는 DELETE를 구현하지 않는 것이 맞는지 확인이 필요하다.
- `design.md` 시스템 다이어그램은 `index.html + JS`, 서버 8080을 전제로 하지만, 구조 분석 시점에는 템플릿·앱 JS가 없고 설정 포트는 8090이었다. 이는 문서와 현재 상태를 구분해 다음 단계에서 검증해야 한다.
- `product.md`에는 “이번 주 완료·요일별 완료·내일 일정”이 화면 요구로 있지만 REQ-3-3과 REQ-4-7에서는 하단 바를 Spec 4로 연기한다. 버전별 요구사항의 우선순위를 확정해야 한다.
- `requirements.md` REQ-3-1은 status를 입력으로 받는다고 하면서 생성 시 WISH/TODO만 허용하고, CreateTaskRequest 설계에는 status가 “필수”로 적혀 있다. 빠른 추가처럼 서버 기본값을 사용하지 않고 항상 status를 보내는 계약인지 확인이 필요하다.
- `requirements.md`의 “초기 카테고리”는 목록 자체가 확정인지 샘플 데이터 분류만 의미하는지 명확하지 않다. `product.md`는 카테고리 목록을 아직 정하지 않은 것으로 표시한다.

### 요구사항에 존재하지만 Tasks에서 직접 추적하기 어려운 항목

- REQ-4-1의 로컬 글꼴 포함(별도 task 미정)
- REQ-4-3의 상세 dueLabel 문구와 HIGH 색상 표시(렌더링 task에 포함되지만 별도 체크 항목 없음)
- REQ-5-2의 마지막 정상 화면 유지( T-17 하위 항목에는 있으나 독립 task 아님)
- REQ-6-1의 자동 로더를 만들지 않는 확인(T-04 하위 항목이 미체크)
- REQ-1-3의 앱 시작·DB·Flyway 실제 동작 확인(T-02에 포함되어 미체크)

## 8. 실제 코드 검증이 필요한 항목

다음은 문서 분석만으로 완료 여부를 판단하지 않고 후속 코드 리뷰·테스트에서 확인해야 한다.

1. 문서 포트 8080과 실제 `application.yml` 포트 8090 중 적용 기준.
2. `.env` 참조, Compose 변수, localhost 바인딩, Flyway 자동 실행과 JPA validate 설정.
3. DB migration의 모든 컬럼·check·partial index와 Entity 매핑의 일치.
4. DOING 단일 전환 시 기존 task 저장 후 flush 순서와 트랜잭션 경계.
5. DONE 전환의 서울 기준 `done_at`, 비-DONE 전환 시 null 초기화.
6. 요청 검증, status 전환 400/409 분류, 공통 오류 응답 형식.
7. DueLabeler의 정확히 120분 경계, 지남·0분·자정·일요일 규칙.
8. Dashboard의 정렬, 최대 표시 수, 숨김 수, 진행률 분모 0 처리와 서울 기준 집계.
9. API 응답의 null 필드 유지, camelCase, ISO-8601 오프셋 규칙.
10. 프론트엔드 파일 구조·Bootstrap 로딩 순서·fetch 위치·DOM textContent·IME·자동 갱신 보존.
11. 현재 Tasks에 미체크된 단위 테스트·Testcontainers 통합 테스트의 실제 존재와 실행 결과.
12. event·글꼴·편집 모달·삭제 기능이 Spec 1의 제외 범위인지 다음 Spec의 예정 범위인지.

이 보고서는 문서 분석 결과만 담았으며, 실제 구현 상태·품질·요구사항 충족 여부는 판단하지 않았다.
