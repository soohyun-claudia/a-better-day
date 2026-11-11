# T-20 DashboardService 정렬·집계 단위 테스트

최종 판정: **검증 미완료**. 확정 계약 검증은 통과했으나 C06·V06의 D04 정책 적합성 판정은 보류한다.

## 1. 범위 및 근거

기존 Phase 3 백엔드의 독립 테스트 작업으로 수행했다. 신규 구현 완료 리뷰 조건은 이번 사용자 지시로 대체했다. C06(개발 계획 124~130줄)·V06(215줄), REQ-3-3, REQ-4-0/2~5, REQ-7-4, design의 DashboardService 로직 및 6-4 테스트 설계를 확인했다. 기존 대화에서 읽은 tech·code-conventions·frontend-conventions를 적용했다.

DashboardService, DashboardResponse, 직접 의존하는 TaskService·TaskResponse·TaskPriority·TaskRepository를 확인했다. 프로덕션 코드 및 .kiro 문서는 수정하지 않았다. T-18·T-19 결과를 변경하지 않았다. 시작 시 Git 작업 트리는 깨끗했으며, 실행 중 나타난 tasks.md와 static/css·index.html 변경은 외부 병행 변경으로 보고 읽거나 수정하지 않았다. T-21을 시작하지 않았고 커밋도 하지 않았다.

작성 파일: `src/test/java/com/abetterday/dashboard/service/DashboardServiceTest.java`.

## 2. 테스트 케이스 목록

| 메서드:줄 | 건수 | 검증 내용 | 구분 |
|---|---:|---|---|
| sortStatusAndPriority:58 | 1 | DOING 우선, HIGH/MEDIUM/LOW, doingTask 선택, dueLabel·urgent 전달 | 계약 검증 |
| sortDueAndCreatedAt:78 | 1 | 날짜 → 시각 → 생성 시각, 마감 없음 후순위 | 계약 검증 |
| stableTie:94 | 1 | 동일 정렬 키의 TODO/WISH 입력 순서 유지 | 현재 tie 동작 검증 |
| panelBoundaries:112 | 5 | 0/4/5/7/8개, TODO 7·WISH 4·DONE 4 제한, 전체·숨김·완료 수, WISH 생성순, doing 없음, 조회 횟수 | 계약 검증 |
| progress:143 | 4 | 분모 0, 0/50/100%, 표시 제한 전 전체수 사용, WISH 제외 | 계약 검증 |
| seoulDate:160 | 3 | 서울 자정 전후·연도 경계의 today 및 한국어 dateLabel, doing 없음 | 계약 검증 |
| observeUntimedDuePolicy:175 | 1 | 같은 날짜에서 시각 있는 항목 → 날짜만 있는 항목 | D04 관찰 |
| observeFractionPolicy:189 | 3 | 현재 1/3→33%, 2/3→67%, 1/8→13% | D04 관찰 |
| fixedRequestConsistency:200 | 2 | 실제 TaskService와 목 Repository, 고정 시각의 자정 전후 완료 조회 구간과 today 일치 | 계약 검증 |
| observeCrossMidnightRequest:218 | 1 | 실제 TaskService와 목 Repository, 자정 걸침의 완료 조회 날짜와 today 불일치 재현 | D04 관찰 |
| 합계 | 22 | 실패 0·오류 0·건너뜀 0 | D04 관찰 5건 포함 |

한 일은 Repository의 `ORDER BY doneAt DESC` → TaskService → DashboardService 순으로 전달되는 계약이다. 이미 정렬된 목 결과의 순서 유지와 제한을 확인했으며 실제 SQL 정렬 실행은 확인하지 않았다. 완전 동률의 입력 순서 유지는 안정 정렬의 현재 동작이다. DB 조회 순서가 고정되거나 ID가 최종 키여야 한다는 요구사항은 없으므로 이를 요구하지 않았다.

## 3. 실행 명령 및 결과

실행 전 대상 클래스에 SpringBootTest·DataJpaTest·SpringExtension·Testcontainers·DataSource·DriverManager가 없음을 확인하고 사용자에게 명령과 범위를 안내했다. 테스트는 JUnit·Mockito로 직접 생성한다. 일반 테스트는 TaskService 목, 날짜 일관성 테스트만 실제 TaskService와 Repository·DueLabeler 목을 사용한다. DB·Docker·bootRun은 실행하지 않았다.

```powershell
$env:JAVA_HOME='C:/Users/Kwon Soohyun/.jdks/corretto-25.0.5'
$env:GRADLE_USER_HOME='C:/Users/Kwon Soohyun/.gradle'
.\gradlew.bat test --tests com.abetterday.dashboard.service.DashboardServiceTest --offline --no-daemon
```

권한 승인 후 기존 캐시로 실행했다. `BUILD SUCCESSFUL in 22s`. 새 의존성은 추가하지 않았다. 기존 테스트 소스도 Gradle 컴파일 대상이지만 실행은 위 클래스만 선택했다.

결과 파일: `build/test-results/test/TEST-com.abetterday.dashboard.service.DashboardServiceTest.xml`, `build/reports/tests/test/index.html`.

## 4. 발견 사항 및 코드 위치

확정된 P0/P1 결함은 없으며 OPEN P0/P1 항목도 없다. P2 잠재 위험 1건(T-20-R01)을 `docs/review/backlog.md`에 기록했다. P3는 0건이다.

- 정렬: `DashboardService.java:36~41`. 상태 → priority → dueDate → dueTime(null 뒤) → createdAt. 테스트에서 상위 키가 하위 키보다 우선함을 확인했다.
- 제한 및 집계: `DashboardService.java:47~58`, `:71~85`. 정렬·제한 전 전체 목록으로 집계한다. TaskService 호출은 상태별 2회와 완료 조회 1회이며 불필요한 전체 조회가 추가되지 않는다.
- 서울 날짜: `DashboardService.java:63`, `TaskService.java:67~69`. 각각 Clock을 읽는다. 고정 Clock에서는 같은 날짜이며, 자정 걸침에서는 다른 날짜를 사용할 수 있다.
- 주석: 검토한 DashboardService의 모든 메서드에 한국어 한 줄 요약 주석이 있다. DTO는 record 자동 접근자만 사용한다.

T-20-R01 재현: 목록을 빈 값으로 주는 목 Repository와 Clock 목을 실제 TaskService·DashboardService에 공유한다. Clock.instant()가 `2026-11-10T14:59:59Z`, 다음 호출에서 `2026-11-10T15:00:00Z`를 반환하게 한다. 완료 조회는 `[11/10 00:00+09:00, 11/11 00:00+09:00)`인데 응답 today는 `2026-11-11`이다. 해당 관찰을 기대값으로 둔 테스트의 통과는 현재 동작 재현만 의미한다.

## 5. D04 결정 필요 사항

| 항목 | 문서 및 현재 동작 | 필요한 결정 |
|---|---|---|
| 같은 날짜의 시각 없는 마감 | 요구사항은 마감 빠른 순, 설계는 날짜→시각 및 마감 없음 후순위. 구현은 dueTime null을 해당 날짜의 모든 시각 뒤에 둔다 | 날짜만 있는 마감을 하루 끝으로 볼지 등 명시. 현재 순서를 제품 정책으로 승인한 것은 아님 |
| 소수 퍼센트 | design 3-4에는 반올림한 정수라고 적혔으나 개발 계획 D04·C06과 사용자 지시는 미확정으로 관리. 구현은 Math.round | 문서 간 상태를 정리하고 반올림/버림 등 확정. 관찰 테스트는 승인 전 회귀 기준으로 간주하지 않음 |
| 자정을 걸치는 요청 | 단일 요청 시간 일관성은 V06 대상이나 기준 시점은 D04 미확정. 현재 서로 다른 날짜 사용 가능 | 요청 시작 시각 등 기준과 완료 조회·상단 날짜·마감 문구 간 공유 범위 결정. 별도 요구 없이 임의로 고치지 않음 |

## 6. 통과·실패·미검증

- 통과: 명확한 정렬 계층, 표시 제한 경계, 전체수·숨김·완료 수, 정수 진행률, 빈 목록, doing 유무, 고정 서울 날짜 및 완료 조회 구간, 현재 tie 동작.
- 실패: 실행 테스트 없음. 확정 요구사항 위반으로 판정한 결함 없음.
- 미검증: D04 세 정책의 최종 적합성. 실제 DB 정렬·트랜잭션·동시성, HTTP·화면 렌더링은 이번 단위 테스트 범위 밖이다.

테스트 결과: 22건 통과, 실패·오류·건너뜀 0건. D04 관찰 5건은 정책 승인과 구분한다.
최종 판정: 검증 미완료 — 확정 계약은 통과했으나 D04 적합성은 정책 결정 후 재검증해야 한다.
발견 사항: P0/P1 없음, P2 잠재 위험 1건(T-20-R01 OPEN), 프로덕션 수정·DB 실행·커밋 없음.
