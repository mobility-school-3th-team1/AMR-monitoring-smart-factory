# TODO.md - 백엔드 개발 계획

## 프로젝트 개요
AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## 현재 상태
- 기본 Spring Boot 프로젝트 구조 생성됨 (AmrControlSystemApplication.java, 테스트 파일).
- 설계 문서 확인 완료: 프로젝트 정의서, API 정의, 데이터 스키마 설계, 협업 컨벤션.
- ERD 및 API 명세 기반으로 구현 필요.
- BE 전용 Docker 환경 구성 완료 (H2, `BE/docker-compose.yml`). 빌드·실행은 Docker로 수행한다.

## 개발 계획 (작은 단위로 순차 진행)

### 0. Docker 실행 환경 (BE 단독)
- [x] `amr_control_system/Dockerfile` (multi-stage, Java 17).
- [x] `amr_control_system/.dockerignore`.
- [x] `BE/docker-compose.yml` (서비스 `be`, 포트 8080).
- [x] `application-docker.yaml` (H2, JWT 환경 변수, Actuator health).
- [x] `BE/.env.example` (JWT_SECRET 등).
- [x] Docker로 `docker compose up --build` 기동 및 `/actuator/health` 응답 확인.
- [ ] DB 영역 merge 후: MySQL 서비스·드라이버·datasource profile 연동 (별도 작업).
- [ ] FE·DB·BE Docker 완료 후: 프로젝트 루트 통합 `docker-compose` 작성 (별도 작업).

### 1. 데이터베이스 설정 및 엔티티 구현
- [x] application.yaml에 H2 인메모리 DB 설정 추가 (MySQL 대신 개발용).
- [x] build.gradle에 JPA 및 H2 의존성 추가.
- [x] JPA 엔티티 클래스 생성 (데이터 스키마 설계.md의 ERD 기반).
  - [x] Site.java
  - [x] Area.java
  - [x] EnvSensor.java
  - [x] EnvReading.java
  - [x] Product.java
  - [x] ProcessMaster.java
  - [x] Routing.java
  - [x] WorkOrder.java
  - [x] AmrChargeStation.java
  - [x] Amr.java
  - [x] AmrChargingSession.java
  - [x] WipLot.java
  - [x] AmrTask.java
  - [x] AmrStatusLog.java
- [x] JPA Repository 인터페이스 생성 (각 엔티티별).
  - [x] SiteRepository.java
  - [x] AreaRepository.java
  - [x] EnvSensorRepository.java
  - [x] EnvReadingRepository.java
  - [x] ProductRepository.java
  - [x] ProcessMasterRepository.java
  - [x] RoutingRepository.java
  - [x] WorkOrderRepository.java
  - [x] AmrChargeStationRepository.java
  - [x] AmrRepository.java
  - [x] AmrChargingSessionRepository.java
  - [x] WipLotRepository.java
  - [x] AmrTaskRepository.java
  - [x] AmrStatusLogRepository.java

### 2. DTO 클래스 구현
- [x] 요청/응답 DTO 생성 (API 정의.md 기반).
  - [x] 인증 관련 DTO (LoginRequestDto, LoginResponseDto, UserDto, RefreshTokenRequestDto, RefreshTokenResponseDto).
  - [x] 대시보드 DTO (DashboardSummaryDto, AlarmSummaryDto, RecentAlarmsDto).
  - [x] AMR 관련 DTO (AmrDto, AmrStatusHistoryDto, AmrPathDto, AmrCommandRequestDto, AmrCommandResponseDto 등).
  - [x] 충전 관련 DTO (ChargingStationDto, ChargingStationListDto, ForecastBucketDto, ChargingForecastDto).
  - [x] 알람 관련 DTO (AlarmDto, AlarmListResponseDto, AlarmAckRequestDto, AlarmAckResponseDto).
  - [x] 작업 이력 DTO (WorkHistoryDto, WorkHistoryListResponseDto).
  - [x] 분석 DTO (AnalyticsKpiDto, AnalyticsBatteryDto, AnalyticsWorkloadDto).

### 3. 보안 및 인증 구현
- [x] Spring Security 설정 클래스 생성 (JwtAuthenticationFilter, SecurityConfig).
- [x] JWT 유틸리티 클래스 생성 (토큰 생성/검증).
- [x] 사용자 엔티티 및 Repository 추가 (기본 사용자 관리).

### 4. 컨트롤러 구현
- [ ] AuthController.java (로그인, 리프레시, 로그아웃).
- [ ] DashboardController.java (요약, 최근 알람 등).
- [ ] AmrController.java (AMR 목록, 상세, 상태 이력 등).
- [ ] ChargingController.java (충전 스테이션, 대기열, 예측 등).
- [ ] AlarmController.java (알람 목록, 확인 등).
- [ ] WorkHistoryController.java (작업 이력 조회, 내보내기).
- [ ] AnalyticsController.java (KPI, 배터리 분석 등).

### 5. 서비스 로직 구현
- [ ] 각 컨트롤러에 대응하는 Service 클래스 생성.
- [ ] 비즈니스 로직 구현 (데이터 조회, 계산, 검증 등).

### 6. WebSocket 구현
- [ ] WebSocket 설정 클래스 생성.
- [ ] 이벤트 발행 서비스 구현 (실시간 데이터 스트리밍).

### 7. 예외 처리 및 로깅
- [ ] 전역 예외 처리 클래스 (@RestControllerAdvice).
- [ ] 커스텀 예외 클래스 정의.

### 8. 테스트 및 검증
- [ ] 단위 테스트 작성 (Service, Repository).
- [ ] 통합 테스트 작성 (Controller).
- [ ] 빌드 및 실행 검증 (Docker: `docker compose build`, `docker compose up`; 테스트는 `docker compose run --rm be` 등).

### 9. 추가 기능
- [ ] 파일 내보내기 기능 (Excel/CSV 다운로드).
- [ ] 캐싱 또는 최적화 (필요 시).

## 작업 우선순위
1. Docker로 BE 기동 가능한 환경 확보 (섹션 0).
2. 인증 API 완성 (Auth refresh/logout, `/api/v1` 경로, JWT 설정, 시드 사용자).
3. 예외 처리 후 대시보드·도메인 API 순차 구현.
4. WebSocket과 실시간 기능 구현.
5. DB merge 후 MySQL 연동 및 루트 통합 compose.
6. 테스트 및 문서화.

## Docker 실행 (BE 폴더에서)
```bash
cp .env.example .env
docker compose up --build
```
- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health

## 참고
- 설계 문서를 변경 시 먼저 수정 후 구현.
- 커밋 메시지: `feat(be): 엔티티 클래스 추가 #이슈번호`
- PR 전 로컬 검증 필수.