# Spec 1 설계: 프로젝트 기반 + task 핵심 기능

## 1. 시스템 구성

```
[Browser: index.html + JS]
        │  REST (JSON) — 화면은 GET /api/dashboard 한 번, 쓰기는 /api/tasks
        ▼
[Spring Boot 4.1.2 :8080]
  ├── TaskController       (/api/tasks)
  ├── DashboardController  (/api/dashboard)
  ├── TaskService / DashboardService
  ├── DueLabeler           (dueLabel·urgent 계산, 한 곳)
  ├── TaskRepository
  └── Task (Entity)
        │  JDBC (HikariCP)
        ▼
[PostgreSQL 17 :5432]
  └── task 테이블 (Flyway 관리)
```

### 1-1. 설정 (.env 읽기)

Spring Boot가 `.env`를 읽도록 `application.yml`에 아래를 둔다. (.env는 `.gitignore`에 포함, `.env.example`은 저장소에 포함)

```yaml
spring:
  config:
    import: "optional:file:.env[.properties]"
```

> 이렇게 하면 `${POSTGRES_USER}`처럼 `.env`의 키를 application.yml에서 직접 참조할 수 있다.  
> DB 접속 정보는 환경 변수로만 다루고 코드·yml에 하드코딩하지 않는다.

---

## 2. 데이터베이스 설계

### 2-1. task 테이블 (V1__create_task.sql)

```sql
CREATE TABLE task (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200)    NOT NULL,
    status      VARCHAR(10)     NOT NULL,
    priority    VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',
    category    VARCHAR(50),
    due_date    DATE,
    due_time    TIME,
    done_at     TIMESTAMPTZ,
    deleted_yn  CHAR(1)         NOT NULL DEFAULT 'N',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_task_status    CHECK (status     IN ('WISH','TODO','DOING','DONE')),
    CONSTRAINT chk_task_priority  CHECK (priority   IN ('HIGH','MEDIUM','LOW')),
    CONSTRAINT chk_task_deleted_yn CHECK (deleted_yn IN ('Y','N')),

    -- due_time은 반드시 due_date와 함께여야 한다 (시각만 단독 저장 금지)
    CONSTRAINT chk_task_due        CHECK (due_time IS NULL OR due_date IS NOT NULL),

    -- DONE 상태이면 done_at이 있어야 하고, DONE이 아니면 done_at이 없어야 한다
    CONSTRAINT chk_task_done_at    CHECK ((status = 'DONE') = (done_at IS NOT NULL))
);

-- DOING은 삭제되지 않은 행 중 동시에 1개만 허용
CREATE UNIQUE INDEX uq_task_single_doing ON task (status) WHERE status = 'DOING' AND deleted_yn = 'N';

-- 자주 쓰는 조회 패턴용 인덱스
CREATE INDEX idx_task_status  ON task (status);
CREATE INDEX idx_task_done_at ON task (done_at) WHERE done_at IS NOT NULL AND deleted_yn = 'N';
```

### 2-2. 마감 저장 규칙

| 화면 표현 | due_date | due_time |
|---|---|---|
| 마감 없음 | null | null |
| 오늘 중 | 오늘 날짜 | null |
| 내일 마감 | 내일 날짜 | null |
| 이번 주 마감 | **이번 주 일요일** (서울 기준, 월~일 기준 주) | null |
| 날짜 지정 (시각 없음) | 지정 날짜 | null |
| 날짜·시각 지정 | 지정 날짜 | 지정 시각 |

> 빠른 추가는 due_date = 오늘, due_time = null 로 저장한다.  
> 승격(이번 주)은 due_date = 이번 주 일요일(서울 기준), due_time = null 로 저장한다.

### 2-3. "이번 주 마감" 저장 및 표기 규칙 (C안 확정)

**저장**: `due_date` = 이번 주 일요일 날짜, `due_time` = null.  
주의 시작은 **월요일**, 시간대는 **Asia/Seoul** 기준으로 계산한다.

```java
// Clock 주입받아 서울 기준 이번 주 일요일 계산
LocalDate today = LocalDate.now(clock);
LocalDate thisSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
```

**화면 표기 우선순위** (due_date != null, due_time == null인 경우):

| 조건 | 표시 문구 |
|---|---|
| due_date < 오늘 | "M/D 마감" (지난 날짜이므로 날짜 그대로 표시) |
| due_date == 오늘 | "오늘 중" |
| due_date == 내일 | "내일 마감" |
| due_date == 이번 주 일요일 | "이번 주 마감" |
| 그 외 | "M/D 마감" |

> **주의**: "이번 주 마감" 판정은 `due_date`가 정확히 이번 주 일요일과 일치할 때만 적용한다.  
> 다음 주가 되면 동일한 날짜가 "M/D 마감"(지난 날짜)으로 자연스럽게 변한다.  
> 사용자가 직접 이번 주 일요일을 날짜로 지정해도 동일하게 "이번 주 마감"으로 표시된다(Spec 2 이후 해당).

**마감 문구 계산 위치**: **백엔드의 `DueLabeler` 한 곳에서만** 계산한다(3-6 참조).  
서버는 `TaskResponse`에 `dueLabel`·`urgent`를 계산해 내려주고, 프론트는 받은 값을 그대로 표시한다.  
집계성 조회(오늘 완료 등)도 서버에서 서울 시간대 기준으로 계산한다.

---

## 3. 백엔드 설계

### 3-1. 패키지 구조

```
com.abetterday/
├── task/
│   ├── controller/   TaskController.java
│   ├── service/      TaskService.java, DueLabeler.java
│   ├── repository/   TaskRepository.java
│   ├── domain/       Task.java, TaskStatus.java, TaskPriority.java,
│   │                 DeletedYnConverter.java
│   └── dto/          TaskResponse.java, CreateTaskRequest.java,
│                     ChangeStatusRequest.java
├── dashboard/
│   ├── controller/   DashboardController.java
│   ├── service/      DashboardService.java
│   └── dto/          DashboardResponse.java (및 중첩 record)
└── common/
    ├── exception/    BusinessException.java, GlobalExceptionHandler.java,
    │                 ErrorResponse.java
    └── config/       JpaConfig.java, ClockConfig.java
```

> `dashboard`는 조회 전용이라 repository·domain 없이 TaskService(이후 EventService)를 조합한다.

**ClockConfig** (시간대를 한 곳에 모음, 테스트에서 시각 고정용)
```java
@Bean
Clock clock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
}
```
> 모든 날짜·시간 계산은 주입받은 `Clock`으로 한다. `LocalDate.now()` 직접 호출 금지.

### 3-2. 도메인 모델

**TaskStatus** (enum)
```java
public enum TaskStatus { WISH, TODO, DOING, DONE }
```

**TaskPriority** (enum)
```java
public enum TaskPriority { HIGH, MEDIUM, LOW }
```

**Task** (Entity)
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_yn = 'N'")          // 모든 조회에서 삭제 행 자동 제외
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Enumerated(EnumType.STRING) private TaskStatus status;
    @Enumerated(EnumType.STRING) private TaskPriority priority;
    private String category;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private OffsetDateTime doneAt;
    @Column(name = "deleted_yn")
    @Convert(converter = DeletedYnConverter.class)
    private boolean deleted;  // deleted_yn
    @CreationTimestamp private OffsetDateTime createdAt;                     // INSERT 시 JPA가 채움
    @UpdateTimestamp private OffsetDateTime updatedAt;                       // DB 트리거 없이 JPA 갱신
    // ...
}
```

- Lombok은 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만. `@Data`/`@Setter`/`@EqualsAndHashCode` 사용 금지.
- `deleted` 필드는 컬럼명이 다르므로 `@Column(name = "deleted_yn")`을 명시하고 `DeletedYnConverter`(AttributeConverter) 한 곳에서 'Y'/'N'↔boolean 변환.
- `created_at`·`updated_at`은 `OffsetDateTime` 매핑. `created_at`은 `@CreationTimestamp`(INSERT 시 채움), `updated_at`은 `@UpdateTimestamp`(UPDATE 시 갱신)로 DB 트리거 없이 JPA가 다룬다.
- 도메인 메서드 (상태 전환은 전부 이 메서드로):

| 메서드 | 동작 |
|---|---|
| `markDone(OffsetDateTime now)` | status = DONE, done_at = now |
| `markDoing()` | status = DOING, done_at = null |
| `markTodo()` | status = TODO, done_at = null |
| `promote(LocalDate thisSunday)` | status = TODO, priority = MEDIUM, due_date = thisSunday, due_time = null, done_at = null |

> `done_at` 초기화: `chk_task_done_at` 제약(DONE ↔ done_at 일관성)과 맞추기 위해,
> DONE이 아닌 상태로 전환하는 모든 도메인 메서드(`markTodo()`, `markDoing()`, `promote()`)는
> **반드시 done_at을 null로 초기화**한다.

### 3-3. API 설계

| 메서드 | 경로 | 설명 | 요청 바디 | 응답 |
|---|---|---|---|---|
| POST | /api/tasks | task 생성 | CreateTaskRequest | 201, TaskResponse |
| GET | /api/tasks | 목록 조회 (status 파라미터 선택) | - | 200, TaskResponse[] |
| GET | /api/dashboard | 화면 전체 데이터 한 번 | - | 200, DashboardResponse |
| PATCH | /api/tasks/{id} | 상태 전환 / 승격 (본문에 바꿀 값) | ChangeStatusRequest | 200, TaskResponse |

> 경로에 동사를 쓰지 않는다. 상태 전환·승격 모두 `PATCH /api/tasks/{id}` 본문 `{"status": ...}` 하나로 보내고, 부수 변경(기존 DOING→TODO, 승격 기본값)은 서버가 처리한다.

**CreateTaskRequest** (record, Bean Validation)
```java
record CreateTaskRequest(
    @NotBlank @Size(max = 200) String title,   // 생성자에서 앞뒤 공백 제거
    TaskStatus status,                          // 필수, 생성 시 WISH/TODO만
    TaskPriority priority,                      // 선택, 기본 MEDIUM
    @Size(max = 50) String category,            // 선택
    LocalDate dueDate,                          // 선택
    LocalTime dueTime                           // 선택, dueDate가 있을 때만
) {}
```

**ChangeStatusRequest** (record)
```java
record ChangeStatusRequest(TaskStatus status) {}
```

**TaskResponse** (record, 응답 전용 — 어노테이션 없음)
```java
record TaskResponse(
    Long id,
    String title,
    TaskStatus status,
    TaskPriority priority,
    String category,
    LocalDate dueDate,
    LocalTime dueTime,
    String dueLabel,        // 백엔드(DueLabeler) 계산
    boolean urgent,         // 백엔드(DueLabeler) 계산
    OffsetDateTime doneAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
```

**DashboardResponse** (record, 화면 전체)
```java
record DashboardResponse(
    TopBar topBar,
    TodayTodoPanel todayTodo,
    DonePanel done,
    WishPanel wish
) {}

record TopBar(
    String today,           // 서울 기준 ISO 날짜 (빠른 추가 dueDate용)
    String dateLabel,       // "2026년 11월 5일 (목)"
    TaskResponse doingTask, // null 가능
    ProgressData progress
) {}

record ProgressData(int done, int total, int percent) {}  // 분모(total) 0이면 percent=0

record TodayTodoPanel(
    List<TaskResponse> items,  // 정렬·최대 7개
    int totalCount,            // 남은 TODO+DOING 전체
    int hiddenCount            // totalCount - items.size()
) {}

record DonePanel(
    List<TaskResponse> items,  // 최대 4개, doneAt 내림차순
    int doneCount               // 오늘 완료 전체 건수
) {}

record WishPanel(
    List<TaskResponse> items,  // 최대 4개, 생성 오름차순
    int totalCount,
    int hiddenCount
) {}
```

### 3-4. 서비스 로직

> `Clock` 주입받아 `now = OffsetDateTime.now(clock)`, `today = LocalDate.now(clock)` 사용.

**changeStatus(id, newStatus)** — `@Transactional`
1. task 조회 (없으면 404)
2. 전환 규칙 검사 (위반 시 409):
   - →DONE: 현재 TODO/DOING만 허용
   - →DOING: 현재 TODO만 허용 (WISH/DONE → 409)
   - →TODO: 현재 WISH만 허용(승격). 그 외 → 409
3. newStatus == DOING — **한 트랜잭션 안에서 아래 순서**:
   a. 현재 DOING인 task 조회
   b. 있으면 해당 task에 `markTodo()` 호출 → 저장 후 **flush**
      > flush 순서가 중요하다. flush 없이 두 변경을 동시에 저장하면 DB가 잠깐이라도
      > DOING 2개를 보게 되어 `uq_task_single_doing` 위반이 발생할 수 있다.
   c. 대상 task에 `markDoing()` 호출 → 저장
4. newStatus == DONE:
   - 대상 task에 `markDone(now)` 호출 → 저장
5. newStatus == TODO (WISH→TODO 승격):
   - `thisSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))`
   - 대상 task에 `promote(thisSunday)` 호출 → 저장
6. `TaskResponse` 반환 (DueLabeler로 dueLabel·urgent 계산)

**createTask(CreateTaskRequest)** — `@Transactional`
1. title 앞뒤 공백 제거(생성자), 검증
2. status는 WISH, TODO만 허용 (DOING/DONE 생성 시 BusinessException 400)
3. due_time은 due_date 있을 때만 허용 (단독이면 BusinessException 400, DB 제약 전에 차단)
4. Task 생성 → 저장 → 201 + TaskResponse

**DashboardService.getDashboard()** — `@Transactional(readOnly = true)`
1. 오늘 할 일: TODO·DOING을 정렬(DOING → priority → due → createdAt)해 최대 7개 + totalCount + hiddenCount
2. 한 일: 오늘(서울 자정~) 완료를 doneAt 내림차순 최대 4개 + doneCount
3. 하고 싶은 일: WISH를 생성 오름차순 최대 4개 + totalCount + hiddenCount
4. 진행률: done / (남은 TODO·DOING + 오늘 완료) 퍼센트 (분모 0이면 0%)
5. 상단 바: today(ISO)·dateLabel·doingTask(없으면 null)·progress
6. 각 TaskResponse는 DueLabeler로 dueLabel·urgent 계산
> 일정·하단 바는 이번 spec 범위 밖이라 빈/자리 데이터로 둔다.

### 3-5. 예외 처리

**BusinessException** — `code`(String), `message`(String), `httpStatus`(HttpStatus, 400/404/409)
> 생성 시 비즈니스 검증 위반(status 위반·due_time 단독)은 400, 없음은 404, 상태 전환 등 규칙 위반은 409를 지정한다.

**GlobalExceptionHandler** (`@RestControllerAdvice`, 규모 작게 유지)
- `BusinessException` → 지정된 httpStatus (400/404/409)
- `MethodArgumentNotValidException` → 400, 필드별 한국어 메시지
- `HttpMessageNotReadableException` → 400 (잘못된 JSON 본문·잘못된 enum 값)
- `MethodArgumentTypeMismatchException` → 400 (쿼리 파라미터 타입 불일치)
- 그 외 → 500
> 분류: 검증 실패·JSON/enum/파라미터 오류(400) · 없음(404) · 규칙 위반(409) · 그 외(500). 사용자에게 스택 트레이스·내부 정보 노출 금지.

**ErrorResponse** (record)
```java
record ErrorResponse(String code, String message) {}
```

### 3-6. 마감 표기 계산 (DueLabeler, 백엔드 한 곳)

`DueLabeler`가 `Clock`을 주입받아 `Task` → `dueLabel`·`urgent`를 계산한다.  
`dueDate`는 `LocalDate`, `dueTime`은 `LocalTime` 또는 null, `now`는 `OffsetDateTime.now(clock)` 기준.

```
[due_date == null]
  → dueLabel = "마감 없음", urgent = false

[due_date != null, due_time != null]
  target = due_date.atTime(due_time) (서울)
  날짜 접두어: due_date == 오늘 → "" / == 내일 → "내일 " / 그 외 → "M/D "
    예: "15:00 마감", "내일 15:00 마감", "11/8 15:00 마감"
  기본 = 접두어 + "HH:mm 마감"
  diff = (target - now) (분 단위, 서울 기준)
  urgent = (diff < 120)   // 2시간 이내 남음 또는 지남
  diff < 0 (지남):
    |diff| < 60         → " · N분 지남"               (urgent = true)
    60 <= |diff| < 1440 → " · N시간 M분 지남"          (urgent = true, M=0이면 "N시간 지남")
    |diff| >= 1440      → " · N일 지남"                (urgent = true, N = floor(|diff|/1440))
  diff == 0             → " · 지금 마감"               (urgent = true)
  0 < diff < 60         → " · N분 남음"                (urgent = true)
  60 <= diff < 120      → " · 1시간 남음"(정확히 60) / " · 1시간 M분 남음"(M>0)  (urgent = true)
  diff >= 120           → (접미어 없음)                (urgent = false)
  → dueLabel = 기본 + 접미어

[due_date != null, due_time == null]  — 아래 순서로 판정
  due_date < 오늘             → "M/D 마감"          (urgent = false)
  due_date == 오늘            → "오늘 중"            (urgent = false)
  due_date == 내일            → "내일 마감"          (urgent = false)
  due_date == 이번 주 일요일  → "이번 주 마감"       (urgent = false)
  그 외                       → "M/D 마감"          (urgent = false)
```

> "이번 주 일요일" 판정은 `now` 기준 `nextOrSame(SUNDAY)`로 계산해 비교. 날짜 비교는 서울 시간대 기준.  
> 날짜 접두어는 due_time이 있을 때만 붙이고, due_time 없는 날짜 마감은 "오늘 중/내일 마감/이번 주 마감/M/D 마감"을 그대로 쓴다.  
> 시각 있는 마감이 지났으면 남은 시간과 무관하게 항상 urgent=true다. 시각 없는 마감은 긴급(주황)으로 표시하지 않는다.  
> 이 계산은 DueLabeler 한 곳에서만 하고, TaskResponse 매핑 시 호출한다.

---

## 4. 프론트엔드 설계

### 4-1. 파일 구조

```
static/
├── index.html
├── css/
│   └── common.css          # CSS는 common.css 하나만
├── js/
│   ├── api.js              # fetch 래퍼, 모든 서버 통신
│   ├── render.js           # DOM 렌더링 함수 (계산 없음)
│   └── app.js              # 진입점, 이벤트 바인딩, setInterval
└── vendor/
    └── bootstrap-5.3.8/    # 이미 로컬에 있음 (다운로드 불필요)
        ├── css/bootstrap.min.css
        └── js/bootstrap.bundle.min.js
```

> 글꼴 파일(`vendor/fonts/`)과 `modal.js`는 별도 task(Spec 2 편집 모달 시점)에 추가한다.

**index.html 연결** (Bootstrap UMD 일반 `<script>`를 먼저, 앱 코드는 ES 모듈)
```html
<link rel="stylesheet" href="/vendor/bootstrap-5.3.8/css/bootstrap.min.css">
...
<script src="/vendor/bootstrap-5.3.8/js/bootstrap.bundle.min.js"></script>
<script type="module" src="/js/app.js"></script>
```
> Bootstrap JS API 호출은 `modal.js` 한 곳에서만(이번 spec엔 모달 없음). 앱 코드는 `window.bootstrap`으로 접근하고 Bootstrap을 ESM import하지 않는다.

### 4-2. 화면 레이아웃 구조

id는 camelCase, class는 snake_case(frontend-conventions). 제목 있는 패널은 `section` prefix.

```
body (100vh, d-flex, flex-column)
├── #topBar          상단 바 (고정 높이)
├── #mainContent     3단 컨텐츠 (flex: 1)
│   ├── #sectionSchedule   왼쪽: 오늘 일정 (자리)
│   ├── #sectionTodo       가운데: 오늘 할 일
│   └── .wrap_right_panels  오른쪽 (d-flex, flex-column)
│       ├── #sectionDone       오른쪽 위: 한 일
│       └── #sectionWish       오른쪽 아래: 하고 싶은 일
└── #bottomBar       하단 바 (자리)
```

### 4-3. CSS 변수 (common.css :root)

```css
--color-bg:           #0B0D11;
--color-card:         #141821;
--color-card-border:  #262D3D;
--color-card-inner:   #1B2030;
--color-divider:      #222836;
--color-text:         #EEF0F4;
--color-text-muted:   #98A0B0;
--color-blue:         #62AEFF;   /* DOING */
--color-green:        #6FD39A;   /* 완료 */
--color-orange:       #FF9A52;   /* 긴급, 우선순위 높음 */
--color-red:          #FF6B6B;   /* 삭제·오류 */
--color-input-border: #2F384B;
--color-btn-border:   #3A4358;
```

### 4-4. JS 모듈 설계

**api.js** - 서버 통신 (fetch는 이 파일에만)
```javascript
export async function fetchDashboard()              // GET /api/dashboard (화면 전체)
export async function fetchTasks(statusFilter)      // GET /api/tasks (범용, 화면 미사용)
export async function createTask(payload)           // POST /api/tasks
export async function patchTask(id, body)            // PATCH /api/tasks/{id}  body 예: {"status":"DOING"}
```

**render.js** - DOM 렌더링 (받은 값만 그림, 계산 없음)
```javascript
export function renderTopBar(topBar)           // topBar.dateLabel, doingTask, progress (시계 HH:mm은 브라우저)
export function renderTodoPanel(todayTodo)     // todayTodo.items(정렬·최대7) + hiddenCount
export function renderDonePanel(done)          // done.items(최대4) + doneCount
export function renderWishPanel(wish)          // wish.items(최대4) + totalCount + hiddenCount
export function renderSchedulePlaceholder()
export function renderBottomBarPlaceholder()
```
> `formatDeadline`/`isUrgent`는 만들지 않는다. 서버가 내려준 `dueLabel`/`urgent`를 그대로 표시한다.

**app.js** - 진입점
```javascript
// refreshAll(): fetchDashboard() → 각 패널 렌더링
// 초기 로드 (DOMContentLoaded)
// setInterval(refreshAll, 30000)
// 30초 갱신 시 포커스·입력 내용 보존
// 자정 감지: 날짜 변경 시 전체 재로드
// 이벤트 위임(체크/▶/이번 주) → patchTask(id, {status:...}) → fetchDashboard 재조회
// 빠른 추가 Enter: POST {title, status:'TODO', priority:'MEDIUM', dueDate: dashboard.topBar.today}
// API 실패 시 오류 토스트, 기존 화면 유지
// DOING 파란 점 깜빡임(CSS, prefers-reduced-motion 대응), 체크 버튼 호버 스타일
```

### 4-5. 자동 갱신 전략

```javascript
// 모달이 열려 있지 않을 때만 목록 갱신 (이번 spec엔 모달 없음)
// 갱신 전: 포커스 있는 입력창, 입력 중인 값 저장
// 갱신 후: 입력창 값 복원, 포커스 복원
// 자정 감지: 마지막 "오늘 날짜"와 현재 날짜 비교
// 데이터 출처는 항상 fetchDashboard() 한 번
```

---

## 5. 개발용 샘플 데이터 (scripts/dev-seed.sql)

> Flyway 마이그레이션이 아니다. `scripts/dev-seed.sql`에 두고 필요할 때 수동으로 실행한다.  
> 앱이 자동으로 넣는 로더는 만들지 않는다.

WISH 7개 (카테고리·우선순위 다양) + TODO 4개 (마감 다양) + DOING 1개 + DONE 3개(오늘 done_at)를 삽입한다.
샘플 데이터는 프로토타입 화면과 유사한 형태로 구성해 모든 패널을 한눈에 확인할 수 있게 한다.

---

## 6. 테스트 설계

### 6-1. TaskServiceTest (단위 테스트, Mockito, 고정 Clock)

| 테스트 케이스 | 검증 내용 |
|---|---|
| 기존 DOING → 새 DOING 전환 시 기존 task가 TODO로 변경됨 | `markTodo()` 호출 + flush 순서 |
| DOING이 없을 때 새 DOING 전환 | 정상 동작 |
| WISH가 아닌 task에 TODO 전환(승격) 시 409 | BusinessException |
| WISH→DOING 등 허용되지 않은 전환 시 409 | BusinessException |
| DONE 전환 시 done_at이 null이 아님 | done_at != null |

### 6-2. TaskRepositoryTest (통합 테스트, Testcontainers)

| 테스트 케이스 | 검증 내용 |
|---|---|
| `findDoneToday` 조회: 오늘 서울 자정 이후 완료 건만 반환 | 어제 완료 건 제외 |
| status 필터 조회 | 해당 상태만 반환 |
| DOING partial unique index(삭제 조건 포함) 위반 시 예외 | DB 수준 방어 확인 |

### 6-3. DueLabelerTest (단위 테스트, 고정 Clock — 경계값)

| 테스트 케이스 | 검증 내용 |
|---|---|
| 시각 마감 긴급 경계 | 정확히 2시간 남음(urgent=false) / 2시간 미만(urgent=true) / 0분(지금 마감) / 지남(urgent=true) |
| 시각 마감 문구 | "N분 남음", "1시간 남음"(정확히 60분), "1시간 M분 남음", "N분 지남", "N시간 M분 지남", "N일 지남" |
| 날짜 접두어 | 오늘(생략) / 내일("내일 ") / 그 외("M/D ") |
| 자정 전후 | 오늘 23:59 vs 내일 00:01 (서울 자정 기준 날짜 비교) |
| 일요일 경계 | 오늘이 일요일일 때 `nextOrSame(SUNDAY)`가 오늘 → "이번 주 마감" |
| 시각 없는 마감 | due_date < 오늘/==오늘/==내일/==이번 주 일요일/그 외, 항상 urgent=false |
| 마감 없음 | due_date == null → "마감 없음", urgent=false |

### 6-4. DashboardServiceTest (단위 테스트, Mockito, 고정 Clock)

| 테스트 케이스 | 검증 내용 |
|---|---|
| 오늘 할 일 정렬 | DOING → priority(HIGH→MEDIUM→LOW) → 마감 빠른 순(없음은 뒤) → createdAt 오름차순 |
| 오늘 할 일 제한 | 최대 7개 + hiddenCount(전체 - 표시) |
| 한 일 제한 | 최대 4개(doneAt 내림차순) + doneCount |
| 하고 싶은 일 제한 | 최대 4개(createdAt 오름차순) + totalCount + hiddenCount |
| 진행률 | 완료 / (남은 TODO·DOING + 오늘 완료), 0/50/100% 경계, 분모 0이면 0% |
| 상단 바 | today(ISO)·dateLabel·doingTask(없으면 null) 조립 |
| DueLabeler 연동 | 각 TaskResponse에 dueLabel·urgent 포함 |
