# TODO.md - 백엔드 개발 계획

## 프로젝트 개요

AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## 현재 상태 (dev, 2026-05-18 기준)

- BE 전용 Docker 실행 환경 구성 완료 (2026-05-17). `BE/docker-compose.yml` + H2.
- **10-A (H2 물리 스키마 정합·API 스모크):** 완료 (2026-05-18).
- **설계 문서(dev):** `docs/API 정의.md`, `docs/데이터 스키마 설계.md`, `docs/화면 설계서.md`, `docs/ADR/20260518-1252-AMR-emergency-logic.md` 반영 완료 (PR #27 merge).
- **DB 영역 Docker compose:** 미 merge. 당분간 **H2 + BE Docker**로 시연·개발.
- **DAS·FE·BE 합의:** 비상 정지는 BE가 DB 갱신 후 `accepted` 응답, FE가 DAS(MQTT) 정지 고지. BE↔DAS 직접 연동 없음.
- **시연 최우선 (BE):** REST 시연 경로 정합 → 비상 정지·자동 복구 → (여유 시) WebSocket.

### 설계 대비 BE 구현 격차 (작업 기준)

| 영역 | 설계 (`docs/API 정의.md` 등) | 현재 BE | 우선순위 |
|------|------------------------------|---------|----------|
| `GET /dashboard/summary` | `amrError`, `amrErrorUnresolved` | **12-B 완료** (Docker 스모크 대기) | — |
| `POST /amrs/{id}/commands` | DB 반영 후 `accepted: true` | **12-C 완료** (emergencyStop, Docker 스모크 대기) | — |
| AMR `status` | `OPERATING`/`IDLE`/`CHARGING`/`ERROR`/`EMERGENCY_STOP` | **12-A 완료** (대문자 enum·normalizer) | — |
| `AmrStatusLog` | `fault_code`, `fault_recovered_at`, `emergency_resolved_at` | **12-A 완료** (엔티티·시드) | — |
| 운행 자동 복구 | ~60초 후 IDLE 등, FE `resume` 없음 | 스케줄러 없음 | **A-필수** |
| `GET /amrs` | `status` 콤마, `sort=unresolvedFirst`, `faultCode` | 단일 status·소문자 비교, sort 없음 | **A-권장** |
| `GET /environment/areas/current` | SCR-01 ③ | 컨트롤러·서비스 없음 | **A-선택** |
| `GET /analytics/kpis` | `errorCount`, `scheduleComplianceRate` | 필드 없음 | **B (시연 후)** |
| `WS /api/v1/stream` | 5종 이벤트, JWT | 미구현 | **B (REST 후)** |
| `AMR_COMMAND` | 명령 이력 | **12-C 완료** (emergencyStop INSERT) | — |

> FE·DAS(MQTT 토픽·payload)는 BE 범위 밖. BE는 DB·REST·(선택) WebSocket만 담당.

---

## 다음 예정 작업 (팀 공유용) — BE 시연 우선

| 순서 | 작업 | 한 줄 설명 | BE 담당 |
|------|------|-----------|---------|
| ~~0~~ | ~~10-A 물리 스키마 정합~~ | **완료** (2026-05-18) | — |
| ~~—~~ | ~~설계 문서 반영~~ | dev에 API·화면·ADR 반영 완료 | — |
| **1** | **Phase A: REST 시연 경로** | 대시보드 에러 KPI, E-Stop DB, 자동 복구, status 정합 | **지금** |
| **2** | **Phase B: WebSocket 최소** | `amrs.status.updated`, `dashboard.summary.updated` (+ 선택 위치 tick) | A 완료 후 |
| (병렬) | **FE·BE 통합** | CORS·프록시·runbook (FE/인프라) | BE: Security·헬스 URL 문서화 |
| 3 | 공통 오류 응답 | API 실패 형식 통일 | 시연 직전·직후 |
| (대기) | **10-B MySQL** | DB compose merge 후 | 시연 필수 아님 |
| (후순) | Analytics KPI 확장, 테스트·CSV | `errorCount` 등 | 시연 후 |

**현재 BE 1순위:** **12-D** 자동 복구 (~60초) (12-C 코드 완료, Docker 스모크 권장).

---

## Phase A — 시연 필수 REST (BE 단독으로 FE 연동 가능)

목표: 로그인 → 대시보드 **에러/미해결 카드** → AMR 비상 정지 → 수치 유지 → **~60초 후 감소** (FE는 REST 폴링으로 반영 가능).

### 12-A. 엔티티·상태 정규화 — 완료 (2026-05-18)

- [x] `AmrStatusLog`에 `faultCode`, `faultMessage`, `faultRecoveredAt`, `emergencyResolvedAt` 매핑 (`DB/init.sql`·`docs/데이터 스키마 설계.md`와 동일).
- [x] `DashboardStatusNormalizer` 보강
  - DB/API 응답: 설계 enum **`OPERATING`, `IDLE`, `CHARGING`, `ERROR`, `EMERGENCY_STOP`** (대문자) 기준.
  - `IDLE`, `EMERGENCY_STOP` 명시 매핑 (`EMERGENCY_STOP` → `waiting` 오분류 수정).
  - 기존 시드·로그의 한글/소문자 혼용은 normalizer에서 흡수.
- [x] `AmrDto`·상세 응답: `faultCode`, `faultMessage`, `loadWeightKg`, `sohPercent`, `totalMileageKm`.

### 12-B. 대시보드 KPI (`GET /dashboard/summary`) — 완료 (2026-05-18)

- [x] `DashboardSummaryDto`에 `amrError`, `amrErrorUnresolved` 추가.
- [x] `DashboardService.getSummary()`: AMR별 최신 `AMR_STATUS_LOG` 기준 집계
  - `amrError`: `status IN ('ERROR', 'EMERGENCY_STOP')`
  - `amrErrorUnresolved`: (`ERROR` AND `fault_recovered_at IS NULL`) OR (`EMERGENCY_STOP` AND `emergency_resolved_at IS NULL`)
- [x] `avgBatteryPercent` 유지.
- [ ] Docker 스모크: summary JSON에 신규 필드 포함 확인 (H2 시드 기준 amr-04 1대 → `amrError`·`amrErrorUnresolved` ≥ 1).

### 12-C. AMR 제어·비상 정지 (`POST /amrs/{amrId}/commands`) — 완료 (2026-05-18)

- [x] `AmrCommand` 엔티티·`AmrCommandRepository` (`AMR_COMMAND` 테이블).
- [x] `AmrService.sendCommand` 트랜잭션 구현
  - `emergencyStop`: 최신 `AMR_STATUS_LOG` UPDATE(또는 INSERT), `status = 'EMERGENCY_STOP'`, `fault_code = null`, `emergency_resolved_at = null`
  - `AMR_COMMAND` INSERT, `accepted = true`, `status = EXECUTED`, `requested_at`/`executed_at`
  - 진행 중 `AMR_TASK` → `CANCELLED` + `drop_time`
  - **commit 성공 후에만** HTTP 200 + `accepted: true`
- [x] `goTo`/`pause`/`resume`/`cancelTask`: 문법은 허용, 실행 시 400 (`emergencyStop`만 DB 반영).
- [ ] Docker 스모크: 정지 전후 `GET /dashboard/summary`, `GET /amrs/{id}` 상태 변경 확인.

### 12-D. 운행 자동 복구 (시연) — 다음

- [ ] `application.yaml` (또는 `app.demo.recovery-seconds`, 기본 60) 설정값.
- [ ] `@Scheduled` (또는 `TaskScheduler`): 미해결 `ERROR`/`EMERGENCY_STOP` 대상
  - `EMERGENCY_STOP` → `emergency_resolved_at = now`, `status = 'IDLE'`(또는 `OPERATING`)
  - `ERROR` → `fault_recovered_at = now`, `fault_code = null`, `status = 'IDLE'`(또는 `OPERATING`)
- [ ] 복구 후 `amrError`/`amrErrorUnresolved` 감소 스모크.
- [ ] FE `resume` 명령은 시연 경로에서 호출하지 않음 (문서 준수, BE에서 막을 필요 없음).

### 12-E. AMR 목록·필터 (`GET /amrs`)

- [ ] `status` 쿼리: 콤마 구분 (`ERROR,EMERGENCY_STOP`) OR 조건.
- [ ] `sort=unresolvedFirst`: 미해결 우선, 동일 시 `EMERGENCY_STOP` 우선.
- [ ] 응답 `status`·`faultCode`가 설계 enum/필드와 일치하는지 확인.

### 12-F. 환경 API (선택, SCR-01 ③)

- [ ] `EnvironmentController` + `EnvironmentService`: `GET /environment/areas/current`
- [ ] `EnvSensor` + `EnvSensorLog` 최신 `measured_at` per sensor 집계.
- [ ] `DashboardDemoDataLoader`에 `ENV_SENSOR_LOG` 시드 추가 (`init.sql`에는 INSERT 없음, H2 시연용).
- [ ] 미구현 시 FE는 하드코딩 유지 가능 → **Phase A 필수 아님**.

### 12-G. Phase A 검증 체크리스트

- [ ] `docker compose up --build` 후 로그인.
- [ ] `GET /dashboard/summary` → `amrError`, `amrErrorUnresolved` 존재.
- [ ] `POST /amrs/amr-01/commands` `emergencyStop` → summary 에러 수치 유지/증가.
- [ ] 60초(설정값) 대기 → summary 에러 수치 감소, `GET /amrs/amr-01` status 복구.
- [ ] `GET /amrs?status=ERROR,EMERGENCY_STOP&sort=unresolvedFirst` (12-E 완료 시).

---

## Phase B — WebSocket 최소 (Phase A 이후)

`docs/API 정의.md` §8. 시연 v1에서는 FE **REST 폴링(3~10초)** 으로도 Phase A 시나리오 가능. 여유 있을 때 진행.

### 6. WebSocket 구현

- [ ] `WebSocketConfig`: `WS /api/v1/stream`, handshake 시 JWT (`?token=` 또는 `Authorization` 중 팀 합의 1안).
- [ ] `SecurityConfig`: stream 경로 인증 예외/필터 연동.
- [ ] `StreamEventPublisher` (또는 동등 서비스): 공통 봉투 `{ event, timestamp, data }`.
- [ ] **필수 발행 (시연)**
  - `amrs.status.updated` — `sendCommand`·자동 복구 직후
  - `dashboard.summary.updated` — 위 이벤트 직후 또는 summary 재계산 시
- [ ] **선택 발행**
  - `amrs.position.updated` — `@Scheduled` 3~5초마다 `pos_x`/`pos_y` 소폭 변경( DAS 없을 때 “움직임” 연출)
  - `alarms.created`, `charging.forecast.updated` — 시연 후
- [ ] Phase A의 명령·복구 서비스에서 publisher 호출 (트랜잭션 commit 이후).

---

## Phase C — 시연 후·인프라

### 7. 예외 처리 및 로깅

- [x] 전역 예외 처리 (`@RestControllerAdvice`) — 인증 최소 구현.
- [x] 커스텀 예외 (AmrNotFoundException 등).
- [ ] 공통 오류 응답 형식 통일 (`docs/API 정의.md` §9).

### Analytics KPI 확장

- [ ] `AnalyticsKpiDto`에 `errorCount`, `scheduleComplianceRate` 추가.
- [ ] `AnalyticsService.getKpis()`: 버킷별 `AMR_STATUS_LOG`·`AMR_TASK`/`PR_ROUTING` 집계 (`docs/API 정의.md` §7).

### 8. 테스트 및 검증

- [ ] Service 단위 테스트 (dashboard 집계, emergencyStop, auto-recovery).
- [ ] Controller 통합 테스트 (MockMvc).
- [ ] CI 또는 로컬 `docker compose` 스모크 스크립트 (선택).

### 9. 추가 기능

- [ ] CSV export 검증·보완.
- [ ] 캐싱 (필요 시).

### 10-B. MySQL 연동 — DB compose merge 후

- [ ] MySQL Connector, `application-mysql.yaml`, `ddl-auto: validate`.
- [ ] BE·DB compose 연동, H2 DataLoader 비활성화.
- [ ] `REFRESH_TOKEN` DB 저장 (`InMemoryRefreshTokenStore` 대체).
- [ ] Phase A·B와 동일 API 스모크 재검증.

### 0. Docker (잔여)

- [x] BE 단독 compose (10-A 완료 후에도 H2 유지).
- [ ] 10-B 후 runbook 갱신.
- [ ] 루트 통합 `docker-compose` (FE·DB·BE 합의 후).

---

## 완료 작업 기록 (팀 공유용)

### [완료] BE 전용 Docker 실행 환경 (2026-05-17)

**산출물:** `amr_control_system/Dockerfile`, `BE/docker-compose.yml`, `application-docker.yaml`, `BE/.env.example`  
**실행:** `BE` 폴더에서 `docker compose up --build`  
**Health:** http://localhost:8080/api/v1/actuator/health

### [완료] 10-A 물리 스키마 정합 (H2, 2026-05-18)

- 엔티티·Repository·Service를 `DB/init.sql` 테이블명·컬럼에 맞춤.
- `DemoUserDataLoader`, `DashboardDemoDataLoader`, `AlarmDemoDataLoader` 시드.
- API 스모크: auth, dashboard, amrs, alarms, charging, work-histories, analytics → HTTP 200.

**10-A에서 하지 않은 것 (의도적):** MySQL, `AMR_COMMAND` 영속, `fault_*`/`emergency_*` 컬럼, API 필드 상세 정합 → **Phase A**에서 처리.

### [완료] 12-A 엔티티·상태 정규화 (2026-05-18)

- `AmrStatusLog` fault/emergency 컬럼, `DashboardStatusNormalizer` 대문자 enum, `AmrDto` 확장.
- H2 시드: amr-04 `ERROR` + `SENSOR_FAULT`. Docker 재기동 시 `docker compose down -v` 권장.

### [완료] 12-B 대시보드 에러 KPI (2026-05-18)

- `DashboardSummaryDto.amrError`, `amrErrorUnresolved` 및 `DashboardService` 집계 (`isUnresolvedAmrError`).

### [완료] 12-C AMR 비상 정지 (2026-05-18)

- `AmrCommand` 영속, `sendCommand` 트랜잭션(`emergencyStop` → `AMR_STATUS_LOG` + `AMR_COMMAND` + 활성 task 취소).

---

## 개발 계획 (레거시 섹션·참고)

아래 0~11은 당초 계획이다. **진행 상태는 상단 Phase A/B/C와 「다음 예정 작업」 표를 따른다.**

### 0~5. 기반 구현 — 완료

Docker, 엔티티(10-A 전 기반), DTO, Security, Controller, Service — [x] 완료. 상세는 git history·위 「완료 작업 기록」 참고.

### 10-A. 스키마 정합 — 완료 (2026-05-18)

| 현재 (BE) | 물리 DB | 비고 |
|-----------|---------|------|
| `AMR_MASTER` 등 | `init.sql` | 10-A 완료 |
| `ENV_SENSOR_LOG` | 엔티티 있음 | **로그 시드·API는 Phase A-12-F** |
| `AMR_COMMAND` | DDL만 | **Phase A-12-C** |

### 11. AMR 비상 정지 — 12-C 완료, 12-D·Phase B 잔여

- [x] `AmrCommand` 엔티티·Repository
- [x] `sendCommand` DB 트랜잭션 (`EMERGENCY_STOP`)
- [x] commit 후 `accepted: true`
- [ ] (Phase B) WebSocket `amrs.status.updated` 발행

---

## 작업 우선순위 (BE 담당자용)

1. **Phase A-12-D** — auto-recovery (**시연 블로커**, 12-A·12-B·12-C 완료)
2. **Phase A-12-E** — amrs 필터·정렬 (에러 카드 클릭 시나리오)
3. **Phase A-12-F** — environment API (시간 있을 때)
4. **Phase B-6** — WebSocket 최소
5. **Phase C** — analytics KPI, 오류 응답, 테스트, 10-B

**한 번에 하나의 Phase A 하위 태스크만** 진행한다 (`AGENTS.md` 규칙).

---

## Docker 실행 (BE 폴더)

```bash
cp .env.example .env
# JWT_SECRET 설정 (아래 「다른 PC에서 JWT 키 갱신」)
docker compose up --build
```

| 용도 | URL |
|------|-----|
| Health | http://localhost:8080/api/v1/actuator/health |
| Login | POST http://localhost:8080/api/v1/auth/login |
| Summary | GET http://localhost:8080/api/v1/dashboard/summary |
| AMRs | GET http://localhost:8080/api/v1/amrs |
| Emergency stop | POST http://localhost:8080/api/v1/amrs/{amrId}/commands |
| Environment (Phase A-F 후) | GET http://localhost:8080/api/v1/environment/areas/current |

---

## 다른 PC에서 JWT 키 갱신 (팀 공유)

`.env`는 Git에 포함되지 않는다.

**PowerShell**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

```env
JWT_SECRET="<생성 문자열>"
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
DEMO_USER_PASSWORD=demo123
```

`docker compose down` 후 `docker compose up --build`, **재로그인** 필요.

---

## AMR 정지·복구 (BE 구현 기준)

```
[1] FE  --POST /amrs/{id}/commands (emergencyStop)-->  BE
[2] BE  --트랜잭션-->  DB (AMR_COMMAND, AMR_STATUS_LOG)
[3] BE  --200 { accepted: true }-->  FE  (DB commit 후만)
[4] FE  --MQTT-->  DAS  (BE 범위 밖)
[5] BE  --@Scheduled-->  자동 복구 (~60s)  (Phase A-12-D)
[6] BE  --(Phase B) WS-->  FE  amrs.status.updated / dashboard.summary.updated
```

- `accepted: true`: **DB 반영 완료** (현재 코드는 미반영 → Phase A-12-C).
- `emergencyStop` → `status = 'EMERGENCY_STOP'` (`ERROR`와 구분, `docs/ADR/20260518-1252-AMR-emergency-logic.md`).
- MQTT 토픽·payload: FE·DAS 이슈.

---

## 참고

- 설계 변경 시 `docs/` 먼저 수정 후 구현.
- 물리 스키마: `DB/init.sql`, `docs/데이터 스키마 설계.md`.
- 화면·API 매핑: `docs/화면 설계서.md`.
- PR 전: `docker compose up --build` + Phase A 검증 체크리스트.
