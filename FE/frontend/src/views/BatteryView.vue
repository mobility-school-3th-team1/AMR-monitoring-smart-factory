<template>
  <div class="battery-root">
    <div class="summary-row">
      <div class="sum-card" style="border-top-color:#3b82f6;"><h4>운영 충전 스테이션</h4><div class="val">{{ stats.stations }}</div><div class="sub">정상 {{ stats.normal }} / 혼잡 {{ stats.busy }}</div></div>
      <div class="sum-card" style="border-top-color:#10b981;"><h4>충전 중 AMR</h4><div class="val">{{ stats.charging }}</div><div class="sub">현재 충전기에 연결된 AMR 수</div></div>
      <div class="sum-card" style="border-top-color:#f59e0b;"><h4>평균 충전률</h4><div class="val">{{ stats.avgBattery }}%</div><div class="sub">전체 스테이션 평균</div></div>
      <div class="sum-card" style="border-top-color:#ef4444;"><h4>완충 예상</h4><div class="val">{{ stats.maxEta }}</div><div class="sub">가장 오래 걸리는 AMR 기준</div></div>
    </div>

    <div class="row-top">
      <article class="widget">
        <div class="widget-title">충전 스테이션별 현황</div>
        <div class="stations-grid">
          <div v-for="st in stations" :key="st.id" :class="['station-card', { busy: st.occupiedCount >= st.capacity }]">
            <div class="station-info">
              <div>
                <p class="station-name">{{ st.name }}, {{ st.location || '—' }}</p>
                <p class="station-meta">상태: {{ st.statusText }}, 평균 배터리 {{ st.averageBatteryPercent }}%</p>
              </div>
              <span class="station-status" :class="statusClass(st)">{{ st.statusLabel }}</span>
            </div>

            <div class="slot-indicator">
              <span class="slot-label">충전기 {{ st.occupiedCount }}/{{ st.capacity }} 사용</span>
              <div class="slots-container">
                <div v-for="slotIndex in st.capacity" :key="slotIndex" :class="['slot', { active: slotIndex <= st.occupiedCount }]" />
              </div>
            </div>

            <div v-if="st.amrs && st.amrs.length" class="amr-list">
              <div v-for="amr in st.amrs" :key="amr.amrId" class="amr-row">
                <div class="left"><strong>{{ amr.amrId }}</strong></div>
                <div class="middle">
                  <div class="progress" :class="progressClass(amr)"><span :style="{ width: amr.batteryPercent + '%' }"></span></div>
                  <span class="battery-pct">{{ amr.batteryPercent }}%</span>
                </div>
                <div class="right">{{ amr.eta }}</div>
              </div>
            </div>
            <div v-else class="station-amr-placeholder">충전 중인 AMR이 없습니다.</div>
          </div>
        </div>
      </article>
    </div>

    <div class="row-bottom">
      <article class="widget">
        <div class="widget-title">충전 대기 큐</div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>AMR ID</th><th>대기시간</th><th>충전소</th><th>상태</th></tr></thead>
            <tbody>
              <template v-if="queue.length === 0">
                <tr class="empty-queue-row">
                  <td colspan="4">대기 중인 항목이 없습니다.</td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="r in queue" :key="r.amrId">
                  <td>{{ r.amrId }}</td>
                  <td>{{ r.eta }}</td>
                  <td>{{ r.stationName }}</td>
                  <td><span class="badge" :style="badgeStyle(r)">{{ r.stateLabel }}</span></td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="side-widgets">
        <div class="widget" style="flex:0.6;">
          <div class="widget-title">충전 완료 예상</div>
          <div class="summary-box">
            <div class="donut-mini" :style="donutStyle" :data-total="totalChargingAmrs"></div>
            <div class="forecast-legend">
              <div><span class="dot dot--g"></span> {{ forecastBucketLabels[0] }}: {{ forecastSummary[0] }}대</div>
              <div><span class="dot dot--b"></span> {{ forecastBucketLabels[1] }}: {{ forecastSummary[1] }}대</div>
              <div><span class="dot dot--o"></span> {{ forecastBucketLabels[2] }}: {{ forecastSummary[2] }}대</div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import api from '@/plugins/axios'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'
import {
  DEMO_FORECAST_BUCKET_LABELS,
  DEMO_PRIMARY_STATION_ID,
  forecastBucketsFromChargingAmrs,
  maxEtaFromChargingAmrs
} from '@/config/demo-simulation'

const forecastBucketLabels = DEMO_FORECAST_BUCKET_LABELS

const stations = ref([])
const queue = ref([])

const stats = ref({ stations: 0, normal: 0, busy: 0, charging: 0, avgBattery: 0, maxEta: '—' })

function statusClass(st) {
  if (st.occupiedCount >= st.capacity) return 'status-busy'
  return 'status-normal'
}

function progressClass(amr) {
  if (amr.batteryPercent >= 80) return 'progress green'
  if (amr.batteryPercent >= 50) return 'progress blue'
  if (amr.batteryPercent >= 20) return 'progress orange'
  return 'progress red'
}

function badgeStyle(r) {
  if (r.state === 'waiting') return { background: '#fff5e6', color: '#d97706' }
  return { background: '#e8f8f1', color: '#0e9f6e' }
}

const forecastSummary = ref([0, 0, 0])

const donutStyle = computed(() => {
  const [within5, within10, over10] = forecastSummary.value
  const bucketTotal = within5 + within10 + over10
  const chargingCount = totalChargingAmrs.value

  if (chargingCount === 0 || bucketTotal === 0) {
    return { background: '#e2e8f0' }
  }

  const ratio5 = (within5 / bucketTotal) * 100
  const ratio10 = (within10 / bucketTotal) * 100
  return {
    background: `conic-gradient(#10b981 0 ${ratio5}%, #3b82f6 ${ratio5}% ${ratio5 + ratio10}%, #f59e0b ${ratio5 + ratio10}% 100%)`
  }
})

const chargingAmrsForForecast = ref([])

const totalChargingAmrs = computed(() => chargingAmrsForForecast.value.length)

function collectPrimaryStationChargingAmrs(stationList) {
  const primaryStation = stationList.find((station) => station.id === DEMO_PRIMARY_STATION_ID)
  if (!primaryStation || !Array.isArray(primaryStation.amrs)) {
    return []
  }
  return primaryStation.amrs
}

function mapApiAmrToChargingRow(amr) {
  return {
    amrId: amr.id,
    amrName: amr.name,
    batteryPercent: amr.batteryPercent
  }
}

async function fetchChargingAmrsForForecast() {
  try {
    const response = await api.get('/amrs?status=CHARGING&limit=50')
    const list = response.data?.data ?? []
    return Array.isArray(list) ? list.map(mapApiAmrToChargingRow) : []
  } catch (fetchError) {
    console.error('BatteryView charging amrs', fetchError)
    return []
  }
}

function mergeChargingAmrRows(stationAmrs, apiAmrs) {
  const merged = new Map()
  for (const row of [...stationAmrs, ...apiAmrs]) {
    if (row?.amrId) {
      merged.set(row.amrId, row)
    }
  }
  return [...merged.values()]
}

function applyChargingForecastFromAmrs(chargingAmrs) {
  chargingAmrsForForecast.value = chargingAmrs
  forecastSummary.value = forecastBucketsFromChargingAmrs(chargingAmrs)
  stats.value.maxEta = maxEtaFromChargingAmrs(chargingAmrs)
}

function normalizeStationDto(raw) {
  const statusRaw = raw.status || 'NORMAL'
  const isBusy = Number(raw.occupiedCount) >= Number(raw.capacity)
  return {
    ...raw,
    statusText: statusRaw,
    statusLabel: isBusy ? '혼잡' : '정상',
    averageBatteryPercent: raw.averageBatteryPercent ?? 0,
    amrs: Array.isArray(raw.amrs) ? raw.amrs : []
  }
}

async function loadData() {
  try {
    const stRes = await api.get('/charging/stations')
    const rawList = stRes.data?.data ?? stRes.data ?? []
    stations.value = (Array.isArray(rawList) ? rawList : []).map(normalizeStationDto)
  } catch (e) {
    stations.value = []
    console.error('BatteryView stations', e)
  }

  try {
    const queueRes = await api.get(`/charging/queue?stationId=${encodeURIComponent(DEMO_PRIMARY_STATION_ID)}`)
    const queueList = queueRes.data?.data ?? queueRes.data ?? []
    queue.value = Array.isArray(queueList) ? queueList : []
  } catch (queueError) {
    queue.value = []
    console.error('BatteryView queue', queueError)
  }

  const stationAmrs = collectPrimaryStationChargingAmrs(stations.value)
  const apiAmrs = await fetchChargingAmrsForForecast()
  const chargingAmrs = mergeChargingAmrRows(stationAmrs, apiAmrs)
  applyChargingForecastFromAmrs(chargingAmrs)

  const stationCount = stations.value.length
  stats.value.stations = stationCount
  stats.value.normal = stations.value.filter((s) => s.occupiedCount < s.capacity).length
  stats.value.busy = stations.value.filter((s) => s.occupiedCount >= s.capacity).length
  stats.value.charging = chargingAmrs.length
  stats.value.avgBattery = stationCount
    ? Math.round(
        stations.value.reduce((acc, s) => acc + (s.averageBatteryPercent || 0), 0) / stationCount
      )
    : 0
  if (chargingAmrs.length === 0) {
    stats.value.maxEta = '—'
  }
}

let refreshTimer = null

onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, DEMO_REST_POLLING_INTERVAL_MS)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
* { box-sizing: border-box }
.battery-root { display:flex; flex-direction:column; gap:10px; height: calc(100vh - 72px); min-height:0; padding:10px; overflow:hidden }
.summary-row { display:grid; grid-template-columns: repeat(4, 1fr); gap:10px }
.sum-card { background:white; border-radius:8px; padding:10px 12px; box-shadow:0 2px 4px rgba(0,0,0,0.05); border-top:3px solid #3b82f6 }
.sum-card h4 { font-size:0.68rem; color:#64748b; margin-bottom:4px }
.sum-card .val { font-size:1.15rem; font-weight:800 }
.sum-card .sub { font-size:0.58rem; color:#94a3b8; margin-top:2px }

.row-top { display:grid; grid-template-columns: 1fr; gap:10px; height: 48%; min-height:0 }
.widget { background:white; border-radius:8px; padding:12px; box-shadow:0 2px 4px rgba(0,0,0,0.05); display:flex; flex-direction:column; min-height:0 }
.widget-title { font-size:0.75rem; font-weight:700; margin-bottom:10px; border-left:3px solid #3b82f6; padding-left:8px }

.stations-grid { display:flex; flex-direction:column; gap:10px; overflow:auto; padding-right:6px; max-height: calc(100vh * 0.45); }
.station-card { border:1px solid #dbe4f0; border-radius:8px; padding:12px; background:linear-gradient(to right,#fafbfc 0,#fff 100%); display:grid; grid-template-columns: 2fr 1.2fr 2.5fr; gap:12px; align-items:center }
.station-card.busy { background:linear-gradient(to right,#fffaf0 0,#fff 100%); border-color:#fcd34d }
.station-info { display:flex; flex-direction:column; gap:6px }
.station-name { font-size:0.78rem; font-weight:800; color:#0f172a }
.station-meta { font-size:0.62rem; color:#64748b }
.station-status { font-size:0.65rem; padding:3px 8px; border-radius:4px; font-weight:700; display:inline-block }
.status-normal { background:#e8f8f1; color:#0e9f6e }
.status-busy { background:#fff5e6; color:#d97706 }

.slot-indicator { display:flex; gap:6px; align-items:center }
.slots-container { display:grid; grid-template-columns: repeat(auto-fit, minmax(18px,1fr)); gap:4px; flex:1; max-width:120px }
.slot { width:100%; aspect-ratio:1; border-radius:4px; border:1px solid #cbd5e1; background:#f1f5f9 }
.slot.active { background:#10b981; border-color:#059669; box-shadow:0 0 8px rgba(16,185,129,0.3) }
.slot-label { font-size:0.62rem; color:#64748b; white-space:nowrap }

.station-amr-placeholder { font-size:0.62rem; color:#94a3b8; font-style: italic; }

.amr-list { display:flex; flex-direction:column; gap:5px }
.amr-row { display:grid; grid-template-columns: 0.7fr 1fr auto; gap:8px; align-items:center; font-size:0.65rem; padding:6px 8px; border-radius:5px; background:#f8fafc; border:1px solid #e2e8f0 }
.battery-pct { font-size:0.6rem; color:#64748b; margin-left:6px }
.progress { margin-top:3px; height:4px; background:#e2e8f0; border-radius:999px; overflow:hidden }
.progress > span { display:block; height:100%; border-radius:999px; background:#10b981 }
.progress.blue > span { background:#3b82f6 }
.progress.orange > span { background:#f59e0b }
.progress.red > span { background:#ef4444 }

.row-bottom { display:grid; grid-template-columns: 1.8fr 1fr; gap:10px; flex:1; min-height:0 }
.table-wrapper { flex:1; overflow:auto; border:1px solid #e2e8f0; border-radius:6px; max-height: calc(100vh * 0.32); }
table { width:100%; border-collapse:collapse; font-size:0.68rem }
th { background:#f8fafc; padding:8px; border-bottom:2px solid #e2e8f0; text-align:left; color:#64748b; position:sticky; top:0 }
td { padding:6px 8px; border-bottom:1px solid #f1f5f9 }
.empty-queue-row td { text-align:center; color:#94a3b8; padding:16px; }
.badge { padding:2px 5px; border-radius:3px; font-size:0.6rem; font-weight:700 }

.side-widgets { display:flex; flex-direction:column; gap:10px }
.summary-box { display:flex; align-items:center; justify-content:space-around }
.forecast-legend { font-size:0.65rem; line-height:1.6; }
.dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:4px; }
.dot--g { background:#10b981; }
.dot--b { background:#3b82f6; }
.dot--o { background:#f59e0b; }
.donut-mini { width:70px; height:70px; border-radius:50%; border:10px solid #10b981; border-top-color:#f59e0b; border-left-color:#3b82f6; position:relative }
.donut-mini::before { content:'총 ' attr(data-total) '대'; position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); font-size:0.55rem; font-weight:700; width:40px; text-align:center }

@media (max-width: 1200px) { .row-bottom { grid-template-columns: 1fr } }

</style>
