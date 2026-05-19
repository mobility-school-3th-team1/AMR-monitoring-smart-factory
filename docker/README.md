# Docker 구성

이 폴더는 프로젝트의 Docker 관련 구성 파일을 모아두는 곳입니다.

## 구성 파일

- `docker-compose.yml`: 이 폴더에서 실행되는 Docker Compose 정의 파일입니다.
- `mysql/`: MySQL 초기화 스크립트와 데이터 디렉토리를 위한 구성입니다.
- `node-red/`: Node-RED 데이터와 플로우 구성을 저장하는 디렉토리입니다.

## 실행 방법

### MySQL 스키마 변경 후

`docker/mysql/init.sql`이 바뀌면 기존 볼륨을 제거한 뒤 재기동해야 합니다.

```bash
docker compose down -v
docker compose up --build
```

### 통합 스택 (BE + DAS + FE, 권장)

프로젝트 루트에서:

```bash
cp .env.example .env
# JWT_SECRET 설정 후
docker compose up --build
```

정의 파일: [compose.yml](../compose.yml). FE UI: http://localhost:3000

### DAS·DB만 (단독)

루트 `.env`에서 환경 변수를 읽습니다:

```bash
docker compose --env-file .env -f docker/docker-compose.yml up --build
```

### 포트 (기존 DAS 우선)

| 용도 | 포트 | 환경 변수 |
| --- | --- | --- |
| MQTT (Node-RED) | 1883 | `MQTT_PORT` |
| MQTT WebSocket (FE 브라우저) | 9001 | `MQTT_WS_PORT` |
| Node-RED UI | 1880 | `NODERED_PORT` |
| MySQL | 3306 | `DB_PORT` |

### 시연 MVP MQTT (Node-RED 탭 「시연 MVP」)

| 토픽 | 방향 | 주기 |
| --- | --- | --- |
| `factory/environment/current` | publish | 1초 (센서 값 변동) |
| `factory/amrs/positions` | publish | 0.5초 (`OPERATING` AMR만 좌표 이동, status는 MySQL `AMR_STATUS_LOG`) |
| `factory/amr/command` | subscribe (`emergencyStop`) | - |

검증 (호스트, Mosquitto 클라이언트 설치 시):

```bash
mosquitto_sub -h localhost -p 1883 -t "factory/environment/current" -C 1
mosquitto_sub -h localhost -p 1883 -t "factory/amrs/positions" -C 1
```

## 새 컨테이너 추가 가이드

- `docker/docker-compose.yml`에 서비스를 추가합니다.
- 새 컨테이너도 `scada-network` 네트워크에 연결해야 합니다.
  - 예:<br>
    `networks:`<br>
      `- scada-network`<br>
    `environment:`<br>
      `- network=scada-network`
- 서비스 간 통신은 컨테이너 이름으로 가능하며, 외부 포트가 아닌 내부 포트를 사용합니다.
  - 예: MySQL에 연결할 때의 주소로 `SCADA_MySQL:3306` 사용
- 외부 포트는 `.env`에 정의하고 `docker-compose.yml`에서 참조하는 것이 좋습니다.
  - 예: `MQTT_PORT`, `DB_PORT`, `NODERED_PORT`
- 민감한 정보는 `.env`에만 두고 `docker-compose.yml`에는 직접 하드코딩하지 않습니다.
- 새로운 서비스가 다른 서비스에 의존하면 `depends_on`을 사용해 시작 순서를 명시합니다.

## 주의사항

- `.env` 파일에는 DB 비밀번호 등 민감 정보가 포함될 수 있으므로, 저장소에 커밋할 때는 주의해야 합니다.
