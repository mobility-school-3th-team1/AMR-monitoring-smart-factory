import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  clearDemoEmergencyScenario,
  readDemoEmergencyScenario
} from '@/config/demo-emergency-scenario'
import api from '@/plugins/axios'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'
import { publish } from '@/plugins/ws'

const MQTT_TOPIC_AMR_COMMAND = 'factory/amr/command'

const STATUS_LABELS = {
  OPERATING: '운행 중',
  IDLE: '대기',
  CHARGING: '충전 중',
  ERROR: '오류',
  EMERGENCY_STOP: '비상 정지',
  STOPPED: '정지'
}

export function useAmrDetail(amrIdSource) {
  const loadError = ref(null)
  const detail = ref(null)
  const resolvedAmrId = ref('')
  const demoScenarioBanner = ref('')

  const displayName = computed(
    () => detail.value?.name || detail.value?.id || resolvedAmrId.value || '-'
  )

  const statusLabel = computed(() => {
    const raw = String(detail.value?.status || '').toUpperCase()
    const taskLine = String(detail.value?.currentTask || '')
    if (taskLine.includes('충전 스테이션')) {
      return '운행 중'
    }
    return STATUS_LABELS[raw] || raw || '-'
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

  const positionZone = computed(() => detail.value?.position?.zone || '—')

  const lastSeenLabel = computed(() => {
    const raw = detail.value?.lastSeenAt
    if (!raw) return ''
    const date = new Date(raw)
    if (Number.isNaN(date.getTime())) return ''
    return `최종 수신: ${date.toLocaleString('ko-KR')}`
  })

  function resolveAmrId() {
    const source = typeof amrIdSource === 'function' ? amrIdSource() : amrIdSource?.value
    return String(source || '').trim()
  }

  function refreshDemoScenarioBanner() {
    const scenario = readDemoEmergencyScenario()
    if (!scenario || !resolvedAmrId.value) {
      demoScenarioBanner.value = ''
      return
    }
    demoScenarioBanner.value =
      scenario.amrId === resolvedAmrId.value ? scenario.message : ''
  }

  async function loadAmrDetail() {
    const amrId = resolveAmrId()
    resolvedAmrId.value = amrId
    if (!amrId) {
      loadError.value = 'AMR 식별자가 없습니다.'
      detail.value = null
      return
    }

    try {
      const response = await api.get(`/amrs/${encodeURIComponent(amrId)}`)
      detail.value = response.data
      loadError.value = null
      refreshDemoScenarioBanner()
    } catch (err) {
      loadError.value = err.response?.data?.message || 'AMR 상세를 불러오지 못했습니다.'
      detail.value = null
      console.error('useAmrDetail load error', err)
    }
  }

  async function triggerEmergency() {
    const amrId = resolvedAmrId.value
    if (!amrId) return

    const confirmAction = window.confirm(`비상 정지 명령을 전송하시겠습니까? (${amrId})`)
    if (!confirmAction) return

    try {
      const resp = await api.post(`/amrs/${encodeURIComponent(amrId)}/commands`, {
        command: 'emergencyStop'
      })
      const accepted = resp?.data?.accepted === true

      if (accepted) {
        clearDemoEmergencyScenario()
        demoScenarioBanner.value = ''
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

  function handleDemoScenarioApplied() {
    loadAmrDetail()
  }

  function startPolling() {
    if (refreshTimer) return
    refreshTimer = setInterval(loadAmrDetail, DEMO_REST_POLLING_INTERVAL_MS)
  }

  function stopPolling() {
    if (!refreshTimer) return
    clearInterval(refreshTimer)
    refreshTimer = null
  }

  function setupLifecycle() {
    onMounted(() => {
      loadAmrDetail()
      startPolling()
      window.addEventListener('demo-scenario-applied', handleDemoScenarioApplied)
    })

    onUnmounted(() => {
      stopPolling()
      window.removeEventListener('demo-scenario-applied', handleDemoScenarioApplied)
    })

    watch(
      () => resolveAmrId(),
      () => {
        loadAmrDetail()
      }
    )
  }

  return {
    loadError,
    detail,
    resolvedAmrId,
    demoScenarioBanner,
    displayName,
    statusLabel,
    statusBadgeClass,
    faultLine,
    detailErrorBanner,
    currentTaskLine,
    batteryLabel,
    mileageDisplay,
    positionZone,
    lastSeenLabel,
    loadAmrDetail,
    triggerEmergency,
    setupLifecycle
  }
}
