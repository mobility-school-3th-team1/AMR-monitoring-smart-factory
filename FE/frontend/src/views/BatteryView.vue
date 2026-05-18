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
                <p class="station-name">{{ st.name }} · {{ st.location }}</p>
                <p class="station-meta">상태: {{ st.statusText }} · 평균 배터리 {{ st.averageBatteryPercent }}%</p>
              </div>
              <span class="station-status" :class="statusClass(st)">{{ st.statusLabel }}</span>
            </div>

            <div class="slot-indicator">
              <span class="slot-label">충전기 {{ st.occupiedCount }}/{{ st.capacity }} 사용</span>
              <div class="slots-container">
                <div v-for="i in st.capacity" :key="i" :class="['slot', { active: i <= st.occupiedCount }]" />
              </div>
            </div>

            <div class="amr-list">
              <div v-for="amr in st.amrs" :key="amr.amrId" class="amr-row">
                <div class="left"><strong>{{ amr.amrId }}</strong></div>
                <div class="middle">
                  <div class="progress" :class="progressClass(amr)"><span :style="{ width: amr.batteryPercent + '%' }"></span></div>
                  <span style="font-size:0.6rem; color:#64748b; margin-left:6px">{{ amr.batteryPercent }}%</span>
                </div>
                <div class="right">{{ amr.eta }}</div>
              </div>
            </div>
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
              <tr v-for="r in queue" :key="r.amrId"><td>{{ r.amrId }}</td><td>{{ r.eta }}</td><td>{{ r.stationName }}</td><td><span class="badge" :style="badgeStyle(r)">{{ r.stateLabel }}</span></td></tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="side-widgets">
        <div class="widget" style="flex:0.6;">
          <div class="widget-title">충전 완료 예상</div>
          <div class="summary-box">
            <div class="donut-mini" :style="donutStyle" :data-total="totalAmrs"></div>
            <div style="font-size:0.65rem; line-height:1.6;">
              <div><span style="color:#10b981;">●</span> 30분 이내: {{ forecastSummary[0] }}대</div>
              <div><span style="color:#3b82f6;">●</span> 1시간 이내: {{ forecastSummary[1] }}대</div>
              <div><span style="color:#f59e0b;">●</span> 1시간 초과: {{ forecastSummary[2] }}대</div>
            </div>
          </div>
        </div>

        <div class="widget alarm-list">
          <div class="widget-title" style="margin-bottom:6px;">충전 스테이션 알림</div>
          <div v-for="a in alarms" :key="a.id" class="alarm-item">
            <span class="alarm-icon">⚠️</span>
            <div><strong>{{ a.message }}</strong> <br><span style="color:#94a3b8">{{ a.time }}</span></div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/plugins/axios'

const stations = ref([])
const queue = ref([])
const forecast = ref([])
const alarms = ref([])

const stats = ref({ stations: 0, normal: 0, busy: 0, charging: 0, avgBattery: 0, maxEta: '0m' })

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

const forecastSummary = ref([0,0,0])

const donutStyle = { borderTopColor: '#10b981', borderLeftColor: '#f59e0b' }

const totalAmrs = computed(() => stations.value.reduce((acc, s) => acc + (s.amrs ? s.amrs.length : 0), 0))

async function loadData() {
  try {
    const stRes = await api.get('/charging/stations')
    stations.value = (stRes.data.data || stRes.data).map(s => ({ ...s, amrs: [] }))
  } catch (e) {
    stations.value = [
      { id: 'st1', name: '충전 스테이션 1', location: '원자재 창고', statusText: '정상', statusLabel: '정상', capacity: 4, occupiedCount: 3, averageBatteryPercent: 61, amrs: [ { amrId: 'AMR-01', batteryPercent:78, eta: '42분' }, { amrId: 'AMR-02', batteryPercent:45, eta: '1h 12m' }, { amrId: 'AMR-06', batteryPercent:61, eta: '55분' } ] },
      { id: 'st2', name: '충전 스테이션 2', location: '포장실', statusText: '정상', statusLabel: '정상', capacity: 4, occupiedCount: 2, averageBatteryPercent: 54, amrs: [ { amrId: 'AMR-03', batteryPercent:50, eta: '1h 00m' }, { amrId: 'AMR-09', batteryPercent:58, eta: '58분' } ] },
      { id: 'st3', name: '충전 스테이션 3', location: '출하장', statusText: '혼잡', statusLabel: '혼잡', capacity: 4, occupiedCount: 4, averageBatteryPercent: 72, amrs: [ { amrId: 'AMR-04', batteryPercent:21, eta: '1h 45m' }, { amrId: 'AMR-05', batteryPercent:40, eta: '1h 00m' }, { amrId: 'AMR-07', batteryPercent:60, eta: '38분' }, { amrId: 'AMR-08', batteryPercent:90, eta: '12분' } ] }
    ]
  }

  try {
    const qRes = await api.get('/charging/queue')
    queue.value = qRes.data.data || qRes.data
  } catch (e) {
    queue.value = [ { amrId: 'AMR-04', eta: '1h 45m', stationName: 'ST 3', state: 'waiting', stateLabel: '대기' }, { amrId: 'AMR-06', eta: '55m', stationName: 'ST 1', state: 'charging', stateLabel: '충전 중' } ]
  }

  try {
    const fRes = await api.get('/charging/forecast')
    forecast.value = fRes.data.data || fRes.data
  } catch (e) {
    forecast.value = [ { bucket: '0-30m', count: 3 }, { bucket: '30-60m', count: 4 }, { bucket: '60m+', count: 4 } ]
  }

  try {
    const aRes = await api.get('/dashboard/recent-alarms')
    alarms.value = (aRes.data.data || aRes.data).map(x => ({ id: x.id, message: x.message, time: new Date(x.occurredAt).toLocaleString() }))
  } catch (e) {
    alarms.value = [ { id: 'alarm1', message: '충전 스테이션 3 혼잡 상태 (사용률 100%)', time: '14:29' }, { id: 'alarm2', message: 'AMR-04 완충까지 1시간 45분 예상', time: '14:28' } ]
  }

  // compute stats
  stats.value.stations = stations.value.length
  stats.value.normal = stations.value.filter(s => s.occupiedCount < s.capacity).length
  stats.value.busy = stations.value.filter(s => s.occupiedCount >= s.capacity).length
  stats.value.charging = stations.value.reduce((acc,s)=> acc + s.occupiedCount,0)
  stats.value.avgBattery = Math.round(stations.value.reduce((acc,s)=> acc + (s.averageBatteryPercent||0),0) / stations.value.length)
  stats.value.maxEta = '1h 20m'

  forecastSummary.value = forecast.value.map(f => f.count)
}

onMounted(() => {
  loadData()
})

// totalAmrs computed above
</script>

<style scoped>
/* Use the mockup styles closely */
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

.amr-list { display:flex; flex-direction:column; gap:5px }
.amr-row { display:grid; grid-template-columns: 0.7fr 1fr auto; gap:8px; align-items:center; font-size:0.65rem; padding:6px 8px; border-radius:5px; background:#f8fafc; border:1px solid #e2e8f0 }
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
.badge { padding:2px 5px; border-radius:3px; font-size:0.6rem; font-weight:700 }

.side-widgets { display:flex; flex-direction:column; gap:10px }
.summary-box { display:flex; align-items:center; justify-content:space-around }
.donut-mini { width:70px; height:70px; border-radius:50%; border:10px solid #10b981; border-top-color:#f59e0b; border-left-color:#3b82f6; position:relative }
.donut-mini::before { content:'총 ' attr(data-total) '대'; position:absolute; top:50%; left:50%; transform:translate(-50%,-50%); font-size:0.55rem; font-weight:700; width:40px; text-align:center }
.alarm-list { flex:1; border:1px solid #f1f5f9; border-radius:6px; padding:5px; overflow:auto; max-height: calc(100vh * 0.32); }
.alarm-item { font-size:0.65rem; padding:5px; border-bottom:1px solid #f1f5f9; display:flex; gap:8px }
.alarm-icon { color:#ef4444; font-weight:700 }

@media (max-width: 1200px) { .row-bottom { grid-template-columns: 1fr } }

</style>
