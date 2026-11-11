# Spec 1 구현 태스크

## 진행 상태 범례
- [ ] 대기
- [~] 진행 중
- [x] 완료

---

## PHASE 1. 환경 설정

### T-01. docker-compose.yml 및 .env 구성
- [x] `docker-compose.yml` 작성: PostgreSQL 17, named volume `pgdata`, `restart: unless-stopped`, .env 참조
  - DB 포트는 `127.0.0.1:5433:5432`로 로컬 바인딩 (외부 노출 금지)
- [x] `.env.example` 작성: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`
- [x] `.env` 작성 (git 제외)
- [x] `.gitignore`에 `.env`, `backups/` 포함 확인

### T-02. Spring Boot 프로젝트 기본 설정
- [x] `build.gradle` 의존성 확인 및 추가: Spring Web, Spring Data JPA, Flyway, PostgreSQL Driver, Validation, Testcontainers, Lombok
- [x] `build.gradle` annotation processor 설정: `annotationProcessor 'org.projectlombok:lombok'`, 테스트용 `testAnnotationProcessor` (Gradle) 또는 `maven-compiler-plugin` annotation processor (Maven)
- [x] `application.yml`로 변환 (application.properties 제거): `spring.config.import: optional:file:.env[.properties]`로 .env 읽기, DB URL 환경 변수 참조, `ddl-auto=validate`, `open-in-view=false`, Flyway 설정
- [x] 앱 실행 후 DB 연결 및 Flyway 정상 동작 확인

---

## PHASE 2. DB 스키마 및 샘플 데이터

### T-03. Flyway V1 마이그레이션: task 테이블 생성
- [x] `V1__create_task.sql` 작성: task 테이블
  - `deleted_yn CHAR(1) NOT NULL DEFAULT 'N'` + `chk_task_deleted_yn` check 제약
  - check 제약: status, priority, due(due_time은 due_date 있을 때만), done_at(DONE ↔ done_at)
  - `uq_task_single_doing`: `WHERE status = 'DOING' AND deleted_yn = 'N'`
  - 조회 인덱스: `idx_task_status`, `idx_task_done_at` (partial, `AND deleted_yn = 'N'`)

### T-04. 개발용 샘플 데이터 스크립트
- [x] `scripts/dev-seed.sql` 작성 (Flyway 마이그레이션 아님, 수동 실행)
  - WISH 7개 이상 (학습·운동·사이드 프로젝트 카테고리, HIGH·MEDIUM·LOW 우선순위 혼합)
  - TODO 4개 (마감 다양: 시각 있음, 오늘 중, 내일, 이번 주, 마감 없음)
  - DOING 1개
  - DONE 3개 (done_at = 오늘 서울 시간대)
- [x] 앱이 자동으로 넣는 로더는 만들지 않음

---

## PHASE 3. 백엔드 도메인 및 API

### T-05. 공통 예외 처리
- [x] `ErrorResponse` record 작성
- [x] `BusinessException` 작성: code, message, HttpStatus(400/404/409)
- [x] `GlobalExceptionHandler` 작성: BusinessException(400/404/409)·MethodArgumentNotValidException(400)·HttpMessageNotReadableException(400, 잘못된 JSON/enum)·MethodArgumentTypeMismatchException(400, 쿼리 파라미터 타입 불일치)·그 외(500)

### T-06. 공통 설정 (Clock, JPA)
- [x] `ClockConfig` 작성: `Clock.system(ZoneId.of("Asia/Seoul"))` 빈
- [x] `JpaConfig` 작성 (필요 시) → 필요한 설정이 없어 생략

### T-07. Task 도메인 모델
- [x] `TaskStatus` enum: WISH, TODO, DOING, DONE
- [x] `TaskPriority` enum: HIGH, MEDIUM, LOW
- [x] `DeletedYnConverter` (AttributeConverter): 'Y'/'N' ↔ boolean 한 곳
- [x] `Task` entity 작성
  - `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@SQLRestriction("deleted_yn = 'N'")`
  - `@Enumerated(EnumType.STRING)` (status, priority)
  - `deleted` 필드: `@Column(name = "deleted_yn")` + `@Convert(converter = DeletedYnConverter.class)`
  - 시간 필드: `@CreationTimestamp` (createdAt), `@UpdateTimestamp` (updatedAt), `OffsetDateTime` (doneAt), `LocalDate` (dueDate), `LocalTime` (dueTime)
  - 도메인 메서드: `markDone(OffsetDateTime)`, `markDoing()`, `markTodo()`, `promote(LocalDate)` — DONE 외 전환은 done_at null

### T-08. TaskRepository
- [x] `TaskRepository` 작성 (`JpaRepository<Task, Long>`)
- [x] `findByStatusIn(List<TaskStatus>)` 메서드 추가
- [x] `findFirstByStatus(TaskStatus)` 메서드 추가 (DOING 단건 조회)
- [x] `findDoneToday(OffsetDateTime start, OffsetDateTime end)` (오늘 완료 조회, doneAt 내림차순)

### T-09. DueLabeler (마감 표기·긴급 계산, 한 곳)
- [x] `DueLabeler` 작성: `Clock` 주입, `Task` → `dueLabel`·`urgent` 계산
- [x] 규칙: due_time 있으면 날짜 접두어(오늘 생략/내일/"M/D ") + "HH:mm 마감" + 남음/지남 문구(지남: 분/시간분/일, 2시간 이내 남음: 분/"1시간 남음"(정확히 60)/"1시간 M분 남음", 2시간+ 남음은 문구 없음; urgent=지남 또는 2시간 이내), due_time 없으면 지남/오늘 중/내일/이번 주 일요일/M/D (urgent=false), due_date 없으면 "마감 없음"

### T-10. TaskService
- [x] `Clock` 주입받아 사용 (`LocalDate.now(clock)` 직접 `LocalDate.now()` 금지)
- [x] `createTask(CreateTaskRequest)` 구현 (title 공백 제거, status는 WISH/TODO만·위반 시 400, due_time은 due_date 있을 때만·단독이면 400)
- [x] `getTasks(List<TaskStatus>)` 구현
- [x] `changeStatus(Long, TaskStatus)` 구현: 전환 규칙 검사(409) + DOING 단일 규칙(flush 순서) + WISH→TODO 승격(due_date=이번 주 일요일, priority=MEDIUM), `@Transactional`
- [x] `TaskResponse` 매핑 시 DueLabeler로 dueLabel·urgent 계산

### T-11. TaskController
- [x] `POST /api/tasks` 구현 (201 + Location)
- [x] `GET /api/tasks` 구현 (status 쿼리 파라미터, 쉼표 구분 다중 값)
- [x] `PATCH /api/tasks/{id}` 구현 (본문 `{"status": ...}`, 상태 전환·승격)

### T-12. Dashboard (조회 전용)
- [x] `DashboardResponse` 및 중첩 record(TopBar, ProgressData, TodayTodoPanel, DonePanel, WishPanel) 작성
- [x] `DashboardService` 작성: 정렬·최대 7/4개·숨김 건수·진행률(분모 0이면 0%)·오늘 완료 집계·today/dateLabel/doingTask 조립 (`@Transactional(readOnly = true)`)
- [x] `DashboardController` 작성: `GET /api/dashboard`
- [x] 일정·하단 바는 빈/자리 데이터로 둠

---

## PHASE 4. 프론트엔드

### T-13. 정적 파일 기반 구성
- [ ] `static/vendor/bootstrap-5.3.8/` 파일 확인 (이미 있음, 다운로드 불필요)
- [ ] `common.css` 작성: `:root` CSS 변수, 기본 레이아웃 스타일
  - (글꼴 `@font-face`는 별도 task에서 추가, 이번 task에서는 글꼴 없이 진행)

### T-14. index.html 기본 레이아웃
- [ ] `index.html` 작성: `data-bs-theme="dark"`, 100vh 고정, 스크롤 방지
- [ ] Bootstrap 연결: `/vendor/bootstrap-5.3.8/css/bootstrap.min.css`, `/vendor/bootstrap-5.3.8/js/bootstrap.bundle.min.js` (일반 `<script>` 먼저), `/js/app.js` (`type="module"`)
- [ ] 상단 바 HTML 골격: 시계, 날짜·요일, 집중할 일, 진행률
- [ ] 3단 레이아웃 HTML 골격 (id camelCase): `#sectionSchedule`(자리), `#sectionTodo`, `.wrap_right_panels`(`#sectionDone`, `#sectionWish`)
- [ ] 하단 바 HTML 골격 (자리, `#bottomBar`)

### T-15. api.js 작성
- [ ] `fetchDashboard()` — GET /api/dashboard (화면 전체)
- [ ] `fetchTasks(statusFilter)` — GET /api/tasks (범용, 화면 미사용)
- [ ] `createTask(payload)` — POST /api/tasks
- [ ] `patchTask(id, body)` — PATCH /api/tasks/{id} (body 예: `{"status":"DOING"}`)
- [ ] 오류 발생 시 콘솔 오류 기록, 호출부에 예외 전파

### T-16. render.js 작성
- [ ] `renderTopBar(topBar)`: dateLabel, 집중할 일 or 안내 문구, 진행률 막대 (시계 HH:mm은 브라우저)
- [ ] `renderTodoPanel(todayTodo)`: 받은 정렬·최대 7개 items + hiddenCount만 그림, 각 행(체크·제목·우선순위·dueLabel·▶), DOING 강조
- [ ] `renderDonePanel(done)`: 받은 최대 4개 items + doneCount, 완료 시각, 빈 상태 문구
- [ ] `renderWishPanel(wish)`: 받은 최대 4개 items + totalCount + hiddenCount, 카테고리 태그, 이번 주 버튼
- [ ] `renderSchedulePlaceholder()` / `renderBottomBarPlaceholder()`
- [ ] `formatDeadline`/`isUrgent` 만들지 않음 (서버 dueLabel/urgent 그대로 표시)
- [ ] 사용자 입력 텍스트는 `textContent`로 삽입 (XSS 방지)

### T-17a. app.js 작성 (갱신·로드)
- [ ] `refreshAll()`: `fetchDashboard()` 후 각 패널 렌더링
- [ ] 초기 로드 (`DOMContentLoaded`)
- [ ] `setInterval(refreshAll, 30000)` 설정
- [ ] 30초 갱신 시 포커스·입력 내용 보존 처리
- [ ] 자정 감지: 날짜 변경 시 전체 재로드
- [ ] API 실패 시 오류 토스트 표시, 기존 화면 유지

### T-17b. app.js 작성 (이벤트·스타일)
- [ ] 이벤트 위임: 체크 → `patchTask(id, {status:'DONE'})` → 재조회
- [ ] 이벤트 위임: ▶ → `patchTask(id, {status:'DOING'})` → 재조회
- [ ] 이벤트 위임: "이번 주" → `patchTask(id, {status:'TODO'})` → 재조회
- [ ] 빠른 추가 Enter 처리: IME 조합 중 무시, 공백 무시, `dueDate: dashboard.topBar.today` 사용, 성공 시 입력창 초기화
- [ ] DOING 파란 점 깜빡임 애니메이션 (CSS, prefers-reduced-motion 대응)
- [ ] 체크 버튼 호버 스타일 적용

---

## PHASE 5. 테스트

### T-18. TaskService 단위 테스트
- [ ] `TaskServiceTest` 작성 (JUnit 5 + Mockito, 고정 Clock)
  - DOING 단일 규칙: ▶ 눌렀을 때 기존 DOING → TODO 전환 + flush 순서 확인
  - DOING이 없는 상태에서 ▶: 정상 동작 확인
  - WISH가 아닌 task에 TODO 전환(승격) 시 `BusinessException`(409) 발생 확인
  - WISH→DOING 등 허용되지 않은 전환 시 `BusinessException`(409) 발생 확인
  - DONE 전환 시 `done_at` != null 확인

### T-19. DueLabeler 단위 테스트
- [ ] `DueLabelerTest` 작성 (JUnit 5, 고정 Clock — 경계값 반드시 포함)
  - 시각 있는 마감 긴급 경계: 정확히 2시간 남음(urgent=false) / 2시간 미만 남음(urgent=true) / 정확히 0분(지금 마감) / 지남(urgent=true)
  - 시각 있는 마감 문구: "N분 남음", "1시간 남음"(정확히 60분), "1시간 N분 남음", "N분 지남", "N시간 M분 지남"(1시간 이상), "N일 지남"(하루 이상)
  - 날짜 접두어: 오늘(생략) / 내일("내일 ") / 그 외("M/D ") — "15:00 마감", "내일 15:00 마감", "11/8 15:00 마감"
  - 시각 없는 마감: due_date < 오늘 → "M/D 마감", == 오늘 → "오늘 중", == 내일 → "내일 마감", == 이번 주 일요일 → "이번 주 마감", 그 외 → "M/D 마감"
  - 자정 전후 경계: 오늘 23:59 vs 내일 00:01 (날짜 비교가 서울 자정 기준임 확인)
  - 일요일 경계: 오늘이 일요일일 때 `nextOrSame(SUNDAY)`가 오늘을 가리키는지 ("이번 주 마감" 판정)
  - 시각 없는 마감은 항상 urgent=false 확인
  - due_date 없음 → "마감 없음", urgent=false 확인

### T-20. DashboardService 단위 테스트
- [ ] `DashboardServiceTest` 작성 (JUnit 5 + Mockito, 고정 Clock)
  - 오늘 할 일 정렬: DOING → priority(HIGH→MEDIUM→LOW) → 마감 빠른 순(없음은 뒤) → 생성 오름차순
  - 오늘 할 일 최대 7개 제한 + hiddenCount(전체 - 표시) 계산 확인
  - 한 일 최대 4개 제한(doneAt 내림차순) + doneCount(오늘 완료 전체) 확인
  - 하고 싶은 일 최대 4개 제한(생성 오름차순) + totalCount + hiddenCount 확인
  - 진행률: 완료 수 / (남은 TODO·DOING + 오늘 완료) 퍼센트 계산 확인 (0%, 50%, 100% 경계, 분모 0이면 0%)
  - 상단 바: today(ISO)·dateLabel·doingTask(없으면 null) 조립 확인
  - DueLabeler로 각 TaskResponse에 dueLabel·urgent 들어감 확인

### T-21. TaskRepository 통합 테스트 (Testcontainers)
- [ ] `TaskRepositoryTest` 작성
  - `findDoneToday`: 어제 완료된 항목이 today 조회에서 제외됨 확인
  - `findByStatusIn`: 상태 필터 정상 동작 확인
  - DOING partial unique index(삭제 조건 포함): 두 번째 DOING 삽입 시 예외 발생 확인

---

## PHASE 6. 검증

### T-22. 통합 확인
- [ ] `docker compose up -d` 후 앱 실행, Flyway V1 마이그레이션 성공 확인
- [ ] 브라우저에서 `localhost:8090` 접속, `GET /api/dashboard`로 모든 패널 렌더링 확인
- [ ] 체크 → DONE, ▶ → DOING (기존 DOING → TODO), 이번 주 버튼 → TODO 승격 동작 확인 (모두 PATCH /api/tasks/{id})
- [ ] 빠른 추가 Enter로 TODO 생성(due_date=서버 today), 공백 입력 무시 확인
- [ ] 30초 자동 갱신(fetchDashboard) 중 입력창 포커스·내용 유지 확인
- [ ] 1920×1080에서 스크롤 없이 한 화면에 표시 확인
- [ ] 한 일 패널에서 오늘 완료 항목 + 완료 시각 확인
- [ ] 하고 싶은 일 패널에서 WISH 샘플 데이터 표시, 4개 초과 시 "외 N개는 접어 두었어요" 확인
- [ ] `scripts/dev-seed.sql` 수동 실행 후 샘플 데이터 표시 확인
