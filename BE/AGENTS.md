# AGENTS.md - 백엔드 (BE)

## 프로젝트 맥락

이 폴더는 AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드이다.
공장 내 AMR 위치/상태, 환경 센서, 충전 스테이션, 작업 이력 데이터를 관리하고, 프론트엔드에 REST API 및 WebSocket을 통해 제공한다.

전체 시스템은 FE(프론트엔드, Vue.js) / BE(백엔드) / DB(MySQL, Node-RED)로 구성되며, BE는 MySQL 데이터베이스와 연동하고 FE에 데이터를 제공하는 중간 계층이다.

## 설계 문서 참조

이 프로젝트의 설계 문서는 상위 디렉토리에 위치한다.
작업 시작 전, 또는 관련 기능 구현 시 반드시 다음 문서를 읽어야 한다:

- `../docs/프로젝트 정의서.md` - 시스템 아키텍처, 핵심 기능, 화면 설계
- `../docs/API 정의.md` - **RESTful API 명세** (엔드포인트, 요청/응답 형식, WebSocket 이벤트)
- `../docs/데이터 스키마 설계.md` - **ERD 및 테이블 정의** (엔티티 매핑에 필수)
- `../docs/협업 컨벤션.md` - Git 워크플로우, 커밋, PR, 코드 품질 규칙

API 엔드포인트를 구현할 때에는 `../docs/API 정의.md`에 정의된 경로, 파라미터, 응답 형식을 정확히 따라야 한다.
엔티티 클래스를 작성할 때에는 `../docs/데이터 스키마 설계.md`의 ERD를 기준으로 한다.

## 공통 규칙

### Git 컨벤션

- 커밋 메시지: `태그(be): 변동사항 설명 스페이스-키 #이슈번호`
  - 태그: `feat`, `fix`, `docs`, `chore`, `refactor`
  - 백엔드 영역은 `be`로 표기한다
- 브랜치: `feature/<스페이스 키>-<이슈번호>-<짧은-기능명>` (소문자, 케밥 케이스)
- main 직접 커밋 금지. PR을 통해서만 merge한다.
- PR 병합은 merge commit 방식만 사용한다.

### 코드 품질

- 파일 인코딩: UTF-8
- 폴더명: 소문자, 공백은 `_`로 대체
- 설계 변경 시: `../docs/`의 명세 문서를 먼저 수정한 뒤, 문서에 따라 구현한다
- PR 전 로컬에서 정합성 검토 및 오류 없이 실행되는지 확인한다

## 작업 계획 (TODO.md)

- 다음 작업을 계획할 때 이 폴더의 `TODO.md`를 최우선으로 참고한다.
- `TODO.md`가 없는 경우, 설계 문서(`../docs/`)와 이 폴더 내 코드의 구현 정도를 분석하여 남은 개발 범위에 대한 `TODO.md`를 새로 작성한다.
- `TODO.md`에는 개발이 완료되기까지의 모든 계획이 포함되어야 하며, 작은 단위로 하나씩 차례대로 진행할 수 있도록 세분화한다.
- 각 작업 항목은 인간 작업자가 보아도 이해할 수 있도록 충분한 설명을 포함한다.
- 한 번에 하나의 태스크만 진행한다.

## 에이전트 행동 규칙

1. 정보를 제시하기 전에 반드시 검증한다. 명확한 근거 없이 추측하거나 가정하지 않는다.
2. 파일 단위로 변경한다. 사용자가 실수를 확인할 수 있도록 한다.
3. 사과 표현을 사용하지 않는다.
4. 코드 주석이나 문서에 "이해했습니다" 등의 피드백을 넣지 않는다.
5. 공백 변경을 제안하지 않는다.
6. 명시적으로 요청받은 변경 외에 임의로 다른 변경을 만들지 않는다.
7. 관련 없는 코드나 기능을 제거하지 않는다. 기존 구조를 보존하는 데 주의를 기울인다.
8. 같은 파일에 대한 편집은 여러 단계로 나누지 않고 한 번에 제공한다.
9. 실제 수정이 필요하지 않은 파일에 대해 불필요한 업데이트를 제안하지 않는다.
10. 컨텍스트에서 생성된 파일이 아닌, 실제 파일 링크를 제공한다.
11. 특별히 요청받지 않는 한 현재 구현을 보여주거나 논의하지 않는다.
12. 현재 파일 내용과 구현을 확인할 때 컨텍스트에서 생성된 파일 내용을 검토한다.
13. 짧고 모호한 변수명 대신 서술적이고 명시적인 변수명을 사용하여 가독성을 높인다.
14. 프로젝트에서 사용 중인 코딩 스타일을 일관되게 따른다.
15. 제안하는 변경이 프로젝트에서 지정한 언어 및 프레임워크 버전과 호환되는지 확인한다.
16. 하드코딩된 값 대신 명명된 상수를 사용하여 코드의 명확성과 유지보수성을 높인다.

## 기술 스택

- Java Spring Boot
- Spring Security (JWT 인증)
- RESTful API + WebSocket
- MySQL (JPA/Hibernate)

## 백엔드 규칙

### 패키지 구조

표준 레이어드 아키텍처를 따른다:

```
src/main/java/com/{group}/{artifact}/
  ├── controller/    REST 컨트롤러
  ├── service/       비즈니스 로직
  ├── repository/    데이터 접근 (JPA Repository)
  ├── entity/        JPA 엔티티
  ├── dto/           요청/응답 DTO
  ├── config/        설정 클래스 (Security, WebSocket 등)
  └── exception/     예외 처리
```

### 네이밍 규칙

- 클래스명: PascalCase (예: `AmrController`, `ChargingStationService`)
- 메서드명: camelCase (예: `findAllAmrs()`, `getStationById()`)
- 엔티티 클래스: DB 테이블명과 매핑한다 (예: `Amr` → `AMR`, `AmrStatusLog` → `AMR_STATUS_LOG`)
- DTO: 용도를 접미사로 표기한다 (예: `AmrResponseDto`, `AlarmCreateRequestDto`)
- 상수: UPPER_SNAKE_CASE (예: `DEFAULT_PAGE_SIZE = 20`)

### REST API 구현

- 기본 경로: `/api/v1`
- 인증: JWT 기반 (`Authorization: Bearer <token>`)
- 목록 API 기본값: `page=1`, `limit=20`
- 날짜/시간: ISO 8601 UTC
- 리소스명은 복수형을 우선한다 (예: `/amrs`, `/alarms`, `/charging/stations`)
- 공통 응답 코드: 200, 201, 202, 204, 400, 401, 403, 404, 409, 422, 500

### 에러 처리

- `@RestControllerAdvice`를 활용한 전역 예외 처리를 구현한다
- 비즈니스 예외는 커스텀 예외 클래스로 정의한다
- 에러 응답은 일관된 형식을 사용한다

### WebSocket

- 엔드포인트: `/api/v1/stream`
- 연결 시 JWT 인증을 수행한다
- 이벤트 타입: `amrs.position.updated`, `amrs.status.updated`, `alarms.created`, `charging.forecast.updated`, `dashboard.summary.updated`

### 보안

- Spring Security 기반 인증/인가를 구현한다
- 모든 변경 API(POST, PUT, DELETE)는 인증이 필요하다
- 비밀번호, 시크릿 키 등 민감 정보는 코드에 하드코딩하지 않는다 (환경 변수 또는 설정 파일 사용)
