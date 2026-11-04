---
inclusion: always
---

# 기술 스택과 규칙

## 스택
- 백엔드: Java 25 (LTS), Spring Boot 4.1.2, Spring Web, Spring Data JPA(Hibernate), Bean Validation, Flyway
- DB: PostgreSQL (로컬 Docker Compose). Oracle, H2는 쓰지 않는다.
- 프론트: HTML, Bootstrap 5, 바닐라 JavaScript. React 등 프레임워크, 번들러, npm 빌드는 쓰지 않는다.
- 서버는 Spring Boot가 정적 파일(src/main/resources/static)과 REST API를 함께 제공한다.
- 로그인과 Spring Security는 넣지 않는다. (나중에 AWS 배포 단계에서 검토)
- 빌드 도구는 프로젝트 생성 시 선택한 것(Gradle 또는 Maven)을 따른다.

## 실행 환경
- DB는 프로젝트 루트의 docker-compose.yml로 띄운다. 데이터는 named volume에 유지하고 restart: unless-stopped를 건다.
- DB 접속 정보는 .env로 분리하고 git에는 올리지 않는다. (.gitignore에 포함)
- 앱은 localhost에서만 접근한다. 외부 바인딩은 하지 않는다.

## JPA 규칙
- 스키마는 Flyway가 관리한다. spring.jpa.hibernate.ddl-auto는 validate로 고정한다. (create, update 사용 금지)
- spring.jpa.open-in-view=false
- enum 컬럼은 @Enumerated(EnumType.STRING)으로 매핑한다.
- 시간 필드는 OffsetDateTime(또는 Instant)으로 매핑한다.
- Entity를 API 응답으로 직접 노출하지 않는다. 요청/응답은 Java record DTO를 쓴다.
- 연관관계는 꼭 필요할 때만 만든다. 이 프로젝트의 task와 event는 서로 참조하지 않는다.
- 쿼리는 Spring Data JPA 메서드와 @Query(JPQL)를 쓴다. 네이티브 쿼리는 꼭 필요할 때만 쓴다.

## 시간 규칙
- 서버, 화면 모두 기준 시간대는 Asia/Seoul이다. "오늘"은 서울 시간 자정 기준이다. 한 주는 월요일에 시작한다.
- DB에는 timestamptz로 저장하고, API는 ISO-8601 (오프셋 포함) 문자열로 주고받는다.

## API 규칙
- 경로는 /api/ 로 시작하고, 리소스는 복수형이다. (/api/tasks, /api/events)
- 요청/응답은 JSON이다. 상태 변경은 PATCH로 한다.
- 성공은 200/201/204, 입력 오류는 400, 없는 리소스는 404, 규칙 위반(예: 상태 전환 불가)은 409를 쓴다.
- 오류 응답 형식은 { "code": "...", "message": "..." } 로 통일한다. 화면에 그대로 보여줄 수 있는 한국어 메시지를 담는다.
- 비즈니스 규칙(DOING 최대 1개, DONE 시 done_at 기록)은 Service 계층에서 트랜잭션으로 보장한다.

## 테스트
- JUnit 5와 Spring Boot Test를 쓴다.
- Repository와 통합 테스트는 실제 운영과 같은 PostgreSQL 환경(Testcontainers)을 우선 사용한다.
- 상태 전환 규칙처럼 비즈니스 규칙이 있는 Service는 단위 테스트를 반드시 작성한다.

## 작업 방식
- Kiro와의 대화, spec 문서(requirements/design/tasks), 코드 주석은 한국어로 쓴다. EARS 키워드(WHEN, THE SYSTEM SHALL 등)는 영어 그대로 둔다.
- 새 라이브러리나 의존성을 추가할 때는 이유를 먼저 설명하고 확인을 받는다.
- 백업은 pg_dump를 주 1회 실행하는 방식이며, 스크립트는 별도 task로 만든다.
