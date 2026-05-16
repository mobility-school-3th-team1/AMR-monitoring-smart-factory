<template>
  <div class="dashboard-view">
    <section class="dashboard-view__stats">
      <BaseStatCard
        v-for="card in statCards"
        :key="card.label"
        v-bind="card"
      />
    </section>

    <div class="dashboard-view__grid">
      <SectionPanel
        class="dashboard-view__map"
        eyebrow="메인 대시보드"
        title="공장 평면도와 AMR 상태"
        subtitle="mockup/dashboard.html 기준으로 구성한 통합 관제 스켈레톤입니다."
      >
        <div class="floor-map">
          <div
            v-for="zone in floorZones"
            :key="zone.name"
            class="floor-map__zone"
            :class="zone.variant"
            :style="zone.style"
          >
            <strong>{{ zone.name }}</strong>
            <span>{{ zone.detail }}</span>
          </div>

          <div
            v-for="robot in robots"
            :key="robot.id"
            class="floor-map__robot"
            :class="robot.statusClass"
            :style="robot.style"
          >
            {{ robot.id }}
            <small>{{ robot.battery }}</small>
          </div>
        </div>
      </SectionPanel>

      <div class="dashboard-view__side">
        <SectionPanel
          eyebrow="환경 현황"
          title="구역별 온도 · 습도 · 먼지"
          subtitle="실측 데이터가 연결되면 차트로 교체합니다."
        >
          <ul class="mini-list">
            <li v-for="item in environmentRows" :key="item.zone">
              <span>{{ item.zone }}</span>
              <strong>{{ item.value }}</strong>
            </li>
          </ul>
        </SectionPanel>

        <SectionPanel
          eyebrow="알람"
          title="실시간 중요 알람"
          subtitle="충돌, 고장, 경로 이탈 같은 이벤트를 표시합니다."
        >
          <ul class="alert-list">
            <li v-for="alert in alerts" :key="alert.message">
              <BaseBadge :tone="alert.tone" :label="alert.type" />
              <div>
                <p>{{ alert.message }}</p>
                <span>{{ alert.time }}</span>
              </div>
            </li>
          </ul>
        </SectionPanel>
      </div>

      <SectionPanel
        class="dashboard-view__log"
        eyebrow="로그"
        title="실시간 작업 로그"
        subtitle="작업 시작, 이동, 충전 이벤트가 흐르는 영역입니다."
      >
        <table class="simple-table">
          <thead>
            <tr>
              <th>시간</th>
              <th>장비</th>
              <th>이벤트</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in logRows" :key="entry.time + entry.robot">
              <td>{{ entry.time }}</td>
              <td>{{ entry.robot }}</td>
              <td>{{ entry.event }}</td>
              <td>{{ entry.status }}</td>
            </tr>
          </tbody>
        </table>
      </SectionPanel>
    </div>
  </div>
</template>

<script setup>
import BaseBadge from '../components/atoms/BaseBadge.vue'
import BaseStatCard from '../components/atoms/BaseStatCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'

const statCards = [
  {
    label: '운영 중 장비',
    value: '18대',
    description: '현재 관제 가능한 AMR 수입니다.',
    tone: 'blue'
  },
  {
    label: '대기 장비',
    value: '4대',
    description: '작업 대기 또는 충전 준비 상태입니다.',
    tone: 'orange'
  },
  {
    label: '주의 알람',
    value: '3건',
    description: '경로 이상 또는 환경 이상 이벤트입니다.',
    tone: 'red'
  },
  {
    label: '가동률',
    value: '96.4%',
    description: '샘플 수치 기반의 초기 대시보드 값입니다.',
    tone: 'green'
  }
]

const floorZones = [
  {
    name: '입고 구역',
    detail: '검수·이송',
    variant: 'zone--soft',
    style: { left: '6%', top: '12%', width: '24%', height: '28%' }
  },
  {
    name: '조립 라인',
    detail: '주요 운송',
    variant: 'zone--primary',
    style: { left: '34%', top: '10%', width: '28%', height: '36%' }
  },
  {
    name: '검사 구역',
    detail: '품질 확인',
    variant: 'zone--accent',
    style: { left: '67%', top: '14%', width: '25%', height: '24%' }
  },
  {
    name: '충전 스테이션',
    detail: '대기열 관리',
    variant: 'zone--warning',
    style: { left: '18%', top: '58%', width: '26%', height: '22%' }
  },
  {
    name: '폐기/출하',
    detail: '마감 처리',
    variant: 'zone--muted',
    style: { left: '58%', top: '56%', width: '30%', height: '24%' }
  }
]

const robots = [
  { id: 'AMR-01', battery: '92%', statusClass: 'robot--safe', style: { left: '18%', top: '28%' } },
  { id: 'AMR-04', battery: '68%', statusClass: 'robot--working', style: { left: '49%', top: '26%' } },
  { id: 'AMR-07', battery: '41%', statusClass: 'robot--warning', style: { left: '73%', top: '32%' } },
  { id: 'AMR-11', battery: '88%', statusClass: 'robot--safe', style: { left: '30%', top: '72%' } }
]

const environmentRows = [
  { zone: '입고 구역', value: '26.4°C / 48% / 0.12mg' },
  { zone: '조립 라인', value: '27.1°C / 46% / 0.18mg' },
  { zone: '검사 구역', value: '25.9°C / 44% / 0.09mg' },
  { zone: '충전 스테이션', value: '28.2°C / 51% / 0.15mg' }
]

const alerts = [
  { type: '경로 이탈', message: 'AMR-07이 우회 경로를 벗어났습니다.', time: '방금 전', tone: 'danger' },
  { type: '온도 상승', message: '충전 구역 온도가 기준치를 넘었습니다.', time: '2분 전', tone: 'warning' },
  { type: '정상', message: 'AMR-04 작업 완료 후 복귀했습니다.', time: '5분 전', tone: 'success' }
]

const logRows = [
  { time: '09:40:12', robot: 'AMR-01', event: '작업 시작', status: '정상' },
  { time: '09:41:03', robot: 'AMR-04', event: '이동 경로 진입', status: '정상' },
  { time: '09:42:18', robot: 'AMR-07', event: '경로 보정', status: '주의' },
  { time: '09:43:55', robot: 'AMR-11', event: '충전 대기열 합류', status: '정상' }
]
</script>

<style scoped>
.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.dashboard-view__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dashboard-view__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.dashboard-view__side {
  display: grid;
  gap: 18px;
}

.dashboard-view__log {
  grid-column: 1 / -1;
}

.floor-map {
  position: relative;
  min-height: 540px;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(217, 228, 240, 0.9);
  background:
    linear-gradient(90deg, rgba(37, 99, 235, 0.035) 1px, transparent 1px),
    linear-gradient(rgba(37, 99, 235, 0.035) 1px, transparent 1px),
    linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
  background-size: 44px 44px, 44px 44px, auto;
}

.floor-map__zone {
  position: absolute;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.floor-map__zone strong {
  font-size: 0.9rem;
}

.floor-map__zone span {
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

.zone--soft { background: rgba(16, 185, 129, 0.08); }
.zone--primary { background: rgba(37, 99, 235, 0.08); }
.zone--accent { background: rgba(6, 182, 212, 0.1); }
.zone--warning { background: rgba(245, 158, 11, 0.12); }
.zone--muted { background: rgba(15, 23, 42, 0.04); }

.floor-map__robot {
  position: absolute;
  min-width: 88px;
  padding: 9px 12px;
  border-radius: 14px;
  color: #ffffff;
  font-size: 0.76rem;
  font-weight: 800;
  text-align: center;
  transform: translate(-50%, -50%);
  box-shadow: 0 14px 26px rgba(15, 23, 42, 0.16);
}

.floor-map__robot small {
  display: block;
  margin-top: 4px;
  font-size: 0.68rem;
  opacity: 0.9;
}

.robot--safe { background: linear-gradient(180deg, #10b981, #0f766e); }
.robot--working { background: linear-gradient(180deg, #2563eb, #1d4ed8); }
.robot--warning { background: linear-gradient(180deg, #f59e0b, #d97706); }

.mini-list,
.alert-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.mini-list {
  display: grid;
  gap: 12px;
}

.mini-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--color-surface-soft);
  border: 1px solid rgba(217, 228, 240, 0.92);
}

.mini-list span,
.mini-list strong {
  font-size: 0.84rem;
}

.alert-list {
  display: grid;
  gap: 14px;
}

.alert-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  background: var(--color-surface-soft);
  border: 1px solid rgba(217, 228, 240, 0.92);
}

.alert-list p,
.alert-list span {
  margin: 0;
  font-size: 0.84rem;
}

.alert-list span {
  display: inline-block;
  margin-top: 6px;
  color: var(--color-text-muted);
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
}

.simple-table th,
.simple-table td {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(217, 228, 240, 0.95);
  text-align: left;
  font-size: 0.84rem;
}

.simple-table th {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

@media (max-width: 1440px) {
  .dashboard-view__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-view__grid {
    grid-template-columns: 1fr;
  }
}
</style>