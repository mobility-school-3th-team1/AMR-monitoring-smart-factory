# AGENTS.md - 데이터베이스 및 DAS (DB)

## 프로젝트 맥락

이 폴더는 AMR 스마트 팩토리 통합 모니터링 시스템의 데이터베이스 스키마와 데이터 수집 시뮬레이션(DAS)을 담당한다.
MySQL 데이터베이스 스키마를 정의/관리하고, Node-RED를 사용하여 시뮬레이션 데이터를 생성한다.

전체 시스템은 FE(프론트엔드, Vue.js) / BE(백엔드, Spring Boot) / DB(데이터베이스, DAS)로 구성되며, DB는 BE가 접근하는 데이터 저장소와 데이터 생성 파이프라인을 관리한다.

## 설계 문서 참조

이 프로젝트의 설계 문서는 상위 디렉토리에 위치한다.
작업 시작 전, 또는 관련 기능 구현 시 반드시 다음 문서를 읽어야 한다:

- `../docs/데이터 스키마 설계.md` - **ERD 및 테이블 정의** (스키마 작업의 기준 문서)
- `../docs/프로젝트 정의서.md` - 시스템 아키텍처, 핵심 기능, 모니터링 요소 정의
- `../docs/API 정의.md` - RESTful API 명세 (BE가 요구하는 데이터 구조 파악에 필요)
- `../docs/협업 컨벤션.md` - Git 워크플로우, 커밋, PR, 코드 품질 규칙

스키마를 변경할 때에는 반드시 `../docs/데이터 스키마 설계.md`를 먼저 수정한 뒤, 변경된 문서에 따라 DDL을 구현한다.
BE의 API 응답 구조에 영향을 주는 변경이 있으면, `../docs/API 정의.md`도 함께 검토한다.

## 공통 규칙

### Git 컨벤션

- 커밋 메시지: `태그(db): 변동사항 설명 스페이스-키 #이슈번호`
  - 태그: `feat`, `fix`, `docs`, `chore`, `refactor`
  - 데이터베이스 영역은 `db`로 표기한다
- 브랜치: `feature/<스페이스 키>-<이슈번호>-<짧은-기능명>` (소문자, 케밥 케이스)
- main 직접 커밋 금지. PR을 통해서만 merge한다.
- PR 병합은 merge commit 방식만 사용한다.

### 코드 품질

- 파일 인코딩: UTF-8
- 폴더명: 소문자, 공백은 `_`로 대체
- 설계 변경 시: `../docs/`의 명세 문서를 먼저 수정한 뒤, 문서에 따라 구현한다
- PR 전 로컬에서 정합성 검토 및 오류 없이 실행되는지 확인한다

## 기술 스택

- MySQL (관계형 데이터베이스)
- Node-RED (시뮬레이션 데이터 생성, 설비 데이터 수집 모사)

## 데이터베이스 규칙

### 테이블/컬럼 네이밍

- 테이블명: UPPER_SNAKE_CASE (예: `AMR_STATUS_LOG`, `AMR_CHARGE_STATION`, `ENV_READING`)
- 컬럼명: lower_snake_case (예: `amr_id`, `battery_pct`, `measured_at`)
- PK 컬럼: `{테이블약어}_id` 형식을 기본으로 한다 (예: `amr_id`, `area_id`, `station_id`)
- FK 컬럼: 참조 테이블의 PK 컬럼명을 그대로 사용한다
- 시각 컬럼: `_at` 또는 `_time` 접미사 (예: `measured_at`, `start_time`, `updated_at`)
- 상태 컬럼: `status` 또는 `{접두어}_status` (예: `status`, `station_status`, `session_status`)

### 주요 엔티티

ERD 기준 테이블 목록 (상세 정의는 `../docs/데이터 스키마 설계.md` 참조):

- `SITE`, `AREA` - 공장/구역 정의
- `ENV_SENSOR`, `ENV_READING` - 환경 센서 및 계측값
- `PRODUCT`, `PROCESS_MASTER`, `ROUTING` - 제품/공정/라우팅
- `WORK_ORDER`, `WIP_LOT` - 주문/생산 LOT
- `AMR`, `AMR_STATUS_LOG`, `AMR_TASK` - AMR 및 상태/작업 관리
- `AMR_CHARGE_STATION`, `AMR_CHARGING_SESSION` - 충전 관리

### DDL 작성 규칙

- DDL 파일은 실행 순서를 고려하여 작성한다 (FK 참조 대상 테이블을 먼저 생성)
- `CREATE TABLE` 문에 `IF NOT EXISTS`를 사용한다
- 외래 키 제약 조건을 명시한다
- 인덱스는 자주 조회되는 컬럼에 생성한다 (특히 FK, 시각 컬럼, 상태 컬럼)
- 문자열 인코딩: `utf8mb4`, 콜레이션: `utf8mb4_unicode_ci`

### 데이터 타입

ERD에 정의된 타입을 따른다:

- ID (PK): `INT` 또는 `BIGINT` (AUTO_INCREMENT)
- 문자열: `VARCHAR(n)`
- 소수점: `DECIMAL(p,s)` 또는 `FLOAT`/`DOUBLE`
- 시각: `DATETIME`
- 날짜: `DATE`

## Node-RED 규칙

### 플로우 구성

- 플로우 파일(`flows.json`)은 기능 단위로 탭을 구분한다
- 노드 이름은 해당 노드의 역할을 명확하게 표현한다
- Function 노드 내부의 JavaScript는 간결하게 유지하고, 복잡한 로직은 별도 모듈로 분리한다

### 시뮬레이션 데이터

- 시뮬레이션 데이터는 ERD에 정의된 테이블 구조와 데이터 타입에 맞추어 생성한다
- 현실적인 범위의 값을 사용한다 (예: 배터리 잔량 0-100%, 온도 -10~50도)
- 시간 데이터는 ISO 8601 UTC를 사용한다
