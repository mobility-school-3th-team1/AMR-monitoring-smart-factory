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
      eyebrow="배터리/충전 스테이션"
      title="충전 현황"
      subtitle="충전 대기열과 스테이션 혼잡도를 보여주는 초기 뼈대입니다."
    >
      <div class="station-grid">
        <BaseCard v-for="station in stations" :key="station.name" class="station-card">
          <p class="station-card__name">{{ station.name }}</p>
          <p class="station-card__detail">{{ station.detail }}</p>
          <BaseBadge :tone="station.tone" :label="station.status" />
        </BaseCard>
      </div>
    </SectionPanel>

    <SectionPanel
      eyebrow="충전 대기열"
      title="스테이션별 대기 현황"
      subtitle="예상 완충 시간과 대기 순서를 표시합니다."
    >
      <table class="simple-table">
        <thead>
          <tr>
            <th>스테이션</th>
            <th>대기 AMR</th>
            <th>예상 완충</th>
            <th>혼잡도</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in queueRows" :key="row.station">
            <td>{{ row.station }}</td>
            <td>{{ row.robot }}</td>
            <td>{{ row.eta }}</td>
            <td>{{ row.congestion }}</td>
          </tr>
        </tbody>
      </table>
    </SectionPanel>
  </div>
</template>

<script setup>
import BaseBadge from '../components/atoms/BaseBadge.vue'
import BaseCard from '../components/atoms/BaseCard.vue'
import BaseStatCard from '../components/atoms/BaseStatCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'

const statCards = [
  { label: '운영 충전소', value: '6개', description: '활성 스테이션 수', tone: 'blue' },
  { label: '충전 중 AMR', value: '5대', description: '현재 충전기에 연결됨', tone: 'green' },
  { label: '평균 충전률', value: '74%', description: '전체 장비 평균 잔량', tone: 'orange' },
  { label: '완충 예상 최대', value: '42분', description: '가장 오래 걸리는 장비 기준', tone: 'purple' }
]

const stations = [
  { name: 'CHG-01', detail: 'AMR-02 연결 중 / 74%', status: '정상', tone: 'success' },
  { name: 'CHG-02', detail: '대기열 2대 / 혼잡도 보통', status: '주의', tone: 'warning' },
  { name: 'CHG-03', detail: '점검 필요 / 사용 중지', status: '이상', tone: 'danger' }
]

const queueRows = [
  { station: 'CHG-01', robot: 'AMR-02', eta: '11분', congestion: '낮음' },
  { station: 'CHG-02', robot: 'AMR-11', eta: '24분', congestion: '보통' },
  { station: 'CHG-03', robot: 'AMR-14', eta: '42분', congestion: '높음' }
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

.station-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.station-card {
  padding: 18px;
}

.station-card__name {
  margin: 0 0 8px;
  font-size: 1rem;
  font-weight: 800;
}

.station-card__detail {
  margin: 0 0 14px;
  font-size: 0.84rem;
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
  .view-stack__stats,
  .station-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>