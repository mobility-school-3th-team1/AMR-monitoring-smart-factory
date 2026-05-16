<template>
  <div class="detail-layout">
    <SectionPanel
      eyebrow="AMR 개별 관제"
      title="선택 장비 상태"
      subtitle="비상 정지와 센서 상태를 함께 배치한 상세 화면 골격입니다."
    >
      <div class="detail-grid">
        <BaseCard class="detail-card">
          <p class="detail-card__label">선택 장비</p>
          <h3>AMR-04</h3>
          <p>현재 작업: 배터리 팩 이송</p>
          <BaseBadge tone="success" label="운영 중" />
        </BaseCard>

        <BaseCard class="detail-card detail-card--action">
          <p class="detail-card__label">비상 제어</p>
          <h3>Emergency Survival Control</h3>
          <p>장비 이상 발생 시 즉시 정지시키는 영역입니다.</p>
          <button class="danger-button" type="button">비상 정지</button>
        </BaseCard>

        <BaseCard class="detail-card">
          <p class="detail-card__label">센서 상태</p>
          <ul class="sensor-list">
            <li v-for="sensor in sensors" :key="sensor.name">
              <span>{{ sensor.name }}</span>
              <strong>{{ sensor.state }}</strong>
            </li>
          </ul>
        </BaseCard>
      </div>
    </SectionPanel>
  </div>
</template>

<script setup>
import BaseBadge from '../components/atoms/BaseBadge.vue'
import BaseCard from '../components/atoms/BaseCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'

const sensors = [
  { name: 'LiDAR', state: '정상' },
  { name: 'Camera', state: '정상' },
  { name: 'IMU', state: '주의' },
  { name: 'Load Cell', state: '정상' },
  { name: 'Drive Unit', state: '정상' }
]
</script>

<style scoped>
.detail-layout {
  display: grid;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.detail-card {
  padding: 20px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
}

.detail-card__label {
  margin: 0 0 10px;
  font-size: 0.76rem;
  font-weight: 800;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.detail-card h3 {
  margin: 0 0 10px;
  font-size: 1.12rem;
}

.detail-card p {
  margin: 0 0 14px;
  font-size: 0.86rem;
  color: var(--color-text-muted);
}

.detail-card--action {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(254, 242, 242, 0.75));
}

.danger-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  background: var(--color-danger);
  color: #fff;
  font-weight: 800;
}

.sensor-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.sensor-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.76);
}

.sensor-list span,
.sensor-list strong {
  font-size: 0.84rem;
}

@media (max-width: 1280px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>