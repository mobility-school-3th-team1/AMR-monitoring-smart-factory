<template>
  <div class="amr-modal-panel">
    <header class="amr-modal-panel__header">
      <div>
        <h3 class="amr-modal-panel__title">{{ displayName }}</h3>
        <span class="amr-modal-panel__id">{{ resolvedAmrId }}</span>
      </div>
      <button type="button" class="amr-modal-panel__close" aria-label="닫기" @click="emit('close')">
        ×
      </button>
    </header>

    <div v-if="loadError" class="amr-modal-panel__error">{{ loadError }}</div>
    <div v-else-if="demoScenarioBanner" class="amr-modal-panel__banner">{{ demoScenarioBanner }}</div>

    <div class="amr-modal-panel__status-row">
      <span class="status-badge" :class="statusBadgeClass">{{ statusLabel }}</span>
      <span v-if="faultLine" class="amr-modal-panel__fault">{{ faultLine }}</span>
    </div>

    <dl class="amr-modal-panel__facts">
      <div class="amr-modal-panel__fact">
        <dt>배터리</dt>
        <dd>{{ batteryLabel }}</dd>
      </div>
      <div class="amr-modal-panel__fact">
        <dt>현재 구역</dt>
        <dd>{{ positionZone }}</dd>
      </div>
      <div class="amr-modal-panel__fact">
        <dt>주행 거리</dt>
        <dd>{{ mileageDisplay }} km</dd>
      </div>
    </dl>

    <p v-if="currentTaskLine !== '현재 작업: —'" class="amr-modal-panel__task">{{ currentTaskLine }}</p>

    <div class="amr-modal-panel__actions">
      <button type="button" class="btn-detail-page" @click="emit('open-detail-page')">
        상세 화면으로
      </button>
      <button type="button" class="btn-emergency" @click="handleEmergency">비상 정지</button>
    </div>
  </div>
</template>

<script setup>
import { toRef, onMounted, onUnmounted } from 'vue'
import { useAmrDetail } from '@/composables/useAmrDetail'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'

const props = defineProps({
  amrId: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['close', 'open-detail-page', 'updated'])

const amrIdRef = toRef(props, 'amrId')
const {
  loadError,
  resolvedAmrId,
  demoScenarioBanner,
  displayName,
  statusLabel,
  statusBadgeClass,
  faultLine,
  currentTaskLine,
  batteryLabel,
  mileageDisplay,
  positionZone,
  loadAmrDetail,
  triggerEmergency
} = useAmrDetail(() => amrIdRef.value)

async function refreshDetail() {
  await loadAmrDetail()
  emit('updated')
}

async function handleEmergency() {
  await triggerEmergency()
  emit('updated')
}

let refreshTimer = null

onMounted(() => {
  refreshDetail()
  refreshTimer = setInterval(refreshDetail, DEMO_REST_POLLING_INTERVAL_MS)
  window.addEventListener('demo-scenario-applied', refreshDetail)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('demo-scenario-applied', refreshDetail)
})
</script>

<style scoped>
.amr-modal-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.amr-modal-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.amr-modal-panel__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
  color: #0f172a;
}

.amr-modal-panel__id {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
}

.amr-modal-panel__close {
  border: none;
  background: transparent;
  font-size: 1.4rem;
  line-height: 1;
  color: #64748b;
  cursor: pointer;
  padding: 0 4px;
}

.amr-modal-panel__error {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff4f4;
  border: 1px solid #fbcaca;
  color: #b91c1c;
  font-size: 0.82rem;
  font-weight: 700;
}

.amr-modal-panel__banner {
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  font-size: 0.78rem;
  font-weight: 700;
}

.amr-modal-panel__status-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
  background: #e2e8f0;
  color: #0f172a;
}

.status-badge.is-operating {
  background: #ecfdf5;
  color: #047857;
}

.status-badge.is-idle {
  background: #fffbeb;
  color: #b45309;
}

.status-badge.is-charging {
  background: #eff6ff;
  color: #1d4ed8;
}

.status-badge.is-danger {
  background: #fef2f2;
  color: #b91c1c;
}

.amr-modal-panel__fault {
  font-size: 0.78rem;
  color: #b91c1c;
  font-weight: 600;
}

.amr-modal-panel__facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.amr-modal-panel__fact {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.amr-modal-panel__fact dt {
  margin: 0 0 4px;
  font-size: 0.68rem;
  font-weight: 700;
  color: #64748b;
}

.amr-modal-panel__fact dd {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 800;
  color: #0f172a;
}

.amr-modal-panel__task {
  margin: 0;
  font-size: 0.82rem;
  font-weight: 700;
  color: #334155;
}

.amr-modal-panel__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-detail-page {
  flex: 1;
  min-width: 120px;
  padding: 9px 14px;
  border: 1px solid #3b82f6;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 800;
  font-size: 0.82rem;
  cursor: pointer;
}

.btn-detail-page:hover {
  background: #dbeafe;
}

.btn-emergency {
  padding: 9px 14px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-weight: 800;
  font-size: 0.82rem;
  cursor: pointer;
}

.btn-emergency:hover {
  background: #dc2626;
}

@media (max-width: 520px) {
  .amr-modal-panel__facts {
    grid-template-columns: 1fr;
  }
}
</style>
