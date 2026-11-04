---
inclusion: always
---

# 프로젝트 구조

```
(project-root)/
├── docker-compose.yml          # 로컬 PostgreSQL
├── .env                        # DB 접속 정보 (git 제외)
├── docs/                       # 프로토타입 캡처 이미지, 개요 문서
├── scripts/                    # 백업 스크립트 등
├── .kiro/
│   ├── steering/               # 이 파일들
│   └── specs/                  # spec 문서 (requirements/design/tasks)
└── src/
    ├── main/
    │   ├── java/<base-package>/
    │   │   ├── task/           # task 도메인: controller, service, repository, domain, dto
    │   │   ├── event/          # event 도메인: 같은 구성
    │   │   └── common/         # 예외 처리, 공통 설정, 시간 유틸
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/   # Flyway: V1__..., V2__...
    │       └── static/
    │           ├── index.html
    │           ├── css/style.css
    │           ├── js/         # api.js(fetch), render.js(그리기), app.js(진입점)
    │           └── vendor/     # Bootstrap 5 로컬 파일 (CDN 사용 안 함)
    └── test/java/<base-package>/
```

## 구성 규칙
- 패키지는 계층별이 아니라 도메인별(task, event)로 나누고, 그 안에서 controller / service / repository / domain / dto로 구분한다.
- controller는 요청/응답 변환만 하고, 비즈니스 규칙은 service에 둔다.
- 도메인 간 직접 호출은 피한다. 필요하면 service에서 조합한다.
- 화면은 index.html 한 장이다. 다른 페이지를 만들지 않는다.
- Bootstrap은 vendor/ 아래 로컬 파일로 포함한다. 상시 대시보드라 CDN 장애나 오프라인 상태에 영향받지 않게 한다.
- 파일명과 패키지명은 소문자, 클래스는 PascalCase, DB 객체는 snake_case를 쓴다.
