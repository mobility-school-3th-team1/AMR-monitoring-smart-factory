# FE 프로젝트 골격 정리 기록

## 오늘 한 일

- Vue 프로젝트에 `vue-router`를 추가하고 페이지 단위 라우팅 구조를 정리했다.
- `WorkspaceLayout`과 `AuthLayout`을 분리해 관제 화면과 로그인 화면의 레이아웃을 나눴다.
- `BaseCard`, `BaseBadge`, `BaseStatCard`, `SectionPanel`로 공통 UI 컴포넌트의 시작점을 만들었다.
- `mockup` 폴더의 HTML 화면을 기준으로 `DashboardView`, `AmrListView`, `AmrDetailView`, `BatteryView`, `WorkHistoryView`, `LoginView`를 생성했다.
- `tokens.css`와 `base.css`로 테마 변수와 전역 스타일을 정리했다.
- 네비게이션 정의를 `src/config/navigation.js`로 분리해 메뉴와 라우팅의 기준점을 만들었다.

## 현재 구조

- `src/router` - 페이지 라우팅
- `src/layouts` - workspace/auth 레이아웃
- `src/components/atoms` - 재사용 기본 UI
- `src/components/molecules` - 사이드바, 상단바, 패널 같은 조합 컴포넌트
- `src/views` - 화면 단위 스켈레톤
- `src/styles` - 토큰과 전역 스타일
- `src/config` - 네비게이션 정의

## 다음에 할 일

- `mockup` HTML의 실제 섹션을 각 Vue view에 더 세밀하게 이식한다.
- 차트가 들어갈 자리를 `ECharts` 컴포넌트로 치환한다.
- API 응답 구조가 정해지면 `src/services`를 만들어 데이터 연동을 시작한다.
- 필요하면 메뉴 상태, 공통 필터, 테이블 컴포넌트를 추가한다.