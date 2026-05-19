# 온보딩 가이드

AMR(자율 이동 로봇) 기반 스마트 팩토리 통합 모니터링 시스템에 합류한 개발자를 위한 시작 문서입니다. 저장소 구조, 로컬 실행 방법, 협업 규칙, 참고 문서를 한곳에서 확인할 수 있습니다.

## 1. 프로젝트 개요

공장 내 AMR 위치·상태, 환경 센서, 충전 스테이션, 작업 이력을 실시간으로 관제·분석하는 웹 애플리케이션입니다.

| 영역 | 기술 | 역할 |
|------|------|------|
| FE | Vue 3, Vite, Pinia, ECharts | 대시보드 UI, REST/WebSocket 연동 |
| BE | Java 17, Spring Boot 3.5, JPA, JWT | REST API, 인증, 비즈니스 로직 |
| DB | MySQL, Node-RED(예정) | 스키마·시드, 시뮬레이션 데이터(DAS) |

시스템 아키텍처와 화면 설계는 `docs/프로젝트 정의서.md`를 참고하세요.

## 2. 저장소 구조

```
AMR-monitoring-smart-factory/
├── FE/          # FE 루트 (세부 앱 경로는 추가 예정)
├── BE/
│   ├── amr_control_system/   # Spring Boot 소스
│   ├── docker-compose.yml    # BE 단독 Docker 실행
│   └── .env.example          # JWT 등 환경 변수 예시
├── DB/
│   └── init.sql              # MySQL DDL·시드 (18개 테이블)
├── docs/                 # 설계 명세 (API, ERD, 협업 규칙 등)
├── .github/              # 이슈·PR 템플릿
├── AGENTS.md             # AI·개발 공통 규칙 (루트)
├── ONBOARDING.md         # 본 문서
└── README.md
```

영역별 상세 규칙은 각 폴더의 `AGENTS.md`와 `BE/TODO.md`를 확인하세요.

## 3. 사전 요구사항

| 도구 | 권장 버전 | 용도 |
|------|-----------|------|
| Git | 최신 | 소스 관리 |
| Docker Desktop | 최신 | BE 로컬 실행(권장) |
| Node.js | 18 LTS 이상 | FE 개발 서버 |
| Java JDK | 17 | BE 로컬 Gradle 실행(선택) |
| MySQL | 8.x | `docker/mysql/init.sql` (DAS compose 시 자동 적용) |

선택 도구: OpenSSL(`openssl rand -base64 32`), IDE(VS Code/Cursor, IntelliJ).

## 4. 필수 설계 문서

기능 구현·스키마 변경 전에 아래 문서를 먼저 읽습니다. **설계 변경은 문서 수정 후 코드 반영**이 원칙입니다.

| 문서 | 경로 | 용도 |
|------|------|------|
| 프로젝트 정의서 | `docs/프로젝트 정의서.md` | 아키텍처, 핵심 기능, 화면 객체 번호 |
| API 정의 | `docs/API 정의.md` | REST·WebSocket 엔드포인트 |
| 데이터 스키마 | `docs/데이터 스키마 설계.md` | ERD, 테이블 정의 |
| 협업 컨벤션 | `docs/협업 컨벤션.md` | Git, PR, 커밋, ADR |

## 5. 로컬 개발 환경

### 5.1 저장소 클론

```bash
git clone https://github.com/mobility-school-3th-team1/AMR-monitoring-smart-factory
cd AMR-monitoring-smart-factory
git checkout dev
```

기본 통합 브랜치는 `dev`입니다. `main`에는 직접 커밋하지 않고 PR로만 병합합니다.

### 5.2 백엔드 실행 (Docker, 권장)

Java 설치 없이 BE만 기동할 때 사용합니다. 현재 프로필은 **H2 인메모리 DB**이며, 컨테이너를 내리면 데이터가 초기화됩니다.

1. `BE/.env` 생성

```bash
cd BE
cp .env.example .env
```

2. `BE/.env`에서 `JWT_SECRET` 설정

- Base64로 인코딩했을 때 **32바이트 이상**이어야 합니다.
- 생성 예: `openssl rand -base64 32`
- 값에 `+`, `=`가 있으면 따옴표로 감쌉니다.
- `.env`는 Git에 커밋하지 않습니다.

3. 컨테이너 기동

```bash
docker compose up --build
```

4. 동작 확인

- 헬스체크: http://localhost:8080/api/v1/actuator/health  
  응답 예: `{"status":"UP"}`
- API 기본 경로: `http://localhost:8080/api/v1`

개발용 데모 계정은 `DEMO_USER_PASSWORD`가 설정되어 있을 때만 생성됩니다(`.env.example` 기본값: `demo123`).

| 항목 | 값 |
|------|-----|
| 사용자명 | `admin` |
| 비밀번호 | `BE/.env`의 `DEMO_USER_PASSWORD` (예: `demo123`) |

API 명세 예시는 `docs/API 정의.md`의 `POST /auth/login`을 참고하세요.

### 5.3 백엔드 실행 (Gradle, 선택)

```bash
cd BE/amr_control_system
# Windows
set JWT_SECRET=<Base64_32바이트_이상>
set DEMO_USER_PASSWORD=demo123
gradlew.bat bootRun

# macOS / Linux
export JWT_SECRET=<Base64_32바이트_이상>
export DEMO_USER_PASSWORD=demo123
./gradlew bootRun
```

### 5.4 프론트엔드 실행

```bash
cd FE/frontend
npm install
npm run dev
```

- 개발 서버: http://localhost:3001 (포트 고정, `strictPort: true`)
- API 호출 기본 경로: `/api/v1` (axios 설정 파일 경로는 현재 저장소 기준 미확정이므로, 실제 파일이 추가되면 해당 경로로 문서를 갱신하세요)

프론트는 **3001**, 백엔드는 **8080**에서 동작하므로, 로컬에서 API를 쓰려면 Vite 프록시 설정이 필요합니다. 아래 설정은 **예시**이며, 실제 `vite.config.*` 파일이 추가된 뒤 저장소 구조에 맞는 경로로 문서를 수정하세요.

```javascript
server: {
  port: 3001,
  strictPort: true,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

설정 후 `npm run dev`를 다시 실행하고, 로그인 화면(http://localhost:3001/login)에서 데모 계정으로 접속을 확인합니다.

기타 스크립트:

| 명령 | 설명 |
|------|------|
| `npm run build` | 프로덕션 빌드 |
| `npm run preview` | 빌드 결과 미리보기 |
| `npm run lint` | ESLint 검사 |

### 5.5 데이터베이스 (MySQL)

물리 스키마와 시드는 `docker/mysql/init.sql`에 정의되어 있습니다.

**DAS compose (권장):**

```bash
# 프로젝트 루트
cp .env.example .env   # 최초 1회
docker compose --env-file .env -f docker/docker-compose.yml up -d --build
```

MySQL만 수동 적용할 때:

```bash
mysql -u <user> -p <database_name> < docker/mysql/init.sql
```

**현재 BE는 MySQL이 아닌 H2를 사용**합니다. JPA 엔티티와 `init.sql` 테이블명·구조가 아직 완전히 일치하지 않으며, MySQL 연동·엔티티 정합은 `BE/TODO.md` 1순위 작업으로 진행 중입니다. DAS·스키마 작업 시 `docker/README.md`, `DB/AGENTS.md`, `docs/데이터 스키마 설계.md`를 함께 봅니다.

Node-RED 시연 플로우: `docker/node-red/flows.json` (탭 「시연 MVP」). Mosquitto **1883**(Node-RED), **9001**(FE WebSocket).

## 6. 현재 환경 제한 사항

온보딩 시 아래 상태를 알고 있으면 혼선을 줄일 수 있습니다.

| 항목 | 현재 상태 |
|------|-----------|
| BE 데이터 저장소 | H2 인메모리 (`application.yaml`, `application-docker.yaml`) |
| 물리 MySQL | `docker/mysql/init.sql`, DAS compose로 기동 가능 |
| Docker Compose | `BE/`, `docker/`, `FE/` 각각 분리. DAS: `docker/docker-compose.yml` |
| WebSocket `/api/v1/stream` | 시연 FE **미사용** |
| FE 실시간 | DAS MQTT `ws://localhost:9001` (`docs/FE-DAS_MQTT_연동.md`) |

상세 백로그는 `BE/TODO.md`를 참고하세요.

## 7. 협업 워크플로

### 7.1 브랜치·커밋

- 분기 기준: 최신 `dev`
- 브랜치 예: `feature/<스페이스-키>-<이슈번호>-<짧은-기능명>`
- 커밋 형식: `태그(영역): 변동사항 설명 스페이스-키 #이슈번호`
  - 태그: `feat`, `fix`, `docs`, `chore`, `refactor`
  - 영역: `fe`, `be`, `db`
- PR 병합: **merge commit만** 사용 (squash/rebase 지양)

### 7.2 이슈 → PR

1. GitHub 이슈 생성 (템플릿: `.github/ISSUE_TEMPLATE/todo.md`)
2. 이슈에서 브랜치 생성
3. 구현 후 로컬에서 실행·린트 확인
4. PR 작성 (템플릿: `.github/pull_request_template.md`)
   - `Closes #<이슈번호>` 필수
   - 이슈 TODO 체크리스트를 PR에 복사해 완료 항목 표시
5. Copilot 리뷰 확인 후 팀 리뷰·approve
6. `dev` 또는 `main`으로 merge

문의·논의는 해당 이슈 코멘트에 남깁니다(Discussion 미사용).

### 7.3 코드 품질

- FE: ESLint (`npm run lint`), pre-commit에 lint-staged 설정
- BE: Checkstyle 미도입. PR 전 로컬 기동·API 스모크 권장
- 인코딩: UTF-8

## 8. 영역별 작업 시작

| 작업 영역 | 먼저 읽을 문서 | 작업 계획 |
|-----------|----------------|-----------|
| 프론트엔드 | `FE/AGENTS.md`, `docs/프로젝트 정의서.md` 화면설계 | `FE/TODO.md`(없으면 생성) |
| 백엔드 | `BE/AGENTS.md`, `docs/API 정의.md` | `BE/TODO.md` |
| DB/DAS | `DB/AGENTS.md`, `docs/데이터 스키마 설계.md` | `DB/TODO.md`(없으면 생성) |

한 번에 하나의 태스크만 진행합니다. API·스키마 변경 시 관련 `docs/` 파일을 먼저 수정합니다.

## 9. 자주 쓰는 URL·경로

| 대상 | URL/경로 |
|------|----------|
| FE 개발 | http://localhost:3001 |
| BE API | http://localhost:8080/api/v1 |
| BE 헬스 | http://localhost:8080/api/v1/actuator/health |
| 로그인 API | `POST /api/v1/auth/login` |
| H2 콘솔 | BE 로컬 실행 시 활성화 (`application.yaml`) |

## 10. 문제 해결

### JWT_SECRET 관련 오류

- 비어 있거나 32바이트 미만이면 애플리케이션이 기동되지 않을 수 있습니다.
- `BE/.env` 또는 환경 변수에 유효한 Base64 값을 설정합니다.

### FE에서 API 404·CORS·연결 실패

- BE가 8080에서 실행 중인지 확인합니다.
- Vite 프록시(`5.4` 절)가 설정되어 있는지 확인합니다.
- 브라우저 네트워크 탭에서 요청 URL이 `http://localhost:3001/api/v1/...`인지, 프록시 후 8080으로 전달되는지 확인합니다.

### Docker BE가 unhealthy

- `BE/.env` 존재 및 `JWT_SECRET` 설정 여부를 확인합니다.
- `docker compose logs be`로 기동 로그를 확인합니다.
- 최초 기동은 `start_period` 동안 시간이 걸릴 수 있습니다.

### 로그인 401

- `DEMO_USER_PASSWORD`가 BE 환경에 설정되어 있는지 확인합니다.
- 사용자명 `admin`, 비밀번호는 `.env`와 동일한지 확인합니다.

## 11. 온보딩 체크리스트

신규 팀원은 아래 순서로 환경을 검증합니다.

- [ ] 저장소 클론 및 `dev` 브랜치 확인
- [ ] `docs/프로젝트 정의서.md`, `docs/협업 컨벤션.md` 읽기
- [ ] `BE/.env` 작성 후 `docker compose up --build`로 BE 기동
- [ ] http://localhost:8080/api/v1/actuator/health → `UP` 확인
- [ ] FE `npm install` 및 Vite 프록시 설정 후 `npm run dev`
- [ ] http://localhost:3001/login 에서 `admin` / 데모 비밀번호 로그인
- [ ] 담당 영역 `AGENTS.md` 및 `TODO.md` 확인
- [ ] 첫 이슈·브랜치·PR 템플릿 흐름 숙지

## 12. 추가 참고

- AI·자동화 도구 규칙: 루트 `AGENTS.md`
- ADR 작성 규칙: `docs/협업 컨벤션.md` 6절
- 외부 협업: Jira(일정), Confluence(docs 미러 가능)

문서가 코드와 어긋나면 **코드와 설계 문서 중 최신 항목을 기준으로 이슈에 기록**하고, 필요 시 본 가이드와 `docs/`를 함께 갱신합니다.
