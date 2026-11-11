# T-18 TaskService 단위 테스트 및 검증

- 판정: **PASS**
- 범위: 기존 Phase 3 백엔드의 C04·V04에 해당하는 생성·상태 전환 단위 검증. 신규 구현 리뷰 시작 조건은 사용자의 명시적 독립 테스트 지시로 대체했다.
- 변경 파일: `src/test/java/com/abetterday/task/service/TaskServiceTest.java` 및 이 보고서.
- 프로덕션 코드, 의존성, tasks.md는 수정하지 않았으며 커밋하지 않았다. T-20/T-21은 시작하지 않았다.

## 1. 근거 및 범위 보존

개발 계획 C04(108~115줄)·V04(213줄), requirements의 REQ-2-2/4, REQ-3-1/4/5/6, REQ-7-1 및 design의 3-2 도메인 메서드, 3-4 서비스 로직, 6-1 테스트 설계를 확인했다. 지정된 steering 세 문서를 적용했다.

시작 시 존재한 DueLabeler 수정, DueLabelerTest, T-19-review.md, backlog.md는 이번 작업에서 수정하지 않았다. 작업 중 design.md와 tasks.md 변경도 확인했으며 T-19 병행 작업으로 제외하고 보존했다. 검토 기준으로 필요한 TaskService 설계 섹션만 읽었다.

## 2. 테스트 케이스

모든 사례는 `TaskServiceTest.java`에 있다. Repository·DueLabeler는 Mockito 목이며 Task는 실제 엔티티다. Clock은 서울 시간대의 고정 Clock이다.

| 메서드 및 위치 | 실행 수 | 검증 내용 |
|---|---:|---|
| createDefaults:56 | 2 | WISH/TODO, MEDIUM 기본값, 제목 strip, 빈 category→null, 마감·doneAt null, 삭제 기본값, 주요 응답 필드 |
| createExplicitFields:83 | 2 | 제목 200자 보존, 명시 우선순위·category, 날짜 단독/날짜+자정 시각, 과거 마감 허용 |
| rejectCreateStatus:104 | 3 | DOING/DONE/null 생성의 INVALID_CREATE_STATUS·400, 저장 및 응답 계산 없음 |
| rejectTimeWithoutDate:116 | 2 | WISH/TODO의 날짜 없는 자정 시각 거부, DUE_TIME_WITHOUT_DATE·400, 저장 없음 |
| transitionMatrix:134 | 16 | 4개 허용 전환 및 12개 금지 전환, 409 코드, 완료 시각 정확한 값, 승격 필드 초기화, 나머지 필드 보존, 불필요한 조회·저장 없음 |
| flushOldDoingBeforeStartingTarget:176 | 1 | 기존 DOING→TODO 저장·flush 시점에 대상은 여전히 TODO, 이후 대상만 DOING, 호출 순서, 기존 필드 보존 |
| missingTask:211 | 4 | 모든 요청 상태에서 없는 ID의 TASK_NOT_FOUND·404, 부수 조회·저장 없음 |
| promotionBoundary:226 | 4 | 서울 일요일 시작·종료 양쪽, 당일 일요일/다음 일요일 계산, priority·dueTime 초기화 |
| stopOnPreviousFlushFailure:243 | 1 | 기존 DOING flush 실패 시 예외 전파, 대상 TODO 유지, 대상 저장·응답 계산 없음 |
| clearStaleDoneAt:262 | 3 | markTodo/markDoing/promote의 방어적 doneAt 초기화 |
| writeTransactionDeclarations:280 | 1 | 클래스 읽기 전용 및 생성·전환 메서드 쓰기 트랜잭션 선언 |
| 합계 | 39 | 실패 0, 오류 0, 건너뜀 0 |

응답 ID·생성/갱신 시각은 목 저장 결과에 채워 매핑만 검증했다. Hibernate의 자동 필드 생성 자체를 검증한 것은 아니다. DueLabeler 반환값의 응답 전달만 확인하고 마감 문구 계산은 T-19에 남겼다.

## 3. 실행 명령과 결과

실행 전 대상 클래스와 명령을 사용자에게 안내했다. 기존 컨텍스트 테스트와 HTTP/DB 통합 테스트는 실행하지 않았다. DB, Docker, bootRun 및 개발 DB 연결은 사용하지 않았다.

```powershell
$env:JAVA_HOME='C:/Users/Kwon Soohyun/.jdks/corretto-25.0.5'
$env:GRADLE_USER_HOME='C:/Users/Kwon Soohyun/.gradle'
.\gradlew.bat test --tests com.abetterday.task.service.TaskServiceTest --offline --no-daemon
```

첫 샌드박스 시도는 기본 Gradle 캐시의 잠금 디렉터리를 만들 수 없어 테스트 진입 전에 중단됐다. 권한 승인 후 기존 사용자 캐시와 Java 25로 실행했다. 최초 35건 통과 후 flush 실패 및 도메인 초기화 4건을 추가하고 위 명령으로 재실행했다. 최종 `BUILD SUCCESSFUL`(26초).

결과 근거: `build/test-results/test/TEST-com.abetterday.task.service.TaskServiceTest.xml`, `build/reports/tests/test/index.html`. 새 의존성은 추가하지 않았다.

## 4. 결함 및 검증 한계

확인된 P0/P1 결함 0건, P2/P3 개선 사항 0건. 새 결함 ID나 OPEN 항목은 없으며 기존 `docs/review/backlog.md`는 변경하지 않았다.

주요 확인 위치: `TaskService.java:37` 생성 검증·기본값, `:77` 전환 및 기존 DOING flush, `:101` 전환표, `:121` 응답 매핑; `Task.java:75` 완료 기록, `:81`·`:87` 완료 시각 초기화, `:93` 승격; `CreateTaskRequest.java:28` 제목·카테고리 정규화.

TaskService와 Task의 검토 대상 Java 메서드에는 요약 주석이 있다. 비공개 Task 생성자에는 요약 주석이 없으나 생성자는 메서드와 구분했으며 결함으로 등록하지 않았다. 요청 DTO의 빈 제목·길이 검증은 Bean Validation 책임이다. 설계의 “제목 검증” 표현을 서비스 직접 호출 시 별도 거부 의무로 확대하지 않았으며 C04의 명시적 C08 분리를 따랐다.

| 구분 | 결과 |
|---|---|
| 생성·전환·예외 코드·순차 DOING 교체·필드·고정 시간 | 통과 |
| 쓰기 트랜잭션 선언 및 로그의 제목 미포함 | 테스트 및 정적 확인 통과(TaskService:53,96,109) |
| 실제 트랜잭션 rollback·경쟁 요청·DB partial unique index | 미검증: 이번 작업의 DB 실행 금지 범위, Mockito로 보장하지 않음 |
| HTTP Bean Validation·JSON 바인딩·응답 상태 | 대상 제외: C08 |
| JPA 자동 ID/감사 시각·실제 SQL flush | 미검증: DB 통합 검증 범위 |

flush 실패 테스트는 대상 변경 중단만 보장한다. 목 객체에 반영된 기존 DOING의 메모리 변경 복원을 주장하지 않는다. 단일 DOING 검증은 정상적인 단일 기존 DOING을 전제로 한 순차 서비스 호출 범위이며 경쟁 요청의 DB 보장은 별도 검증이 필요하다.

테스트 결과: 최종 39건 통과, 실패·오류·건너뜀 0건.
최종 판정: PASS — T-18 범위의 필수 단위 검증 완료, P0/P1 결함 없음.
한계: 실제 DB 제약·경쟁·rollback 및 C08 HTTP 검증은 수행하지 않았다.
