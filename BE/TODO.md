# TODO.md - 백엔드 개발 계획

## 프로젝트 개요

AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## FE 녹화 시연 MVP (현재 1순위, 2026-05-19)

**SSOT:** `docs/시연_MVP_합의.md` §5.1, `docs/FE-DAS_MQTT_연동.md`

| 항목 | 시연 BE 범위 |
| --- | --- |
| **필수** | §5.1 REST (login, summary, **recent-alarms**, recent-logs, amrs, commands, charging stations/forecast, work-histories, workload) |
| **스모크** | `docker compose up` 후 §5.1 경로 회귀 (**B1**) |
| **하지 않음** | 12-F `GET /environment/areas/current`, analytics KPI 확장, position WS 스케줄, queue 시연 데이터 |
| **유지·FE 미사용** | Phase B WebSocket (C-P2-01: 시연 FE = **REST 폴링**) |

**모순 해소 (C-P2, BE 관점):**

| ID | 확정 |
| --- | --- |
| C-P2-01 | 텔레메트리(좌표·환경) = FE←MQTT←DAS. BE WS **시연 필수 아님** |
| C-P2-02 | 12-F **시연 착수 안 함**. 실시간 환경은 DAS MQTT |
| C-P2-03 | BE `@Scheduled` position·`amrs.position.updated` **시연 제외** (DAS와 이중) |
| C-P2-11 | 시연 런타임 = **H2 + BE Docker** (MySQL merge 시연 후) |

| 순 | 작업 | 상태 |
| --- | --- | --- |
| B1 | §5.1 API Docker 스모크 (`scripts/smoke-*.py` 등) | [ ] 녹화 전 재확인 |
| — | 12-F, Phase C, position WS | **시연 후** |

---

## 현재 상태 (dev, 2026-05-19 기준)

- **로컬 실행:** 호스트 JDK 없이 **Docker만** 사용. 빌드·실행은 `BE/docker-compose.yml` (`amr_control_system/Dockerfile` 내 `./gradlew bootJar`).
- BE 전용 Docker 실행 환경 구성 완료 (2026-05-17). 프로파일 `docker` + H2 in-memory (`application-docker.yaml`).
- **10-A (H2 물리 스키마 정합·API 스모크):** 완료 (2026-05-18).
- **Phase A (REST 시연 경로):** 완료 (2026-05-18, Docker 스모크 검증).
- **Phase B (WebSocket):** **완료** (2026-05-19). B-1~4 Docker WS 스모크 통과 (`scripts/smoke-websocket-phase-b.py`).
- **녹화 스프린트:** 신규 BE API **추가 없음**. **B1 스모크**만 (상단 MVP 표).
- **Phase S (Swagger UI):** **S-1~S-3 완료** (2026-05-19). S-4·Phase C·12-F는 **시연 후**.
- **DB 영역 Docker compose:** 미 merge. 당분간 **H2 + BE Docker**로 시연·개발.
- **DAS·FE·BE 합의:** 비상 정지는 BE가 DB 갱신 후 `accepted` 응답, FE가 DAS(MQTT) 정지 고지. BE↔DAS 직접 연동 없음.
- **실시간 텔레메트리(맵 좌표·환경 센서):** FE ← **MQTT** ← DAS. BE `GET /environment/areas/current`(12-F)는 **시연 제외**.
- **FE·BE 실시간:** BE `WS /api/v1/stream` 구현 완료. **시연 FE는 REST 폴링**(C-P2-01).

### 설계 대비 BE 구현 격차 (작업 기준)

| 영역 | 설계 (`docs/API 정의.md` 등) | 현재 BE | 우선순위 |
|------|------------------------------|---------|----------|
| `GET /dashboard/summary` | `amrError`, `amrErrorUnresolved` | **12-B 완료** (Docker 스모크 검증됨) | — |
| `POST /amrs/{id}/commands` | DB 반영 후 `accepted: true` | **12-C 완료** (Docker 스모크 검증됨) | — |
| AMR `status` | `OPERATING`/`IDLE`/`CHARGING`/`ERROR`/`EMERGENCY_STOP` | **12-A 완료** (대문자 enum·normalizer) | — |
| `AmrStatusLog` | `fault_code`, `fault_recovered_at`, `emergency_resolved_at` | **12-A 완료** (엔티티·시드) | — |
| 운행 자동 복구 | ~60초 후 IDLE 등, FE `resume` 없음 | **12-D 완료** (`AmrAutoRecoveryService`) | — |
| `GET /amrs` | `status` 콤마, `sort=unresolvedFirst`, 응답 `faultCode` | **12-E 완료** (`faultCode` 쿼리는 `docs/API 정의.md` §3 미정의) | — |
| `GET /environment/areas/current` | SCR-01 ③ (명세) | 미구현 | **보류** — 실시간 환경은 FE←MQTT←DAS, 설계 재검토 후 |
| `GET /analytics/kpis` | `errorCount`, `scheduleComplianceRate` | 필드 없음 | **B (시연 후)** |
| `WS /api/v1/stream` | 5종 이벤트, JWT | **시연 2종 완료:** handshake·`amrs.status.updated`·`dashboard.summary.updated` (Docker 스모크 검증). **잔여:** 선택 이벤트 3종 | — |
| `AMR_COMMAND` | 명령 이력 | **12-C 완료** (emergencyStop INSERT) | — |

> FE·DAS(MQTT 토픽·payload)는 BE 범위 밖. BE는 DB·REST·(선택) WebSocket만 담당.

---

## 다음 예정 작업 (팀 공유용)

| 순서 | 작업 | 한 줄 설명 | 상태 |
|------|------|-----------|------|
| **1** | **FE 녹화 MVP B1** | §5.1 REST Docker 스모크 | **1순위** |
| ~~—~~ | ~~Phase A REST 시연~~ | 12-A~G (12-F 제외) | **완료** |
| ~~—~~ | ~~Phase B WebSocket 최소~~ | B-1~4 | **완료** |
| (시연 후) | Phase S-4·설계 재검토 | API·화면 vs 구현 | 대기 |
| (시연 후) | 12-F 환경 API | MQTT 합의 후 이력·스냅샷 API로 재정의 | 대기 |
| (시연 후) | Phase C | analytics KPI, 공통 오류 응답 | 대기 |
| (병렬) | FE·BE 통합 | REST·CORS·vite 프록시 (WS·MQTT는 FE) | FE 주도 |
| (대기) | 10-B MySQL | DB compose merge 후 | — |

**현재 BE 1순위:** **녹화 MVP B1(§5.1 스모크)**. 신규 API 구현 **없음**.

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
- [x] Docker 스모크: summary JSON에 신규 필드 포함 확인 (2026-05-18, `amrError`·`amrErrorUnresolved` ≥ 1).

### 12-C. AMR 제어·비상 정지 (`POST /amrs/{amrId}/commands`) — 완료 (2026-05-18)

- [x] `AmrCommand` 엔티티·`AmrCommandRepository` (`AMR_COMMAND` 테이블).
- [x] `AmrService.sendCommand` 트랜잭션 구현
  - `emergencyStop`: 최신 `AMR_STATUS_LOG` UPDATE(또는 INSERT), `status = 'EMERGENCY_STOP'`, `fault_code = null`, `emergency_resolved_at = null`
  - `AMR_COMMAND` INSERT, `accepted = true`, `status = EXECUTED`, `requested_at`/`executed_at`
  - 진행 중 `AMR_TASK` → `CANCELLED` + `drop_time`
  - **commit 성공 후에만** HTTP 200 + `accepted: true`
- [x] `goTo`/`pause`/`resume`/`cancelTask`: 문법은 허용, 실행 시 400 (`emergencyStop`만 DB 반영).
- [x] Docker 스모크: 정지 전후 summary·`GET /amrs/{id}` 상태 변경 (2026-05-18, 테스트 AMR `amr-02`).

### 12-D. 운행 자동 복구 (시연) — 완료 (2026-05-18)

- [x] `application.yaml` / `application-docker.yaml`: `app.demo.recovery-seconds`(60), `recovery-check-interval-ms`(10000).
- [x] `AmrAutoRecoveryService` `@Scheduled`: 미해결 `ERROR`/`EMERGENCY_STOP`, `updated_at` 기준 경과 후 복구.
  - `EMERGENCY_STOP` → `emergency_resolved_at`, `status = IDLE`
  - `ERROR` → `fault_recovered_at`, `fault_code`/`fault_message` null, `status = IDLE`
- [x] 복구 후 `amrError`/`amrErrorUnresolved` 감소 Docker 스모크 (2026-05-18, 65초 대기 후 0 확인).
- [x] FE `resume` 미사용 (BE에서 차단하지 않음, `emergencyStop`만 실행).

### 12-E. AMR 목록·필터 (`GET /amrs`) — 완료 (2026-05-18)

- [x] `status` 쿼리: 콤마 구분 (`ERROR,EMERGENCY_STOP`) OR 조건.
- [x] `sort=unresolvedFirst`: 미해결 우선, 동일 시 `EMERGENCY_STOP` 우선, 그다음 이름.
- [x] 응답 `status`·`faultCode`: `AmrDto`·`buildAmrDto`와 설계 enum 정합.

### 12-F. 환경 API (SCR-01 ③) — **보류 (설계 재검토)**

- 실시간 환경(온도/습도/파티클)은 **FE ← MQTT ← DAS** 합의. BE REST `GET /environment/areas/current`는 `docs/API 정의.md`·화면 설계서와 **재정렬 후** 별도 이슈.
- [ ] (보류) `EnvironmentController` + `EnvironmentService`
- [ ] (보류) `ENV_SENSOR_LOG` 시드·집계
- 엔티티 `EnvSensor`/`EnvSensorLog`는 DB 마스터·향후 적재용으로 유지 가능.

### 12-G. Phase A 검증 체크리스트 — Docker 스모크 검증됨 (2026-05-18)

- [x] `docker compose up --build` 후 로그인.
- [x] `GET /dashboard/summary` → `amrError`, `amrErrorUnresolved` 존재.
- [x] `POST /amrs/amr-02/commands` `emergencyStop` → summary `amrError` 유지·증가 (체크리스트 예시는 `amr-01`과 동등).
- [x] 60초(설정값) 대기 → summary 에러 수치 감소, `GET /amrs/amr-02` status `IDLE` 복구.
- [x] `GET /amrs?status=ERROR,EMERGENCY_STOP&sort=unresolvedFirst` (에러 목록·정렬).

---

## Phase B — WebSocket 최소 (Phase A 이후)

`docs/API 정의.md` §8. FE는 당분간 REST 폴링(10초) 가능. BE→FE 스트림은 **JWT query `?token=`(A안)**.

### B-1. 연결·인증 (handshake) — 완료 (2026-05-19)

- [x] `WebSocketConfig`: `WS /api/v1/stream` (`context-path` `/api/v1` + 핸들러 `/stream`).
- [x] `WebSocketJwtHandshakeInterceptor`: query `token`으로 access JWT 검증 (`JwtUtil`·`UserDetailsService` 재사용).
- [x] `SecurityConfig`: `/stream` permitAll (handshake에서 JWT 거부).
- [x] `StreamWebSocketHandler` + `WebSocketSessionRegistry`: 세션 등록.
- [x] 연결 확인용 `stream.connected` 이벤트 1회 전송 (API §8 필수 이벤트 아님).
- [x] **Docker WS 스모크** (B-4): `scripts/smoke-websocket-phase-b.py`.

### B-2. 이벤트 발행기 — 완료 (2026-05-19)

- [x] `StreamEventDto`, `StreamEventPublisher`: 공통 봉투 `{ event, timestamp, data }`, `WebSocketSessionRegistry` 브로드캐스트.
- [x] `AmrStatusUpdatedEventDataDto`, `publishAmrStatusUpdated` / `publishDashboardSummaryUpdated`.
- [x] `StreamWebSocketHandler` → Publisher로 `stream.connected` 통일.

### B-3. 비즈니스 연동 — 완료 (2026-05-19)

- [x] **필수 발행 (시연)**
  - `amrs.status.updated` — `AmrService.sendCommand`·`AmrAutoRecoveryService` 복구
  - `dashboard.summary.updated` — `StreamNotificationService` afterCommit + `getSummary()` payload
- [x] `StreamNotificationService` + `TransactionSynchronizationManager.afterCommit` (commit 이후 WS 발행).
- [ ] **선택 발행**
  - `amrs.position.updated` — `@Scheduled` 3~5초마다 `pos_x`/`pos_y` 소폭 변경 (DAS 없을 때 연출)
  - `alarms.created`, `charging.forecast.updated` — 시연 후

### B-4. Phase B Docker 검증 체크리스트 — 완료 (2026-05-19)

- [x] `BE/`에서 `docker compose up -d --build` (코드 변경 시 `--no-cache` 권장).
- [x] `POST /auth/login` → `accessToken`.
- [x] `ws://localhost:8080/api/v1/stream?token=<accessToken>` → `stream.connected`.
- [x] 토큰 없음 → 연결 거부.
- [x] `emergencyStop`(amr-02) → `amrs.status.updated` + `dashboard.summary.updated`.
- [x] 65초 대기 → IDLE 복구 이벤트, `amrError`/`amrErrorUnresolved` 0.
- [x] 자동 실행: `python scripts/smoke-websocket-phase-b.py` (의존성: `pip install websocket-client`).

---

## Phase S — Swagger UI (OpenAPI) — **S-3 완료, S-4 대기**

목표: 기능 개발 중단 기간에 **현재 구현 REST API**를 브라우저에서 탐색·호출하고, `docs/API 정의.md`와의 차이를 팀이 확인할 수 있게 한다.

**범위:** REST 8개 컨트롤러 영역, JWT Bearer Try it out.  
**범위 외:** WebSocket `/stream`, DAS/MQTT, FE UI, 12-F·Phase C 신규 API.

### S-1. 의존성·설정 — 완료 (2026-05-19)

- [x] `build.gradle`: `springdoc-openapi-starter-webmvc-ui:2.8.8`
- [x] `OpenApiConfig`, `OpenApiDocumentationConstants`: API 메타, 서버 `http://localhost:8080/api/v1`, JWT Bearer `bearerAuth`
- [x] `SecurityConfig`: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` permitAll
- [x] `application.yaml` / `application-docker.yaml`: springdoc 경로
- [x] Docker: `GET /api/v1/v3/api-docs`·Swagger UI HTML 200 (S-2 Try it out·JWT는 다음)

### S-2. 노출·인증 검증 — 완료 (2026-05-19)

- [x] 컨트롤러 노출: OpenAPI 7 REST 그룹 (`auth`, `dashboard`, `amr`, `alarm`, `charging`, `work-history`, `analytics` controller 태그)
- [x] `POST /auth/login`·`/auth/refresh`: `@SecurityRequirements` (Swagger에서 잠금 없음) → 토큰 발급 후 **Authorize** → `GET /dashboard/summary` 등 200
- [x] 보호 API 무토큰 → **401** (`SecurityConfig` `HttpStatusEntryPoint(UNAUTHORIZED)`)
- [x] 자동 검증: `python scripts/smoke-swagger-phase-s.py` (Docker, `admin` / `demo123`)
- [x] `@Operation` 생략 — springdoc 자동 스캔 유지

### S-3. Docker·문서·회귀 — 완료 (2026-05-19)

- [x] `docker compose up --build` 후 Swagger UI·OpenAPI JSON HTTP 200
- [x] 회귀: `python scripts/smoke-swagger-phase-s.py` (REST·OpenAPI·JWT), `python scripts/smoke-websocket-phase-b.py` (WS) ALL PASSED
- [x] `BE/TODO.md` Docker 표·`BE/AGENTS.md` Swagger URL·JWT Try it out·회귀 명령 기록

### S-4. 완료 후

- [ ] 설계 재검토 이슈: API 명세·화면 설계서·FE/DAS MQTT 경계 정리
- [ ] Phase C·12-F 등 **재개 여부** 팀 합의

---

## Phase C — 시연 후·인프라 (보류: 설계 재검토 후)

### 7. 예외 처리 및 로깅

- [x] 전역 예외 처리 (`@RestControllerAdvice`) — 인증 최소 구현.
- [x] 커스텀 예외 (AmrNotFoundException 등).
- [ ] 공통 오류 응답 형식 통일 (`docs/API 정의.md` §9).

### Analytics KPI 확장

- [ ] `AnalyticsKpiDto`에 `errorCount`, `scheduleComplianceRate` 추가.
- [ ] `AnalyticsService.getKpis()`: 버킷별 `AMR_STATUS_LOG`·`AMR_TASK`/`PR_ROUTING` 집계 (`docs/API 정의.md` §7).

### 8. 테스트 및 검증

- [x] `DashboardStatusNormalizerTest`, `AmrServiceTest` (일부 Service 단위 테스트).
- [ ] dashboard 집계·auto-recovery 등 추가 Service 테스트.
- [ ] Controller 통합 테스트 (MockMvc).
- [ ] CI 또는 `docker compose` 스모크 스크립트 (REST Phase A·WS Phase B).

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
- [x] 멀티스테이지 `Dockerfile`: JDK 17 Alpine에서 `bootJar`, JRE 17 Alpine 런타임 + `curl`(healthcheck).
- [x] Phase B WebSocket 스모크 runbook (`scripts/smoke-websocket-phase-b.py`).
- [x] Phase S Swagger·REST 회귀 runbook (`scripts/smoke-swagger-phase-s.py`).
- [ ] 10-B MySQL merge 후 runbook 갱신.
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

### [완료] 12-D 운행 자동 복구 (2026-05-18)

- `AmrAutoRecoveryService`, `DemoRecoveryProperties`, `SchedulingConfig`. 시드 ERROR `updated_at` = 기동 시각(60초 후 복구).

### [완료] 12-E AMR 목록 필터·정렬 (2026-05-18)

- `GET /amrs`: `status` 콤마 OR, `sort=unresolvedFirst`, `AmrController` `sort` 파라미터.

### [완료] Phase A Docker 스모크 (2026-05-18)

- `BE`에서 `docker compose up -d --build` → 로그인 → summary·amrs 필터·`emergencyStop`(amr-02) → 65초 대기 자동 복구·추가 엔드포인트(alarms, charging, work-histories, analytics) HTTP 200.

### [완료] Phase B-1 WebSocket handshake (2026-05-19)

- `WebSocketConfig`, `WebSocketJwtHandshakeInterceptor`, `StreamWebSocketHandler`, `WebSocketSessionRegistry`, `SecurityConfig` (`/stream` + query `token`).
- 외부 URL: `ws://localhost:8080/api/v1/stream?token=<accessToken>`.

### [완료] Phase B-2·B-3 StreamEventPublisher 및 서비스 연동 (2026-05-19)

- `StreamEventPublisher`, `StreamEventDto`, `StreamNotificationService` (`afterCommit`).
- `emergencyStop`·자동 복구 후 `amrs.status.updated` → `dashboard.summary.updated`.

### [완료] Phase B-4 Docker WebSocket 스모크 (2026-05-19)

- `scripts/smoke-websocket-phase-b.py` ALL PASSED (H2 fresh: `docker compose down -v` 후 기동).

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
| `AMR_COMMAND` | DDL만 | **12-C 완료** |

### 11. AMR 비상 정지 — 12-C·12-D 완료, Phase B 잔여

- [x] `AmrCommand` 엔티티·Repository
- [x] `sendCommand` DB 트랜잭션 (`EMERGENCY_STOP`)
- [x] commit 후 `accepted: true`
- [x] 자동 복구(12-D)
- [x] (Phase B-1) WebSocket 연결·JWT handshake
- [x] (Phase B-2~3) `amrs.status.updated`·`dashboard.summary.updated` 발행
- [x] (Phase B-4) Docker WS 스모크

---

## 작업 우선순위 (BE 담당자용)

1. **Phase S-4** — 설계 재검토 이슈 (`docs/API 정의.md`·화면 설계서 vs BE·FE·DAS MQTT)
2. **Phase C** — analytics KPI, 공통 오류 응답 (재개 시)
3. **12-F** — 환경 API (재검토 후, FE MQTT와 중복 여부 확인)
4. **10-B** — MySQL (DB compose merge 후)

**한 번에 하나의 하위 태스크만** 진행한다 (`AGENTS.md` 규칙). 기능 API는 **설계 재검토 합의 후** 착수한다.

---

## Docker 실행 (BE 폴더)

로컬에 Java를 설치하지 않아도 된다. **이미지 빌드 단계**에서 `amr_control_system/Dockerfile`이 `./gradlew bootJar -x test`를 실행한다.

| 파일 | 역할 |
|------|------|
| `BE/docker-compose.yml` | 서비스 `be`, 포트 `8080:8080`, `SPRING_PROFILES_ACTIVE=docker`, `.env` 필수 |
| `BE/.env.example` | `JWT_SECRET`, 토큰 만료, `DEMO_USER_PASSWORD` 템플릿 |
| `BE/amr_control_system/Dockerfile` | multi-stage: JDK 17 빌드 → JRE 17 + `curl` |
| `BE/amr_control_system/src/main/resources/application-docker.yaml` | H2 in-memory, `app.demo` 복구 60초 |

```bash
cd BE
cp .env.example .env
# JWT_SECRET 설정 (아래 「다른 PC에서 JWT 키 갱신」)
docker compose up --build
```

코드 변경 후 반영: `docker compose up -d --build` (H2 시드 초기화가 필요하면 `docker compose down -v` 후 재기동).

| 용도 | URL |
|------|-----|
| Health | http://localhost:8080/api/v1/actuator/health |
| Login | POST http://localhost:8080/api/v1/auth/login |
| Summary | GET http://localhost:8080/api/v1/dashboard/summary |
| AMRs (에러 목록) | GET …/amrs?status=ERROR,EMERGENCY_STOP&sort=unresolvedFirst |
| AMRs (전체) | GET http://localhost:8080/api/v1/amrs |
| Emergency stop | POST http://localhost:8080/api/v1/amrs/{amrId}/commands |
| **WebSocket (B-1)** | `ws://localhost:8080/api/v1/stream?token=<accessToken>` |
| **Swagger UI** | http://localhost:8080/api/v1/swagger-ui/index.html |
| **OpenAPI JSON** | http://localhost:8080/api/v1/v3/api-docs |

**Swagger UI (Try it out, JWT):**

1. `POST /auth/login` — Body `{"username":"admin","password":"demo123"}` (**Authorize 없이** 실행).
2. 응답 `accessToken` 복사 → 상단 **Authorize** → 토큰만 붙여넣기 (`Bearer ` 접두사 없음).
3. `GET /dashboard/summary` 등 보호 API Try it out.

**Phase S 회귀 스모크 (REST + OpenAPI + JWT):**

```bash
cd BE
docker compose up -d --build
python scripts/smoke-swagger-phase-s.py
```

**Phase B WebSocket 스모크:**

```bash
cd BE
docker compose up -d --build
pip install websocket-client
python scripts/smoke-websocket-phase-b.py
```

(PowerShell 대안: `scripts/smoke-websocket-phase-b.ps1` — 수신은 백그라운드 스레드 필요, **Python 스크립트 권장**.)

**PR 전 권장:** 위 두 Python 스크립트 모두 ALL PASSED.

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

- `accepted: true`: **DB 반영 완료** (Phase A-12-C 구현됨).
- `emergencyStop` → `status = 'EMERGENCY_STOP'` (`ERROR`와 구분, `docs/ADR/20260518-1252-AMR-emergency-logic.md`).
- MQTT 토픽·payload: FE·DAS 이슈.

---

## 참고

- 설계 변경 시 `docs/` 먼저 수정 후 구현.
- 물리 스키마: `DB/init.sql`, `docs/데이터 스키마 설계.md`.
- 화면·API 매핑: `docs/화면 설계서.md`.
- PR 전: `docker compose up --build` + `python scripts/smoke-swagger-phase-s.py` + `python scripts/smoke-websocket-phase-b.py`.
