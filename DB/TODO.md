# DAS·DB 녹화 시연 MVP (TODO)

> **갱신:** 2026-05-19  
> **SSOT:** `docs/시연_MVP_합의.md` §7.1, `docs/FE-DAS_MQTT_연동.md`  
> **에이전트:** `docs/시연_에이전트_프롬프트.md` §3 — 「진행 상황을 확인하고 **DAS·DB** 파트를 구현하라」

## Docker (선행 확인)

- [ ] `DB/docker-compose.yml` 존재 확인 — **있으면 수정만**, **없을 때만** 신규 작성
- [ ] `Dockerfile` 등 동일 규칙

## D1~D3

| 순 | 작업 | 상태 |
| --- | --- | --- |
| D1 | 토픽·payload (`FE-DAS_MQTT_연동.md` Phase 0) | 문서 완료 |
| D2 | Mosquitto WS **9001** publish + Node-RED 발행 | [ ] |
| D3 | 녹화용 고정 시드(좌표·환경 급변 방지) | [ ] |

## MQTT (D2 상세)

- [ ] `factory/environment/current` — 5초, `areas` × `sensor1~4`, JSON 문자열
- [ ] `factory/amrs/positions` — 2초, `amr-01`~`amr-05`, x/y 0~100
- [ ] `factory/amr/command` subscribe — `emergencyStop`

## 검증

- [ ] `cd DB && docker compose up --build`
- [ ] 호스트에서 토픽 수신 확인
