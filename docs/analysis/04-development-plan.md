# 미완료 작업 및 개발 우선순위

## 1. 현재 프로젝트 상태와 계획의 범위

근거는 [구조 분석](01-project-structure.md), [Kiro 분석](02-kiro-specification.md), [구현 검증](03-implementation-status.md)이다. 요구사항 ID의 원본은 `.kiro/specs/spec-1-task-core/requirements.md`, 설계와 Tasks는 같은 디렉터리의 `design.md`, `tasks.md`다. ID 없는 제품 요구사항은 `.kiro/steering/product.md`의 절 이름으로 추적한다.

Spec 1의 30개 하위 요구사항은 구현·미검증 14개, 부분 구현 7개, 미구현 9개다. 백엔드 코드가 있지만 동작 검증은 부족하고, 앱 HTML/CSS/JS 및 네 가지 업무 테스트가 없다. Bootstrap 5.3.8 vendor만 있다. 이전 오프라인 컴파일에서 compileJava는 UP-TO-DATE, compileTestJava는 Testcontainers 의존성 해석 실패였다. 캐시 부족과 관리 버전/좌표 문제는 아직 구별되지 않았다.

Claude는 제품 코드·설정·테스트 코드 구현과 필요한 수정, Codex는 독립 리뷰·안전한 테스트 실행·요구사항 추적과 PASS 판정을 맡는다. Codex가 결함을 발견하면 재현 근거를 Claude에 전달하고 수정 후 재검증한다. Tasks 체크박스만으로 완료 처리하지 않는다.

이 문서는 개발 계획이다. 이번 작업에서는 코드·설정·Kiro 문서를 변경하거나 빌드·테스트·DB 작업을 실행하지 않았다. 기존 DB에 연결하지 말라는 사용자 조건은 후속 검증 계획에도 유지한다. 이전 승인은 오프라인 컴파일에 한정되므로 테스트 실행·온라인 다운로드·격리 DB 검증을 승인받은 것으로 간주하지 않는다.

## 2. 남은 작업 전체 목록

| 구분 | 남은 항목 | 담당 작업 |
|---|---|---|
| 환경 차단 | Testcontainers 좌표/관리 버전/캐시 해석, JDK25 toolchain 확인 | C01 |
| 테스트 안전성 | 기본 contextLoads의 실제 DB 연결 가능성, 테스트 환경 분리 | C02 |
| 설정 불일치 | 앱 loopback 바인딩 누락, DB host 환경변수 누락 | C03 |
| 기존 백엔드 검증 | 상태 전환·flush·doneAt·승격 테스트 없음 | C04 |
| 시간 경계 | DueLabeler 분 절삭·일요일·자정 기대값 검증 부족 | C05 |
| 집계·정렬 | DashboardService 제한·숨김·진행률·자정 검증 부족 | C06 |
| DB 검증 | migration/Entity/삭제 필터/index/서울 날짜 조회 미검증 | C07 |
| API·예외 | JSON·enum·null·status 필터·오류 계약·입력 로그 검증 부족 | C08 |
| 부분 UI | 레이아웃·글꼴·상단·task 세 패널 미구현 | C09, C10 |
| 변경 동작 | api.js·빠른 추가·체크·▶·승격·IME 미구현 | C11 |
| 상시 화면 | 30초 갱신·포커스 보존·실패 복구·Bootstrap Toast 미구현 | C12 |
| 통합 검증 | 독립 테스트 DB에서 seed·브라우저·전체 흐름 미검증 | C13 |
| 확정 필요 | 포트·이름·볼륨·시간 경계·글꼴·응답 필드의 문서 상충 | D01~D07 |
| 운영·품질 | pg_dump/복원 안내 부재, 포맷·주석·증거 기반 성능 검토 | C14, C15 |
| 후속 제품 기능 | task/WISH 편집·삭제 모달, event CRUD/타임라인, 주간집계·내일 일정 | F01~F05 |
| 장기 범위 | 반복 작업·외부 캘린더·AWS/인증 | 미래 백로그, 현재 실행 작업 아님 |

동시 DOING 충돌의 일반 500 가능성, 자정 중간 기준일 변경, Entity 컬럼 메타데이터 차이는 재현이 필요한 위험이다. 재현 전에 잠금·스키마 변경·대규모 리팩터링을 확정 작업으로 만들지 않는다.

### 선행 결정 목록

아래 항목은 Codex가 대안을 정리하고 사용자가 기준을 결정한다. 결정 결과는 후속 작업의 입력이며 이번에 Kiro 문서를 고치지 않는다. 다른 작업은 해당 결정에 의존하는 부분 외에는 계속 진행할 수 있다.

| ID | 쟁점과 근거 | 영향을 받는 작업 |
|---|---|---|
| D01 | Design/T-22의 8080과 실제 8090 중 기준 포트. 기존 코드 포트를 임의 변경하지 않음 | C03, C13 |
| D02 | REQ-4-3의 2시간 이내와 REQ-7-3의 정확히120분 false, 초 단위 절삭 정책. 명시적인120분 false 테스트는 우선 작성 가능 | C05 |
| D03 | Design 일반 규칙의 일요일 오늘 중과 테스트 표의 이번 주 마감. 오늘→내일→일요일 우선순위를 기본 근거로 기록 | C05 |
| D04 | 같은 날짜의 시각 없는 마감 정렬, 퍼센트 소수 처리, 자정 요청 일관성 기대값 | C06 |
| D05 | REQ-4-1의 글꼴 필수와 Design의 Spec2 연기. 글꼴 작업을 제거하지 않고 완료 gate를 결정 | C09, C13 |
| D06 | 일정·하단 자리표시자는 HTML만인지 API null 필드도 필요한지. 현재 Design DTO는 해당 필드 없음 | C08, C09 |
| D07 | structure 명명과 settings/pgdata 차이, 제품 WISH 우선순위 보존과 승격 MEDIUM 규칙 차이 | C15, F01~F02 |

카테고리 3종은 seed의 근거이며, 임의의 허용 목록 제한을 추가하지 않는다. Spec 1에서는 DELETE/event 실제 구현 대신 요구된 자리표시자를 완성한다.

## 3. 우선순위 분류

| 우선순위 | 의미 | 작업 |
|---|---|---|
| P0 | 검증·개발을 막는 환경 및 안전한 테스트 실행 경로 | C01, C02 |
| P1 | 핵심 UI 부재·설정 결함·Spec 1 인수 검증 | C03, C09, C10, C11, C12, C13 |
| P2 | 핵심 로직 검증 보강·발견 결함 수정·운영 백업 | C04, C05, C06, C07, C08, C14 |
| P3 | 동작 변경 없는 품질 정리·증거 기반 구조 개선 | C15 |

P2 테스트도 P1 UI와 병렬로 먼저 수행한다. 우선순위는 중요도이며 의존성을 무시한 실행 순서가 아니다. F01~F05는 제품 MVP 기준 P1이지만 Spec 1 완료 뒤 실행하는 후속 단계다. 장기 백로그는 현 단계 우선순위 밖이다.

## 4. 의존 관계와 권장 순서

```text
C01 → C02 ┬→ C04 → C07 ─┐
          ├→ C05 → C06 ─┼→ C08 ─┐
          └→ C03 ────────────────┤
C09 → C10 → C11 → C12 ──────────┼→ C13 → Spec 1 인수
                                 ┘
Spec 1 인수 → F01 → F02
Spec 1 인수 → F03 → F04
F03 + 검증된 완료 집계 → F05
```

C08의 API 계약 검토는 C07 DB 검증과 별개로 시작 가능하나 전체 PASS에는 둘 다 필요하다. C09는 P0 해결 중에도 정적 목업으로 진행 가능하다. C10/C11은 기존 Design DTO를 기준으로 개발하고 계약 검증 후 연결한다. C14는 격리 복원 환경이 마련되면 병렬 진행한다. C15는 기능 작업 후 작은 변경 단위로 한다.

권장 실행 묶음: (1) C01·C02, (2) C03 및 C04~C08과 C09 병렬, (3) C10·C11·C12, (4) C13, (5) C14·C15와 후속 제품 기능. D01~D07은 해당 gate 전에 해소한다.

## 5. Claude 구현 작업 목록

예상 파일 경로는 `J=src/main/java/com/abetterday`, `T=src/test/java/com/abetterday`, `R=src/main/resources`로 축약한다. 테스트 파일도 Claude가 구현한다. 기존 테스트로 통과하는 제품 코드는 재작성하지 않고 증거를 추가한다.

### C01. 테스트 의존성과 컴파일 환경 복구 — P0

- 목적·요구사항: REQ-1-3, REQ-7 전체, tech.md 스택/테스트. 테스트 코드 컴파일 차단 원인을 구별한다.
- 예상 파일: `build.gradle`, 필요한 경우 wrapper 설정, 실행 환경 안내 문서. 의존성 진단 전 임의 버전 상향 금지.
- 구현 내용: 사용 중인 JDK/Java25 toolchain·Gradle·Boot 관리 버전 확인, Testcontainers 좌표와 관리 버전 및 로컬 캐시 상태 진단, 원인에 맞는 최소 수정. 캐시 문제만이면 제품 설정 변경 불필요.
- AC: 승인된 오프라인 `compileJava compileTestJava` 성공 로그, 실제 사용 JDK 기록, 새 컴파일과 UP-TO-DATE 구분. 캐시 보충 필요 시 필요한 항목·다운로드 범위를 제시하고 실행 권한 확보 후 진행.
- 선행: 없음. 기존 Git 변경 기준점 기록.

### C02. 테스트 격리와 실행 경로 분리 — P0

- 목적·요구사항: REQ-7-1~4, REQ-1-3. 기존 DB를 건드리지 않고 검증할 수 있게 한다.
- 예상 파일: `T/AbetterdayApplicationTests.java`, `src/test/resources/` 테스트 설정, 필요 시 `build.gradle` 테스트 선택 설정.
- 구현 내용: DB 없는 단위/MVC 테스트와 PostgreSQL 통합 테스트 구분. 통합 테스트 접속은 명시적 임시 Testcontainers DB로 고정하며 기본 .env/기존 datasource로의 fallback 차단. 통합 테스트는 승인된 실행에서만 선택한다.
- AC: DB 없는 테스트 경로가 DB·Flyway·Docker를 시작하지 않음. 통합 테스트가 실행될 대상과 생명주기를 코드로 확인 가능. 격리 자원 준비 불가 시 명확한 미실행 결과를 남기고 기존 DB로 대체하지 않음.
- 선행: C01.

### C03. 로컬 실행 설정 정합성 보완 — P1

- 목적·요구사항: REQ-1-1~3, tech.md 실행 환경.
- 예상 파일: `R/application.yml`, `.env.example`, 필요 시 `docker-compose.yml`, 실행 안내. 실제 `.env`는 덮어쓰지 않음.
- 구현 내용: 앱 loopback 명시, DB host도 환경변수 참조, Compose localhost 노출 유지, D01에 따른 포트 일치. validate/OIV/Flyway 정책 보존.
- AC: 비밀값 하드코딩 없음, 로컬 바인딩 설정 명확, 승인된 격리 실행에서 실제 주소·포트 일치, 기존 볼륨 이름·데이터에 영향 없음.
- 선행: C02 및 포트 변경 전 D01.

### C04. task 전환·생성 단위 테스트 및 재현 결함 수정 — P2

- 목적·요구사항: REQ-2-2/4, REQ-3-1/4/5/6, REQ-7-1; T-18.
- 예상 파일: `T/task/service/TaskServiceTest.java`, 도메인 테스트, 재현 시 `J/task/service/TaskService.java`, `J/task/domain/Task.java`.
- 구현 내용: 생성 WISH/TODO·기본값·due 검증, 허용/금지 전환 표, 404, 고정 Clock doneAt, 승격 날짜·priority/time, 기존 DOING flush 순서 검증. 제품 코드 수정은 실패 사례가 있는 부분만.
- AC: 각 전환의 상태·부수 필드·예외가 요구사항과 일치하고 테스트 통과. title Bean Validation은 C08의 HTTP 테스트에서 검증.
- 선행: C02.

### C05. DueLabeler 경계 검증과 시간 처리 보완 — P2

- 목적·요구사항: REQ-4-3, REQ-7-3; T-19, tech.md 마감 규칙.
- 예상 파일: `T/task/service/DueLabelerTest.java`, 필요한 경우 `J/task/service/DueLabeler.java`.
- 구현 내용: 120/119/60/0/-1분, 지남의 분·시간·일, 오늘/내일/주말/자정·마감 없음 검증. 초 단위 경계와 시간 절삭은 D02·D03 결정 후 수정.
- AC: 확정된 기대값 전체 통과, 날짜 전용 urgent 항상 false, 프론트 중복 계산 불필요. 모호한 기대값을 임의 요구사항으로 고정하지 않음.
- 선행: C02; 모호한 동작 수정은 D02·D03.

### C06. 대시보드 정렬·집계 검증 — P2

- 목적·요구사항: REQ-3-3, REQ-4-0/2~5, REQ-7-4; T-20.
- 예상 파일: `T/dashboard/service/DashboardServiceTest.java`, 필요 시 `J/dashboard/service/DashboardService.java`, `J/task/service/TaskService.java`.
- 구현 내용: DOING/priority/due/createdAt 정렬, 7/4 제한과 실제 전체수, 0/50/100%·분모0, doing 없음, today/dateLabel, 자정 걸침 재현. 반복 Clock 조회는 일관성 실패 확인 시 최소 보완.
- AC: 화면 데이터 계약 테스트 통과. 시간 없는 due 순서·소수 퍼센트는 D04 결과에 따라 검증. 기존 전체수와 표시수 의미 보존.
- 선행: C04·C05, 관련 D04.

### C07. PostgreSQL Repository·스키마 통합 검증 — P2

- 목적·요구사항: REQ-1-3, REQ-2-1~4, REQ-7-2, REQ-6-1; T-21.
- 예상 파일: `T/task/repository/TaskRepositoryTest.java`, 테스트 fixture, 필요한 경우 Entity 매핑·새 migration.
- 구현 내용: 격리 PostgreSQL17에서 V1·validate, enum/문자 converter, 삭제 제외, 상태 필터, 서울 오늘의 시작 포함/끝 제외, 제약 위반·삭제된 DOING 허용, 실제 flush/rollback 검증. seed는 C13의 임시 DB에서 별도 수동 확인.
- AC: 격리 자원에서 DB 제약과 ORM 조회 모두 검증됨. 적용된 V1 수정 금지. 결함이 없으면 migration 추가하지 않음.
- 선행: C02·C04, 격리 DB 실행 권한·환경.

### C08. API 계약·오류·로그 검증 및 보완 — P2

- 목적·요구사항: REQ-3-1~7, REQ-2-2, code-conventions.md 로깅/예외.
- 예상 파일: `T/task/controller/`, `T/dashboard/controller/`, `J/common/exception/GlobalExceptionHandler.java`, 재현 시 Controller/DTO/Service/Repository.
- 구현 내용: MVC 단위 테스트로 JSON·enum·null·길이·빈 제목·status 목록·201 Location·404/409·null 필드 계약 검증. 파싱 예외 원문 대신 입력을 포함하지 않는 로그 사용. DB 경쟁 충돌은 C07 후 재현하고 문서의 규칙 위반 409 범위에 맞게 제한적으로 처리.
- AC: 계약 테스트 통과, 한국어 code/message, 내부·입력 원문 미노출. 모든 DB 예외를 일괄409로 바꾸지 않음. 경쟁 결과는 DOING 최대1과 rollback 보장으로 판정하며 잠금 방식은 사전 고정하지 않음.
- 선행: C02; 전체 PASS는 C04·C06·C07, 필드 변경 시 D06.

### C09. Bootstrap 화면 뼈대와 정적 자산 — P1

- 목적·요구사항: REQ-4-1/6/7, 최신 tech.md·frontend-conventions.md UI 원칙; T-13~14.
- 예상 파일: `R/static/index.html`, `css/common.css`, `vendor/fonts/`(D05에 따라), `docs/prototype/`는 참고만.
- 구현 내용: HTML 한 장, 다크 100vh·3단·상하단, Bootstrap flex/grid/spacing 우선, CSS 변수, 로컬 bundle 선로드·ES 모듈 연결, Spec3/4 안내 자리. 글꼴은 독립 자산 변경으로 리뷰 가능하게 분리한다.
- AC: 1920×1080 스크롤 없음, 로컬 Bootstrap 사용, React/npm/CDN 없음, 앱 CSS는 common.css. D05에서 연기를 선택하면 해당 글꼴 요구는 미완료로 남기고 Spec 전체 PASS로 숨기지 않음.
- 선행: 정적 작업은 없음; 완료 기준은 D05·D06.

### C10. 서버 응답 기반 대시보드 렌더링 — P1

- 목적·요구사항: REQ-4-0/2~5, REQ-5-3; T-16.
- 예상 파일: `R/static/js/render.js`, `index.html`, `css/common.css`.
- 구현 내용: 상단·세 패널·숨김/전체수·빈 상태·DOING/HIGH/urgent 표현. 아이콘 aria-label·focus-visible·pulse/reduced-motion. 서버 순서와 제한 유지, 텍스트는 textContent. HH:mm 표시는 서울 기준으로 표현하되 집계·날짜 판정을 새로 계산하지 않음.
- AC: 대표 응답 fixture의 순서·개수·문구와 DOM 일치, 빈 상태 정상, 사용자 문자열을 HTML로 해석하지 않음.
- 선행: C09; 연결 검증 시 C06·C08.

### C11. API 래퍼와 사용자 상태 변경 — P1

- 목적·요구사항: REQ-3, REQ-4-3/5, REQ-5-3; T-15·T-17 일부.
- 예상 파일: `R/static/js/api.js`, `js/app.js`.
- 구현 내용: fetch는 api.js에만, 이벤트 위임으로 체크/▶/승격 PATCH, 빠른 추가 POST. IME·공백·200자 처리, dueDate는 dashboard.topBar.today. 성공 후 dashboard 재조회, 성공한 추가만 입력 초기화.
- AC: 한 동작당 요청 한 번, 실패 시 입력 보존, 잘못된 입력 미전송, 문서 endpoint/JSON 그대로 사용.
- 선행: C10, C08 계약 검토.

### C12. 주기 갱신·실패 복구·Bootstrap Toast — P1

- 목적·요구사항: REQ-5-1~3, REQ-4-2, 최신 UI 규칙; T-17 나머지.
- 예상 파일: `R/static/js/app.js`, `js/modal.js`, `index.html`, `css/common.css`.
- 구현 내용: 30초 dashboard 조회, 브라우저 시계 표시, 서버 today 변경으로 날짜 갱신, 포커스·입력 보존, 마지막 정상 화면 유지와 작은 Bootstrap Toast. Bootstrap JS API는 기존 규칙대로 modal.js에 모은다. 이 파일 도입이 편집 모달 기능 선행 구현을 뜻하지는 않는다.
- AC: 입력 중 갱신·요청 실패에도 내용 유지, Toast 반복 표시 후 DOM/이벤트 누적 없음, 프론트 날짜 계산으로 today를 정하지 않음. 오래 걸린 요청의 응답 역전으로 최신 화면이 되돌아가지 않는지 확인.
- 선행: C11.

### C13. Spec 1 통합 인수 검증 지원 — P1

- 목적·요구사항: REQ-1~7 전체, T-22.
- 예상 파일: 필요한 테스트 fixture·격리 실행 안내·검증 스크립트. 제품 파일은 재현 결함이 있을 때만 별도 수정 단위로 분리.
- 구현 내용: Claude가 안전한 임시 실행 절차와 데이터 준비 방법 제공. Codex가 fixture/seed와 API·브라우저로 통합 검증. 기존 Compose 볼륨·DB를 사용하지 않는다.
- AC: C03~C12 검증 및 Spec1 추적표 통과, seed WISH7 이상·TODO 포함 확인, 제한/숨김·빠른 추가/전환·실패 복구·1920×1080 확인, 미검증·미결정 항목 없음. D05로 연기된 요구가 있으면 조건부 인수로 명시하며 완전 PASS로 처리하지 않음.
- 선행: C03~C12, 관련 결정·실행 권한 확보.

### C14. 파일 백업·복원 절차 — P2

- 목적·요구사항: db-conventions.md 운영, tech.md 작업 방식의 주1회 pg_dump.
- 예상 파일: `scripts/` 백업 스크립트, 복원 안내 문서. `.gitignore`는 기존 backups 제외를 유지.
- 구현 내용: 파일 백업과 수동 복원 절차, 주간 실행 방법 안내. DB 테이블 백업이나 임의 예약 작업 설치는 하지 않음.
- AC: 승인된 임시 DB에서 덤프·별도 임시 DB 복원 검증, 비밀값 출력·파일 Git 포함 없음, 원본 DB 변경 없음.
- 선행: C02·C07 및 격리 실행 권한.

### C15. 품질·명명 정리 — P3

- 목적·요구사항: code-conventions.md, structure.md, STEP3 품질 위험.
- 예상 파일: 위반이 확인된 Java/JS/문서, `settings.gradle`은 D07 결정 시만.
- 구현 내용: 주석·120자·불필요 import 정리, 명명 차이 해결 계획. 전체 목록 메모리 정렬은 데이터 규모와 측정으로 문제 확인 시만 별도 최적화한다.
- AC: 동작/계약 변화 없음, 관련 회귀 통과. 기존 볼륨 rename·삭제·이전은 포함하지 않음. 필요하면 독립 승인·이전 계획으로 분리.
- 선행: C13, 명명 변경 시 D07.

## 6. Codex 리뷰 및 테스트 계획

각 C 작업은 같은 번호의 V 검증으로 대응한다. 아래 PASS는 해당 작업의 AC와 표의 테스트가 모두 충족되고 증거가 저장된 경우다. 실행하지 못한 검증은 미검증으로 남긴다. 새 테스트 프레임워크·의존성 도입을 전제하지 않으며 UI는 기존 도구/브라우저 수동 절차와 mock 응답으로도 검증 가능하다.

| 검증 | 리뷰 체크리스트 | 필수 테스트·회귀 범위 | 보안·성능 검토 및 PASS 추가 기준 |
|---|---|---|---|
| V01 | 관리 버전·좌표·캐시 원인 구분, Java25 유지, 불필요 업그레이드 없음 | 승인된 오프라인 소스/테스트 컴파일, 전체 의존성 변경 diff | DB/앱 시작 없음, 다운로드는 별도 권한. 성공 로그와 컴파일러 근거 확보 |
| V02 | 실제 DB fallback·.env import 경로 차단, 테스트 분류 명시 | DB 없는 선택 테스트, contextLoads 설정 리뷰, 통합 대상 URL 검사 | 기존 DB·볼륨 접근 없음. 환경 부재가 성공으로 오인되지 않음 |
| V03 | loopback·host 변수·포트·validate/OIV 확인 | 설정 검증, 허용된 임시 앱의 리스너 확인, DB 설정 회귀 | 외부 노출/비밀 하드코딩 없음, 기존 볼륨 변경 없음 |
| V04 | 전환표·필드 초기화·flush 순서·쓰기 트랜잭션 | 고정 Clock 단위 테스트, 모든 허용/금지 전환, 생성·404 회귀 | 로그에 제목 없음, 추가 불필요 조회 없음, 재현 실패 해결 |
| V05 | 날짜 전용·시각 마감 분리, 단일 계산 위치 | 120/119/60/0/-1·자정·일요일·초 경계·마감 없음 전부 | 브라우저 재계산 없음, D02·D03 기대값 근거와 일치 |
| V06 | 정렬/제한/전체수 구분, 분모0, 한 요청 시간 일관성 | 0/4/5/7/8개, tie, 진행률, today/doing 없음 회귀 | 불필요 전체조회 증가 없음, D04 확정 동작 통과 |
| V07 | SQL check/index·converter·삭제 필터·매핑 | PostgreSQL17 제약/조회/rollback, 서울 자정 양끝, 삭제 DOING, C04 회귀 | V1 불변, 임시 DB 한정, 기존 데이터 변경 없음 |
| V08 | MVC 바인딩·code/message·예외 범위·입력 로그 | 생성/조회/PATCH/대시보드 HTTP 계약, malformed JSON/enum/null/길이, 경쟁 재현 | 입력/내부정보 미노출, 무조건409 변환 없음, C04~C07 회귀 |
| V09 | Bootstrap 뼈대·로컬 로딩·이름 규칙·CSS 단일화 | 1920×1080, 자산 로딩·자리표시자, 글꼴 적용 여부 | CDN 요청 없음, 기존 bundle 복제 없음, D05 범위 명시 |
| V10 | 서버 순서/문구 그대로, textContent·접근성 | 대표/빈/초과/긴 제목/XSS fixture, reduced-motion·키보드 | 스크립트 실행 없음, 반복 렌더 후 이벤트 누적 없음, V09 회귀 |
| V11 | fetch 단일 위치·이벤트 위임·서버 today·IME | 체크/▶/승격/추가 요청·성공 재조회, 한글 Enter·빈값·긴값·실패 | 제목 유출·중복 요청 없음, 백엔드 전환 계약 회귀 |
| V12 | 타이머 수명·입력 복원·응답 순서·Toast API 위치 | 30초 갱신·날짜변경·오프라인/500·지연 응답·입력 포커스 | 마지막 성공 화면 유지, Toast/타이머/DOM 누적 없음, V10~11 회귀 |
| V13 | REQ30개와 증거 연결, 문서 체크박스에 의존 안 함 | 승인된 격리 전체 흐름, seed·마이그레이션·기존 단위/통합·브라우저 | 기존 DB 접촉0, 실패/미검증 명시, 미해결 핵심 결함 없을 때 인수 |
| V14 | pg_dump 경로·실패 처리·복원 대상·주간 안내 | 임시 DB 덤프→별도 임시 DB 복원→건수/제약 비교 | 비밀/백업 Git 제외, 기존 DB 복원 금지, 새 자동 예약 없음 |
| V15 | 동작 변경 없는 diff, 근거 없는 최적화 제외 | 영향 서비스/API/UI 회귀, 최적화 시 동일 데이터 결과 비교 | 볼륨·데이터 불변, 개선 시 측정 증거 확보 |

실행 증거는 작업 ID, 기준 Git 상태, 실행 명령·환경, 결과/미실행 이유, 발견 결함, 재검증 결과를 포함한다. 검증 기록은 후속 승인된 보고서에 남기며 이번에는 기존 분석 보고서나 Kiro 체크박스를 갱신하지 않는다.

### 후속 제품 MVP 작업과 검증

현재 Spec은 하나뿐이므로 다음은 제품 문서에서 이미 요구한 기능의 준비된 분할안이다. 구현 전에 후속 상세 계약을 확정하며 임의의 API/모델을 만들어 현재 Spec에 편입하지 않는다. 예상 파일은 신규 위치 제안이며 범위는 원문을 따른다.

| ID·우선순위 | 목적·요구사항·예상 파일 | 구현 내용·AC | 선행 | Codex 검증: 리뷰·필수/회귀·보안/성능·PASS |
|---|---|---|---|---|
| F01 P1 | task/WISH 편집·삭제 API. product.md 편집 기능, tech.md API/DB 규칙. `J/task/{controller,dto,service,domain}`, 대응 `T/task/` | 제목/우선순위/마감·카테고리 수정, 논리 삭제。PATCH 필드 미지정/null 의미와 승격 우선순위는 먼저 확정. AC: 기존 status 변경 유지, 삭제행 조회·집계 제외 | C13, 후속 계약, D07 | 허용 필드·길이·due 관계·삭제 검토; 수정/null/삭제·기존 상태/집계 회귀; 입력 로그/과도한 갱신 확인. 계약·단위·격리 DB 테스트 모두 통과 시 PASS |
| F02 P1 | task 수정/WISH 추가수정 모달. product.md 편집 기능, frontend-conventions.md 편집 모달. `R/static/{index.html,js/modal.js,js/api.js,js/app.js,css/common.css}` | Bootstrap Modal, 우선순위·마감·카테고리 입력, 모달 내 삭제 확인, 첫 포커스·Esc·IME Enter·갱신 중 보존. AC: 문서 필드/열기 방식/재조회 모두 동작 | F01, C12 | Modal API 위치·textContent 검토; 모든 저장/삭제/취소/오류·모달 중 갱신; 상태 버튼/빠른 추가 회귀; 키보드/XSS/중복 핸들러 확인. 지정 흐름 전체 통과 |
| F03 P1 | event 모델·API·입력 모달. product.md event/편집, db-conventions.md event. `J/event/`, 새 `R/db/migration/Vn__...sql`, `T/event/`, UI 모달 | task와 독립, 제목/날짜/시작종료/장소/메모·논리삭제, end>start. 길이·경계/날짜 계약은 상세 설계에서 확정. AC: CRUD·검증·삭제 제외·index | C13, 후속 event 계약 | 스키마·시간대·REST/DTO 검토; 입력/자정/시각 역전/삭제·task 회귀; XSS/정보노출/index 확인. 격리DB/API/모달 전부 통과 |
| F04 P1 | 오늘 일정 타임라인. product.md 화면/동작, frontend-conventions.md 레이아웃. `J/dashboard/`, `J/event/service/`, UI render/app/CSS | 서버 startMinute/duration/다음 일정/15분 임박, 09~19시·현재선·지난/진행중 표시。범위 밖 일정 정책은 사용자 결정. AC: 서버 계산과 배치·상태 일치 | F03, 시간범위 정책 확정 | 서버 계산/프론트 단순환산 검토; 15분·09/19·진행중·범위밖·자정; task/갱신 회귀; 반복조회·레이아웃 비용 확인. 확정 경계 전부 통과 |
| F05 P1 | 하단 주간 완료·내일 일정. product.md 하단 바/동작. `J/dashboard/`, task/event repository/service, UI render/CSS | 서울 월~일 done_at 집계·오늘 강조·미래요일 비움·최소8 기준 막대, 내일 일정 최대3. AC: 서버 집계와 화면 수치 동일 | F03, C06~07, 후속 집계 계약 | 시간범위·삭제 제외·막대 정책 검토; 주경계/연월경계/0건/초과3건·진행률 회귀; 날짜 인덱스·조회수 확인. 집계/API/UI 경계 통과 |

F03은 모델/API와 모달을 별도 리뷰 변경으로 제출하고, F04·F05도 서버 응답과 화면을 순차 변경으로 나눈다. 정의가 부족한 후속 기능은 확정 전 착수하지 않으며 원문에 없는 반복 규칙·인증 설계를 이 계획에서 만들지 않는다.

## 7. 가장 먼저 구현할 작업 추천

첫 작업은 **C01: 테스트 의존성과 컴파일 환경 복구**다. STEP3의 실패 로그를 출발점으로 Testcontainers의 관리 버전·좌표와 오프라인 캐시 중 어느 부분이 문제인지 진단하고 최소 범위로 해결한다. 종료 기준은 DB 실행 없는 소스·테스트 컴파일 성공과 실제 Java25 toolchain 확인이다.

그다음 C02로 테스트가 기존 DB에 연결하지 않게 분리한다. 이 두 작업이 준비되면 Claude는 C03·C09를, 다른 독립 변경 단위로 C04~C08을 이어갈 수 있고 Codex는 각 단위의 증거를 검증한다. UI 구현을 진행하더라도 실제 검증이 없는 백엔드를 VERIFIED로 승격하지 않는다.

이번 결과물은 이 계획 보고서 하나이며, 구현 착수와 테스트 실행은 다음 지시에서 진행한다.
