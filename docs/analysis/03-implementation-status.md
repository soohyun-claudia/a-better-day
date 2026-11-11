# 요구사항 대비 실제 구현 상태 검증

분석 기준: STEP 1·2 보고서, 현재 작업 트리의 소스, 최신 Steering의 Bootstrap 보완 규칙. 기존 미커밋 소스도 실제 검증 대상으로 포함했다. Tasks 체크박스는 완료 근거로 사용하지 않았다. 소스·설정·기존 변경사항은 수정하지 않았다.

## 1. 판정 기준과 전체 현황

- VERIFIED: 실제 테스트 통과 또는 해당 요구사항 전체를 입증하는 명확한 검증 근거.
- IMPLEMENTED_UNVERIFIED: 코드 경로는 존재하지만 실행·통합 검증이 부족함.
- PARTIAL: 일부 기준만 구현됨.
- NOT_IMPLEMENTED: 해당 기능·테스트가 없음.
- MISMATCH: 문서와 구현의 명확한 불일치.
- UNKNOWN: 판단 근거 부족.

Spec 1의 하위 요구사항 30개를 판정했다. VERIFIED 0개, IMPLEMENTED_UNVERIFIED 14개, PARTIAL 7개, NOT_IMPLEMENTED 9개다. 컴파일 캐시 적중을 기능 검증으로 취급하지 않았다. DB 적용 여부·HTTP 실제 동작은 확인하지 않았다.

## 2. 전체 요구사항별 구현 현황

모든 REQ의 원본은 `.kiro/specs/spec-1-task-core/requirements.md`다. 아래 소스 경로 약어는 이 표와 이후 설명에서 동일하게 사용한다.

| 약어 | 실제 경로 |
|---|---|
| TC | `src/main/java/com/abetterday/task/controller/TaskController.java` |
| TS | `src/main/java/com/abetterday/task/service/TaskService.java` |
| DL | `src/main/java/com/abetterday/task/service/DueLabeler.java` |
| TR | `src/main/java/com/abetterday/task/repository/TaskRepository.java` |
| TE | `src/main/java/com/abetterday/task/domain/Task.java` |
| CV | `src/main/java/com/abetterday/task/domain/DeletedYnConverter.java` |
| CR | `src/main/java/com/abetterday/task/dto/CreateTaskRequest.java` |
| DS | `src/main/java/com/abetterday/dashboard/service/DashboardService.java` |
| DR | `src/main/java/com/abetterday/dashboard/dto/DashboardResponse.java` |
| EH | `src/main/java/com/abetterday/common/exception/GlobalExceptionHandler.java` |
| SQL | `src/main/resources/db/migration/V1__create_task.sql` |
| CFG | `src/main/resources/application.yml` |

| ID | 상태 | 코드 위치와 판단 근거 | 검증 부족·누락 |
|---|---|---|---|
| REQ-1-1 | IMPLEMENTED_UNVERIFIED | `docker-compose.yml:3` PostgreSQL 17, 5행 restart, 12행 loopback, 14행 named volume | 실제 기동·재시작·영속성 미검증 |
| REQ-1-2 | PARTIAL | Compose 7~12행 변수 참조, CFG:9 .env import, `.env.example` 존재, `git check-ignore .env` 성공 | host는 CFG:11 localhost 하드코딩. 요구사항의 호스트 환경변수화 미충족. 비밀값은 열람하지 않음 |
| REQ-1-3 | IMPLEMENTED_UNVERIFIED | CFG:9,11,16~20 연결 설정, validate, OIV false, Flyway 활성화 | 연결·migration 실행 안 함 |
| REQ-2-1 | IMPLEMENTED_UNVERIFIED | SQL:5~26 모든 컬럼·기본값·check. TE:28~56 필드 매핑 | 실제 PostgreSQL 적용·Hibernate 호환성 미검증 |
| REQ-2-2 | IMPLEMENTED_UNVERIFIED | SQL:30~32 partial unique, TS:76~95 트랜잭션·기존 DOING flush | 동시 요청은 DB가 방어하나 API 충돌 처리 미검증 |
| REQ-2-3 | IMPLEMENTED_UNVERIFIED | TE:25 삭제 필터, 48~50 변환, CV:12~19 Y/N 처리 | 삭제행 조회 제외 통합 테스트 없음. DELETE 연기는 Spec 1에서 허용됨 |
| REQ-2-4 | IMPLEMENTED_UNVERIFIED | TS:91 Clock 기반 완료 시각, TE:75~98 상태 변경 시 doneAt 기록·초기화 | 고정 Clock 실행 검증 없음 |
| REQ-3-1 | IMPLEMENTED_UNVERIFIED | TC:31~34 POST·201·Location, CR:13~27 strip/길이/필수 검증, TS:38~52 생성 상태·due 관계·기본 MEDIUM | HTTP 바인딩·enum·null·길이 테스트 없음 |
| REQ-3-2 | IMPLEMENTED_UNVERIFIED | TC:38~40 선택 List status, TS:58~62 전체/필터, TR:16 | 쉼표 분리 및 잘못된 status HTTP 실행 검증 없음 |
| REQ-3-3 | IMPLEMENTED_UNVERIFIED | DashboardController:19~21, DS:47~74 정렬·제한·집계·진행률, TS:121~125 due 매핑 | DR에는 일정/하단 필드 없음. Design도 동일하므로 자리표시자를 화면에서 제공할 해석 가능. 직렬화·자정 일관성 미검증 |
| REQ-3-4 | IMPLEMENTED_UNVERIFIED | TC:44~46, TS:91,101~117 TODO/DOING→DONE만 허용 | 200·409·404 실제 응답 미검증 |
| REQ-3-5 | IMPLEMENTED_UNVERIFIED | TS:83~95 기존 DOING markTodo→saveAndFlush 후 새 DOING 저장 | 정상·실패 rollback 및 경쟁 요청 미검증 |
| REQ-3-6 | IMPLEMENTED_UNVERIFIED | TS:92,105, TE:93~98 WISH만 승격, MEDIUM/이번 일요일/time null | 일요일·잘못된 전환 테스트 없음 |
| REQ-3-7 | IMPLEMENTED_UNVERIFIED | EH:21~60 400/404/409/500, ErrorResponse record. ChangeStatusRequest:7 null 거부 | MVC 전체 오류와 DB 제약 예외 처리 범위는 부족할 수 있음 |
| REQ-4-0 | PARTIAL | DS와 DL이 서버 계산 수행, 프론트 코드 없음 | 렌더링·브라우저 시계 예외 경로 없음 |
| REQ-4-1 | PARTIAL | static/vendor/bootstrap-5.3.8의 CSS·bundle JS 있음 | index.html, common.css, 로컬 글꼴·100vh 레이아웃 없음 |
| REQ-4-2 | PARTIAL | DS:62~74 dateLabel·doingTask·진행률 데이터 제공 | 시계·날짜·안내문·막대 화면 없음 |
| REQ-4-3 | PARTIAL | DS:36~41 정렬,56행 7개 제한; DL:32~126 마감·긴급 계산 | 행·빠른 추가·IME·강조·버튼 없음. 문서 경계 모호점은 아래 참조 |
| REQ-4-4 | PARTIAL | TS:66~69 오늘 범위, TR:22 내림차순, DS:57 최대4/전체수 | 완료 시각 HH:mm·빈 상태·화면 없음 |
| REQ-4-5 | PARTIAL | DS:51~58 WISH 생성순·최대4·총수/숨김 | 태그·승격 버튼·접힘 문구 화면 없음 |
| REQ-4-6 | NOT_IMPLEMENTED | resources 파일 목록에 일정 패널 HTML 없음 | Spec 3 안내 자리 자체가 없음 |
| REQ-4-7 | NOT_IMPLEMENTED | resources 파일 목록에 하단 바 HTML 없음 | Spec 4 안내 자리 자체가 없음 |
| REQ-5-1 | NOT_IMPLEMENTED | app.js 없음 | 30초 조회·포커스 보존 없음 |
| REQ-5-2 | NOT_IMPLEMENTED | api.js/render.js/app.js 없음 | 오류 알림·마지막 화면 유지 없음 |
| REQ-5-3 | NOT_IMPLEMENTED | 앱 JS/CSS/HTML 없음 | 이벤트 위임·aria-label·호버·pulse/reduced-motion 없음 |
| REQ-6-1 | IMPLEMENTED_UNVERIFIED | `scripts/dev-seed.sql:29` WISH7,40행 TODO7,51행 DOING1,55행 DONE3. 앱 소스에 자동 로더 없음 | 수동 스크립트 실행하지 않음 |
| REQ-7-1 | NOT_IMPLEMENTED | test 트리에 기본 contextLoads만 있음 | TaskServiceTest 없음 |
| REQ-7-2 | NOT_IMPLEMENTED | 동일 | PostgreSQL Repository/Testcontainers 테스트 없음 |
| REQ-7-3 | NOT_IMPLEMENTED | 동일 | DueLabelerTest 없음 |
| REQ-7-4 | NOT_IMPLEMENTED | 동일 | DashboardServiceTest 없음 |

표의 모든 행에 관련 동작을 검증하는 기존 기능별 테스트는 없다. `contextLoads`는 개별 Acceptance Criteria의 근거가 아니다.

## 3. 주요 코드 경로와 설계 비교

### API·도메인·트랜잭션

TC의 네 개 API 중 task POST/GET/PATCH와 DashboardController의 GET이 문서 경로에 대응한다. Controller에서 DTO 검증 후 Service로 위임하며 Service는 readOnly 기본·쓰기 메서드 재정의를 사용한다. 전환 표는 TODO→DOING/DONE, DOING→DONE, WISH→TODO이며 나머지는 409다. 같은 상태로의 재요청도 409다.

TS:85~89의 flush 순서는 설계와 같다. DB unique index 때문에 DOING이 둘 이상 커밋되는 것은 방어하지만, 요청 경쟁을 직렬화하는 잠금·버전 필드는 없다. 경쟁 요청의 성공 보장과 API 오류 계약은 별도 확인해야 한다.

CR은 `strip()`으로 제목과 카테고리를 정규화한다. 필수·길이 검증은 Controller의 `@Valid` 경로에 의존한다. Service 직접 호출에 대한 제목 검증은 없으므로 후속 Service 테스트와 API 테스트를 구분해야 한다.

TE의 enum, converter, 삭제 필터, 시간 필드는 설계와 대응한다. title/category의 명시적 @Column 길이는 없어 JPA 기본 메타데이터와 SQL 길이 차이가 있다. 실제 validate 동작의 실패로 단정할 수는 없으며 PostgreSQL 통합 환경에서 확인해야 한다.

### 마감·대시보드 경계

- DL:70~72는 현재 시각과 target 양쪽을 분 단위로 절삭한다. 예를 들어 10:00:50→12:00:00은 실제 119분10초 남지만 diff=120이라 urgent=false다. 같은 분의 이미 지난 마감도 “지금 마감”이 된다. 문서가 분 단위 표시를 요구하지만 초 단위 긴급 판정의 반올림 정책은 명확하지 않다. 잠재적 경계 불일치로 기록한다.
- 정확히 120분은 코드상 urgent=false다. REQ-4-3의 “2시간 이내”와 REQ-7-3의 “정확히 2시간 false”가 상충한다. 코드가 후자의 명시적 테스트 기준과 Design의 diff<120을 따른다.
- 일요일 날짜 전용 마감은 DL:53~60에서 “오늘 중”이다. Design의 일반 판정 규칙과 일치하지만 Design 6-3의 일요일 테스트 행은 “이번 주 마감”이라고 적혀 있어 테스트 기대값 수정 합의가 필요하다.
- DS:39~40에서 같은 날짜의 시각 없는 마감은 시각 있는 마감보다 뒤다. 날짜 전용 “오늘 중”의 정확한 정렬 의미는 명시되지 않아 결함으로 단정하지 않는다.
- DS:73의 퍼센트는 반올림한다. 소수 처리 정책은 문서에 없고 0/50/100 외 값 검증이 필요하다.
- TS:67, DS:63, DL:48/71/80에서 각각 Clock을 읽는다. 자정에 요청이 걸치면 조회 범위·today·label이 달라질 수 있다. 한 요청의 기준 시각 고정은 없다.
- DS는 목록 전체를 메모리에 가져와 정렬·limit한다. 작은 개인 DB에서는 동작 가능하나 데이터 증가 시 전체 조회·반복 매핑 비용이 발생한다.

## 4. Steering·제품 요구사항과 구현 비교

원본 ID가 없는 Steering 항목은 경로와 절 제목으로 추적한다. 아래 상태는 위 Spec 30개 집계에 포함하지 않는다.

| 원본 문서·절 | 상태 | 근거 |
|---|---|---|
| tech.md 실행 환경: 앱 localhost 전용 | MISMATCH | CFG:1~2에는 port만 있고 server.address 또는 다른 바인딩 제한이 없음. DB loopback 제한은 앱 리스너 제한을 대신하지 않음. 실제 리스너는 실행하지 않아 미확인 |
| structure.md 이름 표기 | MISMATCH | settings.gradle:1은 abetterday, Compose 볼륨은 pgdata. 문서는 a-better-day / a_better_day. Spec Tasks는 pgdata를 요구하므로 문서 간 충돌도 존재 |
| tech.md 스택/JPA/API·도메인별 구조 | IMPLEMENTED_UNVERIFIED | build.gradle 및 com/abetterday 패키지, record DTO, 생성자 주입, Service 트랜잭션 대응. 런타임 프레임워크 연동 미검증 |
| frontend-conventions.md 레이아웃 / 편집 모달, tech.md 최신 UI 원칙 | PARTIAL | Bootstrap vendor만 존재. HTML·바닐라 JS·Bootstrap 레이아웃/유틸리티/Modal/Toast 사용 코드 없음 |
| product.md 편집 기능 | NOT_IMPLEMENTED | 제목·우선순위·마감 수정, WISH 모달·카테고리 선택, 삭제 API 없음. PATCH DTO는 status만 받음. Spec 1에서는 후속 범위 |
| product.md 오늘 일정·하단 바, db-conventions.md event 테이블 | NOT_IMPLEMENTED | event Entity/Repository/API/migration, 타임라인·주간집계·내일 일정 없음. Spec 1 범위 밖이며 자리표시자도 없음 |
| tech.md 테스트 / code-conventions.md 테스트 | NOT_IMPLEMENTED | 업무 단위/통합 테스트 없음. 기존 contextLoads에는 DisplayName/given-when-then도 없음 |
| db-conventions.md 운영 / tech.md 백업 | NOT_IMPLEMENTED | scripts에는 seed만 있고 pg_dump·복원 스크립트/안내 없음. 주간 백업 실제 수행 이력은 UNKNOWN |
| tech.md 로그인·Spring Security 제외 | VERIFIED | 전체 소스·build.gradle에 인증 계층·Security 의존성 없음을 정적으로 확인. Spec 기능 VERIFIED 수와 별도 |
| code-conventions.md 로깅 | PARTIAL | SLF4J·info/warn/error 사용. EH:42,50 예외 message 전체 로깅은 사용자 입력 일부가 포함될 가능성 있음 |
| code-conventions.md 포맷·주석 | PARTIAL | 신규 메서드 대부분 한국어 주석 있음. TE:58 private 생성자와 기존 main/test는 요약 주석 없음. 일부 record/문장이 120자 초과 |
| product.md 향후 반복·외부연동·AWS | NOT_IMPLEMENTED | 관련 소스 없음. 현재 Spec 1 결함·필수 작업으로 산정하지 않음 |

STEP 2의 “event 범위 혼재”는 제품 전체와 Spec 단계가 다른 것으로 해석 가능하므로 event 부재만으로 Spec 1 MISMATCH라고 판정하지 않았다. 초기 카테고리는 확정 전제·seed에 나타나지만 허용 목록을 강제하는 규칙은 없으므로 임의 목록 검증을 요구하지 않는다.

## 5. 문서와 코드의 불일치·부분 구현

- 포트: Design 시스템 구성과 Tasks T-22는 8080, CFG:2는 8090이다.
- 샘플 수: Design 5장·T-04는 TODO4, 실제 seed는 TODO7이다. REQ-6-1의 최소 포함 조건은 만족하며 TODO+DOING=8로 숨김 검증이 가능하다. 문서 동기화 차이다.
- T-12의 일정·하단 자리 데이터 체크는 실제 DR:7에 필드로 나타나지 않는다. Design DTO도 같은 구조이므로 API 필드 필수 여부는 확인 필요하다.
- Tasks의 백엔드 완료 체크에도 HTTP·Service·Repository 테스트 통과 근거가 없다. 테스트·프론트 미체크는 실제 파일 부재와 대응한다.
- 글꼴, 모달, 토스트, 화면 렌더링, 빠른 추가, 주기 갱신은 미구현이다. vendor 파일만으로 UI 구현을 인정하지 않는다.

## 6. 컴파일·테스트 실행 결과

사용자 추가 승인에 따라 오프라인 소스/테스트 컴파일만 실행했다.

```text
.\gradlew.bat --offline --no-daemon compileJava compileTestJava
```

1. 샌드박스 최초 시도: `C:\.gradle\wrapper\dists\...zip.lck` 상위 디렉터리 생성 실패. 빌드 이전 캐시 접근 문제다.
2. 승인된 사용자 환경 실행: 종료 코드 1, `BUILD FAILED in 11s`.
3. `:compileJava UP-TO-DATE`: 기존 산출물이 최신으로 판정됐다. 이번 실행에서 소스를 다시 컴파일한 것은 아니며 새 컴파일 성공으로 표현하지 않는다.
4. `:processResources`, `:classes`는 컴파일 의존 작업으로 UP-TO-DATE였다. DB 실행·마이그레이션 작업은 아니다.
5. `:compileTestJava FAILED`: testCompileClasspath에서 `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`를 해석하지 못했다. 오프라인 캐시에 사용할 버전이 없다는 메시지다. 관련 파일은 `build.gradle`의 두 testImplementation 선언이다. 의존성 버전을 직접 선언하지 않은 구성의 관리 버전 해석도 후속 확인이 필요하며, 이번 로그만으로 단순 다운로드 부재와 좌표/버전 문제를 구별할 수 없다.
6. 실패 지점은 테스트 Java 소스 컴파일 이전 의존성 해석이다. 테스트 소스 문법 오류로 판정하지 않는다.

실제 테스트 실행 수는 0이다. 유일한 기존 테스트 `src/test/java/com/abetterday/AbetterdayApplicationTests.java:6`은 격리 프로필이나 Testcontainers 연결 설정 없는 `@SpringBootTest`다. 기본 CFG를 사용하면 기존 PostgreSQL 연결 및 활성화된 Flyway 실행 가능성이 있어 실행하지 않았다. 사용자 조건에 따라 온라인 재시도, bootRun, seed 실행, Docker 기동, DB 연결·초기화·데이터 변경은 수행하지 않았다.

셸 기본 Java는 17.0.9였지만 프로젝트는 toolchain 25를 요구한다. 승인 환경의 Gradle은 compileJava 캐시 판정까지 진행했으며 사용된 실제 컴파일러 버전은 이번 실행으로 확정하지 않았다.

## 7. 테스트 누락 영역

- REQ-7-1/T-18: 모든 전환 조합, 기존 DOING flush 순서, rollback, 완료 시각 및 null 초기화.
- REQ-7-2/T-21: 서울 자정 시작 포함·다음 자정 제외, 삭제행 필터, enum·converter 매핑, 실제 PostgreSQL check/index.
- REQ-7-3/T-19: 120/119/60/0/-1분, 초·분 절삭, 1시간·하루 지남, 자정·토/일·다음 주, 날짜 전용·마감 없음.
- REQ-7-4/T-20: DOING/priority/due/createdAt 정렬, 0/4/5/7/8개 제한·숨김, 전체 완료 수와 표시 수 구별, 진행률·반올림.
- REQ-3 전반: MVC JSON/enum/null/크기/공백 검증, status 다중 필터, Location, null 필드 직렬화, HTTP 오류 계약.
- 경쟁 요청: 동시에 ▶, 같은 task 완료 중복, 삭제와 전환 경쟁의 정책·409 처리.
- 프론트 전체: 향후 구현 후 Bootstrap 레이아웃·Toast/Modal, 1920×1080, IME·XSS·키보드·reduced-motion·갱신 보존·실패 복구 검증 필요.

## 8. 주요 결함 및 잠재적 위험

| 우선순위 | 항목 | 근거와 영향 |
|---|---|---|
| 높음 | 앱 loopback 바인딩 제한 누락 | CFG:1~2. 인증을 의도적으로 제외한 앱이 외부 인터페이스에 바인딩될 가능성. 실제 네트워크 도달 여부는 미검증 |
| 높음 | 기능 테스트 및 UI 부재 | REQ-4/5/7. 백엔드 체크박스만으로 사용자 기능 정상 동작 판단 불가 |
| 높음 | 오프라인 테스트 컴파일 차단 | build.gradle Testcontainers 의존성 해석 실패. 테스트 작성·실행 이전 빌드 환경 확인 필요 |
| 중간 | 경쟁 DOING 요청의 일반 500 가능성 | TS:85~95 잠금 없음, EH:56 일반 예외 처리. DB unique 위반은 현재 409 전용 변환 없음. 순차 정상 경로와 구별할 것 |
| 중간 | 입력 관련 예외 원문 로깅 | EH:42,50에서 e.getMessage(). 파싱 오류에 사용자 값이 포함될 수 있어 Steering 입력 로그 최소화 취지와 충돌 가능 |
| 중간 | 자정/초 단위 경계 | DL:70~75, DS:63, TS:67. 서버 계산이 여러 시점에 분산되고 분 절삭 정책이 추가됨 |
| 낮음 | 문서 포트·샘플 수·글꼴 범위 불일치 | 실행 절차와 Acceptance Criteria 해석 혼선 |

우선 후속 구현 담당자가 UI·테스트 범위와 실행 환경을 정리하고, 별도로 승인된 격리 PostgreSQL 테스트 환경에서 DB/API 연동을 검증해야 한다. 이 보고서의 IMPLEMENTED_UNVERIFIED는 동작 보증이나 완료 선언이 아니다.
