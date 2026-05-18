---
adr_id: "20260518-1252-AMR-emergency-logic"
status: "Proposed"
created_at: "2026-05-18"
author: "@dapin1490"
related_files:
  - "docs/프로젝트 정의서.md"
  - "docs/화면 설계서.md"
  - "docs/API 정의.md"
  - "docs/데이터 스키마 설계.md"
  - "DB/init.sql"
---

## 배경과 문제 (Context)

### 배경

- 본 프로젝트는 Node-RED(DAS)로 AMR 위치·작업을 **시뮬레이션**한다. 실제 AMR 설비 제어기와의 직접 연동은 범위에 포함하지 않는다.
- 비상 정지(Emergency Stop)는 `docs/화면 설계서.md` SCR-03 ⑤ 및 시연 시나리오에 포함된다.
- DAS와 프론트엔드(FE)는 **MQTT**로 연결된다. 백엔드(BE)는 REST API, WebSocket, DB(MySQL)를 담당한다.
- DAS·FE·BE 기능 구현 논의에서, 시뮬레이션 특성상 **운행 상태는 DB 데이터로 관리**하고, DAS 좌표 중단은 **FE가 MQTT로 고지**하는 흐름으로 합의하였다.
- 기존 `POST /api/v1/amrs/{amrId}/commands` 및 `AMR_COMMAND` 테이블(`DB/init.sql`)은 존재하나, BE↔DAS 경계와 `accepted` 의미가 문서에 명시되어 있지 않았다.

### 해결할 문제

- 시뮬레이션 환경에서 AMR 비상 정지 시, FE·BE·DB·DAS 각 컴포넌트의 **책임 경계와 처리 순서**를 무엇으로 고정할 것인가?

## 최종 결정 (Decision)

- 영역: `Frontend`, `Backend`, `Database` (DAS 연동 규칙은 FE·DAS가 MQTT 명세로 보완)
- 최종 선택
  1. **운행 상태의 기준 저장소는 DB(MySQL)** 이다. 관제·이력·REST 조회는 BE가 DB를 통해 제공한다.
  2. **비상 정지 처리 순서**
     - FE → BE: `POST /api/v1/amrs/{amrId}/commands` (`emergencyStop` 등)
     - BE: 단일 트랜잭션으로 DB 갱신
       - `AMR_COMMAND` INSERT (`command_type`, `accepted`, `status`, `requested_at` 등)
       - `AMR_STATUS_LOG` 운행 상태 반영(기존 행 UPDATE 또는 신규 INSERT). `emergencyStop` 시 `status = 'EMERGENCY_STOP'`, `emergency_resolved_at = NULL`
       - (정책에 따라) 진행 중 `AMR_TASK` 상태 정리
     - BE → FE: DB **commit 성공 후** HTTP 200, `accepted: true`, `commandId`, `amrId`
     - FE → DAS: **MQTT**로 해당 AMR 정지 고지 → 좌표 갱신·작업 시뮬 중단
     - BE → FE (WebSocket 구현 시): `amrs.status.updated` 등으로 UI 실시간 반영
  3. **BE는 DAS/MQTT에 직접 접속하지 않는다.** DAS로의 정지 전달은 FE 책임이다.
  4. **`accepted: true` 의미**: 명령이 큐에만 들어갔음이 아니라, **DB에 명령·운행 상태가 반영되었음**을 뜻한다. DAS 시뮬 중단 완료는 보장하지 않는다.
  5. **시연: 운행 자동 복구** — FE는 `resume` 명령을 보내지 않는다. `EMERGENCY_STOP`·`ERROR` 진입 후 BE가 **일정 시간 경과 시** `emergency_resolved_at`·`fault_recovered_at`를 채우고 `status`를 `IDLE`(또는 `OPERATING`)로 바꾼다. 작업자 현장 복구를 전제한 시뮬레이션이며, 복구 후 대시보드 `amrError`·`amrErrorUnresolved`가 감소한다. 비상 정지 직후에는 두 수치를 유지한다.
- 버전/규칙
  - API: `docs/API 정의.md` §3 AMR, §8 WebSocket
  - 스키마: `docs/데이터 스키마 설계.md`, 물리 DDL `DB/init.sql`
  - 지원 command 문자열: `goTo`, `pause`, `resume`, `cancelTask`, `emergencyStop` (기존 API 명세 유지)
- 선택 이유
  - 시연·소규모 팀에서 BE-DAS 직접 연동을 생략하면 구현·운영 비용이 낮다.
  - FE가 이미 DAS와 MQTT로 연결되므로, 정지 신호를 FE가 전달하는 것이 자연스럽다.
  - DB를 운행 기준으로 두면 REST·대시보드·이력·WebSocket이 한 소스를 바라볼 수 있다.
  - 현업형 설비 제어 경로를 만들지 않아도 프로젝트 목표(통합 관제 시연)를 달성할 수 있다.

## 대안과 트레이드오프

| 옵션 | 장점 | 단점 | 선택 여부 |
| --- | --- | --- | --- |
| **A. FE→BE(DB)→FE→DAS(MQTT)** (채택) | 역할 분리 명확, BE 단순, FE·DAS 병렬 개발 용이 | FE가 정지 후 MQTT 발행 책임, 순서 어긋나면 잠깐 좌표가 더 움직일 수 있음 | **채택** |
| B. FE→BE→BE가 DAS에 MQTT/HTTP 직접 전달 | 단일 백엔드 경로, FE 단순 | BE에 DAS 결합, Node-RED 연동·장애 처리 부담, 시연 범위 확대 | 기각 |
| C. `accepted`만 즉시 반환, DB 반영은 비동기 | HTTP 응답 빠름 | 시뮬레이션에서 DB·UI·DAS 상태 불일치 위험 | 기각 |
| D. DB 미갱신, MQTT만으로 정지 | 구현 최소 | REST·이력·관제와 DAS 상태 불일치 | 기각 |

## 기대 효과와 리스크 (Consequences)

### 기대 효과

- FE·BE·DB·DAS 구현자가 동일한 시퀀스 다이어그램을 기준으로 작업할 수 있다.
- WebSocket·시연 시나리오(정지 후 상태 아이콘 변경)와 REST API 의미가 정렬된다.
- `AMR_COMMAND`, `AMR_STATUS_LOG` 테이블의 목적이 명확해진다.

### 리스크 및 대응

- 리스크: FE가 `accepted` 수신 전에 DAS에 MQTT를내면, 짧은 시간 DB와 DAS가 어긋난다.
  - 대응: API 명세에 순서 명시. FE는 `accepted === true` 이후에만 MQTT publish.
- 리스크: `ERROR`와 `EMERGENCY_STOP`을 혼동하면 대시보드·지도 표시가 어긋난다.
  - 대응: `emergencyStop`은 `EMERGENCY_STOP`만 사용. 자체 고장은 `ERROR` + `fault_code` (`docs/API 정의.md` §3).
- 리스크: MQTT 토픽·payload 미정의.
  - 대응: FE·DAS 담당 이슈에서 명세 작성. ADR은 경계만 고정.

## 미해결 이슈

- [x] `emergencyStop` 시 `AMR_STATUS_LOG.status`: `EMERGENCY_STOP` (고장 `ERROR`와 구분)
- [x] 시연 복구: FE `resume` 없음, BE 자동 복구·수치 감소 (`docs/API 정의.md` §3)
- [ ] 자동 복구 대기 시간(초) BE 설정값 확정
- [ ] `AMR_COMMAND.status` 초기값·전이 규칙 (예: INSERT 시 `EXECUTED`, `accepted=true`)
- [ ] FE → DAS **MQTT 토픽·payload** 명세 (FE·DAS 영역 문서)
- [ ] `goTo`, `pause` 등 비상 정지 외 command의 DB 반영 범위 (시연 범위에 포함 여부)

## 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
| --- | --- | --- | --- |
| v0.1 | 2026-05-18 | 최초 작성 (DAS·FE·BE 합의 반영) | @dapin1490 |
| v0.2 | 2026-05-18 | `EMERGENCY_STOP`·대시보드 에러/미해결 집계·화면 설계서 연계 | @dapin1490 |
| v0.3 | 2026-05-18 | 시연 자동 복구(FE resume 없음)·에러 목록 정렬 | @dapin1490 |
