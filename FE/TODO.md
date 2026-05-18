# 프론트엔드 구현 계획 (TODO)

> 목록 업데이트: 2026-05-18
> 
> **기준 문서**
> - `../docs/프로젝트 정의서.md` - 화면 설계 명세
> - `../docs/API 정의.md` - RESTful API 명세
> - `../mockup/*.html` - 목업 화면

## 1단계: 인증 및 기본 레이아웃 구현

### 1.1 로그인 화면 구현
- [ ] LoginView.vue 컴포넌트 구현
  - 사용자명/비밀번호 입력 필드
  - 로그인 버튼 및 에러 메시지 표시
  - 참고 파일: `mockup/auth-login.html`
  - API 호출: `POST /auth/login`
  - 성공 시 JWT 토큰 저장 및 대시보드로 이동
  - 실패 시 에러 메시지 표시

### 1.2 인증 상태 관리
- [ ] Vuex/Pinia를 이용한 인증 상태 관리 (또는 store/index.js 확장)
  - accessToken, refreshToken, user 정보 저장
  - 토큰 만료 감지 및 refresh 로직
  - 로그아웃 기능 구현

### 1.3 레이아웃 컴포넌트 완성
- [ ] AppSidebar.vue - 네비게이션 메뉴
  - 현재 활성 메뉴 하이라이트
  - 사용자 정보 표시 및 로그아웃 버튼
  - 참고 파일: `mockup/dashboard.html`의 sidebar
- [ ] AppTopBar.vue - 상단 헤더
  - 페이지 제목, 실시간 시간
  - 사용자 정보 및 로그아웃 메뉴
- [ ] WorkspaceLayout.vue 검증
  - 사이드바 + 헤더 + 콘텐츠 영역 레이아웃 확인

---

## 2단계: 대시보드 화면 구현

### 2.1 메인 대시보드 (DashboardView)
- [ ] KPI 요약 카드 구현 (BaseStatCard 활용)
  - 생산 건수, 활성 알람, AMR 운영/충전/대기 수, 평균 배터리, 평균 작업 시간
  - API: `GET /dashboard/summary`
  - 실시간 갱신: WebSocket 이벤트 `dashboard.summary.updated` 수신

- [ ] 공장 평면도 및 AMR 위치 시각화
  - 구역별 공장 평면도 (canvas 또는 SVG)
  - AMR 위치 표시 (원형 아이콘, 상태별 색상)
  - AMR 클릭 시 AmrDetailView로 이동
  - 참고 파일: `mockup/dashboard.html`의 floor-map 영역

- [ ] 환경 현황 (온도/습도/미세먼지)
  - 구역별 실시간 센서 데이터 표시
  - 참고 파일: `mockup/dashboard.html`의 environment section

- [ ] 실시간 중요 알람 위젯
  - 최신 알람 목록 (limit=5)
  - API: `GET /dashboard/recent-alarms`
  - 심각도별 색상 구분 (critical, warning, info)

- [ ] 실시간 작업 로그 위젯
  - 최근 작업 이벤트 표시
  - API: `GET /dashboard/recent-logs`

---

## 3단계: AMR 관제 화면 구현

### 3.1 AMR 전체 관리 화면 (AmrListView)
- [ ] AMR 목록 표/카드 뷰
  - 각 AMR: ID, 상태, 배터리 %, 현재 위치, 목적지, 작업 상태
  - API: `GET /amrs` (page, limit, status 필터)
  - 페이지네이션 구현
  - 참고 파일: `mockup/amr-list.html`

- [ ] 상태 요약 카드
  - 운영/대기/오류 장비 수 집계
  - 가동률 계산 및 표시

- [ ] 통계 차트 (ECharts)
  - 공정별 평균 대기 시간 (막대 차트)
  - 일별 오류 발생 건수 (선 차트)
  - 평균 시간 준수율 (선 차트)
  - 건전성 기준 미달 기체 목록

- [ ] AMR 선택 시 상세 화면으로 이동
  - URL 쿼리 파라미터: `?amrId=amr-001`

### 3.2 AMR 개별 관제 화면 (AmrDetailView)
- [ ] 선택한 AMR의 상세 정보 표시
  - 현재 작업, 배터리, 온도, 습도, 구동 상태
  - API: `GET /amrs/{amrId}`
  - 센서 상태 표시 (LiDAR, 카메라, IMU, 로드셀)
  - 최종 점검일 표시

- [ ] 누적 주행 거리 및 가동 시간 표시
  - 고정 통계 영역

- [ ] 비상 정지 버튼
  - `POST /amrs/{amrId}/commands` (command: emergencyStop)
  - 확인 다이얼로그 표시

- [ ] 시간 준수율 추이 차트
  - 시간대별 시간 준수율 표시 (선 차트)

- [ ] URL 쿼리로 AMR 자동 로드
  - 기본값: 첫 번째 AMR (또는 사용자 지정)

---

## 4단계: 배터리/충전 현황 화면 구현

### 4.1 충전 현황 화면 (BatteryView)
- [ ] 충전 운영 요약 카드
  - 운영 스테이션 수, 충전 중 AMR 수, 평균 충전률, 완충 예상 시간
  - API: `GET /charging/stations`

- [ ] 스테이션별 상세 정보
  - 각 스테이션: 이름, 위치, 상태, 점유율, 평균 배터리, 완충 예상 시간
  - 참고 파일: `mockup/amr-battery.html`

- [ ] 충전 대기열 테이블
  - 스테이션별 대기 AMR 목록
  - API: `GET /charging/queue`

- [ ] 충전 완료 예상 차트 (ECharts)
  - 구간별 분포 (0-30분, 30-60분, 60분 초과)
  - API: `GET /charging/forecast`

- [ ] 충전 스테이션 혼잡 알림
  - 혼잡 상태(occupancy > 80%) 표시
  - 알림 배지 표시

---

## 5단계: 작업 이력 화면 구현

### 5.1 작업 이력 분석 화면 (WorkHistoryView)
- [ ] 작업 요약 카드
  - 일별 작업 건수, 이동 거리, 총 작업 시간, 평균 작업 시간

- [ ] 시간대별 작업 건수 차트 (ECharts)
  - 막대 차트, X축: 시간대, Y축: 작업 건수
  - API: `GET /analytics/workload?groupBy=hour`

- [ ] AMR별 작업 비중 차트 (ECharts)
  - 파이/도넛 차트
  - API: `GET /analytics/workload?groupBy=amr`

- [ ] 작업 내역 목록 테이블
  - 작업 시간, 출발지, 도착지, 작업 상태
  - API: `GET /work-histories` (page, limit, amrId, from, to, taskType 필터)
  - 페이지네이션 구현

- [ ] 일별 작업 요약 테이블
  - 최근 6일 작업 요약 (작업 건수, 이동 거리 등)

- [ ] 필터 UI
  - 기간 선택 (날짜 피커)
  - AMR 선택 (멀티 셀렉트)
  - 작업 유형 필터

- [ ] 내보내기 기능
  - CSV/Excel 다운로드
  - API: `GET /work-histories/export`

---

## 6단계: 실시간 데이터 연동

### 6.1 WebSocket 연결
- [ ] plugins/ws.js 확장 (필요 시)
  - `WS /api/v1/stream` 연결
  - JWT 인증 전달
  - 재연결 로직

### 6.2 실시간 이벤트 구독
- [ ] 대시보드: `dashboard.summary.updated`, `alarms.created`
- [ ] AMR 목록/상세: `amrs.position.updated`, `amrs.status.updated`
- [ ] 배터리: `charging.forecast.updated`

### 6.3 상태 동기화
- [ ] Vuex/Pinia를 이용한 중앙 상태 관리
  - WebSocket 이벤트 수신 시 상태 업데이트
  - 각 컴포넌트에서 상태 구독

---

## 7단계: UI/UX 완성

### 7.1 반응형 디자인
- [ ] FHD(1920x1080) 기준 레이아웃 검증
- [ ] 스크롤 최소화 (한 화면 내 모든 정보 표시)
- [ ] 사이드바 + 헤더 고정 배치

### 7.2 한글 UI 통일
- [ ] 모든 텍스트 라벨 검토 및 통일
- [ ] 날짜/시간 포맷 일관성 (ISO 8601 UTC 표시)

### 7.3 공통 스타일
- [ ] CSS 토큰 정의 (색상, 폰트, 크기)
- [ ] 각 뷰에서 일관된 색상 팔레트 사용
- [ ] 참고: `mockup` 폴더의 CSS 색상 체계

### 7.4 에러 처리
- [ ] API 에러 상황별 사용자 피드백
- [ ] 타임아웃, 네트워크 오류 등 예외 처리
- [ ] 알림 메시지 UI (토스트, 모달)

---

## 8단계: 테스트 및 최적화

### 8.1 기능 테스트
- [ ] 각 화면별 API 호출 및 데이터 표시 검증
- [ ] 필터, 검색, 페이지네이션 동작 확인
- [ ] 실시간 갱신 검증 (WebSocket 이벤트 수신)

### 8.2 성능 최적화
- [ ] 번들 크기 최적화 (필요 시 코드 스플리팅)
- [ ] 이미지 최적화
- [ ] 차트 렌더링 성능 검증

### 8.3 브라우저 호환성
- [ ] Chrome, Edge, Firefox 최신 버전 테스트

---

## 구현 참고 사항

### 디렉토리 구조
```
FE/frontend/src/
├── components/
│   ├── atoms/          # 기본 UI 요소 (Button, Badge, etc.)
│   ├── molecules/      # 조합형 컴포넌트 (AppSidebar, AppTopBar, etc.)
│   └── organisms/      # (필요 시 추가) 복잡한 컴포넌트
├── views/              # 페이지별 뷰 컴포넌트
├── layouts/            # 레이아웃 컴포넌트
├── plugins/            # Axios, WebSocket 등 플러그인
├── store/              # 상태 관리 (Vuex/Pinia)
├── services/           # API 호출 함수 (필요 시 추가)
└── utils/              # 유틸리티 함수 (필요 시 추가)
```

### API 호출 패턴
```javascript
// plugins/axios.js를 통한 인증 요청
import { useAxios } from './plugins/axios'
const { get, post } = useAxios()
const data = await get('/dashboard/summary')
```

### ECharts 사용
- 모든 차트는 Apache ECharts 사용
- 반응형 차트 옵션 설정 (window resize 감지)
- 테마 일관성 유지

### WebSocket 이벤트 패턴
```javascript
// plugins/ws.js
ws.on('amrs.status.updated', (data) => {
  // 상태 업데이트 로직
})
```

---

## 우선순위

1. **필수** (Phase 1): 1단계 ~ 2단계 (인증, 대시보드)
2. **주요** (Phase 2): 3단계 ~ 4단계 (AMR 관제, 배터리)
3. **부가** (Phase 3): 5단계 (작업 이력)
4. **최적화** (Phase 4): 6단계 ~ 8단계 (실시간, 완성)

---

## 진행 상황 추적

- [ ] Phase 1: 인증 및 대시보드 완료
- [ ] Phase 2: AMR 관제 및 배터리 완료
- [ ] Phase 3: 작업 이력 완료
- [ ] Phase 4: 실시간 연동 및 최적화 완료
- [ ] PR 및 merge 완료
