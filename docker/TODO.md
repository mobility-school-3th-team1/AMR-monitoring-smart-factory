# DAS (docker) 녹화 시연 MVP

> **SSOT:** `docs/시연_MVP_합의.md` §7.1, `docs/FE-DAS_MQTT_연동.md`  
> **기동:** 프로젝트 루트에서 `docker compose --env-file .env -f docker/docker-compose.yml up -d --build`

## 진행 상황 (2026-05-19)

| 순 | 작업 | 상태 |
| --- | --- | --- |
| D1 | 토픽·payload (`FE-DAS_MQTT_연동.md` Phase 0) | 문서 확정 |
| D2 | Mosquitto 1883 + WS 9001, Node-RED MVP 발행 | 완료 |
| D3 | 녹화용 고정 시드(좌표·환경 급변 방지) | 완료 |

## MQTT (D2)

- [x] `factory/environment/current` — 5초, 4구역 × sensor1~4, JSON 문자열
- [x] `factory/amrs/positions` — 2초, `amr-01`~`amr-05`, x/y 0~100
- [x] `factory/amr/command` subscribe — `emergencyStop`

## 검증

- [x] `docker compose --env-file .env -f docker/docker-compose.yml up -d --build`
- [x] `mosquitto_sub` 로 환경·AMR 토픽 수신 확인

## 참고

- Node-RED 탭 「시연 MVP」가 시연 발행 담당. 「플로우 1」은 modbus 의존으로 **비활성** (기존 개발 플로우 보존).
- 포트: Node-RED MQTT **1883** (`MQTT_PORT`), FE 브라우저 WebSocket **9001** (`MQTT_WS_PORT`).
