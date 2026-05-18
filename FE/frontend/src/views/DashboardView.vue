<template>
  <div class="dashboard-view">
    <!-- KPI 요약 카드 -->
    <section class="kpi-cards">
      <div class="kpi-card">
        <div class="kpi-label">생산 건수</div>
        <div class="kpi-value">{{ dashboardData.productionCount }}<span class="kpi-unit">건</span></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">활성 알람</div>
        <div class="kpi-value">{{ dashboardData.activeAlarms }}<span class="kpi-unit">건</span></div>
      </div>
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
        <div class="kpi-label">평균 배터리</div>
        <div class="kpi-value">{{ dashboardData.avgBatteryPercent }}<span class="kpi-unit">%</span></div>
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
        <div class="floor-map">
          <div class="map-label-chip">AMR 위치 현황도</div>
          <div class="floor-map__grid">
            <div v-for="amr in amrList.slice(0, 8)" :key="amr.id" class="amr-mark" :class="`status-${amr.status || 'driving'}`" :data-battery="`${amr.batteryPercent || 0}%`" :style="{ left: `${10 + (amrList.indexOf(amr) % 4) * 20}%`, top: `${20 + Math.floor(amrList.indexOf(amr) / 4) * 30}%` }">{{ amr.name }}</div>
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
        <div class="floor-map">
          <div class="map-label-chip">공정 구역 환경 현황</div>
          <div class="floor-map__grid env-grid">
            <div class="map-zone env-low" style="top:6%; left:5%; width:20%; height:30%;"><span class="zone-title">원자재 창고</span><span class="zone-meta">22.6°C · 습도 43%<br>미세먼지 12</span></div>
            <div class="map-zone env-low" style="top:6%; left:30%; width:25%; height:24%;"><span class="zone-title">A 라인</span><span class="zone-meta">24.8°C · 습도 56%<br>미세먼지 16</span></div>
            <div class="map-zone env-mid" style="top:36%; left:30%; width:25%; height:24%;"><span class="zone-title">B 라인</span><span class="zone-meta">21.9°C · 습도 39%<br>미세먼지 18</span></div>
            <div class="map-zone env-low" style="top:6%; left:60%; width:15%; height:58%;"><span class="zone-title">조립 라인</span><span class="zone-meta">23.1°C · 습도 41%<br>미세먼지 14</span></div>
            <div class="map-zone env-mid" style="top:6%; left:80%; width:15%; height:58%;"><span class="zone-title">검사 라인</span><span class="zone-meta">25.3°C · 습도 61%<br>미세먼지 19</span></div>
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
                  <span class="alert-chip" :class="alarm.level === 'CRITICAL' ? 'critical' : (alarm.level === 'WARNING' ? 'warn' : '')">{{ alarm.level }} · {{ formatTime(alarm.occurredAt) }}</span>
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

// 기본값 — API 응답에서 누락된 필드를 0으로 보호
const DASHBOARD_DEFAULTS = {
  productionCount: 0,
  activeAlarms: 0,
  amrOperating: 0,
  amrCharging: 0,
  amrWaiting: 0,
  avgBatteryPercent: 0,
  averageTaskTimeMin: 0
}

// 상태
const isLoading = ref(true)
const error = ref(null)
const dashboardData = ref({ ...DASHBOARD_DEFAULTS })
const amrList = ref([])
const recentAlarms = ref([])
const recentLogs = ref([])

// API 호출
// showLoading: 최초 로드 시에만 true — 폴링 갱신 시에는 스피너 없이 인플레이스 업데이트
const fetchDashboardData = async (showLoading = false) => {
  try {
    if (showLoading) isLoading.value = true
    error.value = null

    const [summaryRes, alarmsRes, logsRes, amrsRes] = await Promise.all([
      api.get('/dashboard/summary'),
      api.get('/dashboard/recent-alarms?limit=5'),
      api.get('/dashboard/recent-logs?page=1&limit=10'),
      api.get('/amrs?page=1&limit=20')
    ])

    dashboardData.value = { ...DASHBOARD_DEFAULTS, ...summaryRes.data }
    recentAlarms.value = alarmsRes.data.data || []
    recentLogs.value = logsRes.data.data || []
    amrList.value = amrsRes.data.data || []
  } catch (err) {
    error.value = err.response?.data?.message || '데이터 로드 실패'
    console.error('Dashboard error:', err)
  } finally {
    if (showLoading) isLoading.value = false
  }
}

// 시간 포맷
const formatTime = (isoString) => {
  if (!isoString) return '-'
  const date = new Date(isoString)
  return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

// 새로고침 타이머
let refreshTimer = null

onMounted(() => {
  fetchDashboardData(true)
  refreshTimer = setInterval(() => fetchDashboardData(false), 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 10px;
  align-items: stretch;
}

.kpi-card {
  background: white;
  border: 1px solid #e6eef8;
  border-left: 4px solid #3b82f6;
  border-radius: 6px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 60px;
}

.kpi-label { font-size: 0.68rem; font-weight: 700; color: #64748b; }
.kpi-value { font-size: 1.05rem; font-weight: 800; color: #0f172a; }
.kpi-unit { font-size: 0.72rem; margin-left: 6px; color: #94a3b8; }

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

.dashboard-grid { display: grid; grid-template-columns: 1.2fr 1fr; grid-template-rows: 1.28fr 0.52fr; gap: 10px; flex: 1; min-height: 0; }

.dashboard-panel { background: white; border: 1px solid #eef6ff; border-radius: 8px; padding: 10px; display: flex; flex-direction: column; min-height: 0; }

.floor-map-panel { /* not used now; kept for compatibility */ }

/* compact map panels for top row */
.compact-map-panel { padding: 8px; }
.floor-map__grid { position: relative; flex: 1; min-height: 0; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
.map-label-chip { position: absolute; right: 8px; top: 8px; padding: 4px 8px; border-radius: 999px; background: rgba(255,255,255,0.92); border: 1px solid #e2e8f0; color: #475569; font-size: 0.68rem; font-weight: 700; z-index: 6; }

.status-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-top: 8px; }
.strip-item { background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%); border: 1px solid #e5edf7; border-radius: 8px; padding: 6px 8px; }
.strip-item .k { font-size: 0.64rem; color: #64748b; }
.strip-item .v { margin-top: 2px; font-size: 0.78rem; font-weight: 800; }

/* bottom grid with alerts and logs */
.bottom-grid { grid-column: 1 / -1; display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 10px; min-height: 0; }
.list-box { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; gap: 6px; }
.list-scroll { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 6px; overflow: auto; padding-right: 4px; }
.alert-row, .log-row { background: #f8fafc; border: 1px solid #e5edf7; border-radius: 8px; padding: 8px; display: grid; grid-template-columns: auto 1fr auto; gap: 8px; align-items: center; }
.alert-severity { width: 10px; height: 10px; border-radius: 50%; box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.08); }
.alert-severity.critical { background: #ef4444; }
.alert-severity.warn { background: #f59e0b; }
.alert-text { display: flex; flex-direction: column; gap: 4px; }
.alert-title { font-size: 0.78rem; font-weight: 800; color: #111827; }
.alert-desc { font-size: 0.72rem; color: #64748b; line-height: 1.2; }
.alert-chip { font-size: 0.68rem; font-weight: 800; padding: 4px 8px; border-radius: 999px; border: 1px solid currentColor; background: white; white-space: nowrap; }
.alert-chip.critical { color: #ef4444; }
.alert-chip.warn { color: #f59e0b; }

.log-row { grid-template-columns: auto 1fr; }
.log-icon { width: 28px; height: 28px; border-radius: 6px; display: grid; place-items: center; background: rgba(59, 130, 246, 0.12); color: #2563eb; font-weight: 900; font-size: 0.68rem; }
.log-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 2px; }
.log-title { font-size: 0.78rem; font-weight: 800; color: #111827; }
.time-badge { font-size: 0.68rem; color: #64748b; white-space: nowrap; }
.log-desc { font-size: 0.72rem; color: #64748b; }

@media (max-width: 1400px) {
  .dashboard-grid { grid-template-columns: 1fr; grid-template-rows: repeat(4, minmax(0, 1fr)); }
  .bottom-grid { grid-column: auto; grid-template-columns: 1fr; }
}

.panel-title { font-size: 0.85rem; font-weight: 800; color: #0f172a; margin: 0 0 8px; }

.floor-map {
  position: relative;
  flex: 1;
  min-height: 320px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fafc 100%);
  border: 1px solid #e8f0fb;
  border-radius: 6px;
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
  .kpi-cards { grid-template-columns: repeat(3, minmax(120px, 1fr)); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .floor-map-panel { grid-row: auto; }
}

@media (max-width: 900px) {
  .kpi-cards { grid-template-columns: repeat(2, minmax(120px, 1fr)); }
}
</style>