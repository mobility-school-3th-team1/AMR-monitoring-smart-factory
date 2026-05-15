<template>
  <div class="view-stack">
    <section class="view-stack__stats">
      <BaseStatCard
        v-for="card in statCards"
        :key="card.label"
        v-bind="card"
      />
    </section>

    <SectionPanel
      eyebrow="작업 이력 및 분석"
      title="분석 요약"
      subtitle="mockup/work-history.html을 기반으로 한 작업 이력 페이지 골격입니다."
    >
      <div class="analysis-grid">
        <BaseCard class="analysis-card">
          <p class="analysis-card__label">시간대별 작업 건수</p>
          <div class="chart-placeholder">차트 영역</div>
        </BaseCard>

        <BaseCard class="analysis-card">
          <p class="analysis-card__label">AMR별 작업 비중</p>
          <div class="chart-placeholder">차트 영역</div>
        </BaseCard>
      </div>
    </SectionPanel>

    <SectionPanel
      eyebrow="작업 내역"
      title="세부 기록 테이블"
      subtitle="실제 API 연동 전까지는 정적 샘플 데이터로 유지합니다."
    >
      <table class="simple-table">
        <thead>
          <tr>
            <th>작업 시간</th>
            <th>AMR</th>
            <th>출발지</th>
            <th>도착지</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in workRows" :key="row.time + row.robot">
            <td>{{ row.time }}</td>
            <td>{{ row.robot }}</td>
            <td>{{ row.from }}</td>
            <td>{{ row.to }}</td>
            <td>{{ row.status }}</td>
          </tr>
        </tbody>
      </table>
    </SectionPanel>
  </div>
</template>

<script setup>
import BaseCard from '../components/atoms/BaseCard.vue'
import BaseStatCard from '../components/atoms/BaseStatCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'

const statCards = [
  { label: '일별 작업 건수', value: '146건', description: '샘플 기준 처리량', tone: 'blue' },
  { label: '이동 거리', value: '82.4km', description: '누적 이동 거리', tone: 'green' },
  { label: '총 작업 시간', value: '15.8h', description: '운영 시간 합계', tone: 'purple' },
  { label: '평균 작업 시간', value: '6.5분', description: '작업당 평균 소요 시간', tone: 'orange' }
]

const workRows = [
  { time: '09:12', robot: 'AMR-01', from: '입고 구역', to: '조립 라인', status: '정상' },
  { time: '09:27', robot: 'AMR-04', from: '조립 라인', to: '검사 구역', status: '정상' },
  { time: '09:54', robot: 'AMR-07', from: '충전 스테이션', to: '출하 구역', status: '주의' }
]
</script>

<style scoped>
.view-stack {
  display: grid;
  gap: 18px;
}

.view-stack__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.analysis-card {
  padding: 18px;
}

.analysis-card__label {
  margin: 0 0 14px;
  font-size: 0.9rem;
  font-weight: 800;
}

.chart-placeholder {
  min-height: 260px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background:
    radial-gradient(circle at center, rgba(37, 99, 235, 0.12), transparent 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(243, 248, 255, 0.96));
  border: 1px dashed rgba(148, 163, 184, 0.6);
  color: var(--color-text-muted);
  font-weight: 800;
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
  .view-stack__stats,
  .analysis-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>