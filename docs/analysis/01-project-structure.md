# A Better Day 프로젝트 구조 분석

## 1. 프로젝트 개요

- Spring Boot 기반 일정 관리 대시보드 프로젝트다.
- 단일 Gradle 프로젝트이며 루트 프로젝트명은 `abetterday`다.
- 백엔드는 `com.abetterday` 패키지 아래에 구성되어 있다.
- 현재 확인된 애플리케이션 도메인은 `task`와 `dashboard`다.
- 프로젝트 루트에는 `.gradle`, `build`, `.idea`, `gradle`, `scripts`, `src`, `docs`, `.kiro`가 있다.
- 빌드 결과물(`build`)과 Gradle 캐시(`.gradle`)는 구조 파악에서 제외했다.
- 이번 분석에서는 기능 구현 완료 여부를 판단하지 않았다.

## 2. 기술 스택 및 버전

| 구분 | 확인 내용 |
|---|---|
| 빌드 | Gradle Wrapper 9.8.2 (`gradle/wrapper/gradle-wrapper.properties`) |
| 언어 | Java toolchain 25 |
| 프레임워크 | Spring Boot 4.1.2 |
| 의존성 관리 | `io.spring.dependency-management` 1.1.7 |
| 웹 | `spring-boot-starter-webmvc` |
| 영속성 | Spring Data JPA, Hibernate/JPA |
| 검증 | Spring Validation |
| DB 마이그레이션 | Spring Boot Flyway starter, PostgreSQL Flyway database 모듈 |
| DB 드라이버 | PostgreSQL JDBC |
| 보조 도구 | Lombok |
| 테스트 | JUnit Platform, Spring Boot test starters, Testcontainers PostgreSQL/JUnit Jupiter |
| 프론트엔드 리소스 | Bootstrap 5.3.8 vendor CSS/JS만 확인 |
| 데이터베이스 컨테이너 | `postgres:17` |

주요 설정 파일은 `build.gradle`, `settings.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `docker-compose.yml`, `src/main/resources/application.yml`이다. `.env`는 읽거나 내용을 보고서에 기록하지 않았으며, `.env.example`가 제공되어 있다.

## 3. 디렉터리 및 패키지 구조

```text
.
├─ .kiro/
│  ├─ steering/
│  └─ specs/spec-1-task-core/
├─ docs/
│  ├─ prototype/
│  └─ spec-prompts.md
├─ gradle/wrapper/
├─ scripts/dev-seed.sql
├─ src/
│  ├─ main/java/com/abetterday/
│  │  ├─ common/config
│  │  ├─ common/exception
│  │  ├─ dashboard/{controller,dto,service}
│  │  └─ task/{controller,domain,dto,repository,service}
│  ├─ main/resources/
│  │  ├─ db/migration
│  │  ├─ static/vendor/bootstrap-5.3.8
│  │  └─ templates/
│  └─ test/java/com/abetterday/
├─ build.gradle
├─ docker-compose.yml
├─ gradlew / gradlew.bat
└─ settings.gradle
```

Spring Boot 진입점은 `src/main/java/com/abetterday/AbetterdayApplication.java`다. `common`은 공통 설정·예외, `task`는 일정 핵심 도메인, `dashboard`는 대시보드 응답 조합을 담당하는 구조다.

## 4. Backend / Frontend / DB 구성

### Backend

- REST Controller: `TaskController`, `DashboardController`
- Service: `TaskService`, `DueLabeler`, `DashboardService`
- Repository: `TaskRepository`
- Domain: `Task`, `TaskStatus`, `TaskPriority`, `DeletedYnConverter`
- DTO: 일정 생성·상태 변경·응답 DTO 및 대시보드 중첩 응답 record
- 공통: `ClockConfig`, `BusinessException`, `ErrorResponse`, `GlobalExceptionHandler`
- 서버 포트는 `application.yml`에서 8090으로 설정되어 있다.
- JPA 스키마 자동 생성은 `validate`이며, Flyway migration 위치는 `classpath:db/migration`이다.

### Frontend

- `src/main/resources/templates/` 디렉터리는 존재하지만 애플리케이션 템플릿 파일은 확인되지 않았다.
- HTML, 애플리케이션 전용 CSS/JavaScript 파일은 확인되지 않았다.
- 확인된 프론트엔드 자산은 다음 두 vendor 파일이다.
  - `src/main/resources/static/vendor/bootstrap-5.3.8/css/bootstrap.min.css`
  - `src/main/resources/static/vendor/bootstrap-5.3.8/js/bootstrap.bundle.min.js`
- 화면 참고 자료는 `docs/prototype/`의 PNG 파일들이다.

### Database

- Docker Compose는 PostgreSQL 17 컨테이너 `a_better_day_db`를 정의한다.
- DB 포트는 환경변수 기반이며 `127.0.0.1`에만 바인딩된다.
- 영속 볼륨 이름은 `pgdata`다.
- Flyway V1 migration은 `task` 테이블, 상태·우선순위·삭제 여부 제약, 단일 DOING unique partial index, 조회용 인덱스를 정의한다.
- 개발용 초기 데이터 스크립트는 `scripts/dev-seed.sql`이다.

## 5. 주요 도메인별 관련 파일 목록

### Task / 일정

- `src/main/java/com/abetterday/task/controller/TaskController.java`
- `src/main/java/com/abetterday/task/service/TaskService.java`
- `src/main/java/com/abetterday/task/service/DueLabeler.java`
- `src/main/java/com/abetterday/task/repository/TaskRepository.java`
- `src/main/java/com/abetterday/task/domain/Task.java`
- `src/main/java/com/abetterday/task/domain/TaskStatus.java`
- `src/main/java/com/abetterday/task/domain/TaskPriority.java`
- `src/main/java/com/abetterday/task/domain/DeletedYnConverter.java`
- `src/main/java/com/abetterday/task/dto/CreateTaskRequest.java`
- `src/main/java/com/abetterday/task/dto/ChangeStatusRequest.java`
- `src/main/java/com/abetterday/task/dto/TaskResponse.java`
- `src/main/resources/db/migration/V1__create_task.sql`
- `scripts/dev-seed.sql`

### Dashboard

- `src/main/java/com/abetterday/dashboard/controller/DashboardController.java`
- `src/main/java/com/abetterday/dashboard/service/DashboardService.java`
- `src/main/java/com/abetterday/dashboard/dto/DashboardResponse.java`
- 화면 참고 자료: `docs/prototype/main.png`, `modal-event.png`, `modal-task-edit.png`, `modal-wish.png`

### 공통 인프라

- `src/main/java/com/abetterday/common/config/ClockConfig.java`
- `src/main/java/com/abetterday/common/exception/BusinessException.java`
- `src/main/java/com/abetterday/common/exception/ErrorResponse.java`
- `src/main/java/com/abetterday/common/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.yml`
- `docker-compose.yml`

## 6. Kiro 문서 전체 파일 목록

### `.kiro/steering/`

- `.kiro/steering/code-conventions.md`
- `.kiro/steering/db-conventions.md`
- `.kiro/steering/frontend-conventions.md`
- `.kiro/steering/product.md`
- `.kiro/steering/structure.md`
- `.kiro/steering/tech.md`

### `.kiro/specs/`

- `.kiro/specs/spec-1-task-core/design.md`
- `.kiro/specs/spec-1-task-core/requirements.md`
- `.kiro/specs/spec-1-task-core/tasks.md`

이번 단계에서는 위 문서의 상세 요구사항 내용은 분석하지 않았다.

## 7. 테스트 및 실행 환경

- 테스트 디렉터리: `src/test/java/com/abetterday/`
- 현재 확인된 테스트 파일: `AbetterdayApplicationTests.java`
- 현재 테스트는 `@SpringBootTest` 기반의 애플리케이션 컨텍스트 로딩 테스트 1개다.
- 표준 실행 명령은 다음과 같다.

```text
./gradlew test
```

Windows에서는 다음을 사용할 수 있다.

```text
gradlew.bat test
```

- 애플리케이션 실행 명령은 `./gradlew bootRun` 또는 Windows의 `gradlew.bat bootRun`이다.
- DB가 필요한 실행·통합 테스트 전에는 Docker Compose로 PostgreSQL을 기동하고 환경변수를 준비해야 한다.
- 이번 단계에서는 코드나 설정을 변경하지 않았고 테스트 실행도 수행하지 않았다.

### Git 상태

읽기 전용으로 확인한 상태는 다음과 같다.

- 브랜치: `dev`, `origin/dev` 추적
- 수정: `.kiro/specs/spec-1-task-core/tasks.md`
- 추가 후 수정 상태: `CLAUDE.md`
- 추적되지 않은 신규 소스 디렉터리: `src/main/java/com/abetterday/common/`, `dashboard/`, `task/`
- 확인된 미커밋 변경사항이 있으므로 작업 시작 전 기준점과 변경 소유자를 확인해야 한다.

## 8. 다음 단계에서 확인해야 할 문서

다음 단계에서 상세 요구사항과 구현·검증 범위를 파악할 때 우선 확인할 문서는 다음 순서가 적절하다.

1. `.kiro/steering/product.md`
2. `.kiro/steering/structure.md`
3. `.kiro/steering/tech.md`
4. `.kiro/steering/db-conventions.md`
5. `.kiro/steering/frontend-conventions.md`
6. `.kiro/steering/code-conventions.md`
7. `.kiro/specs/spec-1-task-core/requirements.md`
8. `.kiro/specs/spec-1-task-core/design.md`
9. `.kiro/specs/spec-1-task-core/tasks.md`

이 문서들은 이번 단계의 범위를 넘어서는 상세 요구사항·설계·작업 상태 확인에 사용한다.
