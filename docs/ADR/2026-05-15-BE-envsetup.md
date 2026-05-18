---
adr_id: "2026-05-15-BE-envsetup"
status: "Accepted"
created_at: "2026-05-15"
author: "@dapin1490"
related_files:
  - "BE/amr_control_system/build.gradle"
  - "BE/amr_control_system/src/main/resources/application.yaml"
  - "BE/docker-compose.yml"
  - "BE/.env.example"
  - "docs/API 정의.md"
  - "docs/프로젝트 정의서.md"
---

## 배경과 문제 (Context)
### 배경
- DB 영역(MySQL 스키마, Node-RED DAS)과 BE를 병렬로 진행하기로 했으나, Spring Initializr 생성 시 포함할 스타터 의존성이 구체화되지 않았다.
- `docs/API 정의.md`에는 JWT 인증, REST 목록 API, WebSocket 실시간 스트림이 이미 정의되어 있어, 최소 구현 범위가 명확하다.
- 팀원 PC에 Java 설치 여부가 달라, 이후 Docker 기반 실행 환경도 함께 고려해야 했다.

### 해결할 문제
- DB 물리 연동 전에 백엔드를 시작할 때, Spring Boot 프로젝트에 어떤 스타터 의존성을 포함·제외할 것인가?

## 최종 결정 (Decision)

- 영역: `Backend`, `Infra/Deploy`
- 최종 선택
  - **1단계(프로젝트 생성)**: DB 드라이버·마이그레이션 없이 API·인증·실시간 채널 중심 스타터로 Boot 프로젝트를 생성한다.
  - **2단계(병렬 개발 중)**: 물리 MySQL 연동 전까지 **H2 인메모리 + JPA**를 임시 저장소로 사용한다. (`runtimeOnly h2`, `spring-boot-starter-data-jpa`)
  - **3단계(로컬 실행)**: `BE/docker-compose.yml`로 Java 미설치 환경에서도 동일하게 BE를 기동한다. JWT 등 민감 값은 `BE/.env`로 주입한다.
- 버전/규칙
  - Java 17, Spring Boot 3.5.x
  - API 컨텍스트 경로: `/api/v1`
  - JWT: `jjwt` 0.12.x, `JWT_SECRET`은 Base64 디코딩 32바이트 이상
- 선택 이유
  - 설계 문서에 정의된 REST·JWT·WebSocket 요구를 초기부터 충족할 수 있다.
  - DB 팀 스키마 확정을 기다리지 않고 Controller·Service·DTO 개발을 진행할 수 있다.
  - H2는 스키마 실험·데모 시드에 비용이 낮고, 이후 `DB/init.sql` 기반 MySQL로 전환 경로가 분리된다.

### 포함 스타터 (Initializr)

| 의존성 | 용도 |
| --- | --- |
| Spring Web | REST API (`/api/v1/*`) |
| Spring Security | JWT 인증·인가 |
| WebSocket | 실시간 스트림 (`/api/v1/stream`, 구현 예정) |
| Validation | 로그인·명령·조회 파라미터 검증 |
| Spring Boot Actuator | 헬스체크 (`/actuator/health`) |
| Lombok | DTO·엔티티 보일러플레이트 축소 |
| Spring Boot DevTools | 로컬 개발 시 자동 재시작 |
| Spring Boot Starter Test | 단위·통합 테스트 기본 |

### 당장 제외·후속 추가

| 의존성 | 처리 |
| --- | --- |
| MySQL Driver | 물리 DB 연동 시 추가 |
| Flyway / Liquibase | `DB/init.sql` 정합 후 도입 검토 |
| MyBatis | JPA 우선, SQL 매퍼는 필요 시 별도 ADR |
| Springdoc OpenAPI | API 안정화 후 추가 검토 |

## 대안과 트레이드오프

| 옵션 | 장점 | 단점 | 선택 여부 |
| --- | --- | --- | --- |
| A. API 전용 최소 스택 (Web, Security, WebSocket, Validation, Actuator, Lombok, DevTools) | DB 팀과 완전 분리, Mock/정적 데이터로 빠른 API 골격 | 영속 계층 없어 통합 테스트·목록 API 검증이 제한됨 | 부분 채택 (생성 시 기준) |
| B. 초기부터 JPA + MySQL | 최종 스택과 동일, 마이그레이션 일원화 | DB 스키마·시드 미완 시 BE 착수 지연 | 미선택 |
| C. A + H2/JPA 임시 연동 + Docker 단독 실행 | API·Repository·시드까지 병렬 검증 가능, 로컬 환경 표준화 | H2와 물리 스키마(`DB/init.sql`) 불일치·이중 작업 위험 | **채택** |
| D. Node/Express 등 대체 백엔드 | 경량 프로토타입 | 설계서·팀 스택(Spring Boot)과 불일치 | 미선택 |

## 기대 효과와 리스크 (Consequences)
### 기대 효과
- DB 영역 완료 전에 인증·대시보드·AMR API 골격을 FE와 연동해 검증할 수 있다.
- Actuator 헬스로 Docker·CI에서 기동 여부를 단순 확인할 수 있다.
- 의존성 범위가 문서화되어 이후 MySQL 전환 시 추가·제거 항목이 명확하다.

### 리스크 및 대응
- 리스크: H2 자동 DDL과 `DB/init.sql` 물리 스키마·테이블명이 어긋날 수 있다.
- 대응: MySQL 연동을 1순위 백로그로 두고, 엔티티·Repository를 `DB/init.sql` 기준으로 정합한다(`BE/TODO.md`).
- 리스크: WebSocket·MySQL 미연동 상태에서 FE 실시간 요구와 gap 발생.
- 대응: REST 우선 완성 후 WebSocket·통합 compose는 별도 ADR/이슈로 분리한다.
- 리스크: `JWT_SECRET` 미설정 시 기동 실패.
- 대응: `BE/.env.example`와 온보딩 문서에 생성·설정 절차를 명시한다.

## 미해결 이슈
- [ ] JPA 엔티티를 `DB/init.sql` 물리 스키마에 맞게 정합하고 MySQL 드라이버로 전환
- [ ] WebSocket `/api/v1/stream` 구현 및 FE 연동
- [ ] 프로젝트 루트 통합 `docker-compose`(FE·DB·BE 일괄 기동)
- [ ] Springdoc OpenAPI 도입 여부 결정

## 변경 이력
| 버전 | 날짜 | 변경 내용 | 작성자 |
| --- | --- | --- | --- |
| v0.0 | 2026-05-15 | 백엔드 초기 의존성·환경 구성 결정 | @jaehyeonlim99 |
| v1.0 | 2026-05-18 | ADR 양식 반영, H2 임시 연동·Docker 실행 범위 명시 | @dapin1490 |
