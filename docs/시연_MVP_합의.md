# FE 녹화 시연 MVP — 설계 합의

**작성일:** 2026-05-19  
**최종 갱신:** 2026-05-19 (팀 신규 합의: DAS MQTT·화면 축소 반영)  
**목적:** 프론트엔드 **5개 화면 + 로그인**을 **사전 녹화**로 제출할 때, 성공 조건·데이터 소스·타 영역 책임·구현 순서를 문서로 고정한다.  
**근거:** `docs/설계-구현_정합성_검토.md` §8·§10.

---

## 1. 시연 성공 조건

| 항목 | 합의 |
| --- | --- |
| **시연 주체** | **프론트엔드 UI만** 녹화·제공 (실시간 라이브 시연 없음) |
| **성공 정의** | **6개 흐름**이 녹화 영상에서 **화면이 보이고**, 클릭·전환·표시가 **오류 없이 동작** |
| **공장 평면도** | FE **정적 이미지 에셋** (`FE/frontend/src/assets/` 등). 좌표·환경은 이미지 위 **오버레이** |
| **실시간 (SCR-01 중심)** | **DAS → MQTT → FE**: AMR 위치(백분율), 구역별 환경 센서(16건). 녹화 시 브로커·플로우 **가동 필수** |
| **운행·명령·집계** | **BE REST** + Docker H2 시드 (로그인, AMR 상태 요약, 비상 정지, 충전·작업 이력 등) |
| **제외** | FE **WebSocket** 구독, `GET /environment/areas/current`(12-F), MySQL·Node-RED 외 문서만 있는 항목 |

### 1.1 녹화 대상 화면 (5 + 로그인)

| 순서 | 화면 ID | 경로 | Vue 컴포넌트 |
| --- | --- | --- | --- |
| 0 | 로그인 | `/login` | `LoginView.vue` |
| 1 | SCR-01 | `/dashboard` | `DashboardView.vue` |
| 2 | SCR-02 | `/amr-list` | `AmrListView.vue` |
| 3 | SCR-03 | `/amr-detail` | `AmrDetailView.vue` |
| 4 | SCR-04 | `/battery` | `BatteryView.vue` |
| 5 | SCR-05 | `/work-history` | `WorkHistoryView.vue` |

**범위 외:** `/charging` (`ChargingStationsView`).

### 1.2 권장 녹화 시나리오 (약 3~5분)

1. 로그인 → SCR-01 (평면도·환경 MQTT·AMR 마커·작업 로그)  
2. SCR-02 → AMR 선택 → SCR-03 (비상 정지 **권장**)  
3. SCR-04 → SCR-05  
4. (선택) SCR-01 재방문  

---

## 2. DAS → FE MQTT 합의 (신규)

> 토픽 문자열·브로커 URL·QoS는 **DAS&DB 담당이 `docs/FE-DAS_MQTT_연동.md`에 확정**한다. FE는 payload·좌표계만 본 절을 따른다.

### 2.1 공통 좌표계 (AMR·레이아웃)

| 항목 | 규칙 |
| --- | --- |
| **레이아웃** | FE 정적 공장 이미지 1장. 가로·세로 **논리 크기** (예: width=40, height=50)는 DAS·FE가 동일 상수로 공유 |
| **AMR 위치** | DAS가 발행하는 `x`, `y`는 **픽셀이 아닌 백분율(0~100)**. 예: 논리 (10, 25) → **x=25%, y=50%** |
| **FE 표시** | 이미지 컨테이너 기준 `left: x%`, `top: y%` (또는 transform)로 마커 배치 |
| **BE `position`** | REST의 절대좌표는 **보조**·목록용. SCR-01 맵 **주 데이터 소스 = MQTT** |

### 2.2 환경 센서 (ENV)

| 항목 | 규칙 |
| --- | --- |
| **규모** | 구역당 **4개** 센서 슬롯, 화면 합계 **16개** 수치 표시 |
| **Payload 예시** | 구역 1건당 발행 형태(합의): |

```json
{
  "sensor1": { "temp": 25, "humid": 33, "particle": 22, "cogas": 5 },
  "sensor2": { "temp": 24, "humid": 32, "particle": 21, "cogas": 4 },
  "sensor3": { "temp": 26, "humid": 34, "particle": 23, "cogas": 6 },
  "sensor4": { "temp": 25, "humid": 33, "particle": 22, "cogas": 5 }
}
```

| 필드 | 의미 |
| --- | --- |
| `temp` | 온도 |
| `humid` | 습도 |
| `particle` | 미세먼지(파티클) |
| `cogas` | CO 가스 |

- **4구역 × 4센서 = 16** 표시: FE는 구역별 MQTT 구독 또는 단일 토픽 내 구역 키 — **DAS 명세 확정 후 FE 매핑**.
- **`GET /environment/areas/current`**: 시연 **미사용**.

### 2.3 AMR 상태 (좌표)

| 항목 | 규칙 |
| --- | --- |
| **내용** | AMR 식별자 + **x%, y%** (백분율). 상태 색·라벨은 **BE REST** `GET /amrs`와 병합 가능 |
| **비상 정지** | ADR 유지: `POST /commands` `accepted` 후 FE → DAS **MQTT 정지** (토픽은 DAS 명세) |

### 2.4 FE MQTT 구현 요약

- `src/plugins/ws.js` — `initMqtt` / `subscribe` / `publish`  
- `main.js` — 로그인 후 브로커 연결 (`VITE_MQTT_URL` 등 환경 변수)  
- **BE WebSocket 미사용**

---

## 3. 화면별 UI 축소·데이터 소스 (2026-05-19 팀 합의)

### 3.1 SCR-01 메인 대시보드

| 구분 | 항목 | 조치 | 데이터 소스 |
| --- | --- | --- | --- |
| **삭제** | 생산 건수 KPI | UI·연동 제거 | — |
| **삭제** | 활성 알람 KPI | UI·연동 제거 | — |
| **삭제** | 평균 배터리 KPI | UI·연동 제거 | — |
| **유지** | 운영/대기/충전/에러(·미해결) 요약, 작업 로그, 평면도, 환경 | 구현·녹화 필수 | BE `summary`·`recent-logs`; MQTT 환경·AMR 좌표 |
| **유지** | 실시간 중요 알람 **목록** | 팀 미삭제 명시 — **유지**(KPI만 삭제) | BE `recent-alarms` (선택) |

### 3.2 SCR-02 AMR 전체 관리

| 구분 | 항목 | 조치 |
| --- | --- | --- |
| **보류(삭제 가능성 높음)** | 공정별 평균 대기 시간 차트 | UI 제거 또는 플레이스홀더 숨김. **API 연동 안 함** |
| **보류(삭제 가능성 높음)** | 평균 시간 준수율 차트 | 동일 |
| **유지** | AMR 목록·상태 테이블·상세 이동 | `GET /amrs`, **status enum 수정** |

### 3.3 SCR-03 AMR 개별 관제

| 구분 | 항목 | 조치 |
| --- | --- | --- |
| **삭제** | 총 가동 시간 | UI 제거 |
| **삭제** | 온·습도(상단/라인) | UI 제거 |
| **삭제** | 시간 준수율 차트 | UI 제거 |
| **삭제** | 센서 리스트(LiDAR·카메라 등) | UI 제거. **향후 BE/DB 제공 센서만** 별도 이슈 |
| **유지** | 장비 ID·작업·배터리·주행거리·비상 정지 | `GET /amrs/{id}`, `POST .../commands` |

### 3.4 SCR-04 배터리/충전

| 구분 | 항목 | 조치 |
| --- | --- | --- |
| **삭제(동작)** | 대기열 **내용** | API 데이터 **채우지 않음**. 테이블 **외형만** 빈 상태 유지(시연용 더미) |
| **삭제** | 충전 스테이션 알람 | UI·`recent-alarms` 호출 제거 |
| **유지** | 스테이션 요약·forecast 등 | `GET /charging/stations`, `forecast` |

### 3.5 SCR-05 작업 이력

| 구분 | 항목 | 조치 |
| --- | --- | --- |
| **삭제** | 총 작업 시간 카드 | UI 제거 |
| **삭제** | 평균 작업 시간 카드 | UI 제거 |
| **유지** | 일별 작업 건수·이동 거리(팀 미삭제), 차트 2종, 작업 테이블 | `work-histories`, `analytics/workload` |

### 3.6 이번 시연에서 하지 않는 것 (갱신)

- FE WebSocket  
- `GET /environment/areas/current`  
- `GET /analytics/kpis` (**SCR-02/03 차트 보류·삭제**)  
- SCR-04 `GET /charging/queue` **실데이터 연동** (빈 테이블만)  
- MySQL compose·`/charging` 라우트  

---

## 4. 타 영역 책임 (갱신)

| 영역 | 이번 시연 책임 | 하지 않음 |
| --- | --- | --- |
| **FE** | 정적 평면도 에셋, MQTT 구독(환경 16·AMR %좌표), REST §5, 화면 §3 축소, vite 프록시 | WS, 12-F REST 환경 |
| **DAS** | MQTT 브로커·토픽·payload §2, 녹화 시 **지속 발행** | BE 직접 연동 |
| **BE** | §5 API·H2 시드·`emergencyStop` | 12-F, KPI 확장, position WS 스케줄, queue 시연 데이터 필수 아님 |
| **DB** | `init.sql` 스키마 참조. **런타임 시드는 BE H2** | MySQL merge(시연 후) |

**녹화 인프라:** `BE/docker compose up` + **MQTT 브로커(DAS)** + `FE/npm run dev`.

---

## 5. FE 녹화 시연 API·MQTT (BE / DAS)

### 5.1 BE REST — 필수

| API | 화면 | 비고 |
| --- | --- | --- |
| `POST /auth/login` | 로그인 | |
| `GET /dashboard/summary` | SCR-01, (SCR-03 요약) | **productionCount·activeAlarms·avgBattery 필드 UI 미사용** |
| `GET /dashboard/recent-logs` | SCR-01 | |
| `GET /amrs` | SCR-01(상태색), SCR-02 | |
| `GET /amrs/{amrId}` | SCR-03 | |
| `POST /amrs/{amrId}/commands` | SCR-03 | `emergencyStop` |
| `GET /charging/stations` | SCR-04 | |
| `GET /charging/forecast` | SCR-04 | |
| `GET /work-histories` | SCR-05 | |
| `GET /analytics/workload` | SCR-05 | |

### 5.2 BE REST — 시연 제외·미호출

| API | 이유 |
| --- | --- |
| `GET /dashboard/recent-alarms` | SCR-01 활성 알람 KPI 삭제. 알람 **목록** 유지 시에만 선택 호출 |
| `GET /charging/queue` | SCR-04 빈 테이블 더미 |
| `GET /analytics/kpis` | SCR-02/03 차트 보류 |
| `GET /environment/areas/current` | MQTT로 대체 |

### 5.3 DAS MQTT — 필수 (토픽명 TBD)

| 용도 | Payload | FE 소비처 |
| --- | --- | --- |
| 환경(구역당) | §2.2 `sensor1`~`sensor4` | SCR-01 환경 영역 (합 16 표시) |
| AMR 좌표 | `amrId` + `x`, `y` (**%**) | SCR-01 평면도 마커 |
| (선택) 비상 정지 | DAS 명세 | SCR-03 `publish` after `accepted` |

---

## 6. 문서 업데이트 우선순위 (갱신)

### Tier A — 시연 전 필수

| 순 | 문서 | 항목 |
| --- | --- | --- |
| A1 | **`docs/시연_MVP_합의.md`** | 본 문서 팀 확정 | (본 문서) |
| A2 | **`docs/FE-DAS_MQTT_연동.md`** (신규) | §2 이관 + **토픽·브로커·발행 주기·구역 4×4 매핑** |
| A3 | **`docs/화면 설계서.md`** | 「녹화 MVP」+ §3 UI 삭제·보류 + 정적 평면도·MQTT | **반영됨** |
| A4 | **`docs/API 정의.md`** | 시연 필수 API = §5.1, environment·queue·kpis 시연 제외 각주 | **반영됨** |
| A0 | **`AGENTS.md`** (루트·FE·BE·DB) | MVP SSOT·우선순위 규칙 | **반영됨** |
| A5 | **`FE/TODO.md`** | §7 구현 순서 반영 |
| A6 | **`BE/TODO.md`** | FE 녹화 지원 + queue/12-F/kpis **시연 불필요** |

### Tier B — 권장

`설계-구현_정합성_검토.md` §10, ADR(녹화 시 MQTT **환경·좌표 필수**, E-stop publish), `프로젝트 정의서.md`, `README.md`(BE+MQTT+FE), `FE/AGENTS.md`.

### Tier C/D

Swagger, for_presentation, `errorCount` KPI 확장 — 시연 후.

---

## 7. 구현 우선순위 (2026-05-19 갱신)

**원칙:** FE > DAS&DB > BE. 녹화 전 **SCR-01 MQTT·평면도**가 최우선.

### 7.1 DAS&DB (P0 — FE 블로커)

| 순 | 작업 |
| --- | --- |
| D1 | `FE-DAS_MQTT_연동.md`에 토픽·브로커·샘플 payload 고정 |
| D2 | Node-RED(또는 시뮬): §2.2 환경 4구역(또는 1토픽×4), AMR **x/y %** 주기 발행 |
| D3 | 녹화 리허설용 **고정 시드 값** (온도·좌표 급변 방지) |

### 7.2 FE (P0)

| 순 | 작업 |
| --- | --- |
| F1 | 공장 레이아웃 **정적 이미지** 에셋 + 컨테이너 비율 고정 |
| F2 | `initMqtt` 활성화, `VITE_MQTT_URL`, 구독 핸들러(환경·AMR %) |
| F3 | SCR-01: 평면도 **% 좌표** 마커, 환경 **16센서** 표시; KPI §3.1 삭제 |
| F4 | `vite` → BE 프록시 |
| F5 | `AmrListView` **status enum** |
| F6 | SCR-03: API·E-stop, §3.3 삭제 항목 제거 |
| F7 | SCR-04: queue **빈 테이블**, 알람 제거; stations/forecast 유지 |
| F8 | SCR-05: API·차트, §3.5 카드 2개 삭제 |
| F9 | SCR-02: §3.2 보류 차트 제거/숨김 |
| F10 | 6흐름 녹화 |

### 7.3 BE (P1 — 기능 추가 최소)

| 순 | 작업 |
| --- | --- |
| B1 | Docker 기동 + §5.1 API 스모크 |
| B2 | (변경 없음) 12-F·analytics KPI 확장·position WS **착수 안 함** |

### 7.4 병행 불가 시 완화 (녹화만)

| 조건 | 완화 |
| --- | --- |
| DAS MQTT 미준비 | SCR-01 환경: **마지막 수신값 고정** 또는 합의된 정적 16값; AMR: REST `position`을 %로 **환산**해 임시 표시(문서화) |
| SCR-02 차트 | 이미 **숨김** — 블로커 아님 |

---

## 8. 확정 체크리스트 (갱신)

- [ ] 시연 = FE 녹화 6흐름  
- [ ] SCR-01 = **정적 평면도 + MQTT(환경 16 + AMR %)**  
- [x] §3 UI 삭제·보류 → `화면 설계서.md` MVP 절, `API 정의.md` 시연 절, `AGENTS.md`(4) 반영  
- [ ] DAS MQTT 명세(`FE-DAS_MQTT_연동.md` §6) 확정  
- [ ] BE = §5.1만 필수  
- [ ] `FE/TODO.md`, `BE/TODO.md` 시연 절 반영  

**확정일:** ___________  
**참여:** FE / BE / DAS&DB
