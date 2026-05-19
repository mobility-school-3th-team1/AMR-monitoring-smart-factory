<template>
  <div class="dashboard-view">
    <!-- KPI 요약 카드 -->
    <section class="kpi-cards">
      <div class="kpi-card">
        <div class="kpi-label">운영 중 AMR</div>
        <div class="kpi-value">{{ dashboardData.amrOperating }}<span class="kpi-unit">대</span></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">충전 중 AMR</div>
        <div class="kpi-value">{{ dashboardData.amrCharging }}<span class="kpi-unit">대</span></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">대기 중 AMR</div>
        <div class="kpi-value">{{ dashboardData.amrWaiting }}<span class="kpi-unit">대</span></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">미해결 알람</div>
        <div class="kpi-value">{{ dashboardData.activeAlarms }}<span class="kpi-unit">건</span></div>
      </div>
    </section>

    <!-- 에러 메시지 -->
    <div v-if="error" class="error-banner">
      <span>✕ {{ error }}</span>
      <button class="error-retry" @click="fetchDashboardData(true)">다시 시도</button>
    </div>

    <!-- 메인 대시보드 그리드 -->
    <div v-if="!isLoading" class="dashboard-grid">
      <!-- 플로어맵 (좌측) -->
      <section class="dashboard-panel compact-map-panel">
        <div class="floor-map-wrap">
          <div class="map-label-chip">AMR 위치 현황도</div>
          <div class="floor-map">
            <div class="floor-map-grid">
              <img class="floor-map-image" :src="factoryLayoutAssetUrl" alt="공장 레이아웃" />
              <div v-if="visibleAmrList.length === 0" class="map-overlay map-overlay--hint">AMR 좌표 수신 대기 중입니다.</div>
              <div
                v-for="amr in visibleAmrList"
                :key="amr.id"
                class="amr-mark"
                :class="mapStatusClass(amr.status)"
                :data-battery="`${amr.batteryPercent || 0}%`"
                :style="{ left: `${amr.mapPosition.x}%`, top: `${amr.mapPosition.y}%` }"
              >
                {{ amr.name || amr.id }}
              </div>
            </div>
          </div>

          <div class="status-strip">
            <div class="strip-item"><div class="k">운행 중 AMR</div><div class="v status-ok">{{ dashboardData.amrOperating }}대</div></div>
            <div class="strip-item"><div class="k">대기 AMR</div><div class="v status-warn">{{ dashboardData.amrWaiting }}대</div></div>
            <div class="strip-item"><div class="k">주의 AMR</div><div class="v status-critical">{{ dashboardData.activeAlarms }}건</div></div>
            <div class="strip-item"><div class="k">가동률</div><div class="v status-ok">{{ Math.round((dashboardData.amrOperating || 0) / Math.max(1, (dashboardData.amrOperating || 0) + (dashboardData.amrWaiting || 0)) * 100) }}%</div></div>
          </div>
        </div>
      </section>

      <!-- 공정 구역 환경 현황 (우측 상단) -->
      <section class="dashboard-panel compact-map-panel">
        <div class="floor-map-wrap">
          <div class="map-label-chip">공정 구역 환경 현황</div>
          <div class="floor-map">
            <div class="floor-map-grid env-grid">
              <img class="floor-map-image" :src="factoryLayoutAssetUrl" alt="공장 레이아웃" />
              <div class="map-overlay">
                <div
                  v-for="area in environmentAreaCards"
                  :key="area.areaId"
                  class="map-zone"
                  :class="area.tone"
                  :style="{
                    top: `${area.overlay.top}%`,
                    left: `${area.overlay.left}%`,
                    width: `${area.overlay.width}%`,
                    height: `${area.overlay.height}%`
                  }"
                >
                  <span class="zone-title">{{ area.areaName }}</span>
                  <div class="zone-sensor-list">
                    <div v-for="sensor in area.sensors" :key="sensor.slot" class="zone-sensor-row">
                      <span class="slot-label">{{ sensor.slotLabel }}</span>
                      <span class="zone-meta">{{ sensor.temp }}°C, H {{ sensor.humid }}%, P {{ sensor.particle }}, CO {{ sensor.cogas }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 하단: 중요 알람 + 작업 로그 -->
      <div class="bottom-grid">
        <section class="dashboard-panel">
          <div class="panel-title">실시간 중요 알람</div>
          <div class="panel-subtitle">AMR 기능 고장 감지, 충돌 감지, 안전 이벤트</div>
          <div class="list-box">
            <div class="list-scroll">
              <template v-if="recentAlarms.length === 0">
                <div class="empty-state"><p>활성 알람이 없습니다.</p></div>
              </template>
              <template v-else>
                <div v-for="alarm in recentAlarms" :key="alarm.id" class="alert-row">
                  <span class="alert-severity" :class="alarm.level === 'CRITICAL' ? 'critical' : (alarm.level === 'WARNING' ? 'warn' : '')"></span>
                  <div class="alert-text">
                    <div class="alert-title">{{ alarm.title || alarm.message }}</div>
                    <div class="alert-desc">{{ alarm.message }}</div>
                  </div>
                  <span class="alert-chip" :class="alarm.level === 'CRITICAL' ? 'critical' : (alarm.level === 'WARNING' ? 'warn' : '')">{{ alarm.level }}, {{ formatTime(alarm.occurredAt) }}</span>
                </div>
              </template>
            </div>
          </div>
        </section>

        <section class="dashboard-panel">
          <div class="panel-title">실시간 작업 로그</div>
          <div class="panel-subtitle">AMR 이동 시작, 스테이션 이동 시작, 명령 수신</div>
          <div class="list-box">
            <div class="list-scroll">
              <template v-if="recentLogs.length === 0">
                <div class="empty-state"><p>최근 작업 로그가 없습니다.</p></div>
              </template>
              <template v-else>
                <div v-for="(log, idx) in recentLogs" :key="idx" class="log-row">
                  <div class="log-icon">{{ (log.event || '').slice(0,2).toUpperCase() }}</div>
                  <div>
                    <div class="log-head">
                      <div class="log-title">{{ log.title || log.event }}</div>
                      <div class="time-badge">{{ formatTime(log.timestamp || log.occurredAt) }}</div>
                    </div>
                    <div class="log-desc">{{ log.detail || log.message || '' }}</div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-else class="loading-state"><div class="spinner"></div><p>데이터 로드 중...</p></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/plugins/axios'
import { subscribe } from '@/plugins/ws'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'
import { ENV_SENSOR_SLOT_KEYS, FACTORY_LAYOUT_AREAS } from '@/config/factory-layout-areas'
import factoryLayoutAssetUrl from '@/assets/factory-layout.png'

// 기본값 — API 응답에서 누락된 필드를 0으로 보호
const DASHBOARD_DEFAULTS = {
  activeAlarms: 0,
  amrOperating: 0,
  amrCharging: 0,
  amrWaiting: 0,
  averageTaskTimeMin: 0
}

const MQTT_TOPICS = {
  environmentCurrent: 'factory/environment/current',
  amrPositions: 'factory/amrs/positions'
}

const DEFAULT_SENSOR_VALUE = {
  temp: '-',
  humid: '-',
  particle: '-',
  cogas: '-'
}

const DASHBOARD_DUMMY_ALARMS = [
  {
    id: 'dummy-critical-1',
    level: 'CRITICAL',
    title: '비상 스위치 점검 필요',
    message: 'AMR-01 비상 스위치 응답이 지연되었습니다.',
    occurredAt: new Date().toISOString()
  },
  {
    id: 'dummy-warning-1',
    level: 'WARNING',
    title: '충전 대기열 혼잡',
    message: '입고 구역 충전 스테이션 대기열이 증가했습니다.',
    occurredAt: new Date().toISOString()
  }
]

const DASHBOARD_DUMMY_ENVIRONMENT = {
  AREA_LOAD_LC: {
    sensor1: { temp: 25, humid: 33, particle: 22, cogas: 5 },
    sensor2: { temp: 24, humid: 32, particle: 21, cogas: 4 },
    sensor3: { temp: 26, humid: 34, particle: 23, cogas: 6 },
    sensor4: { temp: 25, humid: 33, particle: 22, cogas: 5 }
  },
  AREA_ASSEMBLE_01: {
    sensor1: { temp: 24, humid: 42, particle: 16, cogas: 7 },
    sensor2: { temp: 25, humid: 44, particle: 18, cogas: 6 },
    sensor3: { temp: 24, humid: 41, particle: 17, cogas: 6 },
    sensor4: { temp: 25, humid: 43, particle: 19, cogas: 8 }
  },
  AREA_ASSEMBLE_02: {
    sensor1: { temp: 23, humid: 39, particle: 14, cogas: 5 },
    sensor2: { temp: 22, humid: 38, particle: 15, cogas: 5 },
    sensor3: { temp: 23, humid: 37, particle: 14, cogas: 4 },
    sensor4: { temp: 22, humid: 39, particle: 15, cogas: 5 }
  },
  AREA_OUT_BSA: {
    sensor1: { temp: 26, humid: 47, particle: 20, cogas: 9 },
    sensor2: { temp: 27, humid: 49, particle: 21, cogas: 10 },
    sensor3: { temp: 26, humid: 46, particle: 20, cogas: 9 },
    sensor4: { temp: 27, humid: 48, particle: 22, cogas: 11 }
  }
}

const MQTT_POSITION_FALLBACK = {
  startX: 12,
  startY: 22,
  xStep: 18,
  yStep: 28,
  rowSize: 4
}

// 상태
const isLoading = ref(true)
const error = ref(null)
const dashboardData = ref({ ...DASHBOARD_DEFAULTS })
const amrList = ref([])
const amrPositionMap = ref({})
const environmentAreas = ref({ ...DASHBOARD_DUMMY_ENVIRONMENT })
const recentAlarms = ref([])
const recentLogs = ref([])

// 플로어맵에 표시할 AMR 최대 8개 — 매 렌더마다 slice 재계산을 막기 위해 computed 사용
const visibleAmrList = computed(() => {
  return amrList.value.slice(0, 8).map((amr, index) => {
    const mqttPosition = amrPositionMap.value[amr.id]
    const fallbackX = MQTT_POSITION_FALLBACK.startX + (index % MQTT_POSITION_FALLBACK.rowSize) * MQTT_POSITION_FALLBACK.xStep
    const fallbackY = MQTT_POSITION_FALLBACK.startY + Math.floor(index / MQTT_POSITION_FALLBACK.rowSize) * MQTT_POSITION_FALLBACK.yStep

    return {
      ...amr,
      mapPosition: mqttPosition || {
        x: clampPercent(fallbackX),
        y: clampPercent(fallbackY)
      }
    }
  })
})

const environmentAreaCards = computed(() => {
  return FACTORY_LAYOUT_AREAS.map((layoutArea) => {
    const areaPayload = environmentAreas.value[layoutArea.areaId] || {}
    const sensors = ENV_SENSOR_SLOT_KEYS.map((slotKey, index) => {
      const sensorPayload = areaPayload[slotKey] || DEFAULT_SENSOR_VALUE

      return {
        slot: slotKey,
        slotLabel: `S${index + 1}`,
        temp: safeMetric(sensorPayload.temp),
        humid: safeMetric(sensorPayload.humid),
        particle: safeMetric(sensorPayload.particle),
        cogas: safeMetric(sensorPayload.cogas)
      }
    })

    return {
      areaId: layoutArea.areaId,
      areaName: layoutArea.areaName,
      overlay: layoutArea.overlay,
      tone: layoutArea.defaultTone,
      sensors
    }
  })
})

// API 호출
// showLoading: 최초 로드 시에만 true — 폴링 갱신 시에는 스피너 없이 인플레이스 업데이트
const fetchDashboardData = async (showLoading = false) => {
  try {
    if (showLoading) isLoading.value = true
    error.value = null

    const [summaryRes, alarmsRes, logsRes, amrsRes] = await Promise.allSettled([
      api.get('/dashboard/summary'),
      api.get('/dashboard/recent-alarms?limit=5'),
      api.get('/dashboard/recent-logs?page=1&limit=10'),
      api.get('/amrs?page=1&limit=20')
    ])

    const failedEndpoints = []

    if (summaryRes.status === 'fulfilled') {
      dashboardData.value = { ...DASHBOARD_DEFAULTS, ...summaryRes.value.data }
    } else {
      dashboardData.value = { ...DASHBOARD_DEFAULTS }
      failedEndpoints.push('summary')
    }

    if (alarmsRes.status === 'fulfilled') {
      recentAlarms.value = alarmsRes.value.data.data || []
    } else {
      recentAlarms.value = DASHBOARD_DUMMY_ALARMS
      failedEndpoints.push('recent-alarms')
    }

    if (logsRes.status === 'fulfilled') {
      recentLogs.value = logsRes.value.data.data || []
    } else {
      recentLogs.value = []
      failedEndpoints.push('recent-logs')
    }

    if (amrsRes.status === 'fulfilled') {
      amrList.value = amrsRes.value.data.data || []
    } else {
      amrList.value = []
      failedEndpoints.push('amrs')
    }

    if (failedEndpoints.length > 0) {
      error.value = `일부 데이터 갱신 실패: ${failedEndpoints.join(', ')}`
    }
  } catch (err) {
    error.value = err.response?.data?.message || '데이터 로드 실패'
    console.error('Dashboard error:', err)
  } finally {
    if (showLoading) isLoading.value = false
  }
}

const mapStatusClass = (status) => {
  const normalizedStatus = String(status || '').toUpperCase()
  if (normalizedStatus === 'CHARGING') return 'status-charging'
  if (normalizedStatus === 'IDLE') return 'status-waiting'
  if (normalizedStatus === 'ERROR' || normalizedStatus === 'EMERGENCY_STOP') return 'status-risk'
  return 'status-driving'
}

const clampPercent = (value) => {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) return 0
  return Math.min(100, Math.max(0, numericValue))
}

const safeMetric = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return value
}

const handleEnvironmentPayload = (payload) => {
  if (!payload || typeof payload !== 'object') return
  if (!payload.areas || typeof payload.areas !== 'object') return

  environmentAreas.value = {
    ...DASHBOARD_DUMMY_ENVIRONMENT,
    ...payload.areas
  }
}

const handleAmrPositionPayload = (payload) => {
  if (!Array.isArray(payload)) return

  const nextPositionMap = payload.reduce((accumulator, amrPosition) => {
    if (!amrPosition || !amrPosition.amrId) return accumulator

    accumulator[amrPosition.amrId] = {
      x: clampPercent(amrPosition.x),
      y: clampPercent(amrPosition.y)
    }

    return accumulator
  }, {})

  amrPositionMap.value = nextPositionMap
}

// 시간 포맷
const formatTime = (isoString) => {
  if (!isoString) return '-'
  const date = new Date(isoString)
  return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

// 새로고침 타이머
let refreshTimer = null
const mqttUnsubscribeHandlers = []

onMounted(() => {
  fetchDashboardData(true)
  refreshTimer = setInterval(() => fetchDashboardData(false), DEMO_REST_POLLING_INTERVAL_MS)

  try {
    const unsubscribeEnvironment = subscribe(MQTT_TOPICS.environmentCurrent, handleEnvironmentPayload)
    const unsubscribeAmrPositions = subscribe(MQTT_TOPICS.amrPositions, handleAmrPositionPayload)

    mqttUnsubscribeHandlers.push(unsubscribeEnvironment)
    mqttUnsubscribeHandlers.push(unsubscribeAmrPositions)
  } catch (mqttError) {
    console.warn('[Dashboard] MQTT subscribe skipped:', mqttError)
  }
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  mqttUnsubscribeHandlers.forEach((unsubscribeHandler) => {
    if (typeof unsubscribeHandler === 'function') {
      unsubscribeHandler()
    }
  })
})
</script>

<style scoped>

.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 4px;
  height: 100vh;
  min-height: 0;
  padding: 8px 0 0 0;
}

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 6px;
  align-items: stretch;
  min-height: 0;
}

.kpi-card {
  background: white;
  border: 1px solid #e6eef8;
  border-left: 3px solid #3b82f6;
  border-radius: 5px;
  padding: 6px 6px 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-height: 38px;
}

.kpi-label { font-size: 0.60rem; font-weight: 700; color: #64748b; }
.kpi-value { font-size: 0.92rem; font-weight: 800; color: #0f172a; }
.kpi-unit { font-size: 0.62rem; margin-left: 4px; color: #94a3b8; }

.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fff4f4;
  border: 1px solid #fbcaca;
  border-radius: 6px;
  color: #b91c1c;
  font-size: 0.82rem;
}

.error-retry { margin-left: auto; padding: 6px 10px; background: #b91c1c; color: #fff; border: none; border-radius: 4px; font-size: 0.75rem; }

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  grid-template-rows: 1.1fr 0.7fr;
  gap: 6px;
  flex: 1;
  min-height: 0;
  height: 44vh;
}

.dashboard-panel {
  background: #ffffff;
  border: 1px solid rgba(219, 228, 240, 0.9);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  padding: 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* compact map panels for top row */
.compact-map-panel { padding: 12px; }

.floor-map-wrap {
  flex: 1;
  min-height: 0;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fafc 100%);
  border: 1px solid #e5edf7;
  border-radius: 12px;
  padding: 5px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.floor-map {
  display: flex;
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow: hidden;
}

.floor-map-grid {
  display: block;
  width: 100%;
  height: 100%;
  position: relative;
  background: #eef3f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.floor-map-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  opacity: 0.94;
  z-index: 1;
}
.map-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
}
.map-overlay--hint {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 10px;
  color: #475569;
  font-size: 0.62rem;
  font-weight: 700;
  pointer-events: none;
}
.map-label-chip { position: absolute; right: 8px; top: 8px; padding: 4px 8px; border-radius: 999px; background: rgba(255,255,255,0.92); border: 1px solid #e2e8f0; color: #475569; font-size: 0.68rem; font-weight: 700; z-index: 6; }

.floor-map-wrap > .map-label-chip {
  position: absolute;
  right: 8px;
  top: 8px;
}

.amr-mark {
  position: absolute;
  width: 44px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 0.52rem;
  font-weight: 800;
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transform: translate(-50%, -50%);
  z-index: 3;
}

.amr-mark::after {
  content: attr(data-battery);
  font-size: 0.43rem;
  opacity: 0.9;
}

.status-driving { background: #10b981; }
.status-charging { background: #3b82f6; }
.status-waiting { background: #f59e0b; }
.status-risk { background: #ef4444; }

.map-zone {
  position: absolute;
  border: 1px solid #cbd5e1;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 3px;
  padding: 4px 6px;
  font-size: 0.56rem;
  font-weight: 700;
  color: #64748b;
  text-align: left;
  line-height: 1.15;
  border-radius: 4px;
  z-index: 3;
}

.map-zone.env-low { background: rgba(248, 255, 251, 0.86); }
.map-zone.env-mid { background: rgba(255, 250, 242, 0.86); }

.zone-title {
  display: block;
  font-size: 0.62rem;
  color: #0f172a;
  margin-bottom: 1px;
}

.zone-meta {
  display: inline;
  font-size: 0.5rem;
  color: #475569;
  font-weight: 600;
  line-height: 1.15;
}

.zone-sensor-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.zone-sensor-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.slot-label {
  color: #0f172a;
  font-size: 0.48rem;
  font-weight: 800;
  min-width: 14px;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  margin-top: 0;
}
.strip-item {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e5edf7;
  border-radius: 7px;
  padding: 4px 6px;
}
.strip-item .k { font-size: 0.58rem; color: #64748b; }
.strip-item .v { margin-top: 1px; font-size: 0.68rem; font-weight: 800; }

/* bottom grid with alerts and logs */
.bottom-grid {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 6px;
  min-height: 0;
  height: 30vh;
}
.list-box {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.list-scroll {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: auto;
  padding-right: 2px;
}

.alert-row, .log-row {
  background: #f8fafc;
  border: 1px solid #e5edf7;
  border-radius: 7px;
  padding: 5px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 5px;
  align-items: center;
}
.alert-severity { width: 8px; height: 8px; border-radius: 50%; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.08); }
.alert-severity.critical { background: #ef4444; }
.alert-severity.warn { background: #f59e0b; }
.alert-text { display: flex; flex-direction: column; gap: 2px; }
.alert-title { font-size: 0.68rem; font-weight: 800; color: #111827; }
.alert-desc { font-size: 0.62rem; color: #64748b; line-height: 1.2; }
.alert-chip { font-size: 0.58rem; font-weight: 800; padding: 2px 6px; border-radius: 999px; border: 1px solid currentColor; background: white; white-space: nowrap; }
.alert-chip.critical { color: #ef4444; }
.alert-chip.warn { color: #f59e0b; }

.log-row { grid-template-columns: auto 1fr; }
.log-icon { width: 20px; height: 20px; border-radius: 5px; display: grid; place-items: center; background: rgba(59, 130, 246, 0.12); color: #2563eb; font-weight: 900; font-size: 0.58rem; }
.log-head { display: flex; align-items: center; justify-content: space-between; gap: 5px; margin-bottom: 1px; }
.log-title { font-size: 0.68rem; font-weight: 800; color: #111827; }
.time-badge { font-size: 0.58rem; color: #64748b; white-space: nowrap; }
.log-desc { font-size: 0.62rem; color: #64748b; }

@media (max-width: 1400px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(4, minmax(0, 1fr));
    height: auto;
  }
  .bottom-grid {
    grid-column: auto;
    grid-template-columns: 1fr;
    height: auto;
  }
}

.panel-title { font-size: 0.75rem; font-weight: 800; color: #0f172a; margin: 0 0 4px; }

.floor-map {
  position: relative;
  flex: 1;
  min-height: 120px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fafc 100%);
  border: none;
  border-radius: 0;
}

.floor-map__robot {
  position: absolute;
  width: 56px;
  padding: 6px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
  font-size: 0.68rem;
  font-weight: 700;
  text-align: center;
  transform: translate(-50%, -50%);
  box-shadow: 0 4px 10px rgba(2,6,23,0.08);
  z-index: 6;
}

.floor-map__robot small {
  display: block;
  margin-top: 2px;
  font-size: 0.5rem;
  opacity: 0.9;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 0.85rem;
}

.empty-state p {
  margin: 0;
}

.alert-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow-y: auto;
}

.alert-list li {
  display: flex;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  border-left: 3px solid #3b82f6;
  background: #f5fbff;
  font-size: 0.78rem;
}

.alert-list p {
  margin: 0 0 2px;
  font-weight: 600;
  color: #1f2937;
}

.alert-list div span {
  display: block;
  font-size: 0.7rem;
  color: #94a3b8;
}

.alert-critical { border-left-color: #ef4444; background: #fef2f2; }
.alert-warning { border-left-color: #f59e0b; background: #fffbf0; }
.alert-info { border-left-color: #3b82f6; background: #eff6ff; }

.alert-level {
  flex-shrink: 0;
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.65rem;
  font-weight: 700;
  color: white;
}

.alert-critical .alert-level,
.alert-level.alert-critical { background: #ef4444; }
.alert-warning .alert-level,
.alert-level.alert-warning { background: #f59e0b; }
.alert-info .alert-level,
.alert-level.alert-info { background: #3b82f6; }

.simple-table { width: 100%; border-collapse: collapse; font-size: 0.78rem; }
.simple-table th { padding: 6px; border-bottom: 1px solid #eef6ff; text-align: left; font-weight: 700; color: #64748b; background: #fbfdff; }
.simple-table td { padding: 6px; border-bottom: 1px solid #eef6ff; color: #0f172a; }

.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1400px) {
  .kpi-cards { grid-template-columns: repeat(2, minmax(120px, 1fr)); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .floor-map-panel { grid-row: auto; }
}

@media (max-width: 900px) {
  .kpi-cards { grid-template-columns: repeat(2, minmax(120px, 1fr)); }
}
</style>