# Frontend Libraries — 권장 및 현재 사용 현황

## 목적
이 문서는 프로젝트 설계 문서(`프로젝트 정의서.md`, `API 정의.md`, `데이터 스키마 설계.md`)를 바탕으로 프론트엔드(역할: 프론트엔드 담당자)가 사용하면 좋은 라이브러리들과, 현재 `FE/frontend`에서 사용 중인 라이브러리를 정리합니다.

---

## 1) 요건 요약 (docs 기반)
- Vue 3 기반 SPA
- 대시보드 및 시계열/통계 차트(대시보드, 분석)
- 공장 평면도 기반 실시간 위치/경로 시각화(AMR 위치/경로)
- 실시간 이벤트 수신 (WebSocket, JWT 인증)
- REST API 통신 (JWT 인증, 파일 다운로드/내보내기)
- 표/테이블, 카드, 대시보드 레이아웃
- 경고/알람 처리 및 이력 조회

---

## 2) 권장 라이브러리 (프론트엔드 관점)
- 핵심
  - `vue` (이미 사용 중): UI 프레임워크 (Vue 3)
  - `vue-router`: 라우팅 (이미 사용 중)
  - `pinia` 또는 `vuex` (권장: `pinia`): 전역 상태 관리 (인증 토큰, 실시간 이벤트 상태, 선택된 AMR 등)
- 데이터/네트워크
  - `axios`: REST API 호출 (interceptor로 JWT 갱신/실패 처리 구성)
  - WebSocket: 서버가 Spring 기반이고 STOMP를 사용하면 `@stomp/stompjs` + `sockjs-client` 권장; 일반 WebSocket이라면 `reconnecting-websocket` 또는 `native WebSocket` + 재연결 로직 권장
- 시각화/차트
  - `echarts` (Apache ECharts): 시계열/통계 차트 (docs에서 명시)
  - `vue-echarts` (Vue용 래퍼): Vue 컴포넌트 형태로 사용 시 편리
- 맵/평면도(공장 레이아웃)
  - `konva` / `vue-konva`: 2D 캔버스 기반의 드래그/줌/패닝이 필요한 평면도·경로 시각화에 적합
  - 또는 SVG 기반 솔루션(직접 구현) — 경로 애니메이션이나 상호작용이 복잡하면 `konva` 추천
- UI 컴포넌트(옵션)
  - `element-plus` 또는 `ant-design-vue` (Vue 3 지원): 빠른 대시보드 구성(테이블, 폼, modal 등)
- 유틸리티
  - `dayjs` 또는 `date-fns`: 날짜/시간 파싱 및 포맷 (ISO 8601 처리)
  - `lodash` (선택): 유틸 헬퍼
  - `@vueuse/core`: 유용한 Composition API 훅 모음(반응성, 브라우저 API 래퍼 등)
- 파일/내보내기
  - `file-saver` (파일 다운로드), `papaparse` (CSV 파싱/생성)
- 폼 및 유효성
  - `vee-validate` 또는 `vuelidate` (선택)
- 테스트/개발 툴
  - `vitest` (Vite 환경에 맞는 테스트 러너)
  - `msw` (mock service worker) — API mocking

---

## 3) 현재 `FE/frontend`에서 사용 중인 라이브러리
(파일: `FE/frontend/package.json`)

### dependencies
- `core-js`: ^3.8.3
- `vue`: ^3.2.13
- `vue-router`: ^4.5.1

### devDependencies
- `@babel/core`: ^7.12.16
- `@babel/eslint-parser`: ^7.12.16
- `@vitejs/plugin-vue`: ^4.0.0
- `eslint`: ^7.32.0
- `eslint-plugin-vue`: ^8.0.3
- `lint-staged`: ^11.1.2
- `vite`: ^5.0.0

> 메모: 현재 차트(항목 `echarts`), 상태관리(`pinia`), HTTP 클라이언트(`axios`), WebSocket/STOMP 클라이언트 등은 `package.json`에 없습니다. 실시간 WebSocket과 차트 기능은 docs에서 요구되므로 추가 설치가 필요합니다.

---

## 4) 우선 순위 설치 제안 (프론트엔드 담당자 관점)
1. `axios` — API 통신 (필수)
2. `pinia` — 전역 상태 관리 (권장)
3. `echarts` + `vue-echarts` — 차트/대시보드 (필수)
4. WebSocket 클라이언트: 서버가 STOMP 사용 여부에 따라
   - STOMP: `@stomp/stompjs` + `sockjs-client`
   - 일반 WS: `reconnecting-websocket` 또는 커스텀 wrapper
5. `dayjs` — 날짜/시간 포맷/처리
6. `konva`/`vue-konva` — 공장 평면도/경로 시각화(필요 시)
7. UI 라이브러리(옵션): `element-plus` 또는 `ant-design-vue` (개발 속도 우선 시)
8. 파일 처리: `file-saver`, `papaparse` (CSV/엑셀 내보내기)

간단 설치 예시:

```bash
cd FE/frontend
npm install axios pinia echarts vue-echarts dayjs file-saver papaparse
# WebSocket STOMP 필요 시
npm install @stomp/stompjs sockjs-client
# 평면도 필요 시
npm install konva vue-konva
# (선택) UI 라이브러리
npm install element-plus
```

---

## 5) 다음 권장 작업
- 백엔드와 WebSocket 프로토콜(STOMP 사용 여부) 확인 → 적절한 WS 클라이언트 선택
- 우선순위에 따라 `axios`, `pinia`, `echarts`를 바로 설치하고 간단한 통합(로그인/토큰 흐름, 대시보드 차트 샘플, 실시간 위치 토픽 수신)을 구현해 PO 또는 QA와 데모
- CI/CD (build 스크립트)는 이미 `vite build`로 변경됨 — 배포 파이프라인에서 `npm run build` 사용 권장

---

작성자: 프론트엔드 분석 자동 생성
생성일: 2026-05-15
