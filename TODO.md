# 시연 시뮬레이션 확장 — 통합 TODO

**작성일:** 2026-05-20  
**목적:** 녹화 시연에서 공장이 “살아 움직이는” 것처럼 보이도록, BE 시뮬레이션·FE 표시·DAS 연동을 **한 항목씩** 순차 구현한다.  
**SSOT(범위·화면):** `docs/시연_MVP_합의.md`, `docs/FE-DAS_MQTT_연동.md`  
**영역별 상세:** `BE/TODO.md`, `FE/TODO.md`, `docker/TODO.md`

---

## 진행 규칙

1. **한 번에 하나의 ID만** `진행 중`으로 두고 완료 후 다음으로 넘긴다.
2. 아래 **구현 순서는 변경 가능**하다. 다만 **의존성**이 있으면 선행 ID를 먼저 끝낸다.
3. 시연 우선순위가 높을수록 **번호가 앞**에 온다 (D-01이 최우선).
4. 각 ID 완료 시 본 파일의 체크박스를 갱신하고, 필요하면 `docs/시연_MVP_합의.md`에 각주만 추가한다(본문 대량 삭제 금지).

---

## 시연 상수 (합의값)

| 상수 | 값 | 용도 |
| --- | --- | --- |
| `DEMO_TASK_DURATION_SEC` | 20 | IDLE→작업 할당 후 OPERATING 유지 시간, 이후 완료 |
| `DEMO_CHARGE_RATE_PCT_PER_SEC` | 5 | CHARGING 상태 배터리 증가 |
| `DEMO_IDLE_DISCHARGE_PCT` | 1 / 5초 | IDLE 배터리 감소 |
| `DEMO_OPERATING_DISCHARGE_PCT` | 1 / 1초 | OPERATING 배터리 감소 |
| `DEMO_BATTERY_FULL_PCT` | 100 | 완충 기준 (완충 예상·도넛 계산) |
| `DEMO_CHARGE_STATION_ID` | 1번 스테이션만 | `init.sql`·BE 시드의 **첫 번째** 충전 스테이션(개발 편의) |

---

## 구현 순서 (시연 우선순위)

| 순서 | ID | 요약 | 주 영역 | 녹화 화면 |
| --- | --- | --- | --- | --- |
| 1 | [D-01](#d-01-대시보드-비상-상황-발생-버튼) | 대시보드 비상 발생 버튼 | FE (+ BE 기존 API) | SCR-01 |
| 2 | [D-02](#d-02-idle-amr-랜덤-작업-할당--20초-완료) | IDLE 랜덤 작업·20초 완료 | BE, DAS | SCR-01, SCR-05 |
| 3 | [D-03](#d-03-배터리-감소-idle--operating) | IDLE/OPERATING 배터리 감소 | BE | SCR-01~04 |
| 4 | [D-04](#d-04-충전-중-배터리-5초) | CHARGING 배터리 +5%/초 | BE | SCR-04 |
| 5 | [D-05](#d-05-amr-목록-상태-필터) | AMR 목록 상태 필터 | FE (BE 쿼리 기존) | SCR-02 |
| 6 | [D-06](#d-06-충전-스테이션1-충전-중-amr-목록) | 스테이션 1 충전 중 AMR 목록 | BE, FE | SCR-04 |
| 7 | [D-07](#d-07-완충-예상-시간-표시) | 완충 예상(산술) UI | FE | SCR-04 |
| 8 | [D-08](#d-08-충전-완료-예상-도넛-차트) | 충전 완료 예상 도넛 | FE | SCR-04 |
| 9 | [D-09](#d-09-작업-이력-동기화) | 작업 이력·분석 동기화 | BE, FE | SCR-05 |

**순서 변경 예:** D-05(필터)는 BE 의존 없이 FE만으로 빠르게 끝낼 수 있어 D-01 직후로 올려도 된다.

---

## D-01: 대시보드 비상 상황 발생 버튼

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

**시연 목적:** SCR-03으로 가지 않고도 SCR-01에서 비상 정지·AMR 정지를 바로 시연한다.

| 구분 | 내용 |
| --- | --- |
| **FE** | `DashboardView.vue`에 버튼 1개 추가(정적 시나리오 1건 OK). 대상 AMR은 고정(예: `amr-01`) 또는 목록 첫 OPERATING AMR. |
| **연동** | `POST /amrs/{amrId}/commands` body `{ "command": "emergencyStop" }` → `accepted === true` 후 `factory/amr/command` MQTT publish(SCR-03과 동일 패턴). |
| **BE** | 변경 없음(기존 `emergencyStop`·`EMERGENCY_STOP`·자동 복구 유지). |
| **DAS** | 변경 없음(기존 `mqttStopLatch`·DB 상태 동기화). |
| **완료 조건** | 대시보드 버튼 1회 클릭 → 해당 AMR REST 상태 `EMERGENCY_STOP` → MQTT 마커 정지 → 약 20초 내 IDLE 복구(기존 `AmrAutoRecoveryService`). |

---

## D-02: IDLE AMR 랜덤 작업 할당 · 20초 완료

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

**시연 목적:** 대기 AMR이 임의로 작업을 받아 **OPERATING**으로 바뀌고, DAS가 DB를 읽어 **지도에서만 이동**한다.

| 구분 | 내용 |
| --- | --- |
| **BE** | `@Scheduled` 시뮬레이터(신규, 예: `AmrDemoSimulationService`). 주기적으로 `IDLE` AMR 중 1대 선택(랜덤)·`AMR_TASK` INSERT·`AMR_STATUS_LOG`를 `OPERATING`으로 갱신. `DEMO_TASK_DURATION_SEC`(20초) 후 작업 `COMPLETED`·상태 `IDLE` 복귀. `EMERGENCY_STOP`·`ERROR`·`CHARGING`은 스킵. |
| **DB** | `AMR_TASK`, `AMR_STATUS_LOG.updated_at` 갱신. 작업 유형·구역은 시드 `AREA`/`PRODUCT` 중 랜덤 또는 고정 풀. |
| **DAS** | 변경 최소. `sync_amr_status_from_db`가 최신 `status`를 읽으면 `build_amr_positions`가 `OPERATING`만 이동(현행 유지). |
| **FE** | SCR-01 작업 로그·SCR-05 테이블은 D-09에서 연동; D-02 완료 시 REST로 작업·상태만 맞으면 됨. |
| **완료 조건** | 20초 이내에 IDLE→OPERATING→IDLE 1사이클이 반복 가능하고, 그동안 MQTT에서 해당 AMR만 이동한다. |

**의존성:** 없음(D-03·D-04와 병렬 가능하나, 상태 전이 충돌 방지를 위해 배터리 스케줄러는 D-02 이후 권장).

---

## D-03: 배터리 감소 (IDLE / OPERATING)

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

| 구분 | 내용 |
| --- | --- |
| **BE** | 동일 시뮬레이터 또는 전용 tick: `IDLE`은 5초마다 `battery_pct - 1`, `OPERATING`은 1초마다 `- 1`. 하한 0(또는 시연용 최소값). `AMR_STATUS_LOG` 최신 행의 `battery_pct` 갱신. `CHARGING`·`EMERGENCY_STOP`·`ERROR`는 감소 제외. |
| **FE** | `GET /amrs` 폴링으로 SCR-02·03·04에 반영 확인. |
| **완료 조건** | IDLE·OPERATING AMR의 `batteryPercent`가 REST에서 위 주기로 감소한다. |

**의존성:** D-02와 상태 전이 규칙 합의(OPERATING 중 감소 적용).

---

## D-04: 충전 중 배터리 +5%/초

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

| 구분 | 내용 |
| --- | --- |
| **BE** | `CHARGING` 상태 AMR에 1초마다 `battery_pct + 5`, 상한 `DEMO_BATTERY_FULL_PCT`. 100% 도달 시 `IDLE` 전환 및 `AMR_CHARGING_LOG.end_time` 설정(선택). |
| **FE** | SCR-04·목록에서 충전률 상승 확인. |
| **완료 조건** | `amr-03` 등 CHARGING 시드 AMR이 초당 약 5%p 상승한다. |

**의존성:** D-03과 배터리 필드 단일 writer 정리(동시 갱신 race 방지).

---

## D-05: AMR 목록 상태 필터

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

| 구분 | 내용 |
| --- | --- |
| **FE** | `AmrListView.vue`: 상태 멀티/단일 선택 UI → `GET /amrs?status=OPERATING,IDLE,...` (기존 API). “전체” 선택 시 `status` 생략. |
| **BE** | 변경 없음(`AmrController` `status` 파라미터 이미 존재). |
| **완료 조건** | SCR-02에서 상태별로 목록 행 수가 필터에 맞게 줄어든다. |

**의존성:** 없음(FE 단독 가능 → 시연 2번째 화면이므로 우선순위 상위 유지).

---

## D-06: 충전 스테이션(1번) 충전 중 AMR 목록

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

**화면:** 녹화 경로 **SCR-04** `BatteryView.vue` (`/battery`). 별도 `/charging` 라우트는 네비 미포함이나, 동일 API로 `ChargingStationsView` 재사용 가능.

| 구분 | 내용 |
| --- | --- |
| **BE** | `GET /charging/stations` 응답에서 **1번 스테이션**만 `amrs[]`에 `CHARGING` AMR 실데이터 채움(`ChargingService`). `AMR_CHARGING_LOG`·최신 `AMR_STATUS_LOG` 조인. MVP §3.4 “queue 빈 테이블”은 **대기 큐**만 유지 가능. |
| **FE** | `BatteryView.vue`: 스테이션 카드·슬롯에 충전 중 AMR ID·배터리% 표시. `queue` 더미 대신 스테이션 `amrs` 우선 표시. |
| **완료 조건** | SCR-04에서 1번 스테이션에 충전 중 AMR이 REST와 일치하게 보인다. |

**의존성:** D-04(CHARGING 배터리 값이 의미 있음).

---

## D-07: 완충 예상 시간 표시

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

| 구분 | 내용 |
| --- | --- |
| **FE** | `BatteryView.vue` 상단 “완충 예상”(`stats.maxEta`): 충전 중 AMR마다 `ceil((100 - batteryPercent) / DEMO_CHARGE_RATE_PCT_PER_SEC)` 초 → `분:초` 또는 `N분` 표기. 스테이션 내 AMR 중 **최대값**을 카드에 표시. |
| **BE** | 선택: `ChargingStationDto.amrs[].estimatedFullAt` 제공. **시연 최소는 FE 산술만**으로도 완료 가능. |
| **완료 조건** | 충전률이 오를수록 완충 예상 시간이 줄어든다. |

**의존성:** D-04, D-06.

---

## D-08: 충전 완료 예상 도넛 차트

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

| 구분 | 내용 |
| --- | --- |
| **FE** | 기존 “충전 완료 예상” 위젯을 `GET /charging/forecast` 버킷 대신(또는 병행) **D-07과 동일 산술**으로 0~30분 / 30~60분 / 60분+ 구간별 대수 집계 후 도넛·범례 갱신. |
| **BE** | 선택: `GET /charging/forecast`가 위 3버킷을 반환하도록 시뮬레이터와 맞춤. FE 단독 집계도 허용. |
| **완료 조건** | 충전 중 AMR 수·예상 시간 분포가 도넛·범례와 일치한다. |

**의존성:** D-07.

---

## D-09: 작업 이력 동기화

**상태:** [ ] 대기 · [ ] 진행 중 · [ ] 완료

| 구분 | 내용 |
| --- | --- |
| **BE** | D-02에서 생성·완료한 `AMR_TASK`가 `GET /work-histories`·`GET /analytics/workload`에 포함되도록 매핑(필드: AMR ID, 작업명, 시작·종료, 상태, 구역 등). |
| **FE** | `WorkHistoryView.vue` 폴링으로 신규 행·차트 갱신 확인. |
| **완료 조건** | 랜덤 작업 1건 이상 완료 후 SCR-05 테이블·차트에 반영된다. |

**의존성:** **D-02 필수**.

---

## 영역별 체크리스트 (요약)

### BE (`BE/amr_control_system`)

- [ ] D-02 `AmrDemoSimulationService`(가칭): 작업 할당·완료 스케줄
- [ ] D-03·D-04 배터리 tick (`app.demo.*` 설정으로 주기·율 노출 권장)
- [ ] D-06 `ChargingService.listStations()` — 스테이션 1 `amrs` 실데이터
- [ ] D-09 `WorkHistory` / `Analytics` 조회가 시뮬레이션 `AMR_TASK` 반영

### FE (`FE/frontend`)

- [ ] D-01 `DashboardView.vue` 비상 버튼
- [ ] D-05 `AmrListView.vue` 상태 필터
- [ ] D-06~D-08 `BatteryView.vue` 충전 목록·완충 예상·도넛
- [ ] D-09 `WorkHistoryView.vue` 동작 확인

### DAS (`docker/node-red`)

- [ ] D-02 검증: DB `OPERATING` ↔ MQTT 이동만 (기존 플로우 regression)
- [ ] D-01 검증: 비상 시 `mqttStopLatch` + DB `EMERGENCY_STOP` 해제 시 latch 해제

---

## MVP 문서와의 차이 (의도적 확장)

| 기존 MVP | 본 TODO |
| --- | --- |
| SCR-04 `GET /charging/queue` 실데이터 미연동 | D-06에서 스테이션 `amrs`·충전 목록 **실데이터** 연동 |
| SCR-04 forecast API | D-08에서 **배터리 충전 속도 기반** FE 산술 우선 |

구현 완료 후 `docs/시연_MVP_합의.md` §3.4·§5.2에 **각주 1~2줄**만 추가한다.

---

## 검증 (통합 스택)

```powershell
# 프로젝트 루트
docker compose up -d --build
```

| ID | 수동 확인 |
| --- | --- |
| D-01 | SCR-01 버튼 → AMR 정지 → 복구 |
| D-02 | IDLE AMR → 20초 OPERATING·이동 → IDLE |
| D-03·D-04 | REST `batteryPercent` 증감 주기 |
| D-05 | SCR-02 필터 |
| D-06~D-08 | SCR-04 목록·완충 예상·도넛 |
| D-09 | SCR-05 작업 테이블·차트 |

---

## 변경 이력

| 날짜 | 내용 |
| --- | --- |
| 2026-05-20 | 시연 시뮬레이션 9항목 통합 TODO 최초 작성 |
