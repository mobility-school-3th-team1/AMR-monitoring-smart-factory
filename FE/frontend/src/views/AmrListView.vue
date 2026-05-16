<template>
  <div class="view-grid">
    <section class="view-grid__stats">
      <BaseStatCard
        v-for="card in statCards"
        :key="card.label"
        v-bind="card"
      />
    </section>

    <SectionPanel
      eyebrow="AMR 전체 관리"
      title="장비 목록과 통계"
      subtitle="mockup/amr-list.html의 구조를 기준으로 정리한 페이지입니다."
    >
      <table class="simple-table">
        <thead>
          <tr>
            <th>장비</th>
            <th>상태</th>
            <th>현재 위치</th>
            <th>목적지</th>
            <th>작업 상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="robot in robots" :key="robot.id">
            <td>{{ robot.id }}</td>
            <td>{{ robot.status }}</td>
            <td>{{ robot.location }}</td>
            <td>{{ robot.destination }}</td>
            <td>{{ robot.task }}</td>
          </tr>
        </tbody>
      </table>
    </SectionPanel>
  </div>
</template>

<script setup>
import BaseStatCard from '../components/atoms/BaseStatCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'

const statCards = [
  { label: '운영', value: '18대', description: '가동 가능한 장비 수', tone: 'green' },
  { label: '대기', value: '4대', description: '작업 또는 충전 대기', tone: 'orange' },
  { label: '오류', value: '1대', description: '정비가 필요한 장비', tone: 'red' },
  { label: '평균 대기 시간', value: '7.2분', description: '공정 구간별 평균 값', tone: 'blue' }
]

const robots = [
  { id: 'AMR-01', status: '운영', location: '입고 구역', destination: '조립 라인', task: '이동 중' },
  { id: 'AMR-02', status: '대기', location: '충전 스테이션', destination: '검사 구역', task: '충전 완료 대기' },
  { id: 'AMR-07', status: '주의', location: '조립 라인', destination: '출하 구역', task: '경로 재계산' },
  { id: 'AMR-11', status: '운영', location: '검사 구역', destination: '입고 구역', task: '복귀 이동' }
]
</script>

<style scoped>
.view-grid {
  display: grid;
  gap: 18px;
}

.view-grid__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
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
  .view-grid__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>