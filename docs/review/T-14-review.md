# T-14 기본 레이아웃 독립 리뷰

최종 판정: **PASS**. 핵심 HTML 골격과 기준 뷰포트 배치 및 수정된 필수 안내 문구를 재검증했다. P0/P1 없음. T-14-R01 P2는 RESOLVED, T-14-R02 P3는 OPEN이다.

## 1. 검토 파일과 변경사항

사용자가 Claude의 구현 완료를 보고했고 실제 파일을 확인했다. 완료 보고를 근거로 해당 구현 수정이 종료된 것으로 판단했다. `git status --short`, `git diff HEAD`, `git ls-files --others --exclude-standard`로 변경 목록을 확인했다. HEAD 대비 tracked 변경은 tasks.md 체크 및 기존 backlog였으며, 신규 index.html·common.css는 직접 읽었다. 삭제 파일은 없었다.

| 파일 | 검토 범위 |
|---|---|
| `src/main/resources/static/index.html` | T-14 신규 HTML 전체: 상단·3단·하단, 자산 연결, 구조·이름·접근성 |
| `src/main/resources/static/css/common.css` | 공유 CSS의 HTML 적용과 화면 영향만 확인. T-13 자체 품질 판정은 반복하지 않음 |
| 로컬 Bootstrap CSS·bundle | HTML 연결 및 실행 시 로드만 확인. T-13의 자산 검증 유지 |
| `docs/review/T-13-review.md` | PASS 결론, 별도 글꼴 작업, T-13-R01 P3 확인. 수정하지 않음 |

T-13 이후 구현 변경을 식별할 별도 diff는 없으므로 사용자 지시와 tasks.md에 명시된 T-14 HTML 골격을 범위로 삼았다. CSS 전체를 T-14 신규 구현으로 간주하지 않았다. T-20 테스트·문서 변경은 리뷰 대상에서 제외하고 보존했다.

근거 문서는 개발 계획 C09, tasks T-14, REQ-4-1 및 4-2의 상단 골격, REQ-4-6/7, design 4-1/4-2, frontend-conventions다. 데이터 표시·동작까지 T-14 범위로 확대하지 않았다.

## 2. 요구사항별 결과

| 기준 | 결과·위치 |
|---|---|
| HTML 한 장, 다크 테마 | 통과. index.html:1~10의 doctype·lang=ko·dark·charset·viewport |
| 로컬 자산 및 로드 순서 | 통과. :7~8의 Bootstrap→common.css, :95~96의 일반 bundle→앱 module 순서. 실제 bundle 버전 5.3.8 |
| 상단 골격 | 통과. :12~34에 시계·날짜·요일·집중할 일·진행률 영역 존재. 실제 시계·데이터 갱신은 후속 작업 |
| 3단 배치·필수 식별자 | 통과. :36~77에 sectionSchedule/sectionTodo 및 wrap_right_panels 아래 sectionDone/sectionWish. id 중복 없음 |
| 하단 자리 | 위치·골격 및 “Spec 4에서 구현 예정” 안내 통과(:79~93) |
| 일정 자리 안내 | “Spec 3에서 구현 예정” 표시 통과(:40) |
| 100vh·넘침·스크롤 | 통과. 실제 1920×1080 뷰포트에서 document/body 모두 1920×1080, overflow hidden, 모든 측정 영역 화면 내부 |
| CSS·Bootstrap 사용 | common.css 하나만 연결, 인라인 style 0. HTML에서 flex/spacing/display 유틸리티 사용. 직접 grid 정의는 기존 T-13-R01로 유지하며 중복 등록하지 않음 |
| HTML/CSS 네이밍 | id camelCase 및 custom class underscore·prefix 확인. Bootstrap 고유 class는 허용. 앱 함수 추가 없음 |
| 로컬 글꼴 | 미구현·별도 작업. tasks T-13/design에서 연기한 로컬 글꼴을 T-14 신규 결함으로 등록하지 않음. 전체 글꼴 요구 충족을 주장하지 않음 |
| 접근성·가독성 | header/main/section/footer, 목록 ul·제목 h2, 입력 aria-label·maxlength 존재. 입력 focus 및 2px outline 측정 통과. 진행률 의미 정보는 P3 개선(T-14-R02) |
| 불필요한 기능 | API·모달·편집·집계 계산 기능 추가 없음. 하단의 구체적 집계 이름·숫자 자리는 안내만 요구한 REQ-4-7과 차이가 있어 T-14-R01에 포함 |

T-13 공유 스타일과 연결된 정적 골격에서 화면을 깨뜨리는 영향은 확인되지 않았다. 이는 동적 목록이 채워진 화면의 무회귀까지 보장하지 않는다.

## 3. P0/P1 및 P2/P3

확인된 P0/P1 결함은 **0건**이며 OPEN P0/P1 항목은 없다.

| ID | 심각도 | 위치 | 재현·근거 | 기대 동작 | 상태 |
|---|---|---|---|---|---|
| T-14-R01 | P2 | index.html:40, :88 | 최초 리뷰에서 REQ-4-6/7 안내가 누락되었으나 수정 후 두 문구가 각 자리에서 확인됨 | 각 자리에 지정된 구현 예정 안내 표시 | RESOLVED |
| T-14-R02 | P3 | index.html:30 | 진행률 막대가 일반 div이며 역할·이름·값 속성이 없음. HTML 정적 검사로 확인 | 보조기술용 진행률 의미·값 제공 | OPEN |

T-14-R01 P2는 `docs/review/backlog.md`에서 RESOLVED로 갱신했다. T-14-R02 P3와 기존 T-13-R01은 OPEN으로 유지했으며 판정을 변경하지 않았다.

## 4. 실제 화면 검증

작성한 `src/test/t14_layout_review.py`는 Python 표준 라이브러리만 사용한다. 127.0.0.1 임시 정적 서버에서 원본을 제공하되 HTML 응답에만 JS 측정 함수를 삽입한다. 프로덕션 파일은 변경하지 않는다. Chrome은 임시 프로필의 headless 모드로 실행하며 종료 후 서버를 닫는다.

```powershell
python src/test/t14_layout_review.py
```

명령과 범위를 안내하고 실행 권한 승인 후 수행했다. 첫 실행은 `--window-size=1920,1080`에도 실제 콘텐츠 뷰포트가 1898×982여서 **테스트 도구의 뷰포트 단언 실패**로 종료했다. 제품 레이아웃 실패가 아니다. 창 크기를 1942×1178로 보정한 재실행은 실제 JS 측정값 1920×1080으로 성공(exit 0)했다.

최종 DOM 측정 결과:

- viewport/document/body: 모두 1920×1080, body overflow=hidden.
- topBar: x16, y16, 너비1888, 높이103.
- mainContent: x16, y135, 너비1888, 높이약783.41.
- bottomBar: x16, y약934.41, 너비1888, 높이약129.59. 하단 끝1064로 뷰포트 내부.
- sectionSchedule/Todo/Done/Wish와 입력창: 전부 화면 내부. 중복 ID 없음, 인라인 스타일 없음.
- bundle runtime `bootstrap.Modal.VERSION`: 5.3.8.
- 입력창 focus 성공, accessible name 원천인 aria-label 존재, computed outline-width=2px.

`docs/review/T-14-layout-result.json`에 수치를 저장했다. `docs/review/T-14-screen.png`도 직접 확인했다. Chrome screenshot 캡처는 창 크기인 1942×1178 캔버스로 저장되므로 **기준 해상도 적합성 근거는 스크린샷 크기가 아니라 위 DOM 측정값**이다. 참고 이미지에서 3단·상하단 배치와 텍스트 표시를 확인했다. JSON의 missingBootstrapClasses 빈 목록은 예약 필드이며 클래스 전수 검사 결과로 사용하지 않았다.

index.html·common.css·Bootstrap CSS/bundle은 HTTP 200. `/js/app.js`와 favicon은 404다. app.js는 후속 앱 구현으로 남아 있어 module 연결 선언만 확인했으며 실제 실행은 미검증이다. 이 점을 완성된 화면 동작으로 보고하지 않는다.

## 5. 미검증 및 제한 준수

- 화면 데이터·시계 갱신·할 일 추가·긴 제목·실제 7/4개 목록·자동 갱신은 후속 작업이며 검증하지 않았다.
- 키보드 Tab 순회, 스크린리더·접근성 트리·대비 수치, 다른 브라우저·모바일 크기는 미검증이다. 이번 입력 검증은 프로그램 focus와 계산 스타일 수준이다.
- Spring의 실제 정적 서빙 및 DB/API 연동은 미검증이다. DB·Docker·bootRun을 실행하지 않았다.
- 프로덕션·T-13 구현과 보고서·기존 T-18/T-19/T-20 테스트·tasks.md는 수정하지 않았다. Git 커밋 및 T-21 착수 없음.
- T-14 PASS는 C09 전체나 Spec 1 인수를 의미하지 않는다. 별도 글꼴 작업과 D05/D06 정책 상태는 남아 있다.

실행 결과: 수정 후 보정된 정적 화면 측정 성공(exit 0), 실제 뷰포트 1920×1080에서 넘침 없음. 두 안내 문구가 DOM에 표시됨.
최종 판정: PASS — 필수 안내 문구와 기존 레이아웃 재검증 완료, P0/P1 없음.
백로그: T-14-R01 P2 RESOLVED, T-14-R02 P3 OPEN, 기존 T-13 판정과 변경사항 보존.
