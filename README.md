# AMR-monitoring-smart-factory

AMR(자율 이동 로봇) 기반 스마트 팩토리 통합 모니터링 시스템입니다. 공장 내 AMR 위치와 상태, 환경 센서, 충전 스테이션, 작업 이력을 실시간으로 관제하고 분석하는 웹 애플리케이션을 목표로 합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| 프론트엔드 | Vue.js, Apache ECharts |
| 백엔드 | Java Spring Boot, REST API, WebSocket |
| 데이터 및 DAS | MySQL, Node-RED |

상세 규칙은 각 폴더의 `AGENTS.md`와 아래 설계 문서를 따릅니다.

## 저장소 구조

| 경로 | 설명 |
| --- | --- |
| [`FE/`](FE/) | 프론트엔드. 작업 시 [`FE/AGENTS.md`](FE/AGENTS.md)를 우선합니다. |
| [`BE/`](BE/) | 백엔드. 작업 시 [`BE/AGENTS.md`](BE/AGENTS.md)를 우선합니다. |
| [`DB/`](DB/) | DB 스키마 및 DAS. 작업 시 [`DB/AGENTS.md`](DB/AGENTS.md)를 우선합니다. |
| [`compose.yml`](compose.yml) | 통합 Docker Compose (BE + DAS + FE) |
| [`docs/`](docs/) | 설계 및 명세 문서 |
| [`docs/ADR/`](docs/ADR/) | 아키텍처 결정 기록 (ADR). 템플릿: [`adr_template.md`](docs/ADR/adr_template.md) |
| [`.github/`](.github/) | 이슈 및 PR 템플릿 |
| [`AGENTS.md`](AGENTS.md) | 프로젝트 루트 공통 안내 |

## 설계 문서

작업 시작 전 또는 관련 기능 구현 시 아래 문서를 읽습니다.

| 문서 | 내용 |
| --- | --- |
| [프로젝트 정의서](docs/프로젝트%20정의서.md) | 배경, 아키텍처 개념, 핵심 기능 |
| [화면 설계서](docs/화면%20설계서.md) | SCR-01~05 와이어프레임, 인터랙션, 상태·API 매핑 |
| [API 정의](docs/API%20정의.md) | REST, WebSocket, 공통 규칙 |
| [데이터 스키마 설계](docs/데이터%20스키마%20설계.md) | ERD 및 데이터 모델 |
| [협업 컨벤션](docs/협업%20컨벤션.md) | Git, PR, 커밋, 문서 선행 수정 원칙 |

### FE 녹화 시연 (2026-05-19)

시연 범위·UI 축소·MQTT·필수 API는 **[시연 MVP 합의](docs/시연_MVP_합의.md)** 와 **[FE-DAS MQTT 연동](docs/FE-DAS_MQTT_연동.md)** 이 SSOT이다. 구현 태스크는 [FE/TODO.md](FE/TODO.md), [BE/TODO.md](BE/TODO.md) 상단 「녹화 시연 MVP」를 따른다.

### 권장 읽기 순서

1. [프로젝트 정의서](docs/프로젝트%20정의서.md)로 범위를 파악하고, [화면 설계서](docs/화면%20설계서.md)로 화면별 상세를 확인합니다.
2. [API 정의](docs/API%20정의.md)로 클라이언트와 서버 간 계약을 파악합니다.
3. [데이터 스키마 설계](docs/데이터%20스키마%20설계.md)로 저장 구조를 파악합니다.
4. [협업 컨벤션](docs/협업%20컨벤션.md)으로 브랜치, PR, 문서 수정 순서를 맞춥니다.
5. **녹화 시연 시:** [시연 MVP 합의](docs/시연_MVP_합의.md) → [FE-DAS MQTT 연동](docs/FE-DAS_MQTT_연동.md) → FE/BE TODO MVP 절 → [에이전트 프롬프트](docs/시연_에이전트_프롬프트.md).

### 아키텍처 결정 기록 (ADR)

스택 변경, 경계 변경, 규칙 변경 등 중요한 결정은 [`docs/ADR/`](docs/ADR/)에 ADR 파일로 기록합니다. 파일명 규칙과 작성 방법은 [`docs/ADR/adr_template.md`](docs/ADR/adr_template.md)와 [협업 컨벤션](docs/협업%20컨벤션.md)의 ADR 섹션을 따릅니다.

## 협업

브랜치 이름, 커밋 메시지, PR 병합 방식 등은 [협업 컨벤션](docs/협업%20컨벤션.md)을 따릅니다.

PR 작성 시 [.github/pull_request_template.md](.github/pull_request_template.md)를 사용합니다. 이슈 작성 시 [.github/ISSUE_TEMPLATE/todo.md](.github/ISSUE_TEMPLATE/todo.md)를 사용할 수 있습니다.

## 외부 도구와 문서 역할

- [Jira](https://dapin1490-1778398763176.atlassian.net/jira/software/projects/SCADAPRJ/summary): Jira 요약 대시보드 URL. WBS를 기반으로 프로젝트 진행 상황 및 일정 관리.
- [Confluence](https://dapin1490-1778398763176.atlassian.net/wiki/spaces/S): Confluence 문서 루트 URL. 프로젝트 문서 및 공유 문서 관리. 회의록, 참고자료, 설계 및 개발 로그 등을 포함함.
- Gemini, ChatGPT: 자료 조사, 코드 이외의 자료 생성
- GitHub Copilot: 코드 생성(AGENTS.md 반영), PR 코드 리뷰
- Cursor: 코드 생성(AGENTS.md 반영)

- 최신 원본
  - API 경로와 요청·응답 필드는 GitHub의 docs/API 정의.md를 원본으로 하고, 컨플루언스에 작성된 문서는 GitHub 문서화 수동으로 동기화되는 복사본이다.
  - 초기 기획 초안은 Confluence, GitHub docs에는 확정 후 반영분을 둔다.

## 로컬 개발

### 통합 실행 (권장)

**전제:** BE·DAS·FE는 **Docker Compose**로만 기동한다. 호스트 JDK·Node 직접 실행은 하지 않는다.

```bash
# 프로젝트 루트, 최초 1회
cp .env.example .env
# .env 에 JWT_SECRET 설정 (openssl rand -base64 32)

docker compose up --build
```

| 확인 | URL·명령 |
| --- | --- |
| FE UI | http://localhost:3000 (로그인 `admin` / `demo123`) |
| BE health | http://localhost:8080/api/v1/actuator/health |
| BE 스모크 | `cd BE` → `python scripts/smoke-mvp-b1.py` |
| Node-RED | http://localhost:1880 |
| MQTT WS (FE) | `VITE_MQTT_URL=ws://localhost:9001` ([.env.example](.env.example)) |

통합 정의: [compose.yml](compose.yml). BE·DAS는 동일 MySQL(`SCADA_MySQL`)을 사용한다. 환경 변수: 루트 [.env.example](.env.example).

`init.sql` 변경 후에는 `docker compose down -v`로 DB 볼륨을 초기화한 뒤 재기동한다.

### 영역별 단독 실행 (선택)

| 파트 | Compose |
| --- | --- |
| BE | [BE/docker-compose.yml](BE/docker-compose.yml) — `BE/.env` 필요 |
| DAS&DB | [docker/docker-compose.yml](docker/docker-compose.yml) |
| FE | [FE/docker-compose.yml](FE/docker-compose.yml) — BE 선행·`be_default` 네트워크 |

단독 FE 기동 시 [FE/.env.example](FE/.env.example) 참고.

**DAS 포트:** Node-RED → MQTT **1883** (`MQTT_PORT`). FE 브라우저 → WebSocket **9001** (`MQTT_WS_PORT`). 상세: [docker/README.md](docker/README.md), [docs/FE-DAS_MQTT_연동.md](docs/FE-DAS_MQTT_연동.md) §7.

에이전트 지시: [docs/시연_에이전트_프롬프트.md](docs/시연_에이전트_프롬프트.md)

**MQTT·토픽:** [FE-DAS MQTT 연동](docs/FE-DAS_MQTT_연동.md)

**시연 계정:** `docs/API 정의.md` §1 (`admin` / `demo123`)

**하지 않음:** FE WebSocket, `GET /environment/areas/current`

### 영역별 상세

- FE: [FE/AGENTS.md](FE/AGENTS.md), [FE/TODO.md](FE/TODO.md)
- BE: [BE/AGENTS.md](BE/AGENTS.md), [BE/TODO.md](BE/TODO.md)
- DB 스키마(DAS MySQL): [docker/mysql/init.sql](docker/mysql/init.sql)