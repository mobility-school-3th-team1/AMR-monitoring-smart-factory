<template>
  <div class="charging-layout">
    <section class="stations-list">
      <header class="list-header">
        <h3>충전 스테이션</h3>
        <small class="muted">스테이션을 선택하면 우측에 세부 충전 현황이 표시됩니다.</small>
      </header>

      <div class="stations" ref="listWrap">
        <article v-for="st in stations" :key="st.id" :class="['station-card', { active: selectedStation && selectedStation.id === st.id }]" @click="selectStation(st)">
          <div class="station-top">
            <strong class="station-name">{{ st.name }}</strong>
            <span class="station-loc muted">{{ st.location }}</span>
          </div>
          <div class="station-body">
            <div class="occupancy">
              <div class="bar-bg"><div class="bar-fill" :style="{ width: computeOccupancyPct(st) + '%' }"></div></div>
              <div class="occ-meta">{{ st.occupiedCount }} / {{ st.capacity }}</div>
            </div>
            <div class="battery">평균 배터리: <strong>{{ st.averageBatteryPercent }}%</strong></div>
          </div>
        </article>
      </div>
    </section>

    <section class="station-detail">
      <div v-if="!selectedStation" class="empty-state">스테이션을 선택하세요.</div>

      <div v-else class="detail-panel">
        <header class="detail-header">
          <h3>{{ selectedStation.name }}</h3>
          <div class="detail-meta muted">위치: {{ selectedStation.location }} · 용량: {{ selectedStation.capacity }}대 · 점유: {{ selectedStation.occupiedCount }}</div>
        </header>

        <div class="detail-body">
          <section class="queue-section">
            <h4>충전 중 / 대기 AMR</h4>
            <div v-if="queue.length === 0" class="muted">현재 충전 AMR이 없습니다.</div>
            <ul class="amr-list">
              <li v-for="a in queue" :key="a.amrId" class="amr-item">
                <div class="amr-main">
                  <strong>{{ a.amrId }}</strong>
                  <span class="muted">{{ a.status }}</span>
                </div>
                <div class="amr-stats">
                  <div class="battery-bar"><div class="battery-fill" :style="{ width: a.batteryPercent + '%' }"></div></div>
                  <div class="battery-val">{{ a.batteryPercent }}%</div>
                </div>
              </li>
            </ul>
          </section>

          <section class="forecast-section">
            <h4>완료 예측</h4>
            <div v-if="forecast.length === 0" class="muted">예측 데이터 없음</div>
            <div v-else class="forecast-chart">
              <div class="bucket" v-for="b in forecast" :key="b.bucket">
                <div class="count">{{ b.count }}</div>
                <div class="bucket-label">{{ b.bucket }}</div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/plugins/axios'

const stations = ref([])
const selectedStation = ref(null)
const queue = ref([])
const forecast = ref([])

async function fetchStations() {
  try {
    const res = await api.get('/charging/stations')
    stations.value = res.data.data || res.data
  } catch (e) {
    // fallback sample for dev
    stations.value = [
      { id: 'station-1', name: '충전 스테이션 1', location: '원자재 창고', status: 'normal', capacity: 4, occupiedCount: 3, averageBatteryPercent: 61 },
      { id: 'station-2', name: '충전 스테이션 2', location: '조립 라인', status: 'normal', capacity: 6, occupiedCount: 2, averageBatteryPercent: 72 }
    ]
  }
}

async function fetchQueue(stationId) {
  try {
    const res = await api.get('/charging/queue', { params: { stationId } })
    queue.value = res.data.data || res.data
  } catch (e) {
    queue.value = [
      { amrId: 'AMR-01', batteryPercent: 86, status: 'charging' },
      { amrId: 'AMR-04', batteryPercent: 38, status: 'charging' }
    ]
  }
}

async function fetchForecast(stationId) {
  try {
    const res = await api.get('/charging/forecast', { params: { stationId } })
    forecast.value = res.data.data || res.data
  } catch (e) {
    forecast.value = [ { bucket: '0-30m', count: 1 }, { bucket: '30-60m', count: 2 }, { bucket: '60m+', count: 1 } ]
  }
}

function selectStation(st) {
  selectedStation.value = st
  fetchQueue(st.id)
  fetchForecast(st.id)
}

function computeOccupancyPct(st) {
  if (!st || !st.capacity) return 0
  return Math.round((st.occupiedCount / st.capacity) * 100)
}

onMounted(async () => {
  await fetchStations()
  if (stations.value.length) selectStation(stations.value[0])
})
</script>

<style scoped>
.charging-layout { display: grid; grid-template-columns: 360px 1fr; gap: 16px; height: calc(100vh - 80px); }
.stations-list { background: var(--color-surface-soft); border-radius: 8px; padding: 12px; display:flex; flex-direction:column; }
.list-header h3 { margin:0; }
.stations { margin-top:10px; overflow:auto; padding-right:6px; }
.station-card { padding:10px; border-radius:8px; background:white; margin-bottom:8px; cursor:pointer; border:1px solid transparent; }
.station-card.active { border-color:#3b82f6; box-shadow: 0 6px 12px rgba(59,130,246,0.06); }
.station-top { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
.station-name { font-weight:800; }
.station-loc { font-size:0.82rem; }
.station-body { display:flex; justify-content:space-between; align-items:center; gap:12px; }
.bar-bg { width:140px; height:8px; background:#eef2ff; border-radius:6px; overflow:hidden; }
.bar-fill { height:100%; background:#6366f1; }
.occ-meta { font-size:0.85rem; color:#475569; }
.battery { font-size:0.85rem; color:#475569; }

.station-detail { background: var(--color-surface-soft); border-radius:8px; padding:14px; display:flex; flex-direction:column; }
.empty-state { color:#94a3b8; font-weight:600; display:flex; align-items:center; justify-content:center; height:100%; }
.detail-header h3 { margin:0; }
.detail-body { margin-top:12px; display:grid; grid-template-columns: 1fr 320px; gap:16px; }
.queue-section { background:white; border-radius:8px; padding:12px; border:1px solid #e2e8f0; }
.amr-list { list-style:none; margin:0; padding:0; display:flex; flex-direction:column; gap:8px; max-height:420px; overflow:auto; }
.amr-item { display:flex; align-items:center; justify-content:space-between; padding:8px; border-radius:8px; background:#f8fafc; }
.battery-bar { width:160px; height:8px; background:#e6f0ff; border-radius:6px; overflow:hidden; margin-right:8px; }
.battery-fill { height:100%; background:#10b981; }
.battery-val { min-width:38px; text-align:right; font-weight:700; }

.forecast-section { background:white; border-radius:8px; padding:12px; border:1px solid #e2e8f0; }
.forecast-chart { display:flex; gap:8px; align-items:end; height:120px; }
.bucket { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:flex-end; }
.bucket .count { background:#eef2ff; padding:6px 8px; border-radius:6px; font-weight:800; margin-bottom:6px; }
.bucket-label { font-size:0.82rem; color:#64748b; margin-top:8px; }

@media (max-width: 1000px) { .charging-layout { grid-template-columns: 1fr; grid-auto-rows: auto; } .detail-body { grid-template-columns: 1fr; } }
</style>
