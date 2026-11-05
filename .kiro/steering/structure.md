---
inclusion: always
---

# 프로젝트 구조

프로젝트 이름은 "A Better Day"다. 이름은 위치마다 아래 표기를 쓴다.

| 쓰는 곳 | 표기 |
|---|---|
| 저장소, 폴더, 빌드 프로젝트명 | a-better-day |
| Java 베이스 패키지 | com.abetterday |
| Application 클래스 | ABetterDayApplication |
| DB 이름, 볼륨 이름 | a_better_day |
| 화면에 보이는 이름 | A Better Day |

```
a-better-day/
├── docker-compose.yml          # 로컬 PostgreSQL
├── .env                        # DB 접속 정보 (git 제외)
├── .editorconfig               # 들여쓰기 등 포맷 규칙
├── backups/                    # pg_dump 결과 (git 제외)
├── docs/                       # 프로토타입 캡처 이미지(docs/prototype/), 개요 문서
├── scripts/                    # 백업 스크립트, 개발용 샘플 데이터(dev-seed.sql)
├── .kiro/
│   ├── steering/               # 이 파일들
│   └── specs/                  # spec 문서 (requirements/design/tasks)
└── src/
    ├── main/
    │   ├── java/com/abetterday/
    │   │   ├── ABetterDayApplication.java
    │   │   ├── task/           # task 도메인: controller, service, repository, domain, dto
    │   │   ├── event/          # event 도메인: 같은 구성
    │   │   ├── dashboard/      # 화면 전체 조회(GET /api/dashboard): controller, service, dto
    │   │   └── common/         # 예외 처리, 공통 설정(Clock 등), 변환 클래스
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/   # Flyway: V1__..., V2__...
    │       └── static/
    │           ├── index.html
    │           ├── css/common.css
    │           ├── js/         # api.js(fetch), render.js(그리기), modal.js(Bootstrap 모달), app.js(진입점)
    │           ├── images/     # 아이콘 등 이미지 (임시 이미지는 images/temp/)
    │           └── vendor/     # Bootstrap 5.3.8, 글꼴 로컬 파일 (CDN 사용 안 함)
    └── test/java/com/abetterday/
```

## 구성 규칙
- 패키지는 계층별이 아니라 도메인별(task, event, dashboard)로 나누고, 그 안에서 controller / service / repository / domain / dto로 구분한다. (dashboard는 조회 전용이라 repository, domain 없이 task, event의 service를 조합한다.)
- 패키지 이름은 java로 시작하지 않는다. (java.* 패키지는 JVM이 금지한다.)
- controller는 요청/응답 변환만 하고, 비즈니스 규칙은 service에 둔다.
- 도메인 간 직접 호출은 피한다. 필요하면 service에서 조합한다.
- 화면은 index.html 한 장이다. 다른 페이지를 만들지 않는다. (Spring Boot가 /를 index.html로 연결하므로 파일명을 바꾸지 않는다.)
- CSS는 소규모 프로젝트 규칙에 따라 common.css 하나만 둔다.
- Bootstrap은 vendor/ 아래 로컬 파일로 포함한다. 상시 대시보드라 CDN 장애나 오프라인 상태에 영향받지 않게 한다.
- 파일명과 패키지명은 소문자, 클래스는 PascalCase, DB 객체는 snake_case를 쓴다. HTML의 id와 class 이름은 frontend-conventions의 네이밍 규칙을 따른다.
