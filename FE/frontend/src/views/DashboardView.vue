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
      <button class="error-retry" @click="fetchDashboardData">다시 시도</button>
    </div>

    <!-- 메인 대시보드 그리드 -->
    <div v-if="!isLoading" class="dashboard-grid">
      <!-- 공장 평면도 -->
      <section class="dashboard-panel floor-map-panel">
        <div class="floor-map">
          <div style="position: absolute; top: 8px; right: 10px; font-size: 0.7rem; font-weight: 700; color: #64748b; background: white; padding: 2px 6px; border-radius: 4px; border: 1px solid #e2e8f0; z-index: 10;">AMR 위치 현황도</div>
          <div v-for="amr in amrList.slice(0, 8)" :key="amr.id" class="floor-map__robot" :style="{ left: `${10 + (amrList.indexOf(amr) % 4) * 20}%`, top: `${20 + Math.floor(amrList.indexOf(amr) / 4) * 30}%` }">
            {{ amr.name }}<small>{{ amr.batteryPercent }}%</small>
          </div>
        </div>
      </section>

      <!-- 실시간 알람 -->
      <section class="dashboard-panel">
        <h3 class="panel-title">실시간 중요 알람</h3>
        <div v-if="recentAlarms.length === 0" class="empty-state"><p>활성 알람이 없습니다.</p></div>
        <ul v-else class="alert-list">
          <li v-for="alarm in recentAlarms" :key="alarm.id">
            <span class="alert-level" :class="`alert-${alarm.level}`">{{ alarm.level }}</span>
            <div><p>{{ alarm.message }}</p><span class="alert-time">{{ formatTime(alarm.occurredAt) }}</span></div>
          </li>
        </ul>
      </section>

      <!-- 실시간 작업 로그 -->
      <section class="dashboard-panel" style="grid-column: 1 / -1;">
        <h3 class="panel-title">실시간 작업 로그</h3>
        <div v-if="recentLogs.length === 0" class="empty-state"><p>최근 작업 로그가 없습니다.</p></div>
        <table v-else class="simple-table">
          <thead><tr><th>시간</th><th>장비</th><th>이벤트</th><th>상태</th></tr></thead>
          <tbody><tr v-for="(log, i) in recentLogs" :key="i"><td>{{ formatTime(log.timestamp) }}</td><td>{{ log.amrId }}</td><td>{{ log.event }}</td><td>{{ log.status }}</td></tr></tbody>
        </table>
      </section>
    </div>

    <!-- 로딩 상태 -->
    <div v-else class="loading-state"><div class="spinner"></div><p>데이터 로드 중...</p></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/plugins/axios'

// 상태
const isLoading = ref(true)
const error = ref(null)
const dashboardData = ref({
  productionCount: 0,
  activeAlarms: 0,
  amrOperating: 0,
  amrCharging: 0,
  amrWaiting: 0,
  avgBatteryPercent: 0,
  averageTaskTimeMin: 0
})
const amrList = ref([])
const recentAlarms = ref([])
const recentLogs = ref([])

// API 호출
const fetchDashboardData = async () => {
  try {
    isLoading.value = true
    error.value = null

    const [summaryRes, alarmsRes, logsRes, amrsRes] = await Promise.all([
      api.get('/dashboard/summary'),
      api.get('/dashboard/recent-alarms?limit=5'),
      api.get('/dashboard/recent-logs?page=1&limit=10'),
      api.get('/amrs?page=1&limit=20')
    ])

    dashboardData.value = summaryRes.data
    recentAlarms.value = alarmsRes.data.data || []
    recentLogs.value = logsRes.data.data || []
    amrList.value = amrsRes.data.data || []
  } catch (err) {
    error.value = err.response?.data?.message || '데이터 로드 실패'
    console.error('Dashboard error:', err)
  } finally {
    isLoading.value = false
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
  fetchDashboardData()
  refreshTimer = setInterval(fetchDashboardData, 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  flex-shrink: 0;
}

.kpi-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-left: 3px solid #3b82f6;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
}

.kpi-value {
  font-size: 1.4rem;
  font-weight: 800;
  color: #1f2937;
}

.kpi-unit {
  font-size: 0.85rem;
  margin-left: 4px;
  color: #94a3b8;
  font-weight: 600;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.85rem;
}

.error-retry {
  margin-left: auto;
  padding: 4px 12px;
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.dashboard-panel {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.floor-map-panel {
  grid-row: 1 / 3;
}

.panel-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 12px;
  padding: 0;
}

.floor-map {
  position: relative;
  flex: 1;
  min-height: 400px;
  background: linear-gradient(180deg, #fbfdff 0%, #f7fafc 100%);
  border: 1px solid #e5edf7;
  border-radius: 8px;
}

.floor-map__robot {
  position: absolute;
  width: 50px;
  padding: 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
  font-size: 0.6rem;
  font-weight: 700;
  text-align: center;
  transform: translate(-50%, -50%);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  z-index: 5;
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
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  border-left: 3px solid #3b82f6;
  background: #eff6ff;
  font-size: 0.8rem;
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

.simple-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
}

.simple-table th {
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
}

.simple-table td {
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
  color: #1f2937;
}

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
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .floor-map-panel {
    grid-row: auto;
  }
}
</style>