# TODO.md - 백엔드 개발 계획

## 프로젝트 개요
AMR 스마트 팩토리 통합 모니터링 시스템의 백엔드 구현.
기술 스택: Java Spring Boot, Spring Security (JWT), RESTful API + WebSocket, MySQL (JPA/Hibernate).

## 현재 상태
- 기본 Spring Boot 프로젝트 구조 생성됨 (AmrControlSystemApplication.java, 테스트 파일).
- 설계 문서 확인 완료: 프로젝트 정의서, API 정의, 데이터 스키마 설계, 협업 컨벤션.
- ERD 및 API 명세 기반 API·서비스 로직 구현 진행 중.
- **BE 전용 Docker 실행 환경 구성 완료** (2026-05-17). PC에 Java를 설치하지 않아도 Docker만으로 백엔드 서버를 빌드·실행할 수 있다.
- 데이터 저장은 당분간 **임시 DB(H2)** 를 사용한다. DB 담당 영역 merge 후 MySQL로 전환 예정.

---

## 완료 작업 기록 (팀 공유용)

### [완료] BE 전용 Docker 실행 환경 구성 (2026-05-17)

**목적**  
개발 PC에 Java·Gradle 등을 따로 설치하지 않아도, Docker만으로 백엔드 서버를 동일한 방식으로 실행·확인할 수 있게 한다. 프론트엔드·DB·백엔드는 각자 Docker 환경을 먼저 만들고, 이후 프로젝트 루트에서 하나로 통합하기로 한 합의에 따라, **백엔드 영역만 단독으로** 구성했다.

**산출물 (주요 파일)**  
| 구분 | 위치 | 설명 |
|------|------|------|
| 컨테이너 빌드 정의 | `amr_control_system/Dockerfile` | 소스 코드를 패키징해 실행 가능한 서버 이미지를 만든다 |
| 실행 오케스트레이션 | `BE/docker-compose.yml` | 백엔드 서버 컨테이너 1개를 띄운다 (포트 8080) |
| Docker 전용 설정 | `application-docker.yaml` | 컨테이너 안에서 쓰는 DB·보안 토큰·상태 확인 설정 |
| 환경 변수 예시 | `BE/.env.example` | 보안 토큰 등 민감 설정의 입력 예시 (실제 값은 `.env`에 두며 Git에는 올리지 않음) |

**범위에 포함된 것**  
- 백엔드 서버 단독 기동 (http://localhost:8080)  
- 서버 정상 동작 확인용 상태 URL (`/actuator/health` → `{"status":"UP"}` 확인 완료)  
- 개발용 임시 DB(H2, 메모리). 서버를 끄면 데이터는 사라진다.

**범위에 포함되지 않은 것 (추후 작업)**  
- MySQL 등 DB 담당자 merge 결과 반영  
- 프론트엔드·DB와 한 번에 띄우는 **프로젝트 루트 통합 docker-compose**  
- 로그인·대시보드 등 **비즈니스 API** 구현 (별도 TODO)

**실행 방법 (개발자·검증 담당)**  
`BE` 폴더에서: `docker compose up --build`  
상태 확인: http://localhost:8080/actuator/health

---

## 다음 예정 작업 (팀 공유용)

| 순서 | 작업 | 한 줄 설명 | 영향 |
|------|------|-----------|------|
| 1 | **로그인·인증 API 완성** | 사용자 로그인, 토큰 갱신·로그아웃을 명세대로 제공 | 화면에서 로그인·API 호출이 가능해지는 첫 단계 |
| 2 | **공통 오류 응답 정리** | API 실패 시 형식을 통일 | 프론트·운영이 오류를 일관되게 처리 |
| 3 | **대시보드 API** | 메인 화면 KPI·최근 알람 등 요약 데이터 제공 | 관제 메인 화면 연동 시작 |
| 4 | AMR·충전·알람 등 도메인 API | 설계 문서의 나머지 REST API 순차 구현 | 기능별 화면 연동 |
| 5 | 실시간(WebSocket) | 위치·알람 등 실시간 푸시 | 대시보드 실시간 갱신 |
| (병렬) | DB merge 후 Docker·MySQL 연동 | 실제 DB 스키마와 백엔드 연결 | 임시 H2 대신 운영에 가까운 DB 사용 |
| (병렬) | 루트 통합 Docker | FE·DB·BE를 한 명령으로 기동 | 통합 데모·QA 환경 |

**현재 진행 예정 1순위:** 위 표의 **「로그인·인증 API 완성」** (TODO 섹션 4·5의 Auth 관련 항목).

---

## 개발 계획 (작은 단위로 순차 진행)

### 0. Docker 실행 환경 (BE 단독) — 완료 (2026-05-17)
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
- [ ] **다음 작업** AuthController.java (로그인, 리프레시, 로그아웃). 로그인만 부분 구현됨.
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
1. ~~Docker로 BE 기동 가능한 환경 확보 (섹션 0).~~ **완료**
2. **다음:** 인증 API 완성 (Auth refresh/logout, `/api/v1` 경로, JWT 설정, 시드 사용자).
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