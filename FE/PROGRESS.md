# FE 프로젝트 진행 현황

**마지막 업데이트:** 2026-05-15

## 개요

AMR 스마트 팩토리 모니터링 시스템의 프론트엔드(Vue 3)를 구축했습니다. 
초기 Vue CLI 프로젝트를 Vite로 마이그레이션하고, 필요한 라이브러리를 선정·설치한 후,
인증, HTTP 통신, WebSocket(MQTT) 기반의 실시간 데이터 수신 시스템을 완성했습니다.

---

## 1단계: 빌드 도구 마이그레이션 (Vue CLI → Vite)

### 목표
- Vue CLI의 느린 개발 서버 및 복잡한 설정 개선
- 현대적인 ES module 기반 빌드 시스템으로 전환
- 개발 속도 향상, 빌드 시간 단축

### 작업 내용

#### 1-1. Vite 설정 파일 생성
- **파일:** `FE/frontend/vite.config.js`
- **주요 설정:**
  - `@vitejs/plugin-vue`: Vue 3 SFC 지원
  - `resolve.alias`: `@` 별칭을 `src/` 매핑
  - `server.port: 3001, strictPort: true`: 개발 서버 포트 고정
- **이유:** 정확한 포트 바인딩, 모듈 해석 최적화

#### 1-2. package.json 스크립트 업데이트
```json
{
  "scripts": {
    "dev": "vite",                    // Vue CLI serve → Vite dev
    "build": "vite build",            // Vue CLI build → Vite build
    "preview": "vite preview",        // 빌드 결과 미리보기
    "lint": "eslint src"              // ESLint 검사
  }
}
```

#### 1-3. Vue CLI 의존성 제거
- 제거한 패키지: `@vue/cli-*`, `@vue/cli-service` 등
- 이유: Vite가 이들 기능을 포함하거나 불필요

#### 1-4. index.html 이동 및 수정
- **기존:** `public/index.html`
- **변경:** `FE/frontend/index.html` (루트)
- **이유:** Vite가 루트 index.html을 SPA 진입점으로 자동 인식
- **추가 설정:**
  ```html
  <script type="module" src="/src/main.js"></script>
  ```

#### 1-5. ESLint 설정 업데이트
- **파일:** `.eslintrc.js` (존재 확인 후 유지)
- **변경 사항:**
  - `requireConfigFile: false` 추가 (babel 설정 불필요)
  - Parser: `@babel/eslint-parser` 유지

#### 1-6. 결과
- ✅ `npm run dev`: localhost:3001에서 정상 작동
- ✅ `npm run build`: 239개 모듈, 221.88 kB 번들 생성 성공
- ✅ `npm run lint`: 에러 없음

---

## 2단계: 프론트엔드 라이브러리 선정 및 문서화

### 목표
- 프로젝트 요구사항에 맞는 핵심 라이브러리 식별
- 기존 의존성과 신규 설치 필요 라이브러리 검증

### 작업 내용

#### 2-1. 설계 문서 분석
- 분석 대상:
  - `docs/프로젝트 정의서.md`: 시스템 아키텍처, 기능 요구사항
  - `docs/API 정의.md`: REST API 엔드포인트, WebSocket 이벤트
  - `docs/데이터 스키마 설계.md`: 데이터 구조

#### 2-2. 라이브러리 선정 및 정리
- **생성 문서:** `docs/frontend-libraries.md`
- **선정 기준:**
  - HTTP 통신 (REST API) → `axios`
  - 상태 관리 (인증, 전역 상태) → `pinia`
  - 차트/시각화 (대시보드, 그래프) → `echarts` + `vue-echarts`
  - 날짜/시간 (로그, 타임스탬프 포매팅) → `dayjs`
  - 파일 처리 (CSV 내보내기) → `file-saver`, `papaparse`
  - 실시간 통신 (센서 데이터 스트리밍) → `mqtt` (WebSocket)
  - 유틸리티 → `reconnecting-websocket`

---

## 3단계: 필수 라이브러리 설치

### 목표
- 프로젝트 의존성 완성
- 빌드 및 린트 검증

### 작업 내용

#### 3-1. 설치 명령어
```bash
npm install axios pinia echarts dayjs file-saver papaparse reconnecting-websocket mqtt
```

#### 3-2. 설치된 패키지 (총 57개)
- **메인 의존성:** axios, pinia, echarts, dayjs, file-saver, papaparse, mqtt
- **peer/부가 의존성:** (npm 자동 해결)

#### 3-3. 검증
- ✅ `npm run build`: 성공
- ✅ `npm run lint`: 에러 없음

---

## 4단계: 인증 시스템 구현

### 목표
- JWT 기반 사용자 인증
- 토큰 저장, 갱신, API 요청 자동 주입

### 작업 내용

#### 4-1. Pinia 상태 저장소
- **파일:** `src/store/index.js`
- **주요 함수:**
  ```javascript
  useAuthStore() // 인증 상태: accessToken, refreshToken, user
  setTokens(access, refresh) // 토큰 저장
  logout() // 토큰 삭제
  createStore(app) // 애플리케이션에 Pinia 등록
  ```
- **저장 위치:** localStorage (리프레시 토큰), 메모리 (액세스 토큰)

#### 4-2. Axios 인터셉터 및 JWT 핸들링
- **파일:** `src/plugins/axios.js`
- **기능:**
  - **요청 인터셉터:** 모든 요청에 `Authorization: Bearer {accessToken}` 추가
  - **응답 인터셉터:**
    - 401 Unauthorized 감지
    - `/api/v1/auth/refresh` 호출 → 새 액세스 토큰 취득
    - 원본 요청 자동 재시도
  - **설정:** axios 인스턴스, baseURL, timeout 등
- **통합:** `src/main.js`에서 `setupAxios(auth)` 호출

#### 4-3. 흐름도
```
API 요청 → Request Interceptor (Bearer 토큰 추가) → 서버 응답
                                                        ↓
                                                    401 에러?
                                                   ↙      ↘
                                                No       Yes
                                                ↓        ↓
                                           반환     Refresh Token
                                                   (자동 갱신)
                                                     ↓
                                               요청 재시도
```

---

## 5단계: WebSocket 실시간 통신 (STOMP → MQTT 전환)

### 목표
- 실시간 데이터 스트리밍 (AMR 위치, 센서, 알람, 로그)
- IoT 환경에 최적화된 프로토콜 선택

### 작업 내용

#### 5-1. 초기 설계 (STOMP)
- **선택 사유:** Spring Boot 백엔드와 통합 가정
- **설치:** `@stomp/stompjs`, `sockjs-client`
- **문제:**
  - sockjs-client가 브라우저에서 Node.js `global` 객체 요구 → 폴리필 필요
  - STOMP는 메시징 middleware 프로토콜 (AMR/IoT 모니터링에 과도함)

#### 5-2. 최종 선택 (MQTT over WebSocket)
- **선택 사유:**
  - MQTT: IoT 환경에 표준 (가볍고, 발행-구독 패턴 지원)
  - WebSocket: 브라우저에서 안정적인 양방향 통신
  - `mqtt` 패키지: WebSocket 통신 기본 지원
- **설치 및 제거:**
  ```bash
  npm uninstall @stomp/stompjs sockjs-client
  npm install mqtt
  ```

#### 5-3. MQTT 구현
- **파일:** `src/plugins/ws.js` (재작성)
- **주요 함수:**
  ```javascript
  initMqtt({ brokerUrl = 'mqtt://localhost:8080', clientId })
    // MQTT 클라이언트 초기화, 자동 재연결
  
  subscribe(topic, callback)
    // 특정 토픽 구독, 메시지 수신 시 callback 호출
  
  publish(topic, message)
    // 토픽에 메시지 발행
  ```
- **이벤트:**
  - `connect`: 연결 성공
  - `message`: 메시지 수신
  - `error`: 에러 발생
  - `close`: 연결 종료

#### 5-4. 메인 애플리케이션 통합
- **파일:** `src/main.js`
- **변경 사항:**
  ```javascript
  import { initMqtt } from './plugins/ws'
  
  if (auth.accessToken) 
    initMqtt({ brokerUrl: 'mqtt://localhost:8080' })
  ```
- **시점:** 인증 완료 후 MQTT 연결

#### 5-5. 예상 토픽 구조
```
amrs/position/updated       // AMR 위치 업데이트
amrs/status/changed         // AMR 상태 변경 (이동/대기/충전/오류)
alarms/created              // 알람 발생
sensors/temperature         // 환경 센서: 온도
sensors/humidity            // 환경 센서: 습도
battery/level               // 배터리 레벨
work_history/logged         // 작업 이력 기록
```

---

## 6단계: 애플리케이션 초기화 및 통합

### 목표
- 전체 시스템 부트스트랩
- Pinia, axios, MQTT 동시 초기화

### 작업 내용

#### 6-1. main.js 구조
```javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 1. Pinia 스토어 생성 및 인증 상태 로드
const pinia = createStore(app)
const auth = useAuthStore(pinia)

// 2. Axios HTTP 클라이언트 설정 (JWT 자동 주입)
setupAxios(auth)

// 3. MQTT 연결 초기화 (토큰 있을 시)
if (auth.accessToken) initMqtt({ brokerUrl: 'mqtt://localhost:8080' })

// 4. 라우터 및 Vue 앱 마운트
app.use(router).mount('#app')
```

#### 6-2. 라우터 구성
- **파일:** `src/router/index.js`
- **주요 경로:**
  - `/` → 리다이렉트 또는 대시보드
  - `/login` → 로그인 페이지 (인증 전)
  - `/dashboard` → 메인 대시보드
  - `/amr-list` → AMR 목록
  - `/amr-detail` → AMR 상세 (선택한 로봇)
  - `/battery` → 배터리 관리
  - `/work-history` → 작업 이력
- **레이아웃:**
  - `AuthLayout`: 로그인 페이지 (사이드바/톱바 숨김)
  - `WorkspaceLayout`: 앱 페이지 (사이드바/톱바 표시)

---

## 7단계: 디버깅 및 정리

### 목표
- 초기 브라우저 화이트 스크린 해결
- 임시 디버깅 코드 제거

### 작업 내용

#### 7-1. 화이트 스크린 원인 분석
- **근본 원인:** sockjs-client의 `global` 객체 미정의
- **임시 해결:** index.html에 폴리필 추가
  ```html
  <script>
    window.global = window
    globalThis.global = globalThis
  </script>
  ```
- **최종 해결:** STOMP/sockjs-client 제거 → MQTT로 완전 전환

#### 7-2. 에러 오버레이 도구 제거
- **목적:** 런타임 에러 시각화 (임시)
- **제거:** index.html 정리
  - 글로벌 폴리필 삭제
  - 에러 오버레이 스크립트 삭제
  - HTML 최소화 (표준 구조만 유지)

#### 7-3. 최종 index.html
```html
<!DOCTYPE html>
<html lang="ko">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="icon" href="/favicon.ico" />
    <title>AMR Monitoring</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

---

## 현재 상태 요약

| 항목 | 상태 | 비고 |
|------|------|------|
| **빌드 시스템** | ✅ 완료 | Vite 5.0.0, vue.config.js 제거 |
| **라이브러리 선정** | ✅ 완료 | 문서화됨 (docs/frontend-libraries.md) |
| **라이브러리 설치** | ✅ 완료 | 57개 패키지 |
| **인증 시스템** | ✅ 완료 | Pinia + Axios JWT 인터셉터 |
| **HTTP 통신** | ✅ 완료 | axios + 401 토큰 갱신 자동화 |
| **WebSocket** | ✅ 완료 | MQTT over WebSocket |
| **애플리케이션 부트** | ✅ 완료 | main.js 통합 |
| **라우터** | ✅ 완료 | 6개 주요 경로 정의 |
| **개발 서버** | ✅ 작동 | localhost:3001 |
| **빌드 검증** | ✅ 성공 | 221.88 kB |
| **린트 검증** | ✅ 성공 | 에러 없음 |

---

## 다음 작업 (TODO)

### 단계 1: 백엔드 연동 확인
- [ ] MQTT 브로커 URL 확인 (BE 팀)
  - 현재 설정: `mqtt://localhost:8080`
  - 프로덕션: 변경 필요 (환경 변수화 추천)
- [ ] REST API 엔드포인트 테스트
  - 로그인: `POST /api/v1/auth/login`
  - 토큰 갱신: `POST /api/v1/auth/refresh`
  - 기타 조회 API

### 단계 2: 화면 구현
- [ ] LoginView: 로그인 폼 구현
- [ ] DashboardView: 대시보드 레이아웃 구체화
- [ ] AmrListView: AMR 목록 테이블
- [ ] AmrDetailView: 개별 AMR 상세 정보 및 위치 지도
- [ ] BatteryView: 배터리 현황 및 충전 스케줄
- [ ] WorkHistoryView: 작업 이력 로그 및 필터링

### 단계 3: 데이터 연동
- [ ] API 호출 함수 작성 (각 엔드포인트별)
- [ ] MQTT 토픽 구독 (실시간 데이터)
- [ ] Pinia 상태 관리 확장 (AMR, 센서, 알람 등)

### 단계 4: 컴포넌트 고도화
- [ ] 반응형 디자인 (모바일, 태블릿)
- [ ] 에러 처리 및 로딩 상태
- [ ] 차트 커스터마이징 (echarts)
- [ ] 실시간 데이터 업데이트 (MQTT 연동)

### 단계 5: 성능 및 배포
- [ ] 번들 최적화 (코드 스플리팅)
- [ ] 이미지 최적화
- [ ] 빌드 결과 검증
- [ ] 배포 환경 설정

---

## 기술 스택 최종 확정

| 분류 | 도구/라이브러리 | 버전 | 용도 |
|------|----------------|------|------|
| **프레임워크** | Vue.js | 3.2.13 | SPA 프론트엔드 |
| **빌드** | Vite | 5.0.0 | 모듈 번들링, 개발 서버 |
| **라우팅** | vue-router | 4.5.1 | 페이지 네비게이션 |
| **상태관리** | Pinia | (최신) | 전역 상태 (인증, 앱 상태) |
| **HTTP** | axios | (최신) | REST API 통신 |
| **JWT 인증** | (커스텀) | - | Axios 인터셉터 |
| **WebSocket** | mqtt | (최신) | MQTT over WebSocket |
| **차트** | echarts | (최신) | 데이터 시각화 |
| **날짜** | dayjs | (최신) | 날짜/시간 포매팅 |
| **파일** | file-saver, papaparse | (최신) | CSV, 파일 다운로드 |
| **린터** | ESLint | 7.32.0 | 코드 품질 검사 |
| **Git 훅** | lint-staged | 11.1.2 | Pre-commit 자동 린트 |

---

## 주요 파일 구조

```
FE/frontend/
├── vite.config.js                 # Vite 빌드 설정
├── index.html                     # SPA 진입점 (루트)
├── package.json                   # 의존성 및 스크립트
├── .eslintrc.js                   # ESLint 규칙
├── lint-staged.config.js          # Pre-commit 훅
│
├── src/
│   ├── main.js                    # 애플리케이션 부트스트랩
│   ├── App.vue                    # 루트 Vue 컴포넌트
│   │
│   ├── store/
│   │   └── index.js               # Pinia 상태 저장소 (인증)
│   │
│   ├── plugins/
│   │   ├── axios.js               # HTTP 클라이언트 + JWT
│   │   └── ws.js                  # MQTT 실시간 통신
│   │
│   ├── router/
│   │   └── index.js               # Vue Router 설정
│   │
│   ├── views/
│   │   ├── LoginView.vue          # 로그인 페이지
│   │   ├── DashboardView.vue      # 메인 대시보드
│   │   ├── AmrListView.vue        # AMR 목록
│   │   ├── AmrDetailView.vue      # AMR 상세
│   │   ├── BatteryView.vue        # 배터리 관리
│   │   └── WorkHistoryView.vue    # 작업 이력
│   │
│   ├── layouts/
│   │   ├── AuthLayout.vue         # 인증 화면 레이아웃
│   │   └── WorkspaceLayout.vue    # 앱 화면 레이아웃
│   │
│   ├── components/
│   │   ├── atoms/                 # 기본 UI 컴포넌트
│   │   ├── molecules/             # 합성 컴포넌트
│   │   └── organisms/             # (필요시) 복잡한 컴포넌트
│   │
│   ├── styles/
│   │   ├── base.css               # 글로벌 스타일
│   │   └── tokens.css             # 디자인 토큰
│   │
│   ├── config/
│   │   └── navigation.js          # 네비게이션 메타데이터
│   │
│   └── assets/                    # 이미지, 폰트 등
│
└── public/
    └── index.html                 # (구 위치, 이제 루트)
```

---

## 주의사항 및 체크리스트

- [ ] MQTT 브로커 URL을 환경 변수로 관리 (`.env` 파일)
- [ ] 토큰 저장소를 안전한 위치로 변경 (localStorage → sessionStorage 또는 httpOnly cookie)
- [ ] API 응답 에러 처리 강화
- [ ] 실시간 데이터 수신 시 중복 처리 및 순서 보장
- [ ] CORS 설정 확인 (BE에서 FE 도메인 허용)
- [ ] 배포 시 백엔드 URL, MQTT 브로커 주소 변경

---

## 참고자료

- [Vite 공식 문서](https://vitejs.dev)
- [Vue 3 공식 문서](https://vue3.dev)
- [Pinia 공식 문서](https://pinia.vuejs.org)
- [MQTT 프로토콜](https://mqtt.org)
- [ECharts 공식 문서](https://echarts.apache.org)
