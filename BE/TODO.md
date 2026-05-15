# TODO.md - 백엔드 개발 계획

## 프로젝트 개요
AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## 현재 상태
- 기본 Spring Boot 프로젝트 구조 생성됨 (AmrControlSystemApplication.java, 테스트 파일).
- 설계 문서 확인 완료: 프로젝트 정의서, API 정의, 데이터 스키마 설계, 협업 컨벤션.
- ERD 및 API 명세 기반으로 구현 필요.

## 개발 계획 (작은 단위로 순차 진행)

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
- [ ] Spring Security 설정 클래스 생성 (JwtAuthenticationFilter, SecurityConfig).
- [ ] JWT 유틸리티 클래스 생성 (토큰 생성/검증).
- [ ] 사용자 엔티티 및 Repository 추가 (기본 사용자 관리).

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
- [ ] 빌드 및 실행 검증 (Gradle build, 로컬 실행).

### 9. 추가 기능
- [ ] 파일 내보내기 기능 (Excel/CSV 다운로드).
- [ ] 캐싱 또는 최적화 (필요 시).

## 작업 우선순위
1. 먼저 엔티티와 Repository부터 구현하여 데이터 모델 구축.
2. DTO와 기본 컨트롤러로 API 골격 완성.
3. 보안 구현 후 서비스 로직 추가.
4. WebSocket과 실시간 기능 구현.
5. 테스트 및 문서화.

## 참고
- 설계 문서를 변경 시 먼저 수정 후 구현.
- 커밋 메시지: `feat(be): 엔티티 클래스 추가 #이슈번호`
- PR 전 로컬 검증 필수.