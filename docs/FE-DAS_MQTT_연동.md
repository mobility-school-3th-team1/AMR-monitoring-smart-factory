# FE ↔ DAS MQTT 연동 (녹화 시연)

**작성일:** 2026-05-19  
**상태:** Draft — **토픽·브로커 URL은 DAS&DB 확정 필요**  
**상위 문서:** `docs/시연_MVP_합의.md` §2

---

## 1. 역할

| 방향 | 내용 |
| --- | --- |
| DAS → FE | 환경 센서(구역당 4슬롯, 화면 합 16), AMR 좌표(**x/y 백분율**) |
| FE → DAS | 비상 정지 (`accepted: true` 이후). 토픽·payload **TBD** |
| BE | MQTT **미참여**. 운행 상태·명령은 REST |

---

## 2. 좌표계

- FE: 공장 레이아웃 **정적 이미지** 1장.
- 논리 크기 예: `LAYOUT_WIDTH=40`, `LAYOUT_HEIGHT=50` (DAS·FE 동일 상수).
- DAS 발행 `x`, `y`: **0~100 백분율** (픽셀 아님).  
  예: 논리 (10, 25) → `{ "x": 25, "y": 50 }`.
- FE: 마커 `left: x%`, `top: y%`.

---

## 3. 환경 센서 payload (확정)

구역 1회 발행 예:

```json
{
  "sensor1": { "temp": 25, "humid": 33, "particle": 22, "cogas": 5 },
  "sensor2": { "temp": 24, "humid": 32, "particle": 21, "cogas": 4 },
  "sensor3": { "temp": 26, "humid": 34, "particle": 23, "cogas": 6 },
  "sensor4": { "temp": 25, "humid": 33, "particle": 22, "cogas": 5 }
}
```

| 키 | 단위/의미 |
| --- | --- |
| `temp` | 온도 |
| `humid` | 습도(%) |
| `particle` | 미세먼지 |
| `cogas` | CO 가스 |

- **4구역 × 4센서 = 16** UI 슬롯.
- 구역별 토픽 vs 단일 토픽: **TBD (DAS)**

---

## 4. AMR 좌표 payload (형식 TBD)

```json
{
  "amrId": "amr-01",
  "x": 25.0,
  "y": 50.0
}
```

- `x`, `y`: float, **percent 0~100**.
- 다중 AMR: 배열 vs 토픽 per AMR — **TBD (DAS)**

---

## 5. FE 설정 (구현)

| 항목 | 값 |
| --- | --- |
| 플러그인 | `FE/frontend/src/plugins/ws.js` |
| 환경 변수 | `VITE_MQTT_URL` (예: `ws://localhost:1883/mqtt`) |
| 활성화 | `main.js` 로그인 후 `initMqtt()` |

**미사용:** `reconnecting-websocket`, BE `WS /api/v1/stream`.

---

## 6. DAS 확정 체크리스트

- [ ] 브로커 URL·포트  
- [ ] 환경 토픽(구역 4개)  
- [ ] AMR 좌표 토픽  
- [ ] 비상 정지 subscribe 토픽·payload  
- [ ] 발행 주기(녹화용 안정 값)  
- [ ] `LAYOUT_WIDTH` / `LAYOUT_HEIGHT` 상수  

**확정일:** ___________
