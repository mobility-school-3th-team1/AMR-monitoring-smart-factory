# AGENTS.md - 프론트엔드 (FE)

## 프로젝트 맥락

이 폴더는 AMR 스마트 팩토리 통합 모니터링 시스템의 프론트엔드이다.
공장 내 AMR 위치/상태, 환경 센서, 충전 스테이션, 작업 이력을 실시간으로 관제하고 분석하는 대시보드 웹 애플리케이션을 구현한다.

전체 시스템은 FE(프론트엔드) / BE(백엔드, Spring Boot) / DB(MySQL, Node-RED)로 구성되며, FE는 BE가 제공하는 REST API 및 WebSocket을 통해 데이터를 수신한다.

## 설계 문서 참조

이 프로젝트의 설계 문서는 상위 디렉토리에 위치한다.
작업 시작 전, 또는 관련 기능 구현 시 반드시 다음 문서를 읽어야 한다:

- `../docs/프로젝트 정의서.md` - 시스템 아키텍처, 핵심 기능, **화면 설계 명세**
- `../docs/API 정의.md` - RESTful API 명세 (엔드포인트, 요청/응답 형식, WebSocket 이벤트)
- `../docs/데이터 스키마 설계.md` - ERD 및 테이블 정의 (API 응답 구조 이해에 필요)
- `../docs/협업 컨벤션.md` - Git 워크플로우, 커밋, PR, 코드 품질 규칙

특히 화면 구현 시 `../docs/프로젝트 정의서.md`의 "화면설계" 섹션에 정의된 객체 번호, 설명을 준수해야 한다.
API 호출 구현 시 `../docs/API 정의.md`의 엔드포인트, 쿼리 파라미터, 응답 형식을 정확히 따라야 한다.

## 공통 규칙

### Git 컨벤션

- 커밋 메시지: `태그(fe): 변동사항 설명 스페이스-키 #이슈번호`
  - 태그: `feat`, `fix`, `docs`, `chore`, `refactor`
  - 프론트엔드 영역은 `fe`로 표기한다
- 브랜치: `feature/<스페이스 키>-<이슈번호>-<짧은-기능명>` (소문자, 케밥 케이스)
- main 직접 커밋 금지. PR을 통해서만 merge한다.
- PR 병합은 merge commit 방식만 사용한다.

### 코드 품질

- 파일 인코딩: UTF-8
- 폴더명: 소문자, 공백은 `_`로 대체
- 설계 변경 시: `../docs/`의 명세 문서를 먼저 수정한 뒤, 문서에 따라 구현한다
- PR 전 로컬에서 정합성 검토 및 오류 없이 실행되는지 확인한다

## 기술 스택

- Vue.js (프론트엔드 프레임워크)
- Apache ECharts (차트/그래프)
- HTML, CSS, JavaScript

## 프론트엔드 규칙

### 파일/컴포넌트 네이밍

- Vue 단일 파일 컴포넌트: PascalCase (예: `DashboardSummary.vue`, `AmrStatusCard.vue`)
- 디렉토리: 소문자, 공백은 `_`로 대체 (예: `src/components/`, `src/views/`)
- 조합형 컴포넌트명을 사용한다. 단일 단어 컴포넌트명은 피한다 (예: `AmrList.vue`, `ChargingQueue.vue`)

### Vue.js 컨벤션

- Composition API(`<script setup>`)를 기본으로 사용한다
- 컴포넌트 props는 명시적으로 타입을 정의한다
- `v-for` 사용 시 반드시 `:key`를 지정한다
- 컴포넌트 파일 내부 순서: `<template>` → `<script setup>` → `<style scoped>`

### API 연동

- REST API 기본 경로: `/api/v1`
- 인증: `Authorization: Bearer <JWT>` 헤더
- 목록 API 기본값: `page=1`, `limit=20`
- 날짜/시간: ISO 8601 UTC
- 실시간 데이터: WebSocket (`/api/v1/stream`) 이벤트 수신과 REST 조회를 혼합한다
- API 호출 함수는 재사용 가능한 모듈로 분리한다
- 에러 응답은 공통 핸들러로 처리한다 (상태 코드: 400, 401, 403, 404, 500 등)

### ECharts 사용

- 차트 옵션 객체는 별도 파일 또는 composable로 분리하여 관리한다
- 반응형 리사이즈를 처리한다 (`window.resize` 이벤트 대응)
- 차트 인스턴스는 컴포넌트 unmount 시 반드시 `dispose()`한다

### ESLint

- ESLint를 사용한다. 린트 오류가 없는 상태에서 커밋한다.
- 자동 수정 가능한 항목은 저장 시 자동 수정을 권장한다.
