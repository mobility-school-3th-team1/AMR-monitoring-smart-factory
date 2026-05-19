# FE ↔ DAS MQTT 연동 (녹화 시연)

**작성일:** 2026-05-19  
**상태:** Phase 0 확정 (2026-05-19) — 토픽·URL은 로컬 기본값, 배포 시 `.env`로 조정  
**상위 문서:** `docs/시연_MVP_합의.md` §2

---

## Phase 0 확정 요약

| ID | 결정 |
| --- | --- |
| 0-1 | 브로커: 개발 기본 `ws://localhost:9001` (브라우저 WebSocket MQTT). `VITE_MQTT_URL` |
| 0-2 | 환경: **단일 토픽**, 4구역 전체 payload, **JSON 문자열** 1회 발행 |
| 0-3 | AMR 좌표: **단일 토픽**, 전 AMR 배열, **JSON 문자열** 발행 |
| 0-4 | 구역 마스터: `DB/init.sql` **`AREA`** (`area_id`, `area_name`, 임계값). UI 오버레이 위치는 §2.3 |
| 0-5 | 좌표 % 기준: 팀 공유 **공장 레이아웃 원본 이미지** (FE 정적 에셋). 픽셀·논리 40×50 상수 **사용 안 함** |
| 0-6 | SCR-01 알람 **목록 유지** (`GET /dashboard/recent-alarms`). 실패 시 **더미 목록** |
| 0-7 | 비상 정지 **녹화 포함**. FE 화면만 녹화 가능하면 됨(`POST`+UI). DAS MQTT publish **권장·미연결 시에도 녹화 가능** |

---

## 1. 역할

| 방향 | 내용 |
| --- | --- |
| DAS → FE | 환경(4구역·구역당 sensor1~4), AMR 좌표(x/y **%)** |
| FE → DAS | `emergencyStop` (`accepted: true` 이후) |
| BE | MQTT 미참여. 운행·명령·알람·집계는 REST |

---

## 2. 좌표계·레이아웃 (0-5)

| 항목 | 규칙 |
| --- | --- |
| 이미지 | 팀 공유 원본 → `FE/frontend/src/assets/` (파일명은 FE 구현 시 확정, 예: `factory-layout.png`) |
| AMR `x`, `y` | **0~100**, 공장 이미지 **표시 영역** 기준 백분율 (가로·세로 각각) |
| FE 배치 | `left: x%`, `top: y%` (이미지 `object-fit`·컨테이너와 동일 비율 유지) |
| DAS | 이미지 픽셀 크기 불필요. **%만** 발행 |

---

## 2.3 구역 마스터·UI 매핑 (0-4)

### DB 참조 (`DB/init.sql`)

시연 4구역 (`AREA`):

| `area_id` | `area_name` |
| --- | --- |
| `AREA_LOAD_LC` | Lower Case 로딩 구역 |
| `AREA_ASSEMBLE_01` | 조립 구역 1 |
| `AREA_ASSEMBLE_02` | 조립 구역 2 |
| `AREA_OUT_BSA` | BSA 출고 구역 |

- 임계값(`temp_min`/`temp_max`, `humidity_*`, `particle_*`, `co_gas_*`)은 상태 색·경고에 사용 가능.
- `ENV_SENSOR`는 구역당 TEMP/HUMIDITY/PARTICLE/GAS 4행. MQTT `sensor1`~`sensor4`는 **시연용 논리 슬롯**(DAS 시뮬). DB 행과 1:1이 아님.

### FE 평면도 오버레이 (DB에 없음)

`AREA`에는 **이미지 위 위치(%)** 가 없다. FE는 구현 시 `area_id`별 오버레이 사각형을 **레이아웃 이미지에 맞춰 1회 정의**한다 (예: `FE/frontend/src/config/factory-layout-areas.js`).

| `area_id` | `overlay` (예, 팀이 이미지에 맞게 조정) |
| --- | --- |
| `AREA_LOAD_LC` | `{ "left": 5, "top": 6, "width": 20, "height": 30 }` |
| `AREA_ASSEMBLE_01` | `{ "left": 30, "top": 6, "width": 25, "height": 24 }` |
| … | … |

환경 수치 MQTT는 `area_id` 키로 매칭한다.

---

## 3. MQTT 공통 규칙

- Payload는 **UTF-8 JSON을 직렬화한 문자열** (`JSON.stringify` 후 publish).
- FE 수신: `message.toString()` → `JSON.parse`.
- 발행 주기(녹화): 환경 **5초**, AMR 좌표 **2초** (DAS, 녹화용 안정 값).

---

## 4. 환경 센서 (0-2)

| 항목 | 값 |
| --- | --- |
| **토픽 (DAS publish)** | `factory/environment/current` |
| **FE** | `subscribe('factory/environment/current')` |

### Payload (객체 구조 → 문자열로 발행)

```json
{
  "timestamp": "2026-05-19T08:00:00Z",
  "areas": {
    "AREA_LOAD_LC": {
      "sensor1": { "temp": 25, "humid": 33, "particle": 22, "cogas": 5 },
      "sensor2": { "temp": 24, "humid": 32, "particle": 21, "cogas": 4 },
      "sensor3": { "temp": 26, "humid": 34, "particle": 23, "cogas": 6 },
      "sensor4": { "temp": 25, "humid": 33, "particle": 22, "cogas": 5 }
    },
    "AREA_ASSEMBLE_01": { "sensor1": { }, "sensor2": { }, "sensor3": { }, "sensor4": { } },
    "AREA_ASSEMBLE_02": { },
    "AREA_OUT_BSA": { }
  }
}
```

| 필드 | 의미 |
| --- | --- |
| `temp` | 온도 |
| `humid` | 습도(%) |
| `particle` | 미세먼지 |
| `cogas` | CO 가스 |

- **4구역 × 구역당 4슬롯 = 화면 16칸** (슬롯당 4수치 표시 또는 UI 디자인에 따름).

---

## 5. AMR 좌표 (0-3)

| 항목 | 값 |
| --- | --- |
| **토픽 (DAS publish)** | `factory/amrs/positions` |
| **FE** | `subscribe('factory/amrs/positions')` |

### Payload (배열 → 문자열로 발행)

```json
[
  { "amrId": "amr-01", "x": 25.0, "y": 50.0 },
  { "amrId": "amr-02", "x": 40.0, "y": 30.0 }
]
```

- `amrId`: BE `GET /amrs`의 `id`와 동일 문자열.
- `x`, `y`: float, **percent 0~100**.

---

## 6. 비상 정지 (0-7)

| 항목 | 값 |
| --- | --- |
| **토픽 (FE publish)** | `factory/amr/command` |
| **FE** | `publish('factory/amr/command', JSON.stringify({...}))` |
| **DAS** | subscribe 후 해당 AMR 시뮬 정지 |

### Payload 예시

```json
{
  "amrId": "amr-01",
  "command": "emergencyStop",
  "timestamp": "2026-05-19T08:10:00Z"
}
```

**녹화 성공 조건 (FE만):**

1. `POST /api/v1/amrs/{amrId}/commands` → `accepted: true`
2. UI에 `EMERGENCY_STOP` 반영(REST 폴링)
3. (선택) 위 MQTT publish — DAS 미가동이어도 1·2만으로 녹화 가능

---

## 7. FE 설정

| 항목 | 값 |
| --- | --- |
| 기동 | `cd FE` → `docker compose up --build` (**호스트 npm 금지**) |
| 플러그인 | `FE/frontend/src/plugins/ws.js` |
| 환경 변수 | `FE/.env.example` → `.env` — `VITE_MQTT_URL=ws://localhost:9001` (G4) |
| BE REST | **F4(G3):** FE 에이전트가 `vite.config.js` + `FE/docker-compose.yml` 에 프록시·`be` 서비스 연동 |
| 활성화 | `main.js` 로그인 후 `initMqtt()` |

**미사용:** `reconnecting-websocket`, BE `WS /api/v1/stream`.

### 브로커 (0-1, DAS Docker)

- **DAS:** `DB/docker-compose.yml` — Mosquitto WebSocket **9001** 호스트 publish.
- 호스트 브라우저 MQTT: `ws://localhost:9001`.
- FE·DAS가 동일 compose 네트워크일 때만 컨테이너 내부에서 `ws://mosquitto:9001` 등 서비스명 사용.

---

## 8. DAS 구현 체크리스트

- [x] 브로커 URL (기본 `ws://localhost:9001`)
- [x] 환경 토픽 `factory/environment/current` (단일·JSON 문자열)
- [x] AMR 토픽 `factory/amrs/positions` (단일·JSON 문자열)
- [x] E-stop `factory/amr/command` subscribe
- [x] 발행 주기 5s / 2s (권장)
- [x] 구역 키 = `DB/init.sql` `AREA.area_id`
- [ ] Node-RED 플로우 구현·녹화 리허설

**Phase 0 확정일:** 2026-05-19
