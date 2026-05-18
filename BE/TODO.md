# TODO.md - 백엔드 개발 계획

## 프로젝트 개요
AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## 현재 상태
- 기본 Spring Boot 프로젝트 구조 생성됨 (AmrControlSystemApplication.java, 테스트 파일).
- 설계 문서 확인 완료: 프로젝트 정의서, API 정의, 데이터 스키마 설계, 협업 컨벤션.
- ERD 및 API 명세 기반 API·서비스 로직 구현 진행 중.
- **BE 전용 Docker 실행 환경 구성 완료** (2026-05-17). PC에 Java를 설치하지 않아도 Docker만으로 백엔드 서버를 빌드·실행할 수 있다.
- **DB 물리 스키마 merge 완료** (`DB/init.sql`). **10-A (H2 스키마 정합·API 스모크):** 완료 (2026-05-18).
- **DB 영역 Docker compose는 아직 merge되지 않음.** 당분간 **H2 + BE Docker**로 시연·개발.
- **DAS·FE·BE 합의:** AMR 비상 정지는 BE가 DB 상태를 갱신한 뒤 `accepted` 응답, FE가 DAS(MQTT)에 정지 고지 (설계 문서 반영 예정, 섹션 11).
- **시연 최우선:** WebSocket → AMR 정지(DB 반영) → (병렬) FE·BE 통합 기동.

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
- 물리 스키마에 맞춘 JPA 엔티티 정합 (섹션 10-A)  
- MySQL·DB Docker compose 연동 (섹션 10-B, DB compose merge 후)  
- 프론트엔드·DB와 한 번에 띄우는 **프로젝트 루트 통합 docker-compose**  
- WebSocket 실시간 스트리밍 (별도 TODO)

**실행 방법 (개발자·검증 담당)**  
`BE` 폴더에서: `docker compose up --build`  
상태 확인: http://localhost:8080/api/v1/actuator/health

---

## 다음 예정 작업 (팀 공유용) — 시연 우선

| 순서 | 작업 | 한 줄 설명 | 영향 |
|------|------|-----------|------|
| ~~0~~ | ~~10-A 물리 스키마 정합~~ | **완료** (2026-05-18) | REST API 스모크 통과 |
| **1** | **WebSocket + 데모 이벤트** | `/api/v1/stream`, 위치·상태·알람 푸시 | 시연 시 화면이 “살아 있음” |
| **2** | **AMR 비상 정지 (DB 반영)** | `POST .../commands` → DB 갱신 → `accepted` | 합의한 정지 시나리오의 BE 구간 |
| **3** | **설계 문서 반영** | 정지·DAS·MQTT 흐름 명세 (섹션 11) | FE·DB·BE 구현 기준 통일 |
| (병렬) | **FE·BE 통합 기동** | 루트/문서화된 compose, CORS·URL | 시연 당일 원클릭 기동 |
| 4 | 공통 오류 응답 정리 | API 실패 형식 통일 | FE 연동 품질 |
| (대기) | **10-B MySQL 연동** | DB compose merge 후 | 시연 필수 아님 |
| (후순) | 단위·통합 테스트, CSV 등 | 회귀·부가 기능 | 시연 이후 |

**현재 진행 예정 1순위:** 섹션 **6 WebSocket** (시연용 주기 이벤트 포함).

---

## 개발 계획 (작은 단위로 순차 진행)

### 0. Docker 실행 환경 (BE 단독) — 완료 (2026-05-17)
- [x] `amr_control_system/Dockerfile` (multi-stage, Java 17).
- [x] `amr_control_system/.dockerignore`.
- [x] `BE/docker-compose.yml` (서비스 `be`, 포트 8080).
- [x] `application-docker.yaml` (H2, JWT 환경 변수, Actuator health).
- [x] `BE/.env.example` (JWT_SECRET 등).
- [x] Docker로 `docker compose up --build` 기동 및 `/actuator/health` 응답 확인.
- [ ] 10-A 완료 후에도 H2·BE 단독 compose 유지 (10-B 전까지).
- [ ] 10-B: DB compose merge 후 MySQL 연동 (별도 작업).
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

> **주의:** 위 엔티티는 `DB/init.sql` 물리 스키마와 테이블명·컬럼명·PK 타입이 다르다. 섹션 **10-A**에서 일괄 정합한다.

### 2. DTO 클래스 구현
- [x] 요청/응답 DTO 생성 (API 정의.md 기반).
  - [x] 인증, 대시보드, AMR, 충전, 알람, 작업 이력, 분석 DTO.

### 3. 보안 및 인증 구현
- [x] Spring Security 설정 클래스 생성 (JwtAuthenticationFilter, SecurityConfig).
- [x] JWT 유틸리티 클래스 생성 (토큰 생성/검증).
- [x] 사용자 엔티티 및 Repository 추가 (기본 사용자 관리).
- [x] 10-A: `USER_ACCOUNT` 엔티티 매핑 (H2). `REFRESH_TOKEN`·DB 저장 전환은 10-B.

### 4. 컨트롤러 구현
- [x] AuthController, DashboardController, AmrController, ChargingController.
- [x] AlarmController, WorkHistoryController, AnalyticsController.

### 5. 서비스 로직 구현
- [x] AuthenticationService, DashboardService, AmrService, ChargingService.
- [x] AlarmService, WorkHistoryService, AnalyticsService.
- [x] 10-A: Repository·매핑 로직 수정 (엔티티 rename 반영).

### 6. WebSocket 구현 — **시연 1순위**
- [ ] WebSocket 설정 (`/api/v1/stream`), 연결 시 JWT 인증.
- [ ] 이벤트 발행 서비스 (`amrs.position.updated`, `amrs.status.updated`, `alarms.created`, `dashboard.summary.updated` 등).
- [ ] 시연용 주기 발행(선택): AMR 좌표·상태가 주기적으로 갱신되도록 스케줄 또는 DAS 연동 전 임시 시뮬레이터.
- [ ] AMR 정지 후 `amrs.status.updated` 등으로 FE에 상태 반영 (섹션 11과 연동).

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

### 10. 물리 DB 스키마 정합 — **완료 (10-A, 2026-05-18)**

`DB/init.sql` 및 `docs/데이터 스키마 설계.md`를 기준으로 백엔드를 맞춘다.  
**2단계로 진행:** 지금은 **10-A(H2 유지)** 만 수행하고, **10-B(MySQL)** 는 DB 영역 `docker-compose` merge 이후에 한다.

---

#### 10-A. 스키마 정합 (H2 유지) — **지금 진행**

**목표:** JPA가 물리 스키마와 **같은 테이블명·컬럼명·관계**를 쓰도록 맞춘다. DB 엔진은 계속 H2이며, `application-docker.yaml`의 `ddl-auto: create-drop`으로 기동 시 스키마를 생성한다. `BE/docker-compose.yml`은 **변경하지 않는다.**

**10-A-1. 엔티티·테이블 매핑** — 완료 (2026-05-18)

| 현재 (BE) | 물리 DB (`init.sql`) | 주요 변경 |
|-----------|----------------------|-----------|
| `AMR` | `AMR_MASTER` | 테이블명, PK 수동 할당(INT, AUTO 없음) |
| `ENV_READING` | `ENV_SENSOR_LOG` | `EnvSensorLog` 엔티티로 교체 |
| `PROCESS_MASTER` | `PR_PROCESS` | `pr_process_id`(VARCHAR PK) |
| `ROUTING` | `PR_ROUTING` | `pr_routing_id`, `seq_no` |
| `AMR_CHARGING_SESSION` | `AMR_CHARGING_LOG` | 테이블명 |
| `ALARM` | `ALARM_LOG` | `acknowledged_at`만 영속화 |
| `USERS` | `USER_ACCOUNT` | `user_id`, `password_hash` |
| (없음) | `REFRESH_TOKEN` | 10-B 또는 후속(인메모리 refresh 유지) |
| (없음) | `AMR_COMMAND` | 후속(제어 명령 비영속 유지) |

추가 컬럼·FK:

- [x] `WorkOrder`: `wo_id`→`work_id`, `wo_no` 제거.
- [x] `WipLot`: `work_id`, `pr_routing_id`, PK `BIGINT`.
- [x] `EnvSensor`: `env_sensor_id`(VARCHAR PK).
- [x] `AmrStatusLog`: `amr_statlog_id` `BIGINT`, `SOH_pct`, `load_weight` INT.
- [x] `Alarm`/`ALARM_LOG`: API ack 응답은 username·시각만 반환(DB에 `ack_by` 없음).
- [x] `Area`/`Product`: VARCHAR PK, AREA 허용 범위 컬럼 추가.

**10-A-2. Repository·Service** — 완료 (2026-05-18)

- [x] Repository ID 타입·`EnvSensorLogRepository` 반영.
- [x] `AlarmService`, `AuthenticationService`, `AnalyticsService`, `WorkHistoryService` 등 매핑 수정.
- [ ] `docs/임시-스키마-변경-알람.md` 문서 정리(후속).

**10-A-3. H2 시드 데이터 (`init.sql` 대체)** — 완료 (2026-05-18)

- [x] `DemoUserDataLoader`, `DashboardDemoDataLoader`, `AlarmDemoDataLoader`를 `init.sql` ID·값에 맞게 수정.
- [x] FK 삽입 순서 준수.

**10-A-4. 검증 (BE Docker만)** — 완료 (2026-05-18)

- [x] `docker compose build` 성공.
- [x] `docker compose up` 후 API 스모크(로컬 `.env` 필요).
- [x] Health, login, dashboard, amrs, alarms, charging, work-histories, analytics KPIs → HTTP 200 확인.
- [ ] API 응답 필드가 `docs/API 정의.md`와 일치하는지 상세 대조(후속).

**10-A에서 하지 않는 것**

- MySQL Connector 추가, `docker-compose`에 MySQL 서비스 추가
- `init.sql` 마운트, `ddl-auto: validate`
- BE가 DB compose를 대신 구성하는 작업

---

#### 10-B. MySQL 연동 — **DB compose merge 후**

**전제:** DB 영역 `docker-compose`(또는 루트 통합 compose) merge, `init.sql` 기동 경로 확정.

- [ ] `build.gradle`: MySQL Connector/J 의존성.
- [ ] `application-mysql.yaml`(또는 profile): MySQL datasource, dialect, `ddl-auto: validate` 또는 `none`.
- [ ] BE·DB compose 연동: BE가 MySQL에 접속, `init.sql` 시드 사용.
- [ ] `.env.example`: DB 호스트·포트·계정 변수.
- [ ] H2 DataLoader 비활성화(시드는 DB init 담당).
- [ ] `InMemoryRefreshTokenStore` → `REFRESH_TOKEN` DB 저장(미완 시).
- [ ] MySQL 환경에서 10-A와 동일 API 스모크 재검증.

---

### 11. AMR 비상 정지 및 DAS 연동 (합의 반영) — **시연 2순위**

팀 합의: FE → BE(명령·DB 갱신·`accepted`) → FE → DAS(MQTT 정지 고지). BE는 DAS와 직접 통신하지 않음.

- [ ] `docs/` 설계 문서 반영 (프로젝트 정의서·API 정의·필요 시 ERD 주석). 상세는 본 문서 하단 「AMR 정지 합의」 참고.
- [ ] `AmrCommand` 엔티티·Repository (`AMR_COMMAND` 테이블).
- [ ] `AmrService.sendCommand`: `emergencyStop` 등 시 DB 트랜잭션 내
  - `AMR_COMMAND` INSERT (`accepted=true`, `status` 등)
  - 최신 `AMR_STATUS_LOG` 상태 갱신(예: `STOPPED`/`ERROR`) 또는 신규 로그 행 INSERT
  - (선택) 진행 중 `AMR_TASK` 상태 정리
- [ ] DB 커밋 성공 후에만 HTTP 200 + `accepted: true` 응답.
- [ ] (WebSocket 구현 시) `amrs.status.updated` 이벤트 발행.
- [ ] FE·DAS: MQTT 토픽·페이로드는 FE·DB 영역 문서에 정의 (BE 범위 밖).

## 작업 우선순위 (시연 기준)

1. ~~Docker·REST API·10-A~~ **완료**
2. **WebSocket + 시연용 이벤트** (섹션 6)
3. **AMR 비상 정지 DB 반영 + 설계 문서** (섹션 11)
4. **FE·BE 통합 시연 경로** (루트 compose 또는 runbook)
5. 공통 오류 응답 (섹션 7)
6. 10-B MySQL, 테스트·추가 기능 (시연 후)

## Docker 실행 (BE 폴더에서)

10-A·10-B 공통: **지금은 BE 컨테이너만** 띄운다.

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

> **10-B 완료 후** 실행 방법은 DB compose·MySQL 연결 방식에 맞게 이 절을 갱신한다.

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

## AMR 정지 합의 (설계 문서 반영용 메모)

DAS·DB·BE·FE 논의 결과. 구현·문서 수정 시 기준.

```
[1] FE  --POST /amrs/{id}/commands (emergencyStop)-->  BE
[2] BE  --트랜잭션-->  DB (AMR_COMMAND, AMR_STATUS_LOG 등 운행 상태)
[3] BE  --200 { accepted: true, commandId }-->  FE
[4] FE  --MQTT-->  DAS  (좌표 갱신·작업 시뮬 중단)
```

- 시뮬레이션: 실제 AMR 없음. 운행 상태는 **DB가 기준**, DAS는 MQTT로 좌표·작업 생성.
- BE ↔ DAS 직접 연동 없음 (현업형 설비 제어 경로 생략, 프로젝트 시연 목적).
- `accepted: true` 의미: **DB 반영 완료 후** 응답 (현재 BE는 DB 미반영·즉시 true → 섹션 11에서 수정).

## 참고
- 설계 문서를 변경 시 먼저 수정 후 구현.
- 물리 스키마 기준: `DB/init.sql`, `docs/데이터 스키마 설계.md`.
- PR 전 로컬 검증: `docker compose up --build`.
