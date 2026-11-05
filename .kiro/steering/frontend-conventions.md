---
inclusion: always
---

# 프론트엔드 규칙 (HTML, Bootstrap 5, 바닐라 JS)

## 네이밍 규칙 (id, class, 파일)
Bootstrap이 제공하는 클래스(d-flex, gap-2, btn 등)는 그대로 쓰고, 우리가 직접 만드는 id와 class에 아래 규칙을 적용한다.

**공통**
- 이름의 첫 글자에 숫자, 특수문자, 대문자를 쓰지 않는다. (list_notice2 O, 2list_notice X)
- class는 '형태_의미_상태' 순서로 조합하고, 3단계를 넘지 않는다. (btn_task_check, box_news, box_reply_open)
- prefix, suffix를 최대한 활용한다. 단독 사용 class는 피하고 prefix와 조합한다. (예외: fst, lst, hide, on)
- prefix가 2개 이상 겹치면 기능을 나타내는 prefix를 맨 앞에 둔다.
- 서브 prefix는 underscore로 구분한다. (line / line_dot)

**id와 class**
- id는 camelCase, class는 underscore(snake_case)다. (id="boardView", class="link_view")
- id는 화면에서의 고유 기능을 드러내게 짓는다. (btnSearch X, btnGnbSearch O) 문서 안에서 한 번만 쓴다.
- class는 요소의 기능을 드러내게 짓는다. id와 class 이름은 가급적 같게 짓지 않는다.
- JS가 요소를 고를 때는 id를 쓰고, 목록 행의 이벤트 위임에는 data-action, data-id 속성을 쓴다. (data 속성 이름은 HTML 규칙대로 소문자 kebab-case)

**prefix (형태)**
| 분류 | prefix | 설명 |
|---|---|---|
| 타이틀 | tit | 일반적인 타이틀 |
| 영역 | section | 제목 태그를 가진 영역 (중첩 지양) |
| 묶음 | wrap | 일반 영역의 묶음 (중첩 지양) |
| 안쪽 묶음 | inner | 부모 wrap 안에서 자식 묶음이 단독으로 필요할 때 |
| 목록 | list | 일반 목록 (ul, ol, 리스트 형식의 dl) |
| 폼 | tf | text 입력 (input text, textarea) |
| 폼 | inp | radio, checkbox 등 |
| 폼 | opt | selectbox |
| 폼 | lab | label |
| 폼 | fld | fieldset |
| 버튼 | btn | 버튼 |
| 박스 | box | 박스 |
| 아이콘 | ico | 아이콘 |
| 선 | line_방향 | 실선 (점선은 line_dot_방향) |
| 배경 | bg | 배경 |
| 텍스트 | txt | 일반 텍스트 |
| 숫자 | num | num1, num2 (숫자 앞에 underscore를 쓰지 않는다) |
| 시간 | time | 날짜와 시각 |
| 강조 | emph | 강조 |
| 링크 | link | 일반 링크 (더 보기는 link_more) |
| 순서 | fst, mid, lst | 첫째, 중간, 마지막 |
| 팝업 | popup | 팝업 |
| 레이어 | layer | 레이어 (모달 안쪽 내용 영역 등) |
| 상세 | desc | 상세 내용 |
- 위 목록에 없는 형태(타임라인 카드 등)는 가장 가까운 prefix(box 등)에 의미를 붙여 짓는다.
- 사용하지 않는 분류(gnb, lnb, snb, tab, tbl, paging, bnr, widget, cmt 등 이 프로젝트에 없는 요소)는 만들지 않는다.

**suffix (상태)**
- 상태: _on / _off / _over / _hit / _focus, 위치: _top / _mid / _bot / _left / _right, 순서: _fst / _lst, 이전/다음: _prev / _next
- 이 프로젝트의 의미 상태(진행 중, 긴급, 지난 일정, 현재 일정)는 위 suffix에 없으므로 의미가 전달되는 단어를 쓴다. (_doing, _urgent, _past, _now) on 클래스로 단일 요소의 상태를 바꿀 때는 suffix로 쓴다.
- 예: list_task(목록), box_task_doing(하는 중인 행), btn_task_check(체크 버튼), txt_task_title(제목), time_clock(시계), box_event_urgent(임박한 일정 카드), layer_task_edit(할 일 수정 모달 내용)

**파일과 폴더 (소규모 프로젝트 규칙)**
- HTML은 index.html 한 장, CSS는 common.css 하나만 둔다.
- 이미지는 static/images/ 에 모두 두고, 임시 이미지만 images/temp/ 폴더를 만든다. 이미지 수정은 git으로 관리하므로 '기존이름_수정날짜' 파일을 따로 만들지 않는다.
- 숨김 텍스트는 Bootstrap의 visually-hidden을 쓴다. 원본 규칙의 예약어(daumIndex 등)와 ir_pm, screen_out 같은 클래스는 쓰지 않는다.

## 레이아웃
- 전체 높이는 100vh로 고정하고 페이지 스크롤은 막는다. 상단 바, 3단, 하단 바는 Bootstrap의 d-flex, flex-column, gap 유틸리티로 구성한다.
- 기준 해상도는 1920×1080이다. 글자 크기와 간격은 px 고정이 아니라 rem이나 clamp()로 잡아서, 해상도가 달라져도 한 화면에 들어오게 한다.
- 일정 타임라인은 Bootstrap으로 부족하므로 직접 CSS를 쓴다. 일정 카드는 position: absolute로 두고, top과 height는 서버가 내려준 startMinute(09:00 기준 분)와 durationMinutes에 1분당 0.9px을 곱해 JS로 설정한다. 시각 계산이나 정렬은 프론트에서 하지 않는다. (tech.md의 역할 분리 규칙) 프로토타입 기준으로 표시 범위는 09:00~19:00(높이 540px), 1분당 0.9px이다. 일정 카드는 최소 24px로 하고, 높이가 48px 미만이면 "시각 제목"을 한 줄로 표시한다.

## 테마와 색
- data-bs-theme="dark"를 쓰고, 색은 common.css의 :root CSS 변수로 정의한다. 컴포넌트에 색 코드를 직접 쓰지 않는다. CSS 변수 이름은 CSS 표준대로 kebab-case를 쓴다. (예: --color-bg)
- 프로토타입에서 쓴 값:
  - 배경 #0B0D11, 카드 #141821, 카드 테두리 #262D3D
  - 본문 글자 #EEF0F4, 보조 글자 #98A0B0
  - 파랑(하는 중, 포커스) #62AEFF, 초록(완료) #6FD39A
  - 주황(긴급) #FF9A52
  - 빨강 #FF6B6B (삭제와 입력 오류에만 사용)
  - 카드 안쪽 배경 #1B2030, 구분선 #222836, 입력창 테두리 #2F384B, 버튼 테두리 #3A4358
- 글꼴은 본문 Noto Sans KR, 시각·숫자는 IBM Plex Mono이다. 글꼴 파일은 로컬에 포함한다.
- 색의 의미(주황 긴급 / 파랑 하는 중 / 초록 완료)는 다른 용도로 쓰지 않는다.

## JavaScript
- 앱 코드는 ES 모듈(<script type="module">)이고, 프레임워크와 번들러는 쓰지 않는다. Bootstrap은 bootstrap.bundle.min.js(UMD)를 일반 <script>로 먼저 불러오고 window.bootstrap으로 접근한다. Bootstrap JS API 호출은 modal.js 한 파일에서만 한다.
- 서버 통신은 fetch로 하며 api.js에 모은다. 그리는 로직은 render.js, 모달 열고 닫기는 modal.js, 이벤트 연결과 갱신 주기는 app.js에 둔다.
- 화면 데이터는 GET /api/dashboard 한 번으로 받아서 그린다. 저장, 삭제, 체크, ▶ 같은 변경이 성공하면 대시보드를 다시 불러와 전체를 다시 그린다.
- 입력 검증(앞뒤 공백 제거, 빈 제목 거부, 제목 200자, 카테고리 50자)은 프론트에서도 한 번 한다. 규칙은 서버와 같지만, 최종 판단은 서버다.
- 모든 함수에 한 줄 요약 주석을 단다. (code-conventions 참고)
- 사용자가 입력한 텍스트를 innerHTML에 넣을 때는 반드시 이스케이프하거나 textContent를 쓴다.
- Enter 입력 처리 시 한글 IME 조합 중인지 확인한다. (event.isComposing이 true이거나 keyCode가 229이면 무시) 그렇지 않으면 한 번 입력에 두 번 추가된다.
- 30~60초마다 setInterval로 갱신한다. 대상은 시계, 현재 시각선, 남은 시간, 목록 데이터다.
- 자동 갱신으로 다시 그릴 때 입력창의 포커스와 입력 중인 내용을 지우지 않는다.
- 자정을 넘기면 "오늘" 기준으로 목록을 다시 불러온다.
- API 호출이 실패해도 화면은 마지막으로 성공한 데이터를 유지하고, 작은 오류 표시만 보여준다. 상시 화면이 빈 화면이 되면 안 된다.
- 체크, ▶(하는 중으로 변경), 승격 같은 반복 동작 버튼은 이벤트 위임으로 처리한다.
- 아이콘 버튼에는 aria-label을 붙인다. (예: "{제목} 완료", "{제목} 시작", "{제목} 이번 주 할 일로 올리기") 키보드 포커스 윤곽선(focus-visible)을 유지한다.
- 체크 버튼은 호버하면 초록 테두리와 연한 초록 배경이 되고 체크 아이콘이 나타난다.
- 하는 중 표시의 파란 점은 2초 주기로 깜빡이게(pulse) 하되, prefers-reduced-motion이면 애니메이션을 끈다.
- 시간 길이 표기는 "N분", "N시간 M분"으로 통일한다. (예: 40분, 1시간 40분) 이 문구는 서버가 만들어 내려주고 프론트는 그대로 표시한다.
- 서버가 내려준 정렬 순서, 개수 제한, 문구, urgent 값을 프론트에서 다시 계산하거나 바꾸지 않는다.

## 편집 모달
- 편집 모달은 Bootstrap 5.3.8의 Modal 컴포넌트를 쓰고, 다크 테마와 프로토타입의 색·글자 크기를 따른다.
- 모달이 열려 있는 동안에는 자동 갱신이 모달의 입력 내용과 포커스를 바꾸지 않는다. 뒤쪽 목록만 갱신한다.
- 모달이 열릴 때 첫 입력창에 포커스를 둔다. Esc로 닫고, Enter로 저장한다. (한글 IME 조합 중 Enter는 무시)
- 삭제는 모달 안에서 확인 단계를 한 번 더 거친다. 별도의 브라우저 confirm 창은 쓰지 않는다.
- 저장하거나 삭제하면 서버 응답을 받은 뒤 대시보드를 다시 불러와 화면을 다시 그린다.
