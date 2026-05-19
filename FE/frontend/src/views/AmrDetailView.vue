<template>
  <div class="detail-layout">
    <SectionPanel
      eyebrow="AMR 개별 관제"
      title="선택 장비 상태"
      subtitle="REST 상세 조회 및 비상 정지 명령"
    >
      <div v-if="loadError" class="error-banner">{{ loadError }}</div>

      <div class="detail-header">
        <div>
          <strong class="amr-title">{{ displayName }}</strong>
          <span class="amr-id-chip">{{ resolvedAmrId }}</span>
          <small v-if="lastSeenLabel" class="amr-meta">{{ lastSeenLabel }}</small>
        </div>
        <div class="detail-header__actions">
          <span v-if="detailErrorBanner" class="error-msg">{{ detailErrorBanner }}</span>
          <button type="button" class="btn-emergency" @click="triggerEmergency">비상 정지</button>
        </div>
      </div>

      <div class="info-grid">
        <BaseCard class="info-card info-card--main">
          <p class="card-label">상태 및 작업</p>
          <div class="status-line">
            <span class="status-badge" :class="statusBadgeClass">{{ statusLabel }}</span>
            <span v-if="faultLine" class="fault-text">{{ faultLine }}</span>
          </div>
          <p class="task-line">{{ currentTaskLine }}</p>
          <p class="battery-line">배터리 {{ batteryLabel }}</p>
        </BaseCard>

        <BaseCard class="info-card info-card--stat">
          <p class="card-label">총 주행 거리</p>
          <div class="stat-value">
            {{ mileageDisplay }} <small>km</small>
          </div>
        </BaseCard>
      </div>
    </SectionPanel>
  </div>
</template>

<script setup>
import BaseCard from '../components/atoms/BaseCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/plugins/axios'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'
import { publish } from '@/plugins/ws'

const route = useRoute()

const MQTT_TOPIC_AMR_COMMAND = 'factory/amr/command'

const loadError = ref(null)
const detail = ref(null)
const resolvedAmrId = ref('')

const displayName = computed(() => detail.value?.name || detail.value?.id || resolvedAmrId.value || '-')

const statusLabel = computed(() => {
  const raw = String(detail.value?.status || '').toUpperCase()
  const labels = {
    OPERATING: '운행 중',
    IDLE: '대기',
    CHARGING: '충전 중',
    ERROR: '오류',
    EMERGENCY_STOP: '비상 정지'
  }
  return labels[raw] || raw || '-'
})

const statusBadgeClass = computed(() => {
  const raw = String(detail.value?.status || '').toUpperCase()
  if (raw === 'ERROR' || raw === 'EMERGENCY_STOP') return 'is-danger'
  if (raw === 'CHARGING') return 'is-charging'
  if (raw === 'IDLE') return 'is-idle'
  if (raw === 'OPERATING') return 'is-operating'
  return 'is-default'
})

const faultLine = computed(() => {
  if (String(detail.value?.status || '').toUpperCase() !== 'ERROR') return ''
  return detail.value?.faultMessage || detail.value?.faultCode || ''
})

const detailErrorBanner = computed(() => {
  if (String(detail.value?.status || '').toUpperCase() === 'EMERGENCY_STOP') {
    return 'EMERGENCY_STOP'
  }
  return ''
})

const currentTaskLine = computed(() => {
  const task = detail.value?.currentTask
  return task ? `현재 작업: ${task}` : '현재 작업: —'
})

const batteryLabel = computed(() => {
  const pct = detail.value?.batteryPercent
  if (pct === null || pct === undefined) return '—'
  return `${pct}%`
})

const mileageDisplay = computed(() => {
  const km = detail.value?.totalMileageKm
  if (km === null || km === undefined) return '—'
  return Number.isFinite(Number(km)) ? Number(km).toFixed(1) : String(km)
})

const lastSeenLabel = computed(() => {
  const raw = detail.value?.lastSeenAt
  if (!raw) return ''
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return ''
  return `최종 수신: ${date.toLocaleString('ko-KR')}`
})

function resolveAmrIdFromRoute() {
  const queryAmr = route.query.amr
  if (typeof queryAmr === 'string' && queryAmr.trim()) return queryAmr.trim()
  const paramAmr = route.params.amr
  if (typeof paramAmr === 'string' && paramAmr.trim()) return paramAmr.trim()
  return ''
}

async function loadAmrDetail() {
  const amrId = resolveAmrIdFromRoute()
  resolvedAmrId.value = amrId
  if (!amrId) {
    loadError.value = 'AMR 식별자가 없습니다. 목록에서 장비를 선택하세요.'
    detail.value = null
    return
  }

  try {
    const response = await api.get(`/amrs/${encodeURIComponent(amrId)}`)
    detail.value = response.data
    loadError.value = null
  } catch (err) {
    loadError.value = err.response?.data?.message || 'AMR 상세를 불러오지 못했습니다.'
    detail.value = null
    console.error('AmrDetailView load error', err)
  }
}

async function triggerEmergency() {
  const amrId = resolvedAmrId.value
  if (!amrId) return

  const confirmAction = window.confirm(`비상 정지 명령을 전송하시겠습니까? (${amrId})`)
  if (!confirmAction) return

  try {
    const resp = await api.post(`/amrs/${encodeURIComponent(amrId)}/commands`, { command: 'emergencyStop' })
    const accepted = resp?.data?.accepted === true

    if (accepted) {
      window.alert('비상 정지 명령이 수락되었습니다.')
      await loadAmrDetail()
      try {
        publish(MQTT_TOPIC_AMR_COMMAND, {
          amrId,
          command: 'emergencyStop',
          timestamp: new Date().toISOString()
        })
      } catch (pubErr) {
        console.warn('MQTT publish skipped:', pubErr)
      }
    } else {
      window.alert('비상 정지 요청이 전송되었으나 서버에서 수락 응답을 받지 못했습니다.')
    }
  } catch (err) {
    console.error('triggerEmergency error', err)
    const msg = err?.response?.data?.message || '비상 정지 요청 중 오류가 발생했습니다.'
    window.alert(msg)
  }
}

let refreshTimer = null

onMounted(() => {
  loadAmrDetail()
  refreshTimer = setInterval(loadAmrDetail, DEMO_REST_POLLING_INTERVAL_MS)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

watch(
  () => [route.query.amr, route.params.amr],
  () => {
    loadAmrDetail()
  }
)
</script>

<style scoped>
.detail-layout {
  display: grid;
}

.error-banner {
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: #fff4f4;
  border: 1px solid #fbcaca;
  color: #b91c1c;
  font-size: 0.82rem;
  font-weight: 700;
}

.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 10px; }
.detail-header__actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.amr-title { font-size: 1.1rem; }
.amr-id-chip {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  font-size: 0.72rem;
  font-weight: 700;
  color: #475569;
}
.amr-meta { display: block; margin-top: 6px; color: #94a3b8; font-size: 0.75rem; }

.error-msg { color: #ef4444; font-weight: 800; font-size: 0.9rem; }

.btn-emergency { padding: 8px 14px; background: #ef4444; color: #fff; border: none; border-radius: 6px; font-weight: 800; cursor: pointer; box-shadow: 0 4px 6px rgba(239,68,68,0.2); }
.btn-emergency:hover { background: #dc2626; }

.info-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 12px; margin-bottom: 12px; }
.info-card { background: white; border-radius: 8px; padding: 12px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }

.card-label { color: #64748b; font-size: 0.75rem; font-weight: 800; margin: 0 0 8px; }

.status-line { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 8px; }
.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
  background: #e2e8f0;
  color: #0f172a;
}
.status-badge.is-operating { background: #ecfdf5; color: #047857; }
.status-badge.is-idle { background: #fffbeb; color: #b45309; }
.status-badge.is-charging { background: #eff6ff; color: #1d4ed8; }
.status-badge.is-danger { background: #fef2f2; color: #b91c1c; }
.status-badge.is-default { background: #f1f5f9; color: #334155; }

.fault-text { font-size: 0.78rem; color: #b91c1c; font-weight: 600; }

.task-line { margin: 0 0 6px; font-size: 0.88rem; font-weight: 700; color: #0f172a; }
.battery-line { margin: 0; font-size: 0.82rem; color: #475569; }

.stat-value { font-size: 1.6rem; font-weight: 800; text-align: center; }
.stat-value small { font-size: 0.9rem; color: #64748b; }

@media (max-width: 960px) {
  .info-grid { grid-template-columns: 1fr; }
}
</style>
