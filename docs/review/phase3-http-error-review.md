# Phase 3 HTTP 오류 응답 선행 결함 분석

> 2026-11-11 재검증: **PH3-HTTP-01 RESOLVED / PASS**. 아래 1~7절은 최초 분석 당시의 기록이다.
> 현재 수정 내용·테스트·제한은 8절에 기록하며, 4절의 결함 상태는 재검증 결과를 반영했다.

- 분석일: 2026-11-11, 작업 식별자: PH3-HTTP.
- 범위: 사용자가 지정한 HTTP 오류 응답과 직접 관련된 MVC 매핑·공통 예외 처리.
- 이번 요청은 T-13 구현 전 독립 분석을 명시적으로 승인한 작업이다. T-13 구현 완료 리뷰로 취급하지 않는다.
- 시작 시 `git status --short`, `git diff HEAD`, untracked 목록 모두 비어 있었다. 미커밋 변경·삭제 파일은 없었다.
  Claude의 동시 편집 여부를 직접 확인하는 수단은 없으며, 이 보고서는 현재 디스크의 구현을 대상으로 한다.
- 프로덕션 코드·설정·Spec·작업 체크박스 변경, 커밋, 실행 중인 앱 호출, 개발 DB 연결은 수행하지 않았다.

## 1. 문제 요약

표준 MVC 예외를 `GlobalExceptionHandler`의 `@ExceptionHandler(Exception.class)`가 먼저 처리하면서
원래의 404·405·415 상태를 모두 `500 / INTERNAL_ERROR`로 바꾼다.
서로 다른 요청이지만 수정 지점과 근본 원인이 같으므로 **PH3-HTTP-01 한 건**으로 등록한다.
오류 응답의 `code`·`message` 구조는 유지되지만 HTTP 상태와 오류 분류가 잘못된다.

## 2. 재현 요청 및 실제/예상 HTTP 상태

실제 컨트롤러·Advice를 사용하는 `@WebMvcTest`에서 아래 9건을 재현했다.
서비스는 Mockito 모의 객체이고 모든 요청에서 서비스 호출이 없음을 검증했다.
실행 중인 앱과 원래 Phase 3 로그는 사용하지 않았으므로, 아래 예외 유형은 이번 격리 재현의 확인 결과다.

| 요청 | 격리 재현 실제 응답 | 기대 상태 | 확인된 예외 |
|---|---|---|---|
| GET /favicon.ico | 500 / INTERNAL_ERROR | 404 | NoResourceFoundException |
| GET /api/none | 500 / INTERNAL_ERROR | 404 | NoResourceFoundException |
| GET /nothing | 500 / INTERNAL_ERROR | 404 | NoResourceFoundException |
| POST /api/none | 500 / INTERNAL_ERROR | 404 | NoResourceFoundException |
| GET /api/dashboard/x | 500 / INTERNAL_ERROR | 404 | NoResourceFoundException |
| PUT /api/tasks/19 | 500 / INTERNAL_ERROR | 405 | HttpRequestMethodNotSupportedException |
| PATCH /api/tasks/19, Content-Type: text/plain, 본문 `{"status":"DONE"}` | 500 / INTERNAL_ERROR | 415 | HttpMediaTypeNotSupportedException |
| GET /api/tasks/99999 | 500 / INTERNAL_ERROR | **405** | HttpRequestMethodNotSupportedException |
| DELETE /api/tasks/99999 | 500 / INTERNAL_ERROR | **405** | HttpRequestMethodNotSupportedException |

PATCH는 원래 Phase 3 요청의 경로·본문이 제공되지 않아 위 조건으로 구체화했다.
미디어 타입을 지원하지 않는 기존 상태 변경 API의 입력 처리 결함이며, 일반 필드 수정 API 추가 요구가 아니다.

### 추가 요청의 기대 응답 판단

- `src/main/java/com/abetterday/task/controller/TaskController.java:31`은 생성 POST,
  `:38`은 목록 GET, `:44`는 `/{id}` 상태 변경 PATCH다. 단건 GET·DELETE는 없다.
  같은 경로 패턴에 PATCH가 존재하므로 GET·DELETE는 ID 조회 전에 405가 된다.
  `99999`의 DB 존재 여부는 이 판단에 영향을 주지 않는다.
- `.kiro/specs/spec-1-task-core/design.md:208`의 API 표에도 단건 GET·DELETE가 없다.
  `requirements.md:15`, `:72`는 DELETE 구현을 Spec 2로 연기한다.
  `docs/analysis/04-development-plan.md:234`의 F01도 일반 필드 수정·논리 삭제를 후속 작업으로 분류한다.
- 구현된 PATCH로 없는 ID를 변경하는 경우에는 별개로 404가 맞다.
  `src/main/java/com/abetterday/task/service/TaskService.java:77`의 조회 실패는
  `:79`에서 `BusinessException("TASK_NOT_FOUND", ..., NOT_FOUND)`가 된다.
  이번에는 실제 DB 조회 대신 해당 예외를 모의 주입하여 응답 변환만 검증했다.

## 3. 확인된 원인과 근거

### 예외 처리 흐름

1. 알 수 없는 경로는 기본 정적 리소스 매핑으로 넘어가며, 리소스가 없으면
   `NoResourceFoundException`이 발생한다. POST /api/none도 이번 환경에서는 같은 예외였다.
2. 존재하는 `/{id}` 패턴에 지원하지 않는 메서드를 보내면
   `HttpRequestMethodNotSupportedException`이 발생한다.
3. PATCH의 `@RequestBody ChangeStatusRequest`를 text/plain으로 읽을 수 없어
   `HttpMediaTypeNotSupportedException`이 발생한다. `consumes`를 명시하지 않았어도 실제 재현됐다.
4. `GlobalExceptionHandler.java:21`, `:29`, `:40`, `:48`의 구체 핸들러에는 위 예외가 포함되지 않는다.
   따라서 `:56`의 포괄 핸들러가 선택되고 `:59`가 무조건 500을 반환한다.
   예외가 처리된 결과로 끝나므로 MVC 기본 예외 처리기가 원래 상태로 응답할 기회를 잃는다.

포괄 Advice를 제외한 별도의 standalone MVC 대조 구성에서는 미매핑 경로 404,
단건 GET 405 및 `Allow: PATCH`, text/plain PATCH 415를 확인했다.
따라서 컨트롤러나 서비스에서 새로 500을 던지는 문제가 아니라 Advice의 변환 문제로 판단한다.

### NoHandlerFoundException의 구분

실제 설정을 사용하는 9개 요청에서는 `NoHandlerFoundException`이 확인되지 않았다.
정적 리소스 매핑이 없는 standalone 대조 구성에서 GET /api/none을 보냈을 때만 발생했고,
동일한 Advice를 적용하면 이 예외도 500이 됐다.
이를 원래 Phase 3 요청의 실제 예외로 단정하지 않는다.
설정 변경 시에도 404를 유지하도록 수정 후 보조 회귀 대상으로 포함한다.

### 코드와 로그 증거

- `src/main/java/com/abetterday/common/exception/GlobalExceptionHandler.java:56`:
  `@ExceptionHandler(Exception.class)` → `:59`의 `ResponseEntity.internalServerError()`.
- `src/main/java/com/abetterday/task/controller/TaskController.java:44`:
  `@PatchMapping("/{id}")`; `:45`에서 요청 DTO를 받는다.
- `src/main/java/com/abetterday/dashboard/controller/DashboardController.java:12`, `:19`:
  `/api/dashboard`의 GET만 존재하며 `/x` 하위 경로는 없다.
- `src/main/java/com/abetterday/common/exception/ErrorResponse.java:4`:
  `ErrorResponse(String code, String message)` 규약.
- `src/test/java/com/abetterday/Phase3HttpErrorReviewTest.java:68`:
  9개 요청의 상태·본문·실제 예외 클래스·서비스 미호출을 검증한다.
  `:91`은 NoHandler 보조 재현, `:108`은 Advice 없는 대조 구성이다.

실행 산출물 `build/test-results/test/TEST-com.abetterday.Phase3HttpErrorReviewTest.xml`에는
다음 로그가 `GlobalExceptionHandler : 예상하지 못한 오류`와 함께 남았다.
산출물은 다음 테스트 실행 시 덮어써질 수 있으므로 핵심 내용을 여기에 기록한다.

```text
NoResourceFoundException: No static resource favicon.ico for request '/favicon.ico'.
NoResourceFoundException: No static resource api/none for request '/api/none'.
HttpRequestMethodNotSupportedException: Request method 'PUT' is not supported
HttpRequestMethodNotSupportedException: Request method 'GET' is not supported
HttpRequestMethodNotSupportedException: Request method 'DELETE' is not supported
HttpMediaTypeNotSupportedException: Content-Type 'text/plain;charset=UTF-8' is not supported
```

### 문서와 기대 동작의 차이

`requirements.md:128`의 REQ-3-7과 `design.md:326`의 예외 설계는
400·404·409·그 외 500으로 기술되어 있고 405·415를 별도로 명시하지 않는다.
현재 코드는 그 설계의 포괄 분기를 그대로 따르는 것으로 보인다.
이번 분석의 405·415 기대값은 사용자가 명시한 목표와 MVC 기본 처리 대조 결과에 근거한다.
Spec에 이미 405·415가 명시되어 있다고 해석하지 않는다. 문서의 보완 여부는 별도로 결정해야 한다.
관련 계획 항목은 `docs/analysis/04-development-plan.md:140`의 C08이며 T-13과 구분한다.

## 4. P0/P1 결함 목록

| ID | 심각도 | 파일:줄 | 문제 | 재현/근거 | 기대 동작 | 상태 |
|---|---|---|---|---|---|---|
| PH3-HTTP-01 | P1 | src/main/java/com/abetterday/common/exception/GlobalExceptionHandler.java:56, :59 (최초 분석 위치) | MVC 표준 클라이언트 오류를 포괄 핸들러가 모두 500으로 변환 | 최초 9건 재현 후, 수정본 코드 리뷰와 회귀 테스트 22건으로 상태·본문·헤더·로그 검증 | 표준 상태 및 필요한 헤더를 보존하면서 한국어 code/message로 통일하고, 예상하지 못한 오류만 500 처리 | RESOLVED |

공통 오류 분류가 요청 종류 전반에 걸쳐 잘못되는 핵심 HTTP 계약 결함이므로 P1로 판단한다.
P0 0건, P1 1건. P2/P3 추가 발견 0건이며 `docs/review/backlog.md`에 추가할 항목은 없다.
분석 시작 시 해당 백로그 파일도 존재하지 않았다. 중복 결함은 등록하지 않았다.

## 5. Claude가 수정해야 할 최소 범위

1. `GlobalExceptionHandler` 한 곳에서 위 표준 예외를 포괄 `Exception` 처리와 구분한다.
   리소스/핸들러 없음은 404, 메서드 미지원은 405, 미디어 타입 미지원은 415로 변환한다.
   작은 명시적 핸들러 또는 MVC 표준 처리 기반 접근 중 기존 구조에 맞는 방식을 선택한다.
2. 기존 `ErrorResponse`를 사용하고 한국어 메시지를 유지한다. 405의 `Allow` 등
   표준 예외가 제공하는 응답 헤더도 보존한다. 새 오류 코드 이름은 확정되지 않았으므로 Claude가 계약을 명시해야 한다.
3. 기존 구체 400 핸들러, `BusinessException`의 400·404·409, 예상하지 못한 오류의
   일반화된 500 본문과 내부 로그를 유지한다. 포괄 핸들러 삭제만으로 끝내면 오류 본문 규약을 잃을 수 있다.
4. Controller·Service·Repository·DB 스키마 변경이나 단건 GET·DELETE·일반 PATCH API 추가는 필요하지 않다.
   예외 전체를 일괄 400/404/409로 바꾸거나 예외 원문을 응답 메시지로 노출하지 않는다.

`ResponseEntityExceptionHandler` 상속 방식을 택한다면 기존 검증·파싱 핸들러와
상위 클래스의 예외 매핑 중복 또는 응답 형식 변경이 생길 수 있다. 이는 수정 방식에 따른 잠재 위험이며,
현재 코드에서 발생한 별도 결함으로 등록하지 않는다.

## 6. 수정 후 필요한 회귀 테스트

- 위 9개 요청의 기대 상태와 한국어 `code`·`message`를 검증하고, 405의 `Allow`에 PATCH가 포함되는지 확인한다.
- 정적 리소스가 없는 실제 MVC 구성의 404와 정적 매핑을 제외한 구성의 NoHandler 404를 각각 검증한다.
- 정상 정적 자산 GET, 정상 목록·대시보드 조회, 정상 JSON PATCH를 모의 서비스로 확인한다.
- 잘못된 JSON·enum·빈 필수 상태·잘못된 ID/쿼리 타입은 기존 400과 오류 코드를 유지하는지 확인한다.
- 서비스의 비즈니스 400·리소스 404·상태 전환 409, 예상하지 못한 예외 500의 기존 응답을 유지하는지 확인한다.
  내부 예외 문구가 응답에 노출되지 않는지도 확인한다.
- MVC 컨텍스트 초기화가 성공하고 핸들러 중복 등록이 없는지 확인한다.

현재 테스트는 결함 상태인 500을 의도적으로 확인하는 **현상 확인 테스트**다.
테스트 성공이 기능 정상이나 결함 해결을 뜻하지 않는다. 수정 후에는
`recordsCurrentRoutingFailure`와 `recordsNoHandlerFailure`를 올바른 상태를 기대하는 회귀 테스트로 바꿔야 한다.

### 이번 테스트 실행과 안전성

- `@WebMvcTest`로 MVC 계층만 구성하고 두 서비스를 `@MockitoBean`으로 대체했다.
  DataSource·JPA·Flyway·Repository를 생성하는 전체 앱 테스트는 실행하지 않았다.
  MockMvc 내부 요청만 사용했고 서버 포트·개발 PostgreSQL 5433에 연결하지 않았다.
- JDK 25를 사용했다. 기본 환경의 JAVA_HOME은 JDK 17이어서 실행 프로세스에서만 변경했다.
- 최초 기본 실행은 Gradle 잠금 파일 생성 제한으로 중단됐다.
  승인된 실행 환경에서 재실행한 기본 빌드는 오프라인 Testcontainers 의존성 해석 실패로
  `compileTestJava` 전에 중단됐다. 기본 빌드 전체의 정상 여부는 검증하지 못했다.
- 공용 `build.gradle`을 변경하거나 의존성을 추가하지 않았다.
  `src/test/phase3-http-review.init.gradle`에서 이번 MVC 검증에 불필요한
  Testcontainers 의존성만 실행 시 제외했다. 이 스크립트로 DB 테스트를 실행하면 안 된다.
- 아래 명령으로 지정 클래스만 실행한 결과 **19건, 실패 0, 오류 0, 건너뜀 0**이었다.
  그중 9건이 요청 재현, 2건이 대조/보조 구성, 4건이 입력 오류, 4건이 서비스 모의 예외 변환이다.

```powershell
$env:JAVA_HOME='C:/Users/Kwon Soohyun/.jdks/corretto-25.0.5'
$env:GRADLE_USER_HOME='C:/Users/Kwon Soohyun/.gradle'
./gradlew.bat --offline -I src/test/phase3-http-review.init.gradle test --tests com.abetterday.Phase3HttpErrorReviewTest --console=plain
```

## 7. 추가 확인이 필요한 사항

- 원래 Phase 3 실행 로그와 Content-Type 실패 요청의 구체 조건은 제공되지 않았다.
  원래 실행과 이번 재현이 동일한 예외였는지는 단정하지 않는다.
- 서비스 내부의 실제 DB 조회·트랜잭션·ID 부재는 이번 HTTP 변환 검증 범위 밖이다.
  DB 의존 테스트 및 실행 중인 애플리케이션에 대한 요청은 실행하지 않았다.
- 405·415 문서 계약과 새 오류 코드 명칭을 확정할 필요가 있다. 이번에는 Spec을 수정하지 않았다.
- 기본 빌드의 Testcontainers 의존성 해석 문제는 이번 결함과 분리한다.
  원인 범위를 넓혀 분석하거나 공용 의존성을 수정하지 않았다.

최초 분석 판정은 FAIL이었다. 아래 재검증 결과가 현재 판정을 대체한다.

## 8. PH3-HTTP-01 수정 결과 재검증

### 검토 범위와 시작 상태

- 사용자가 Claude의 구현 완료를 보고하고 재검증을 요청했다. PH3-HTTP-01 한 건만 검토했으며 T-13은 제외했다.
- HEAD 대비 tracked 변경은 `GlobalExceptionHandler.java`와 `design.md` 두 파일이었다.
  untracked인 기존 보고서·현상 확인 테스트·이전 init 스크립트는 선행 분석 산출물로 구분해 직접 읽었다.
  삭제 파일은 없었다. 기존 init 스크립트는 이번에 실행하거나 수정하지 않았다.
- Claude가 보고서에 기재한 FIXED 내용과 실제 diff가 일치했다.
  검토 중 확인한 변경 범위도 동일했다. 동시 편집 자체를 탐지하는 도구는 없어 그 여부를 별도로 확증하지는 않았다.
- 이번 편집은 `src/test/`와 이 보고서에 한정했다. 프로덕션·Spec·공용 빌드·작업 체크박스는 수정하지 않았다.

### 코드 리뷰 결과

| 위치 | 확인 결과 |
|---|---|
| `src/main/java/com/abetterday/common/exception/GlobalExceptionHandler.java:67` | 두 종류의 리소스/핸들러 없음 예외를 명시적으로 처리하고 404 RESOURCE_NOT_FOUND 반환 |
| 같은 파일 `:69` | 실제 리소스 경로가 favicon.ico이면 DEBUG, 다른 리소스/핸들러 부재는 WARN |
| 같은 파일 `:80` | 메서드 미지원 예외를 405 METHOD_NOT_ALLOWED로 처리하고 `e.getHeaders()` 복사 |
| 같은 파일 `:89` | 미디어 타입 미지원 예외를 415 UNSUPPORTED_MEDIA_TYPE으로 처리하고 표준 헤더 복사 |
| 같은 파일 `:29`, `:37`, `:47`, `:55` | 기존 비즈니스·검증·파싱·타입 예외 처리의 응답과 로그 로직이 diff에서 유지됨 |
| 같은 파일 `:98` | 예상하지 못한 예외는 기존 500 INTERNAL_ERROR, 고정 한국어 메시지, ERROR 스택 로그 유지 |
| `.kiro/specs/spec-1-task-core/design.md:337` | 404/405/415 예외·오류 코드·Allow 보존이 추가되어 구현과 일치 |

새 핸들러의 모든 Java 메서드에 한국어 요약 Javadoc이 있다.
기존 포괄 핸들러보다 구체적인 예외 매핑이 선택되며 MVC 컨텍스트 초기화도 성공했다.
Controller·Service·DB 변경은 없고 미구현 단건 GET·DELETE·일반 필드 PATCH를 추가하지 않았다.

### HTTP 회귀 검증 결과

| 요청/조건 | 수정 후 결과 | 추가 검증 |
|---|---|---|
| GET /favicon.ico | 404 RESOURCE_NOT_FOUND | 한국어 메시지, DEBUG 이벤트 정확히 1건 |
| GET /api/none, GET /nothing, POST /api/none, GET /api/dashboard/x | 404 RESOURCE_NOT_FOUND | NoResourceFoundException, WARN 이벤트 |
| PUT /api/tasks/19 | 405 METHOD_NOT_ALLOWED | 예외의 Allow 헤더와 응답이 동일하며 PATCH 포함, WARN |
| GET 및 DELETE /api/tasks/99999 | 405 METHOD_NOT_ALLOWED | 동일한 Allow 보존, 서비스 미호출, API 추가 불필요 |
| text/plain PATCH /api/tasks/19 | 415 UNSUPPORTED_MEDIA_TYPE | 실제 예외 클래스·표준 응답 헤더 보존, WARN |
| 정적 매핑 없는 별도 MVC 구성의 GET /api/none | 404 RESOURCE_NOT_FOUND | NoHandlerFoundException, 한국어 본문, WARN |
| 빈 필수 상태 `{}` | 400 VALIDATION_ERROR | 기존 검증 응답, WARN |
| 잘못된 JSON·enum·ID 타입·status 쿼리 | 400 BAD_REQUEST | 기존 파싱·타입 응답, WARN |
| 서비스 모의 비즈니스 예외 | 지정된 400/404/409 및 기존 오류 코드 | 기존 메시지 보존, WARN |
| 서비스 모의 IllegalStateException | 500 INTERNAL_ERROR | 고정 한국어 메시지, 내부 원문·예외명·스택 비노출, ERROR |
| 정상 Bootstrap CSS GET 및 task 목록 GET | 200 | 정상 응답에 Advice 로그 없음 |

오류 응답마다 `code`·`message`만 있는 2필드 JSON인지 검증했다.
500 테스트는 실제 `IllegalStateException` 객체를 모의 서비스에서 던져 MVC 예외 처리 경로를 통과시켰다.
응답에 내부 예외 메시지·예외 클래스·stackTrace·스택 위치가 없고,
서버 ERROR 로그의 ThrowableProxy에는 원래 예외 메시지가 남는 것까지 확인했다.
실제 운영 장애를 일으킨 테스트는 아니다.

### 로그 레벨 검증

`Phase3HttpErrorReviewTest`의 `captureAdviceLogs()`와 `assertAdviceLogLevel()`에서
실제 Logback ListAppender 이벤트를 수집해 수준과 건수를 검증했다.
테스트마다 Advice 로거를 DEBUG로 설정하고 종료 후 원래 수준으로 복원한다.
기본 운영 로그 설정을 DEBUG로 변경한 것은 아니다.
대표 실행 로그는 `build/phase3-http-review/junit.log`에 있으며 일부를 아래에 보존한다.

```text
DEBUG GlobalExceptionHandler : favicon 요청 리소스 없음
WARN  GlobalExceptionHandler : 요청한 리소스를 찾을 수 없음: No static resource api/none ...
WARN  GlobalExceptionHandler : 지원하지 않는 메서드: Request method 'PUT' is not supported
WARN  GlobalExceptionHandler : 지원하지 않는 미디어 타입: Content-Type 'text/plain;charset=UTF-8' is not supported
WARN  GlobalExceptionHandler : 비즈니스 예외: code=INVALID_STATUS_TRANSITION, ...
ERROR GlobalExceptionHandler : 예상하지 못한 오류
```

### 실행 방법과 빌드 제한

1. JDK 25에서 임시 init 스크립트 없이 기본 Gradle 명령을 오프라인·일반 모드로 실행했다.
   모두 `compileTestJava` 의존성 해석에서 중단됐다. 일반 모드의 실제 오류는
   `Could not find org.testcontainers:postgresql:.` 및 `org.testcontainers:junit-jupiter:.`이다.
   이 실행에서 테스트가 수행됐다고 기록하지 않는다.
2. 기존에 확보된 Gradle 테스트 worker 클래스패스와 최신 `build/classes/java/main`을 사용해
   수정된 테스트를 JDK 25 javac로 별도 컴파일하고 JUnit Launcher로 실행했다.
   기존 main 컴파일은 기본 Gradle 실행에서 UP-TO-DATE로 확인됐다.
   의존성 추가·버전 변경·다운로드나 Gradle init 스크립트 적용은 하지 않았다.
3. JShell 실행 시도는 JDK 콘솔의 UnsupportedOperationException으로 완료되지 않아 결과로 인정하지 않았다.
   이후 일반 Java Launcher 실행에서 **22건 시작·22건 성공·실패/중단/건너뜀 0건**을 확인했다.
   클래스패스에 apiguardian 메타데이터가 없어 컴파일 경고가 있었지만 javac와 Java 실행은 성공했다.

재현용 보조 도구는 `src/test/manual/`에 두어 기본 Gradle Java 테스트 소스 집합에 포함되지 않는다.
새 의존성은 추가하지 않았다. 기본 Gradle 실행이 복구되면 지정 테스트 클래스만 다시 실행할 수 있다.
이 직접 실행 결과가 전체 프로젝트 빌드나 전체 테스트의 성공을 뜻하지는 않는다.

```powershell
# 기본 빌드 시도: init 스크립트 미사용, 현재는 의존성 해석에서 중단됨
./gradlew.bat test --tests com.abetterday.Phase3HttpErrorReviewTest --console=plain

# 실제 성공한 직접 JUnit 실행 절차: 프로젝트 루트, 기존 캐시 클래스패스 필요
./src/test/manual/run-phase3-http-review.ps1 `
  -WorkerClasspathFile 'C:/Users/Kwon Soohyun/.gradle/.tmp/gradle-worker-classpath6030244447386216543txt' `
  -ReviewJdkPath 'C:/Users/Kwon Soohyun/.jdks/corretto-25.0.5'
```

### 안전성, 추가 결함과 미검증 항목

- 실행 전 테스트 구성을 확인했다. `@WebMvcTest`에 실제 Controller·Advice만 로드하고
  두 서비스는 `@MockitoBean`으로 대체했다. 각 테스트의 요청 전에 DataSource 빈 부재와
  서비스가 모의 객체임을 단언했고, 별도 안전성 테스트로 EntityManagerFactory·Flyway 부재도 확인했다.
- 개발 DB 5433·실행 중인 bootRun 앱·외부 HTTP 포트는 사용하지 않았다.
  DB 데이터/설정 변경 및 DB 의존 테스트는 수행하지 않았다.
- 관련 수정에서 추가 P0/P1/P2/P3 결함은 발견하지 않았다. 기존 의존성 해석 장애는 선행 보고서의
  알려진 실행 제한이며 이번 변경으로 발생한 회귀가 아니므로 중복 결함이나 백로그로 등록하지 않는다.
  `docs/review/backlog.md`에 추가할 항목은 없다.
- 전체 Gradle 빌드·전체 테스트, 실제 DB를 통과한 서비스 로직, 실행 중인 앱에서의 동작,
  원래 Phase 3 실행 로그와의 일치 여부는 미검증이다. PH3-HTTP-01의 필수 HTTP 검증과 구분한다.
- 위 한정된 코드 리뷰와 필수 회귀 검증을 통과했으므로 **FIXED → RESOLVED**로 판정한다.

테스트 결과: DB 없는 직접 JUnit 회귀 테스트 22/22 성공, 임시 Gradle init 스크립트 미사용.
최종 판정: **PASS** — PH3-HTTP-01 RESOLVED, 상태·본문·Allow·로그 수준 및 기존 오류 처리 회귀 검증 통과.
제한: 기본 Gradle 실행은 기존 Testcontainers 의존성 해석 실패로 중단됐으며 전체 빌드·DB 동작은 미검증.
