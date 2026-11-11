# 리뷰 개선 사항 백로그

| ID | 작업 | 심각도 | 파일:줄 | 문제 | 재현/근거 | 기대 동작 및 수정 권장 | 상태 |
|---|---|---|---|---|---|---|---|
| T-19-R02 | T-19 | 이전 P2, 오등록 철회 | `src/main/java/com/abetterday/task/service/DueLabeler.java:53-54` | 요구사항 해석 오류로 “오늘 마감”을 기대한 테스트 문제였으며 프로덕션 결함이 아님 | REQ-4-3 및 design의 실제 문구는 “오늘 중”. 오늘 분기가 일요일보다 먼저 적용됨. 기대값 정정 후 일요일 자정·정오·23:59:59 테스트 3개 모두 통과 | 테스트 기대값을 “오늘 중”, urgent=false로 정정함. 프로덕션 문구 변경 권고 철회. 상세 근거는 `docs/review/T-19-review.md` 2·4·5절 참조 | RESOLVED |

| T-20-R01 | T-20 | P2 잠재 위험 | `src/main/java/com/abetterday/task/service/TaskService.java:67`, `src/main/java/com/abetterday/dashboard/service/DashboardService.java:63` | 자정 걸침 요청에서 완료 조회 구간과 상단 today가 다른 날짜를 사용할 수 있음. 정책 위반 여부는 D04 미확정 | `DashboardServiceTest.observeCrossMidnightRequest:218`에서 Clock을 서울 11/10 23:59:59 → 11/11 00:00:00으로 반환시키면 완료 조회 구간은 11/10, today는 11/11로 재현됨 | D04에서 요청 기준 시점·날짜 공유 여부를 먼저 결정하고 필요 시 Clock 기준값 전달 방식 보완. 현재 동작을 허용할 경우 근거 명시. 이번 테스트 통과는 정책 승인 아님 | OPEN |

| T-13-R01 | T-13 | P3 | `src/main/resources/static/css/common.css:128`, `:200` | 기본 3단·하단 배치를 직접 CSS grid와 gap으로 정의하여 Bootstrap 기본 배치 유틸리티 우선 원칙 반영이 부족함 | `#mainContent`의 display/grid-template-columns/gap 및 `#bottomBar`의 동일 속성을 직접 선언. tech·frontend-conventions는 기본 뼈대와 간격에 Bootstrap grid/flex/spacing 우선 사용 명시 | Bootstrap 배치·간격 유틸리티로 옮길 수 있는 부분을 우선 사용하고 고유 열 비율 등만 CSS로 유지. T-14 HTML과 함께 후속 조정하되 이번 리뷰에서 수정하지 않음 | OPEN |

| T-14-R01 | T-14 | P2 | `src/main/resources/static/index.html:40`, `:88` | 일정·하단 자리의 지정된 구현 예정 안내 누락 | REQ-4-6/7의 문구가 누락되었으나, 수정 후 일정에 “Spec 3에서 구현 예정”, 하단에 “Spec 4에서 구현 예정”이 표시됨 | 지정된 안내를 각 자리에 표시하고 아직 구현되지 않은 집계로 오해하지 않도록 정리 | RESOLVED |
| T-14-R02 | T-14 | P3 | `src/main/resources/static/index.html:30` | 진행률 막대가 일반 div여서 보조기술에 진행률 역할·이름·범위·값을 제공하지 않음 | box_progress_bar/progressFill에 role 및 aria 속성 없음. 별도 텍스트 0/0은 있으나 진행률 위젯으로 연결되지 않음 | Bootstrap 5.3.8 progress 패턴에 맞춰 역할·aria-labelledby·min/max/now를 제공하고 향후 렌더링 시 값도 함께 갱신 | OPEN |

T-19 오등록 정정 이력은 보존한다. T-20-R01은 재현된 현재 동작에 대한 정책 미확정 위험이며 확정 결함으로 판단하지 않았다.
