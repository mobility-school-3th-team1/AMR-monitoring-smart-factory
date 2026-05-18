# RESTful API 명세서

- 기본 경로: /api/v1
- 인증 방식: Authorization: Bearer <JWT>
- 응답 형식: application/json
- 공통 규칙:
    - 목록 API는 기본적으로 page=1, limit=20을 사용한다.
    - 날짜와 시간은 ISO 8601 UTC를 사용한다.
    - 리소스명은 복수형을 우선한다.
    - 모든 변경 API는 인증이 필요하다.

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

응답 예시:

```json
{
    "productionCount": 156,
    "activeAlarms": 3,
    "amrOperating": 9,
    "amrCharging": 11,
    "amrWaiting": 2,
    "avgBatteryPercent": 67,
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

## 3. AMR(AMR Fleet)

### GET /amrs

설명: AMR 목록 조회

쿼리 파라미터: page, limit, status, batteryMin, batteryMax, search

응답 예시:

```json
{
    "data": [
        {
            "id": "amr-01",
            "name": "AMR-01",
            "status": "charging",
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

처리 순서 (`emergencyStop` 포함):

1. 인증 및 `amrId` 유효성 검증
2. DB 트랜잭션: `AMR_COMMAND` INSERT, `AMR_STATUS_LOG`(및 정책에 따른 `AMR_TASK`) 갱신
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

설명: 기간별 KPI 집계

쿼리 파라미터: from, to, groupBy=hour|day

### GET /analytics/battery

설명: 배터리 시계열 데이터 조회

쿼리 파라미터: amrId, stationId, from, to, groupBy=hour|day

### GET /analytics/workload

설명: 작업 건수 및 비중 통계 조회

쿼리 파라미터: from, to, groupBy=amr|taskType|hour

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
        "status": "charging"
    }
}
```

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
- 비상 정지 전체 흐름은 `docs/프로젝트 정의서.md`, `docs/ADR/20260518-1252-AMR-emergency-logic.md`를 따른다.