# DAS·DB 녹화 시연 MVP (TODO)

> **갱신:** 2026-05-19  
> **SSOT:** `docs/시연_MVP_합의.md` §7.1, `docs/FE-DAS_MQTT_연동.md`  
> **DAS 런타임·검증:** `docker/TODO.md`, `docker/README.md` (구현 SSOT는 **`docker/`**)

## Docker

- [x] `docker/docker-compose.yml` — Mosquitto **1883** + WS **9001**, Node-RED, MySQL
- [x] `docker/mosquitto/mosquitto.conf`, `docker/node-red/flows.json` (탭 「시연 MVP」)

## D1~D3

| 순 | 작업 | 상태 |
| --- | --- | --- |
| D1 | 토픽·payload (`FE-DAS_MQTT_연동.md` Phase 0) | 완료 |
| D2 | Mosquitto 1883/9001 + Node-RED MQTT 발행 | 완료 |
| D3 | 녹화용 고정 시드(좌표·환경 급변 방지) | 완료 |

## MQTT (D2)

- [x] `factory/environment/current` — 5초
- [x] `factory/amrs/positions` — 2초, `amr-01`~`amr-05`
- [x] `factory/amr/command` subscribe — `emergencyStop`

## 검증

- [x] `docker compose --env-file .env -f docker/docker-compose.yml up -d --build`
- [x] `mosquitto_sub -h localhost -p 1883` 토픽 수신
