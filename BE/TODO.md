# TODO.md - 백엔드 개발 계획

## 프로젝트 개요
AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## 현재 상태
- 기본 Spring Boot 프로젝트 구조 생성됨 (AmrControlSystemApplication.java, 테스트 파일).
- 설계 문서 확인 완료: 프로젝트 정의서, API 정의, 데이터 스키마 설계, 협업 컨벤션.
- ERD 및 API 명세 기반 API·서비스 로직 구현 진행 중.
- **BE 전용 Docker 실행 환경 구성 완료** (2026-05-17). PC에 Java를 설치하지 않아도 Docker만으로 백엔드 서버를 빌드·실행할 수 있다.
- **DB 물리 스키마 merge 완료** (`DB/init.sql`, 18개 테이블·시드 데이터). 백엔드 JPA 엔티티는 아직 H2 자동 생성·구(舊) 테이블명 기준이며, **물리 스키마와 불일치**한다.
- 데이터 저장은 당분간 **임시 DB(H2)** 를 사용 중이다. **다음 1순위**는 물리 스키마에 맞춘 엔티티·MySQL 연동이다.

---

## 완료 작업 기록 (팀 공유용)

### [완료] BE 전용 Docker 실행 환경 구성 (2026-05-17)

**목적**  
개발 PC에 Java·Gradle 등을 따로 설치하지 않아도, Docker만으로 백엔드 서버를 동일한 방식으로 실행·확인할 수 있게 한다. 프론트엔드·DB·백엔드는 각자 Docker 환경을 먼저 만들고, 이후 프로젝트 루트에서 하나로 통합하기로 한 합의에 따라, **백엔드 영역만 단독으로** 구성했다.

**산출물 (주요 파일)**  
| 구분 | 위치 | 설명 |
|------|------|------|
| 컨테이너 빌드 정의 | `amr_control_system/Dockerfile` | 소스 코드를 패키징해 실행 가능한 서버 이미지를 만든다 |
| 실행 오케스트레이션 | `BE/docker-compose.yml` | 백엔드 서버 컨테이너 1개를 띄운다 (포트 8080) |
| Docker 전용 설정 | `application-docker.yaml` | 컨테이너 안에서 쓰는 DB·보안 토큰·상태 확인 설정 |
| 환경 변수 예시 | `BE/.env.example` | 보안 토큰 등 민감 설정의 입력 예시 (실제 값은 `.env`에 두며 Git에는 올리지 않음) |

**범위에 포함된 것**  
- 백엔드 서버 단독 기동 (http://localhost:8080)  
- 서버 정상 동작 확인용 상태 URL (`/actuator/health` → `{"status":"UP"}` 확인 완료)  
- 개발용 임시 DB(H2, 메모리). 서버를 끄면 데이터는 사라진다.

**범위에 포함되지 않은 것 (추후 작업)**  
- 물리 MySQL 스키마(`DB/init.sql`)와 백엔드 JPA 정합  
- 프론트엔드·DB와 한 번에 띄우는 **프로젝트 루트 통합 docker-compose**  
- WebSocket 실시간 스트리밍 (별도 TODO)

**실행 방법 (개발자·검증 담당)**  
`BE` 폴더에서: `docker compose up --build`  
상태 확인: http://localhost:8080/api/v1/actuator/health

---

## 다음 예정 작업 (팀 공유용)

| 순서 | 작업 | 한 줄 설명 | 영향 |
|------|------|-----------|------|
| **1** | **물리 DB 스키마 정합** | `DB/init.sql`에 맞게 엔티티·MySQL·서비스 수정 | H2 자동 DDL 대신 실제 DB·시드 데이터 사용 |
| 2 | 공통 오류 응답 정리 | API 실패 시 형식을 통일 | 프론트·운영이 오류를 일관되게 처리 |
| 3 | WebSocket | 위치·알람 등 실시간 푸시 | 대시보드 실시간 갱신 |
| 4 | 테스트·검증 | 단위·통합 테스트, Docker 스모크 | 품질·회귀 방지 |
| (병렬) | 루트 통합 Docker | FE·DB·BE를 한 명령으로 기동 | 통합 데모·QA 환경 |
| (후순) | 추가 기능 | CSV/Excel보내기, 캐싱 등 | 필요 시 |

**현재 진행 예정 1순위:** 섹션 10 **물리 DB 스키마 정합** (DB merge 반영, 2026-05-18).

---

## 개발 계획 (작은 단위로 순차 진행)

### 0. Docker 실행 환경 (BE 단독) — 완료 (2026-05-17)
- [x] `amr_control_system/Dockerfile` (multi-stage, Java 17).
- [x] `amr_control_system/.dockerignore`.
- [x] `BE/docker-compose.yml` (서비스 `be`, 포트 8080).
- [x] `application-docker.yaml` (H2, JWT 환경 변수, Actuator health).
- [x] `BE/.env.example` (JWT_SECRET 등).
- [x] Docker로 `docker compose up --build` 기동 및 `/actuator/health` 응답 확인.
- [ ] 물리 스키마 정합 후: MySQL 서비스·드라이버·datasource profile 연동 (섹션 10).
- [ ] FE·DB·BE Docker 완료 후: 프로젝트 루트 통합 `docker-compose` 작성 (별도 작업).

### 1. 데이터베이스 설정 및 엔티티 구현 (H2·구 스키마 기준 — 정합 전)
- [x] application.yaml에 H2 인메모리 DB 설정 추가 (MySQL 대신 개발용).
- [x] build.gradle에 JPA 및 H2 의존성 추가.
- [x] JPA 엔티티 클래스 생성 (초기 ERD·임시 스키마 기반).
  - [x] Site.java, Area.java, EnvSensor.java, EnvReading.java, Product.java
  - [x] ProcessMaster.java, Routing.java, WorkOrder.java, WipLot.java
  - [x] AmrChargeStation.java, Amr.java, AmrChargingSession.java
  - [x] AmrTask.java, AmrStatusLog.java
  - [x] Alarm.java (임시 `ALARM`, `docs/임시-스키마-변경-알람.md`)
  - [x] User.java (임시 `USERS`, 인메모리 refresh)
- [x] JPA Repository 인터페이스 생성 (각 엔티티별).

> **주의:** 위 엔티티는 `DB/init.sql` 물리 스키마와 테이블명·컬럼명·PK 타입이 다르다. 섹션 10에서 일괄 정합한다.

### 2. DTO 클래스 구현
- [x] 요청/응답 DTO 생성 (API 정의.md 기반).
  - [x] 인증, 대시보드, AMR, 충전, 알람, 작업 이력, 분석 DTO.

### 3. 보안 및 인증 구현
- [x] Spring Security 설정 클래스 생성 (JwtAuthenticationFilter, SecurityConfig).
- [x] JWT 유틸리티 클래스 생성 (토큰 생성/검증).
- [x] 사용자 엔티티 및 Repository 추가 (기본 사용자 관리).
- [ ] 물리 스키마 정합: `USER_ACCOUNT`·`REFRESH_TOKEN` 테이블 연동 (섹션 10).

### 4. 컨트롤러 구현
- [x] AuthController, DashboardController, AmrController, ChargingController.
- [x] AlarmController, WorkHistoryController, AnalyticsController.

### 5. 서비스 로직 구현
- [x] AuthenticationService, DashboardService, AmrService, ChargingService.
- [x] AlarmService, WorkHistoryService, AnalyticsService.
- [ ] 물리 스키마 정합 후 Repository·매핑 로직 수정 (섹션 10).

### 6. WebSocket 구현
- [ ] WebSocket 설정 클래스 생성.
- [ ] 이벤트 발행 서비스 구현 (실시간 데이터 스트리밍).

### 7. 예외 처리 및 로깅
- [x] 전역 예외 처리 클래스 (@RestControllerAdvice) — 인증 관련 최소 구현.
- [x] 커스텀 예외 클래스 정의 (InvalidRefreshTokenException 등, 도메인별 확장 예정).
- [ ] 공통 오류 응답 형식 통일 (API 정의·프론트 연동 기준).

### 8. 테스트 및 검증
- [ ] 단위 테스트 작성 (Service, Repository).
- [ ] 통합 테스트 작성 (Controller).
- [ ] 빌드 및 실행 검증 (Docker: `docker compose build`, `docker compose up`; 테스트는 `docker compose run --rm be` 등).

### 9. 추가 기능
- [ ] 파일보내기 기능 (Excel/CSV 다운로드).
- [ ] 캐싱 또는 최적화 (필요 시).

### 10. 물리 DB 스키마 정합 (DB merge 반영) — **다음 1순위**

`DB/init.sql` 및 `docs/데이터 스키마 설계.md`를 기준으로, 백엔드 전역을 실제 MySQL 물리 스키마에 맞춘다. H2 `ddl-auto: create-drop`으로 구 스키마를 자동 생성하는 방식은 중단한다.

#### 10-1. 엔티티·테이블 매핑 수정

| 현재 (BE) | 물리 DB (`init.sql`) | 주요 변경 |
|-----------|----------------------|-----------|
| `AMR` | `AMR_MASTER` | 테이블명, PK 수동 할당(INT, AUTO 없음) |
| `ENV_READING` | `ENV_SENSOR_LOG` | `reading_id`→`sensor_log_id`, `sensor_id`→`env_sensor_id`, `value_num`→`sensor_value` |
| `PROCESS_MASTER` | `PR_PROCESS` | `process_id`(INT AI)→`pr_process_id`(VARCHAR PK) |
| `ROUTING` | `PR_ROUTING` | `routing_id`→`pr_routing_id`, `process_id`→`pr_process_id`, `sequence`→`seq_no` |
| `AMR_CHARGING_SESSION` | `AMR_CHARGING_LOG` | 테이블명 |
| `ALARM` | `ALARM_LOG` | `ack_by`/`ack_note` 없음, `ack_at`→`acknowledged_at` |
| `USERS` | `USER_ACCOUNT` | `user_id`(VARCHAR PK), `password_hash`, `created_at` |
| (없음) | `REFRESH_TOKEN` | JPA 엔티티·Repository 신규 |
| (없음) | `AMR_COMMAND` | AMR 제어 명령 영속화 (현재 비저장) |

추가 컬럼·FK 정합:

- [ ] `WorkOrder`: `wo_id`→`work_id`, `wo_no` 제거(물리 스키마에 없음).
- [ ] `WipLot`: `wo_id`→`work_id`, `current_routing_id`→`pr_routing_id`, `lot_no` 제거, PK `BIGINT`.
- [ ] `EnvSensor`: `sensor_id`(INT AI)→`env_sensor_id`(VARCHAR PK).
- [ ] `AmrStatusLog`: `amr_statlog_id` `BIGINT`, `SOH_pct` 컬럼명 매핑, `load_weight` 타입.
- [ ] `Alarm`→`AlarmLog`(또는 동등 명명): API의 ack 필드와 DB 컬럼 차이 처리 방침 확정.

#### 10-2. 인프라·설정

- [ ] `build.gradle`: MySQL Connector/J 의존성 추가.
- [ ] `application-docker.yaml`(또는 `application-mysql.yaml`): MySQL datasource, `ddl-auto: validate`(또는 `none`), dialect 변경.
- [ ] `BE/docker-compose.yml`: MySQL 서비스 추가, `DB/init.sql` 마운트·초기화, BE가 MySQL 기동 후 연결.
- [ ] `.env.example`: DB 호스트·포트·계정 변수 추가.
- [ ] H2 프로필은 로컬 단독 테스트용으로만 유지할지, 완전 제거할지 팀 합의 후 반영.

#### 10-3. Repository·Service·시드 데이터

- [ ] 영향 Repository 메서드·JPQL·Specification 수정 (테이블·컬럼 rename 반영).
- [ ] `AlarmService`, `AmrService`, `ChargingService`, `WorkHistoryService`, `DashboardService`, `AnalyticsService` 매핑 수정.
- [ ] `DemoUserDataLoader`, `AlarmDemoDataLoader`, `DashboardDemoDataLoader`: MySQL 시드(`init.sql`) 사용 시 비활성화 또는 조건부 실행.
- [ ] `InMemoryRefreshTokenStore` → `REFRESH_TOKEN` DB 저장으로 전환(또는 프로필별 분기).
- [ ] `docs/임시-스키마-변경-알람.md` 의존 제거·문서 정리.

#### 10-4. 검증

- [ ] Docker: MySQL + BE 기동, `init.sql` 시드 데이터로 주요 API 스모크 (login, dashboard, amrs, alarms, charging, work-histories).
- [ ] API 응답 필드가 `docs/API 정의.md`와 일치하는지 확인 (DB 컬럼 축소로 인한 DTO 조정 포함).

---

## 작업 우선순위

1. ~~Docker로 BE 기동 가능한 환경 확보 (섹션 0).~~ **완료**
2. ~~인증 API 완성~~ **완료**
3. ~~Dashboard·Amr·Charging·Alarm·WorkHistory·Analytics API~~ **완료**
4. **다음: 물리 DB 스키마 정합** (섹션 10) — 엔티티·MySQL·서비스·Docker
5. 공통 오류 응답 정리 (섹션 7)
6. WebSocket 실시간 스트리밍 (섹션 6, `/api/v1/stream`)
7. 테스트 및 검증 (섹션 8)
8. 프로젝트 루트 통합 docker-compose (섹션 0 후속)
9. 추가 기능 (섹션 9)

## Docker 실행 (BE 폴더에서)
```bash
cp .env.example .env
# .env 에 JWT_SECRET 설정 (아래 「다른 PC에서 JWT 키 갱신」 참고)
docker compose up --build
```
`JWT_SECRET` 등 민감 값은 `.env`에만 두며, `.env`는 Git에 커밋하지 않는다.
- API: http://localhost:8080
- Health: http://localhost:8080/api/v1/actuator/health
- Auth login: POST http://localhost:8080/api/v1/auth/login
- Dashboard summary: GET http://localhost:8080/api/v1/dashboard/summary (Bearer 토큰)
- AMR list: GET http://localhost:8080/api/v1/amrs (Bearer 토큰)
- Charging stations: GET http://localhost:8080/api/v1/charging/stations (Bearer 토큰)
- Alarms list: GET http://localhost:8080/api/v1/alarms (Bearer 토큰)
- Work histories: GET http://localhost:8080/api/v1/work-histories (Bearer 토큰)
- Analytics KPIs: GET http://localhost:8080/api/v1/analytics/kpis (Bearer 토큰)

> 물리 스키마 정합(섹션 10) 완료 후에는 MySQL 컨테이너를 함께 띄우는 방식으로 실행 방법이 갱신된다.

## 다른 PC에서 JWT 키 갱신 (팀 공유)

`.env`는 Git에 포함되지 않으므로, **저장소를 clone/pull한 각 PC마다** 로컬 `BE/.env`를 직접 만들거나 갱신해야 한다.  
과거 커밋에 JWT 시크릿이 노출된 적이 있다면, **예전 값은 폐기**하고 아래 절차로 **새 키를 발급**한다 (PC마다 동일한 키를 쓸 필요는 없고, 로컬·개발용이면 PC별로 달라도 된다).

### 1) 최초 설정 (`.env`가 없을 때)

```bash
cd BE
cp .env.example .env
```

### 2) 새 JWT_SECRET 발급

**Linux / macOS / Git Bash**
```bash
openssl rand -base64 32
```

**Windows PowerShell**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

출력된 문자열을 `BE/.env`의 `JWT_SECRET`에 넣는다.

### 3) `.env` 예시

값에 `+`, `/`, `=`가 포함되면 **반드시 따옴표**로 감싼다.

```env
JWT_SECRET="<위에서 생성한 Base64 문자열>"
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
DEMO_USER_PASSWORD=demo123
```

- `DEMO_USER_PASSWORD`: API 명세 개발용 계정(`admin` / `demo123`). 로그인 테스트에 필요하면 설정한다. 비우면 시드 사용자는 생성되지 않는다.

### 4) 기동 및 확인

```bash
cd BE
docker compose up --build
```

브라우저 또는 curl로 확인:
- Health: http://localhost:8080/api/v1/actuator/health → `{"status":"UP"}`
- Login: `POST http://localhost:8080/api/v1/auth/login` (body: `{"username":"admin","password":"demo123"}`)

### 5) 이미 `.env`가 있을 때 (키만 교체)

1. `BE/.env`를 연다.
2. `JWT_SECRET` 값만 새로 생성한 문자열로 **교체**한다 (예전에 Git에 올라갔을 수 있는 값은 사용하지 않는다).
3. `docker compose down` 후 `docker compose up --build`로 재기동한다.
4. 이전에 발급된 access/refresh 토큰은 무효이므로, **다시 로그인**해 테스트한다.

### 주의

- `.env` 파일을 Slack·이메일·Issue에 붙여 넣지 않는다. 공유가 필요하면 **1Password 등 비밀 관리 도구** 또는 팀 합의된 안전한 채널을 사용한다.
- 운영(스테이징/프로덕션) 환경은 별도 시크릿을 사용하고, 개발 PC `.env`와 동일하게 두지 않는다.

## 참고
- 설계 문서를 변경 시 먼저 수정 후 구현.
- 물리 스키마 기준: `DB/init.sql`, `docs/데이터 스키마 설계.md`.
- 커밋 메시지: `feat(be): 물리 스키마 정합 - 엔티티 AMR_MASTER 매핑 #이슈번호`
- PR 전 로컬 검증 필수.
