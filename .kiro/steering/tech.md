---
inclusion: always
---

# 기술 스택과 규칙

## 스택
- 프로젝트 이름은 A Better Day, 베이스 패키지는 com.abetterday다. (표기는 structure.md 참고)
- 백엔드: Java 25 (LTS), Spring Boot 4.1.2, Spring Web, Spring Data JPA(Hibernate), Bean Validation, Flyway, Lombok
- DB: PostgreSQL (로컬 Docker Compose). Oracle, H2는 쓰지 않는다.
- 프론트: HTML, Bootstrap 5.3.8, 바닐라 JavaScript. React 등 프레임워크, 번들러, npm 빌드는 쓰지 않는다. Bootstrap 6은 알파라서 쓰지 않는다.
- Bootstrap은 bootstrap.bundle.min.js(UMD, Popper 포함)를 static/vendor/에 로컬 파일로 두고, index.html에서 일반 <script>로 먼저 불러온다. 앱 코드(ES 모듈)는 window.bootstrap으로 접근하며 Bootstrap 파일을 ESM으로 import하지 않는다. Bootstrap JS API 호출은 modal.js 한 파일에서만 한다.
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
- 삭제는 논리 삭제(deleted_yn)다. Entity에는 @SQLRestriction("deleted_yn = 'N'")을 두고, 'Y'/'N'과 boolean 변환은 변환 클래스(AttributeConverter) 한 곳에서 한다. 자세한 규칙은 db-conventions를 따른다.
- 어노테이션은 꼭 필요한 것만 쓴다.
  - Entity: @Entity, @Id, @GeneratedValue, @Enumerated를 기본으로 하고, @Column은 이름이나 길이가 기본값과 다를 때만 쓴다.
  - 요청 DTO: Bean Validation(@NotBlank, @Size)만 쓴다. 응답 DTO에는 어노테이션을 붙이지 않는다. Jackson 어노테이션은 꼭 필요할 때만 쓴다.
- Lombok은 @Getter, @NoArgsConstructor(access = AccessLevel.PROTECTED)(Entity), @RequiredArgsConstructor(controller, service)만 쓴다. @Data, @Setter, Entity의 @EqualsAndHashCode는 쓰지 않는다. DTO는 record라 Lombok이 필요 없다.
- 연관관계는 꼭 필요할 때만 만든다. 이 프로젝트의 task와 event는 서로 참조하지 않는다.
- 쿼리는 Spring Data JPA 메서드와 @Query(JPQL)를 쓴다. 네이티브 쿼리는 꼭 필요할 때만 쓴다.

## 시간 규칙
- 서버, 화면 모두 기준 시간대는 Asia/Seoul이다. Clock 빈 하나로 시간대를 모은다. "오늘"은 서울 시간 자정 기준이다. 한 주는 월요일에 시작한다.
- DB에는 timestamptz로 저장하고, API는 ISO-8601 (오프셋 포함) 문자열로 주고받는다.

## 마감(due) 저장과 표기
- task의 마감은 due_date(DATE)와 due_time(TIME) 두 컬럼으로 저장한다. 시간대 없는 서울 기준 로컬 값이며, timestamptz 규칙의 의도된 예외다.
- 시각이 있는 마감은 date + time, 날짜만 있는 마감(오늘 중, 내일, 11/6)은 date만, 마감 없음은 둘 다 null이다. due_time만 있는 행은 만들 수 없다.
- "이번 주"는 이번 주(월~일)의 일요일 날짜를 due_date에 저장하고 due_time은 null로 둔다. 별도 컬럼은 두지 않는다.
- 마감 표기 우선순위: 지남 → 오늘 중 → 내일 → 이번 주 일요일이면 "이번 주 마감" → 그 외 "M/D 마감". 일요일에는 "오늘 중"이 먼저 적용된다.
- 이 표기 계산(dueLabel, urgent)은 백엔드의 한 곳에서만 한다.

## 역할 분리 (백엔드 / 프론트)
- 데이터 가공은 전부 백엔드에서 한다. 프론트는 받은 값을 그리기만 한다.
- 백엔드가 응답에 담는 것: 정렬된 목록, 표시 개수 제한과 "N개는 숨김" 건수, 마감 표기 문구(dueLabel), 긴급 여부(urgent), 진행률(완료/전체/퍼센트), 집계(이번 주 완료, 요일별 완료), 다음 일정까지 남은 분, 타임라인 배치용 startMinute와 durationMinutes
- 프론트가 하지 않는 것: 정렬, 필터, 집계, 날짜·시간 계산, 마감 문구 만들기, 긴급 판정
- 프론트가 하는 것: fetch, DOM 렌더링, 이벤트 처리, 레이아웃 환산(분 × 0.9px 같은 단순 곱셈), 시계 표시
- 예외: 시계(HH:mm)는 1초 단위 갱신이 필요해 브라우저 시간으로 그린다. 표시에만 쓰고 판단에는 쓰지 않는다.
- 예외: 입력 검증(공백 제거, 빈 제목 거부, 글자 수 제한)은 사용자에게 빨리 알리기 위해 프론트에서도 한다. 최종 판단은 서버다.
- "N분 후", "N분 남음" 같은 값은 서버가 계산해 내려주고, 프론트가 30~60초마다 목록을 다시 불러와서 갱신한다.
- 비즈니스 규칙(DOING 1개, 승격 기본값 등)은 서버가 강제하고, 프론트는 서버 응답으로 화면을 갱신한다.
- 서버의 Asia/Seoul이 시간 기준이다. 프론트는 브라우저 시간으로 상태를 판단하지 않는다.

## API 규칙
- RESTful 스타일을 지킨다. 경로는 /api/ 로 시작하고, 리소스는 복수형 명사이며 경로에 동사를 쓰지 않는다. (/api/tasks, /api/events)
  - 목록 조회 GET /api/tasks?status=TODO,DOING, 생성 POST /api/tasks(201 + Location 헤더), 수정과 상태 변경 PATCH /api/tasks/{id}(본문에 바꿀 값, 예: {"status": "DOING"}), 삭제 DELETE /api/tasks/{id}(204)
  - ▶ 전환처럼 다른 task의 상태가 함께 바뀌어도 요청은 PATCH /api/tasks/{id} 하나로 보내고, 부수 변경은 서버가 처리한다.
- 화면 전체 데이터는 GET /api/dashboard 한 번으로 조회한다. 이 응답에 상단 바, 오늘 일정, 오늘 할 일, 한 일, 하고 싶은 일, 하단 바 데이터가 모두 들어간다. 쓰기는 /api/tasks, /api/events로 하고, 성공하면 프론트가 대시보드를 다시 불러온다.
- 요청/응답은 JSON이다. 필드 이름은 camelCase, 시각은 ISO-8601 문자열이며 값이 없는 필드는 null로 내려준다(필드를 생략하지 않는다).
- 입력 검증: 제목은 앞뒤 공백을 제거한 뒤 비어 있으면 거부하고 최대 200자, 카테고리는 최대 50자다. 앞뒤 공백 제거는 요청 record 생성자에서 한다. 프론트에서도 같은 규칙으로 한 번 막지만 최종 판단은 서버가 한다.
- 성공은 200/201/204, 입력 오류는 400, 없는 리소스는 404, 규칙 위반(예: 상태 전환 불가)은 409를 쓴다.
- 오류 응답 형식은 { "code": "...", "message": "..." } 로 통일한다. 화면에 그대로 보여줄 수 있는 한국어 메시지를 담는다. 변환은 common 패키지의 @RestControllerAdvice 한 곳에서만 한다. 규모는 작게(검증 실패, 없음, 규칙 위반, 그 외) 유지한다.
- 비즈니스 규칙(DOING 최대 1개, DONE 시 done_at 기록)은 Service 계층에서 트랜잭션으로 보장한다.
- ▶ 전환은 한 트랜잭션에서 기존 DOING을 TODO로 바꾸고 flush한 뒤 새 task를 DOING으로 바꾼다. (DOING 유니크 인덱스는 문장 단위로 검사되므로 순서가 중요하다.)
- updated_at은 DB 트리거 없이 JPA(@UpdateTimestamp 등)로 갱신한다.

## 테스트
- JUnit 5와 Spring Boot Test를 쓴다.
- Repository와 통합 테스트는 실제 운영과 같은 PostgreSQL 환경(Testcontainers)을 우선 사용한다.
- 상태 전환 규칙처럼 비즈니스 규칙이 있는 Service는 단위 테스트를 반드시 작성한다.

## 작업 방식
- Kiro와의 대화, spec 문서(requirements/design/tasks), 코드 주석은 한국어로 쓴다. EARS 키워드(WHEN, THE SYSTEM SHALL 등)는 영어 그대로 둔다.
- 새 라이브러리나 의존성을 추가할 때는 이유를 먼저 설명하고 확인을 받는다. (이미 승인된 것: Lombok, Testcontainers)
- 백업은 pg_dump를 주 1회 실행하는 방식이며, 스크립트는 별도 task로 만든다. 백업은 DB에 테이블을 더 만드는 방식이 아니라 파일로 내보내는 방식이다.
- 개발용 샘플 데이터는 scripts/dev-seed.sql로 만들고 수동으로 실행한다. 앱이 자동으로 넣는 로더는 만들지 않는다.
