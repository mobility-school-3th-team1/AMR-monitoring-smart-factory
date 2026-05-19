# Docker 구성

이 폴더는 프로젝트의 Docker 관련 구성 파일을 모아두는 곳입니다.

## 구성 파일

- `docker-compose.yml`: 이 폴더에서 실행되는 Docker Compose 정의 파일입니다.
- `mysql/`: MySQL 초기화 스크립트와 데이터 디렉토리를 위한 구성입니다.
- `node-red/`: Node-RED 데이터와 플로우 구성을 저장하는 디렉토리입니다.

## 실행 방법

이 프로젝트는 루트 `.env` 파일에서 환경 변수를 읽도록 구성되어 있습니다.
루트 폴더에서 아래 명령으로 실행하세요:

```bash
docker compose --env-file .env -f docker/docker-compose.yml up
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
