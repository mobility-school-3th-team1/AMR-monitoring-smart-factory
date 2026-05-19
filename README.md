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

### FE 녹화 시연 실행 순서 (Docker Compose)

**전제:** BE·DB(DAS)·FE는 각각 **자기 폴더의 `docker-compose.yml`** 로만 기동한다. 호스트 JDK·Node 직접 실행은 하지 않는다.

| 순 | 영역 | 명령 |
| --- | --- | --- |
| 1 | BE | `cd BE` → `cp .env.example .env` → `docker compose up --build` |
| 2 | BE 스모크 | [BE/TODO.md](BE/TODO.md) B1 (`:8080` health·§5.1 REST) |
| 3 | DAS&DB | `cd DB` → `docker compose up --build` (Mosquitto WS **9001** publish, Node-RED MQTT 발행) |
| 4 | FE | `cd FE` → `cp .env.example .env` → `docker compose up --build` |
| 5 | 녹화 | 호스트 브라우저에서 FE publish URL 접속 → 로그인 → SCR-01~05 |

| 파트 | Compose·이미지 |
| --- | --- |
| BE | [BE/docker-compose.yml](BE/docker-compose.yml) (**main에 있음**) |
| DB | `DB/docker-compose.yml` — **없을 때만** DAS 에이전트 작성 (미병합 브랜치 있을 수 있음) |
| FE | `FE/docker-compose.yml` — **없을 때만** FE 에이전트 F4에서 작성 |

에이전트 지시: [docs/시연_에이전트_프롬프트.md](docs/시연_에이전트_프롬프트.md) — 「진행 상황을 확인하고 **FE** / **BE** / **DAS·DB** 파트를 구현하라」

**환경 변수 (G4):** [FE/.env.example](FE/.env.example), [FE/frontend/.env.example](FE/frontend/.env.example) — `VITE_MQTT_URL=ws://localhost:9001`

**MQTT·토픽:** [FE-DAS MQTT 연동](docs/FE-DAS_MQTT_연동.md)

**시연 계정:** `docs/API 정의.md` §1 (`admin` / `demo123`)

**하지 않음:** FE WebSocket, `GET /environment/areas/current`

### 영역별 상세

- FE: [FE/AGENTS.md](FE/AGENTS.md), [FE/TODO.md](FE/TODO.md)
- BE: [BE/AGENTS.md](BE/AGENTS.md), [BE/TODO.md](BE/TODO.md)
- DB 스키마: [DB/init.sql](DB/init.sql)