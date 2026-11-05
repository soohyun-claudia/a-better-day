---
inclusion: always
---

# 코드 컨벤션

## 공통
- 들여쓰기는 Java 4칸, HTML·CSS·JS 2칸이다. 줄 끝 공백은 두지 않고, 파일은 개행으로 끝낸다. (.editorconfig로 고정한다.)
- 한 줄은 120자 이내로 한다.
- 주석과 로그 메시지는 한국어로 쓴다. 코드(클래스, 메서드, 변수명)는 영어로 쓴다.
- 사용하지 않는 코드, 주석 처리된 코드, import는 남기지 않는다.

## 주석
- 모든 메서드(Java)와 함수(JS)에 한 줄 요약 주석을 단다. 무엇을 하는 메서드인지 한 줄로 쓴다.
  - Java는 Javadoc(`/** ... */`), JS는 JSDoc 형식의 한 줄 주석(`/** ... */`)을 쓴다.
  - 추가로 꼭 알아야 할 내용(규칙, 예외, 트랜잭션, 부수 효과, 파라미터 단위 등)이 있으면 요약 아래에 한두 줄만 덧붙인다. 없으면 쓰지 않는다.
  - `@param`, `@return`은 이름만으로 의미가 분명하지 않을 때만 쓴다.
  - Lombok이 만드는 getter와 record의 자동 접근자에는 쓰지 않는다.
- 메서드 안의 주석은 "무엇"이 아니라 "왜"를 적는다. 코드로 알 수 있는 내용은 반복하지 않는다.

예시:
```java
/** 지정한 task를 하는 중(DOING)으로 바꾼다. */
@Transactional
public TaskResponse start(Long id) { ... }

/**
 * 마감 표기 문구를 만든다.
 * 우선순위: 지남 → 오늘 중 → 내일 → 이번 주 → M/D 마감
 */
private String toDueLabel(Task task) { ... }
```
```js
/** 오늘 할 일 목록을 화면에 그린다. */
function renderTodayTasks(tasks) { ... }
```

## Java 구조
- 패키지는 도메인별로 나누고 그 안에서 controller, service, repository, domain, dto로 나눈다. (structure.md 참고)
- 호출 방향은 controller → service → repository 한 방향이다. controller에는 비즈니스 로직을 두지 않는다.
- controller는 REST API로 만들고 RESTful 규칙(tech.md)을 따른다. 경로에 동사를 쓰지 않는다.
- Entity는 API로 노출하지 않는다. 요청/응답은 record DTO를 쓰고, 변환은 DTO나 전용 매퍼에서 한다.
- 의존성은 생성자 주입만 쓴다. (필드 주입 금지) Lombok의 @RequiredArgsConstructor를 쓴다.
- 어노테이션은 꼭 필요한 것만 쓴다. DTO에는 특히 최소로 쓴다. 허용 범위는 tech.md의 JPA 규칙을 따른다.
- 트랜잭션은 service 계층에서 건다. service 클래스에 @Transactional(readOnly = true)를 기본으로 붙이고, 쓰기 메서드에만 @Transactional을 다시 붙인다.
- 날짜·시간 계산은 Clock을 주입받아서 한다. LocalDate.now() 같은 직접 호출은 쓰지 않는다. (테스트에서 시각을 고정하기 위해)
- null 대신 Optional 반환이나 빈 컬렉션을 쓴다. 컬렉션을 null로 반환하지 않는다.
- 매직 넘버와 문자열(최대 표시 7개, 4개, 긴급 기준 2시간, 임박 기준 15분)은 상수나 설정으로 분리한다.

## 네이밍 (Java, API)
- 클래스: PascalCase (TaskService, TaskResponse). 요청은 XxxRequest, 응답은 XxxResponse로 끝낸다.
- 메서드와 변수: camelCase. boolean은 is, has로 시작한다. (isUrgent)
- 상수: UPPER_SNAKE_CASE. enum 값도 같다. (TODO, DOING)
- 테스트 메서드: 한국어 @DisplayName을 쓰고, 메서드명은 영어 camelCase로 한다.
- REST 경로: 소문자 kebab-case, 복수형 명사. 경로에 동사를 쓰지 않는다.
- DB 이름은 db-conventions를 따른다.

## 예외 처리
- 비즈니스 규칙 위반은 전용 예외(예: BusinessException + 에러 코드 enum)로 던진다.
- 예외 변환은 @RestControllerAdvice 한 곳에서만 한다. 응답 형식은 tech.md의 오류 응답 규칙을 따른다.
- 예외를 잡고 아무것도 하지 않는 코드(빈 catch)는 쓰지 않는다. 로그를 남기거나 다시 던진다.
- 사용자에게 보이는 메시지에 스택 트레이스나 내부 정보를 담지 않는다.

## 로깅
- SLF4J를 쓴다. System.out.println은 쓰지 않는다.
- 로그 레벨: 상태 변경은 info, 예상된 규칙 위반은 warn, 예상 못한 오류는 error. 사용자 입력 내용 전체를 로그에 남기지 않는다.

## 테스트
- 테스트 구조는 given / when / then 주석으로 구분한다.
- 테스트 하나는 하나의 동작만 검증한다.
- 시각에 의존하는 로직(마감 표기, 긴급 판정, 이번 주 계산)은 고정한 Clock으로 테스트한다. 경계값(자정, 일요일, 정확히 2시간, 15분)을 반드시 포함한다.

## JavaScript (바닐라 JS, ES 모듈)
- const를 기본으로 쓰고, 재할당이 필요할 때만 let을 쓴다. var는 쓰지 않는다.
- 변수와 함수는 camelCase, 상수는 UPPER_SNAKE_CASE, 파일명은 소문자 한 단어(api.js, render.js, modal.js, app.js)로 한다.
- 비교는 === 만 쓴다. 문자열 연결보다 템플릿 리터럴을 쓴다.
- 한 함수는 한 가지 일만 한다. 화면을 그리는 함수는 값을 받아 DOM을 만들 뿐, 계산하지 않는다. (역할 분리 규칙은 tech.md 참고)
- 요소 선택은 id(camelCase)로 하고, 같은 요소가 여러 개인 목록 행의 이벤트 위임에는 data-action, data-id 속성을 쓴다. 스타일용 클래스를 JS 선택자로 쓰지 않는다.
- 전역 변수를 만들지 않는다. 모듈 스코프 안에서 상태를 관리한다.
- fetch 호출은 api.js에만 둔다. 다른 파일에서 fetch를 직접 쓰지 않는다.

## HTML / CSS
- id, class 이름은 frontend-conventions의 네이밍 규칙(id는 camelCase, class는 underscore)을 따른다.
- 시맨틱 태그(header, main, section, button)를 쓰고, 클릭 가능한 요소는 button으로 만든다. (div에 클릭 이벤트를 붙이지 않는다.)
- 색과 크기는 CSS 변수와 Bootstrap 유틸리티를 쓴다. 인라인 style 속성은 JS가 동적으로 계산한 값(타임라인 top, height)에만 허용한다.

## Git
- 커밋 메시지는 "타입: 한국어 요약" 형식이다. 타입은 feat, fix, refactor, docs, test, chore를 쓴다.
- 한 커밋은 하나의 변경 목적만 담는다.
