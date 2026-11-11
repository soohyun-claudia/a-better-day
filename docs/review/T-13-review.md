# T-13 정적 파일 기반 구성 리뷰

최종 판정: **PASS** — T-13 필수 정적 자산·공통 스타일 검증 완료. P0/P1 없음, P3 개선 사항 1건.

## 1. 시작 조건 및 변경 범위

- 사용자가 Claude의 T-13/T-14 구현 완료를 보고했고 실제 신규 파일을 확인했다. 완료 보고를 근거로 해당 구현 파일의 병행 수정이 끝난 것으로 판단했으며 리뷰 도중 해당 파일은 수정하지 않았다.
- `git status --short`, `git diff HEAD`, `git ls-files --others --exclude-standard`로 범위를 확인했다. diff는 두 tracked 변경 파일 전체를 열어 확인했고 untracked 두 구현 파일도 직접 읽었다. 삭제 파일은 없었다.
- 시작 시 변경: `.kiro/specs/spec-1-task-core/tasks.md`, `docs/review/backlog.md`, 신규 `static/css/common.css`, `static/index.html`.
- tasks.md는 T-13/T-14 체크 변경이었다. backlog.md의 기존 T-20 변경은 이전 작업 결과로 분리·보존했다. 기존 T-18/T-19/T-20 테스트는 수정하지 않았다.

| 파일 | 검토 범위 |
|---|---|
| `src/main/resources/static/css/common.css` | T-13 직접 대상: 변수·기본 스타일·레이아웃·포커스 표시 전체 |
| `src/main/resources/static/vendor/bootstrap-5.3.8/css/bootstrap.min.css` | 기존 tracked 자산 존재·버전 헤더·실제 로드 |
| `src/main/resources/static/vendor/bootstrap-5.3.8/js/bootstrap.bundle.min.js` | 기존 tracked UMD bundle 존재·버전 헤더·실제 로드 |
| `src/main/resources/static/index.html` | T-14 신규 파일. CSS 연결·Bootstrap 로드 순서·스타일 적용 및 기본 HTML 구조 확인에 한정 |
| `.kiro/specs/spec-1-task-core/tasks.md` | T-13 범위 식별용. 수정하지 않음 |

C09는 T-13과 T-14를 묶은 항목이므로 이번 PASS는 C09 전체 완료나 T-14 독립 검증을 뜻하지 않는다.

## 2. 요구사항별 검증

근거: 개발 계획 C09(148~155줄), REQ-4-1, design 4-1 파일 구조·연결, T-13 체크 항목 및 tech/code-conventions/frontend-conventions. REQ-4-6/7의 자리 배치는 T-14에 속하므로 화면 참고 확인만 했다.

| 기준 | 결과 및 근거 |
|---|---|
| 로컬 Bootstrap 5.3.8 | 통과. 두 파일의 5.3.8 헤더 확인, HTTP 200. 기존 파일이며 다운로드·변경 없음 |
| common.css 하나 및 CSS 변수 | 통과. common.css:2~21의 :root에 색·글꼴 fallback·간격·반경 정의. 실제 색값은 변수 정의에 모임 |
| Bootstrap 5.3.8 호환 | 검토 범위에서 문제 없음. 기존 자산으로 렌더링 성공. Bootstrap 6 전용 구문이나 새 UI 라이브러리 없음 |
| 다크 테마·100vh·페이지 스크롤 방지 | 통과. common.css:23~35, index.html:2. 1920×1080 스크린샷에서 주요 영역과 하단 바가 화면 안에 배치됨. 실제 데이터가 채워진 화면은 미검증 |
| 기본 배치·rem/clamp | rem·clamp 사용 확인. Bootstrap flex·spacing도 사용하나 직접 CSS grid 정의에 P3 개선 사항 있음 |
| CDN·외부 의존성 | 통과. HTML의 CSS/script 참조는 모두 로컬 절대 경로. common.css에 @import·url·외부 URL 없음. 새 의존성·React/npm 빌드 없음 |
| 글꼴 | Noto Sans KR/IBM Plex Mono fallback 선언 확인(common.css:17~18). 로컬 폰트·@font-face는 tasks T-13 및 design 4-1에 따라 별도 작업. 전체 글꼴 요구 충족으로 표시하지 않음 |
| 기본 HTML·접근성 | 참고 확인: lang=ko, viewport, header/main/section/footer, 목록 ul, 입력 aria-label, 중복 ID 없음. common.css:193~197의 focus-visible 표시와 :focus-within 확인. 키보드 조작·접근성 트리 검증 및 T-14 전체 시맨틱 판정은 수행하지 않음 |
| 불필요한 기능 | T-13 범위에서 기능 추가 없음. JS·API 호출·데이터 정렬 로직을 추가하지 않음. 버튼 공통 CSS는 정적 스타일이며 상호작용 기능은 구현하지 않음 |

글꼴 관련 문서 상태: steering은 로컬 글꼴을 요구하나 tasks/design은 별도 작업으로 연기했다. C09는 글꼴을 독립 자산 변경으로 분리할 수 있고 연기 시 전체 요구 미완료를 명시하도록 한다. 따라서 T-13 범위에서는 제외하지만 Spec 전체 PASS의 근거로 사용할 수 없다. D05 완료 시점 결정은 별도로 남는다.

## 3. 실행 및 안전한 화면 검증

테스트 파일: `src/test/t13_static_review.py`. Python 표준 라이브러리만 사용했다. HTML 자산 참조·ID, CSS 외부 참조 및 vendor 헤더를 확인한 뒤 127.0.0.1의 임시 정적 서버와 별도 임시 Chrome 프로필로 실행한다. 실행 종료 후 서버를 닫는다.

```powershell
python src/test/t13_static_review.py
```

실행 명령과 범위를 안내하고 권한 승인 후 실행했다. 종료 코드 0. Chrome headless의 `--window-size=1920,1080`, 배율 1로 화면을 저장하고 직접 확인했다. 숨김 스크롤바 옵션으로 촬영했으므로 이미지 자체를 scrollWidth/scrollHeight 측정으로 간주하지 않았다. 페이지 스크롤 차단은 CSS 선언과 실제 배치로 확인했다.

- Bootstrap CSS·bundle, common.css, index.html: HTTP 200.
- 중복 ID 없음, 앱 CSS의 외부 URL·@import 없음.
- `/js/app.js`: HTTP 404. 파일이 아직 없으며 T-15~후속 앱 구현 범위로 분리. 이 화면은 정적 골격만 표시하고 동작 검증은 하지 않았다.
- `/favicon.ico`: 브라우저 자동 요청 404. T-13 요구사항에 favicon이 없어 결함 등록하지 않음.
- 화면과 정적 결과: `docs/review/T-13-screen.png`, `docs/review/T-13-static-result.json`. JSON의 SHA-256은 검토한 로컬 자산 식별값이며 배포본과의 진위 대조를 의미하지 않는다.

DB·Docker·bootRun·Spring은 시작하지 않았으며 프로덕션 파일·T-14 코드·기존 테스트를 수정하거나 커밋하지 않았다.

## 4. 발견 사항

확인된 P0/P1 결함: **0건**. 따라서 OPEN P0/P1 표 항목 없음.

P2: 0건. P3: 1건, **T-13-R01 OPEN**. `common.css:128~133` 및 `:200~204`에서 기본 배치·간격을 직접 CSS grid로 정의한 부분은 Bootstrap 기본 뼈대·유틸리티 우선 원칙 반영이 부족하다. 현재 화면 오류로 재현된 결함은 아니며 유지보수·규칙 준수 개선 사항이다. 구체적 근거 및 권장 조치는 `docs/review/backlog.md`에 기록했다. 기존 백로그 항목과 중복되지 않는다.

## 5. 미검증 및 판정 범위

- T-14 독립 AC, 실제 데이터 렌더링·긴 제목·모바일·다른 브라우저, 상호작용·스크린리더는 미검증이다.
- Spring 정적 리소스 매핑 및 실제 서버 응답은 bootRun 금지에 따라 미검증이다. 이번 로드 검증은 Python 정적 서버 기준이다.
- 로컬 글꼴은 별도 작업으로 남는다. T-13 PASS로 C09 전체·Spec 1 전체 인수를 선언하지 않는다.

실행 결과: 정적 검사 및 Chrome 1920×1080 렌더링 성공, exit 0.
최종 판정: PASS — T-13 필수 범위 확인 완료, P0/P1 결함 없음.
개선 및 한계: P3 1건 백로그 기록, T-14·동적 화면·로컬 폰트 전체 요구는 별도 검증.
