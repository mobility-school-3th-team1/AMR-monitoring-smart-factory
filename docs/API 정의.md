# RESTful API 명세서

- 기본 경로: /api/v1
- 인증 방식: Authorization: Bearer <JWT>
- 응답 형식: application/json
- 공통 규칙:
    - 목록 API는 기본적으로 page=1, limit=20을 사용한다.
    - 날짜와 시간은 ISO 8601 UTC를 사용한다.
    - 리소스명은 복수형을 우선한다.
    - 모든 변경 API는 인증이 필요하다.

## FE 녹화 시연 (2026-05-19)

상세·화면 매핑·UI 삭제는 `docs/시연_MVP_합의.md`를 따른다. **시연 구현 시 본 절과 MVP 문서가 우선**하며, 아래 본문 전체 명세와 충돌하는 API는 시연에서 호출하지 않는다.

### 시연 필수 REST (BE)

| 메서드·경로 | 화면 |
| --- | --- |
| `POST /auth/login` | 로그인 |
| `GET /dashboard/summary` | SCR-01 (UI: `productionCount`·`activeAlarms`·`avgBatteryPercent` **미표시**) |
| `GET /dashboard/recent-logs` | SCR-01 |
| `GET /amrs` | SCR-01, SCR-02 |
| `GET /amrs/{amrId}` | SCR-03 |
| `POST /amrs/{amrId}/commands` | SCR-03 (`emergencyStop`) |
| `GET /charging/stations` | SCR-04 |
| `GET /charging/forecast` | SCR-04 |
| `GET /work-histories` | SCR-05 |
| `GET /analytics/workload` | SCR-05 |

### 시연에서 호출하지 않음

| 메서드·경로 | 사유 |
| --- | --- |
| `GET /environment/areas/current` | SCR-01 환경 → **DAS MQTT** (`docs/FE-DAS_MQTT_연동.md`) |
| `GET /charging/queue` | SCR-04 대기열 **빈 테이블 더미** |
| `GET /analytics/kpis` | SCR-02/03 차트 보류 |
| `GET /dashboard/recent-alarms` | SCR-01 활성 알람 KPI 삭제 (알람 목록 유지 시만 선택) |

### 시연 실시간 (비-REST)

- AMR **x/y 백분율**, 구역별 환경 `sensor1`~`sensor4`: **DAS → MQTT → FE**. 명세는 `docs/FE-DAS_MQTT_연동.md`.
- FE **WebSocket** (`/api/v1/stream`): 시연 **미사용**.

---

## 1. 인증(Auth)

### POST /auth/login

설명: 사용자 인증 및 토큰 발급

요청 예시:

```json
{
    "username": "admin",
    "password": "demo123"
}
```

성공 응답 예시:

```json
{
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
        "id": "user-001",
        "username": "admin",
        "displayName": "관리자",
        "role": "admin"
    }
}
```

실패 응답: 401 아이디 또는 비밀번호 불일치

### POST /auth/refresh

설명: refresh token으로 access token 재발급

요청 예시:

```json
{
    "refreshToken": "eyJhbGciOi..."
}
```

성공 응답 예시:

```json
{
    "accessToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

### POST /auth/logout

설명: 세션 종료 및 refresh token 무효화

응답: 204 No Content

## 2. 대시보드(Dashboard)

### GET /dashboard/summary

설명: 메인 대시보드 KPI 요약 조회

- `amrOperating` / `amrWaiting` / `amrCharging` / `amrError` / `amrErrorUnresolved`: AMR별 **최신** `AMR_STATUS_LOG` 기준 집계(저장 컬럼 아님).
- `amrWaiting`: `status = IDLE`.
- `amrError`: `status IN ('ERROR', 'EMERGENCY_STOP')`.
- `amrErrorUnresolved`: 미복구 `ERROR` + 비상 정지 조치가 필요한 `EMERGENCY_STOP` (아래 집계 규칙).
- 가동률(`utilizationRate`)은 본 API 범위에서 제외한다. 필요 시 `§7 Analytics`에서 제공한다.

**`amrErrorUnresolved` 집계 규칙**

- 미복구 ERROR: `status = 'ERROR'` AND `fault_recovered_at IS NULL`
- 비상 정지 조치 필요: `status = 'EMERGENCY_STOP'` AND `emergency_resolved_at IS NULL`

**에러 수량 변화 (시연)**

- `emergencyStop` 직후: `status = 'EMERGENCY_STOP'` → `amrError`·`amrErrorUnresolved` **유지·증가**
- **자동 복구**(아래 §3) 완료 후: `status`가 `IDLE` 또는 `OPERATING` 등으로 바뀌면 `amrError`·`amrErrorUnresolved` **감소** (복구 시각 필드가 채워지고 위험 상태에서 벗어남)

응답 예시:

```json
{
    "productionCount": 156,
    "activeAlarms": 3,
    "amrOperating": 9,
    "amrWaiting": 2,
    "amrCharging": 11,
    "amrError": 3,
    "amrErrorUnresolved": 2,
    "averageTaskTimeMin": 9.3
}
```

### GET /dashboard/recent-alarms

설명: 최신 알람 요약 조회

쿼리 파라미터: limit

응답 예시:

```json
{
    "data": [
        {
            "id": "alarm-001",
            "level": "warning",
            "message": "충전 스테이션 3 혼잡 상태",
            "occurredAt": "2026-05-13T14:29:00Z"
        }
    ]
}
```

### GET /dashboard/recent-logs

설명: 최근 운영 로그 조회

쿼리 파라미터: page, limit

### GET /environment/areas/current

설명: 구역별 최신 환경 센서값 조회 (메인 대시보드 SCR-01 ③)

쿼리 파라미터: areaId (선택, 미지정 시 전체 구역)

데이터 출처: `ENV_SENSOR`, `ENV_SENSOR_LOG`(센서별 최신 `measured_at` 1건). BE는 DB 집계만 수행한다.

응답 예시:

```json
{
    "data": [
        {
            "areaId": "AREA_ASSEMBLE_01",
            "areaName": "조립 구역 1",
            "readings": [
                {
                    "sensorType": "TEMP",
                    "sensorName": "온도 센서 2호",
                    "value": 24.5,
                    "unit": "°C",
                    "status": "normal",
                    "measuredAt": "2026-05-13T08:20:00Z"
                },
                {
                    "sensorType": "HUMIDITY",
                    "sensorName": "습도 센서 2호",
                    "value": 48.0,
                    "unit": "%",
                    "status": "normal",
                    "measuredAt": "2026-05-13T08:20:00Z"
                },
                {
                    "sensorType": "PARTICLE",
                    "sensorName": "파티클 센서 2호",
                    "value": 42.0,
                    "unit": "ug/m3",
                    "status": "normal",
                    "measuredAt": "2026-05-13T08:20:00Z"
                }
            ]
        }
    ]
}
```

## 3. AMR(AMR Fleet)

### GET /amrs

설명: AMR 목록 조회

쿼리 파라미터: page, limit, status, batteryMin, batteryMax, search, sort

- `status`: 단일 또는 콤마 구분. 허용 값은 `OPERATING`, `IDLE`, `CHARGING`, `ERROR`, `EMERGENCY_STOP`이며, 그 외 값은 `400 Bad Request`. 에러 목록(SCR-01 ④ 클릭) 예: `status=ERROR,EMERGENCY_STOP`
- `sort=unresolvedFirst` (시연·SCR-01 ④): **미해결**(`fault_recovered_at`·`emergency_resolved_at` NULL) 우선, 동일 시 `EMERGENCY_STOP` 우선, 그다음 `ERROR`

응답 예시:

```json
{
    "data": [
        {
            "id": "amr-01",
            "name": "AMR-01",
            "status": "CHARGING",
            "faultCode": null,
            "batteryPercent": 86,
            "position": { "zone": "Zone A", "x": 123.4, "y": 56.7 },
            "destination": { "zone": "Zone B", "x": 140.1, "y": 70.5 },
            "currentTask": "부품 운반",
            "lastSeenAt": "2026-05-13T08:23:12Z"
        }
    ],
    "total": 12,
    "page": 1,
    "limit": 20
}
```

### GET /amrs/{amrId}

설명: AMR 상세 조회

응답 예시:

```json
{
    "id": "amr-01",
    "name": "AMR-01",
    "status": "ERROR",
    "faultCode": "SENSOR_FAULT",
    "faultMessage": "LiDAR data invalid",
    "batteryPercent": 72,
    "position": { "zone": "Zone A", "x": 123.4, "y": 56.7 },
    "loadWeightKg": 48,
    "sohPercent": 96,
    "totalMileageKm": 1205.3,
    "lastSeenAt": "2026-05-13T08:23:12Z"
}
```

### AMR `status` 및 `faultCode`

| status | 설명 |
| --- | --- |
| `OPERATING`, `IDLE`, `CHARGING` | 정상 운행 분류 |
| `ERROR` | AMR 자체 진단 고장. `faultCode` 필수 |
| `EMERGENCY_STOP` | `emergencyStop` 명령 DB 반영. `faultCode`는 null |

`faultCode` (`ERROR` 시, AMR 온보드 진단 가능 범위): `SENSOR_FAULT`, `COMM_LOST`, `COLLISION`, `OVERLOAD`, `LOAD_IMBALANCE`, `DRIVE_FAULT`, `NAVIGATION_FAULT`.

환경·공장 사고(먼지 초과 등)는 구역 `ENV_SENSOR` 및 `ALARM_LOG`로 처리하며 AMR `faultCode`가 아니다.

### GET /amrs/{amrId}/status-history

설명: 특정 AMR 상태 이력 조회

쿼리 파라미터: from, to, groupBy=minute|hour|day

### GET /amrs/{amrId}/path

설명: 특정 AMR 이동 경로 조회

쿼리 파라미터: from, to

응답 예시:

```json
{
    "amrId": "amr-01",
    "path": [
        { "time": "2026-05-13T08:00:00Z", "x": 1.2, "y": 3.4, "zone": "A-1" }
    ]
}
```

### POST /amrs/{amrId}/commands

설명: AMR 제어 명령 전송. **시뮬레이션 환경**에서 운행 상태는 DB에 반영되며, DAS(좌표·작업 시뮬) 제어는 FE가 MQTT로 수행한다. **BE는 DAS에 명령을 전달하지 않는다.** (아키텍처: `docs/ADR/20260518-1252-AMR-emergency-logic.md`)

지원 명령: goTo, pause, resume, cancelTask, emergencyStop

**시연 범위: 운행 자동 복구 (`resume` FE 트리거 없음)**

- 현장에서는 작업자가 AMR을 직접 복구한 뒤 AMR이 정상 운행 신호를 다시 내는 것으로 이해한다. **시연에서는 FE가 `resume` 명령을 보내지 않는다.**
- BE(또는 시뮬레이션 동기화)는 `ERROR`·`EMERGENCY_STOP` 진입 후 **일정 시간이 지나면 자동 복구**한다. 실제 정비 완료 여부는 검증하지 않으며, **대시보드 수치 변화**가 목적이다.
- 자동 복구 시(인간 작업자 복구 완료를 전제한 시뮬레이션):
  - `EMERGENCY_STOP` → `status = 'IDLE'`(또는 `OPERATING`), `emergency_resolved_at = now`
  - `ERROR` → `status = 'IDLE'`(또는 `OPERATING`), `fault_recovered_at = now`, `fault_code`는 null
  - (WebSocket 구현 시) `amrs.status.updated`, `dashboard.summary.updated` 발행
- 자동 복구 대기 시간(예: 60초)은 BE 설정값으로 두며, 시연 시나리오에 맞게 조정한다.
- `resume` HTTP 명령은 API에 유지할 수 있으나 **시연 필수 경로는 아니다.**

처리 순서 (`emergencyStop` 포함):

1. 인증 및 `amrId` 유효성 검증
2. DB 트랜잭션: `AMR_COMMAND` INSERT, `AMR_STATUS_LOG`(및 정책에 따른 `AMR_TASK`) 갱신. `emergencyStop` 시 `AMR_STATUS_LOG.status = 'EMERGENCY_STOP'`, `fault_code`는 null, `emergency_resolved_at`는 null
3. commit 성공 시 HTTP 200 및 `accepted: true` 반환
4. commit 실패 시 4xx/5xx (본문에 `accepted: true`를 내리지 않음)
5. FE는 `accepted: true` 수신 **이후** DAS에 MQTT 정지 고지
6. (WebSocket 사용 시) BE는 `amrs.status.updated` 이벤트 발행

`accepted` 의미: 명령이 **DB에 반영되었음**을 뜻한다. DAS 시뮬 중단 완료를 보장하지는 않는다.

요청 예시:

```json
{
    "command": "emergencyStop",
    "params": {}
}
```

성공 응답: 200 OK

```json
{
    "accepted": true,
    "commandId": "cmd-123",
    "amrId": "amr-01"
}
```

실패 예시: 400 지원하지 않는 command, 404 AMR 없음, 401/403 인증·권한 오류

## 4. 충전 관리(Charging)

### GET /charging/stations

설명: 충전 스테이션 목록 및 상태 조회

응답 예시:

```json
{
    "data": [
        {
            "id": "station-1",
            "name": "충전 스테이션 1",
            "location": "원자재 창고",
            "status": "normal",
            "capacity": 4,
            "occupiedCount": 3,
            "averageBatteryPercent": 61,
            "estimatedFullChargeAt": "2026-05-13T10:20:00Z"
        }
    ]
}
```

### GET /charging/stations/{stationId}

설명: 특정 충전 스테이션 상세 조회

### GET /charging/queue

설명: 충전 대기열 조회

쿼리 파라미터: stationId, status

### GET /charging/forecast

설명: 충전 완료 예상 차트 데이터 조회

쿼리 파라미터: stationId, from, to, groupBy=30m|1h

응답 예시:

```json
{
    "data": [
        { "bucket": "0-30m", "count": 3 },
        { "bucket": "30-60m", "count": 4 },
        { "bucket": "60m+", "count": 4 }
    ]
}
```

### GET /charging/history

설명: 충전 이력 및 배터리 추이 조회

쿼리 파라미터: amrId, stationId, from, to, groupBy=hour|day

## 5. 알람(Alarms)

### GET /alarms

설명: 알람 목록 조회

쿼리 파라미터: level, acknowledged, sourceType, sourceId, from, to, page, limit

응답 예시:

```json
{
    "data": [
        {
            "id": "alarm-001",
            "sourceType": "station",
            "sourceId": "station-3",
            "level": "warning",
            "message": "충전 스테이션 3 혼잡 상태",
            "occurredAt": "2026-05-13T14:29:00Z",
            "acknowledged": false
        }
    ],
    "total": 42
}
```

### GET /alarms/{alarmId}

설명: 알람 상세 조회

### POST /alarms/{alarmId}/ack

설명: 알람 확인 처리

요청 예시:

```json
{
    "note": "현장 확인 완료"
}
```

응답 예시:

```json
{
    "acknowledged": true,
    "ackBy": "operator-01",
    "ackAt": "2026-05-13T14:35:00Z"
}
```

### POST /alarms

설명: 외부 시스템 알람 생성

## 6. 작업 이력(Work History)

### GET /work-histories

설명: 작업 이력 목록 조회

쿼리 파라미터: amrId, taskType, from, to, result, page, limit

응답 예시:

```json
{
    "data": [
        {
            "id": "wh-001",
            "amrId": "amr-01",
            "taskType": "transport",
            "startTime": "2026-05-12T09:00:00Z",
            "endTime": "2026-05-12T09:15:00Z",
            "from": "A 라인",
            "to": "조립 라인",
            "result": "success"
        }
    ],
    "total": 200
}
```

### GET /work-histories/{workHistoryId}

설명: 작업 이력 상세 조회

### GET /work-histories/{workHistoryId}/export

설명: 단건 작업 이력 파일 다운로드

응답: Content-Disposition attachment

### GET /work-histories/export

설명: 조건에 맞는 작업 이력 일괄 다운로드

쿼리 파라미터: amrId, from, to, taskType

## 7. 분석(Analytics)

### GET /analytics/kpis

설명: 기간별 KPI 집계. SCR-02 ③④(일별 오류·시간 준수율), SCR-03 ⑦(시간 준수율) 차트에 사용한다.

쿼리 파라미터: from, to, groupBy=hour|day (`groupBy=day` 권장: 일별 오류·준수율 차트)

버킷별 추가 필드:

- `errorCount`: 해당 기간에 `AMR_STATUS_LOG.status = 'ERROR'`로 기록된 건수(일별·시간별 버킷). `EMERGENCY_STOP`은 포함하지 않는다.
- `scheduleComplianceRate`: 해당 기간에 **완료**된 `AMR_TASK`(`pick_time`, `drop_time` 존재) 중, 실제 소요(분) ≤ 연결 `PR_ROUTING.standard_lead_time`(분)인 비율(0~100). `AMR_TASK`·`WIP_LOT`·`PR_ROUTING` 조인으로 산출한다.

응답 예시 (`groupBy=day`):

```json
{
    "data": [
        {
            "timestamp": "2026-05-12T00:00:00Z",
            "productionCount": 150,
            "activeAlarms": 2,
            "amrOperating": 8,
            "amrWaiting": 2,
            "amrCharging": 10,
            "avgBatteryPercent": 65,
            "averageTaskTimeMin": 9.1,
            "errorCount": 3,
            "scheduleComplianceRate": 88.5
        },
        {
            "timestamp": "2026-05-13T00:00:00Z",
            "productionCount": 156,
            "activeAlarms": 3,
            "amrOperating": 9,
            "amrWaiting": 2,
            "amrCharging": 11,
            "avgBatteryPercent": 67,
            "averageTaskTimeMin": 9.3,
            "errorCount": 1,
            "scheduleComplianceRate": 91.0
        }
    ]
}
```

### GET /analytics/battery

설명: 배터리 시계열 데이터 조회

쿼리 파라미터: amrId, stationId, from, to, groupBy=hour|day

### GET /analytics/workload

설명: 작업 건수 및 비중 통계 조회 (SCR-05 ②③)

쿼리 파라미터: from, to, groupBy=amr|taskType|hour

- `groupBy=hour`: 시간대별 작업 건수(SCR-05 ②)
- `groupBy=amr`: AMR별 작업 건수·비중(SCR-05 ③)

데이터 출처: `AMR_TASK`(`pick_time` 기준 집계).

응답 예시 (`groupBy=hour`):

```json
{
    "data": [
        { "timestamp": "2026-05-13T08:00:00Z", "taskCount": 5 },
        { "timestamp": "2026-05-13T09:00:00Z", "taskCount": 8 }
    ]
}
```

## 8. 실시간 스트리밍(WebSocket)

### WS /api/v1/stream

설명: 실시간 위치, 상태, 알람 이벤트 수신

인증: 연결 시 JWT 전달

- AMR 제어 명령(특히 `emergencyStop`)으로 DB 상태가 변경된 경우 `amrs.status.updated`를 발행하여, FE가 REST 재조회 없이 UI를 갱신할 수 있다.
- DAS 좌표·작업 중단은 FE가 MQTT로 수행하며, WebSocket은 BE→FE 방향이다.

이벤트 예시:

- amrs.position.updated
- amrs.status.updated
- alarms.created
- charging.forecast.updated
- dashboard.summary.updated

이벤트 payload 예시:

```json
{
    "event": "amrs.status.updated",
    "timestamp": "2026-05-13T08:23:12Z",
    "data": {
        "amrId": "amr-01",
        "status": "EMERGENCY_STOP",
        "faultCode": null
    }
}
```

`dashboard.summary.updated` 이벤트 payload에는 `amrError`, `amrErrorUnresolved` 등 `GET /dashboard/summary`와 동일 키를 포함할 수 있다.

## 9. 공통 응답 코드

- 200 OK
- 201 Created
- 202 Accepted
- 204 No Content
- 400 Bad Request
- 401 Unauthorized
- 403 Forbidden
- 404 Not Found
- 409 Conflict
- 422 Unprocessable Entity
- 500 Internal Server Error

## 10. 구현 권고

- 화면 데이터는 페이지 단위로 분리된 REST API에서 가져온다.
- 실시간 화면은 WebSocket 이벤트와 REST 조회를 혼합한다.
- 명령 API: HTTP 응답의 `accepted`는 **DB 반영 완료**를 의미한다. DAS 시뮬 반영은 FE→MQTT 경로이며, 그 결과는 WebSocket 이벤트 또는 이후 REST 조회로 확인한다.
- 비상 정지 전체 흐름은 `docs/화면 설계서.md`(SCR-03), `docs/ADR/20260518-1252-AMR-emergency-logic.md`를 따른다.

**녹화 시연 MVP:** REST·MQTT 범위는 본 문서 상단 「FE 녹화 시연」절 및 `docs/시연_MVP_합의.md` §5. FE WebSocket 구독은 시연에서 생략한다.
