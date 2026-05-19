<template>
  <div class="view-stack">
    <section class="view-stack__stats">
      <BaseStatCard
        v-for="card in statCards"
        :key="card.label"
        v-bind="card"
      />
    </section>

    <SectionPanel
      eyebrow="작업 이력 및 분석"
      title="분석 요약"
      subtitle="GET /analytics/workload 기반 차트, 최근 작업 이력은 GET /work-histories"
    >
      <div class="analysis-grid">
        <BaseCard class="analysis-card">
          <p class="analysis-card__label">시간대별 작업 건수</p>
          <div ref="hourlyChartRef" class="chart-host"></div>
        </BaseCard>

        <BaseCard class="analysis-card">
          <p class="analysis-card__label">AMR별 작업 비중</p>
          <div ref="amrShareChartRef" class="chart-host"></div>
        </BaseCard>
      </div>
    </SectionPanel>

    <SectionPanel
      eyebrow="작업 내역"
      title="세부 기록 테이블"
      subtitle="페이지 단위 조회 (기본 20건)"
    >
      <div v-if="tableError" class="table-error">{{ tableError }}</div>
      <table class="simple-table">
        <thead>
          <tr>
            <th>작업 시간</th>
            <th>AMR</th>
            <th>출발지</th>
            <th>도착지</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <template v-if="workRows.length === 0">
            <tr>
              <td colspan="5" class="empty-cell">표시할 작업 이력이 없습니다.</td>
            </tr>
          </template>
          <template v-else>
            <tr v-for="row in workRows" :key="row.id">
              <td>{{ row.time }}</td>
              <td>{{ row.robot }}</td>
              <td>{{ row.from }}</td>
              <td>{{ row.to }}</td>
              <td>{{ row.status }}</td>
            </tr>
          </template>
        </tbody>
      </table>
    </SectionPanel>
  </div>
</template>

<script setup>
import BaseCard from '../components/atoms/BaseCard.vue'
import BaseStatCard from '../components/atoms/BaseStatCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'
import { ref, shallowRef, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import api from '@/plugins/axios'

const POLLING_INTERVAL_MS = 10000
const WORK_HISTORY_PAGE_LIMIT = 20

const hourlyChartRef = ref(null)
const amrShareChartRef = ref(null)
const hourlyChartInstance = shallowRef(null)
const amrShareChartInstance = shallowRef(null)

const workloadByHour = ref([])
const workloadByAmr = ref([])
const workHistoryTotal = ref(0)
const workHistoryRows = ref([])
const tableError = ref(null)

const statCards = computed(() => [
  {
    label: '일별 작업 건수',
    value: `${workHistoryTotal.value}건`,
    description: 'work-histories 응답 total',
    tone: 'blue'
  },
  {
    label: '이동 거리',
    value: '—',
    description: 'API 미제공',
    tone: 'green'
  }
])

function parseApiDateTime(raw) {
  if (!raw) return null
  if (typeof raw === 'string') return dayjs(raw)
  if (Array.isArray(raw) && raw.length >= 3) {
    const year = raw[0]
    const month = raw[1]
    const day = raw[2]
    const hour = raw.length > 3 ? raw[3] : 0
    const minute = raw.length > 4 ? raw[4] : 0
    const second = raw.length > 5 ? raw[5] : 0
    return dayjs(new Date(year, month - 1, day, hour, minute, second))
  }
  return dayjs(raw)
}

function formatWorkloadTimestamp(raw) {
  const parsed = parseApiDateTime(raw)
  if (!parsed || !parsed.isValid()) return ''
  return parsed.format('HH:mm')
}

function mapResultLabel(result) {
  const key = String(result || '').toLowerCase()
  if (key === 'success') return '정상'
  if (key === 'warning') return '주의'
  if (key === 'failed' || key === 'fail') return '실패'
  return result || '—'
}

const workRows = computed(() =>
  workHistoryRows.value.map((row) => {
    const start = parseApiDateTime(row.startTime)
    return {
      id: row.id,
      time: start?.isValid() ? start.format('YYYY-MM-DD HH:mm') : '—',
      robot: row.amrId || '—',
      from: row.from || '—',
      to: row.to || '—',
      status: mapResultLabel(row.result)
    }
  })
)

function disposeCharts() {
  if (hourlyChartInstance.value) {
    hourlyChartInstance.value.dispose()
    hourlyChartInstance.value = null
  }
  if (amrShareChartInstance.value) {
    amrShareChartInstance.value.dispose()
    amrShareChartInstance.value = null
  }
}

function renderHourlyChart() {
  if (!hourlyChartRef.value) return
  if (!hourlyChartInstance.value) {
    hourlyChartInstance.value = echarts.init(hourlyChartRef.value)
  }
  const sorted = [...workloadByHour.value].sort((a, b) => {
    const ta = parseApiDateTime(a.timestamp)?.valueOf() ?? 0
    const tb = parseApiDateTime(b.timestamp)?.valueOf() ?? 0
    return ta - tb
  })
  const categories = sorted.map((row) => formatWorkloadTimestamp(row.timestamp))
  const values = sorted.map((row) => row.taskCount ?? 0)
  hourlyChartInstance.value.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 32 },
    xAxis: { type: 'category', data: categories, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#2563eb' } }]
  })
}

function renderAmrShareChart() {
  if (!amrShareChartRef.value) return
  if (!amrShareChartInstance.value) {
    amrShareChartInstance.value = echarts.init(amrShareChartRef.value)
  }
  const pieData = workloadByAmr.value
    .filter((row) => row.amrId)
    .map((row) => ({ name: row.amrId, value: row.taskCount ?? 0 }))
  const resolvedPieData =
    pieData.length > 0 ? pieData : [{ name: '데이터 없음', value: 1, itemStyle: { color: '#e2e8f0' } }]
  amrShareChartInstance.value.setOption({
    tooltip: { trigger: 'item' },
    legend: { type: 'scroll', bottom: 0, show: pieData.length > 0 },
    series: [
      {
        type: 'pie',
        radius: ['36%', '62%'],
        data: resolvedPieData,
        label: { formatter: '{b}: {c}' }
      }
    ]
  })
}

async function loadWorkloadAndHistory() {
  try {
    const [hourRes, amrRes] = await Promise.all([
      api.get('/analytics/workload', { params: { groupBy: 'hour' } }),
      api.get('/analytics/workload', { params: { groupBy: 'amr' } })
    ])
    workloadByHour.value = Array.isArray(hourRes.data) ? hourRes.data : []
    workloadByAmr.value = Array.isArray(amrRes.data) ? amrRes.data : []
  } catch (err) {
    console.error('WorkHistoryView workload', err)
    workloadByHour.value = []
    workloadByAmr.value = []
  }

  try {
    const whRes = await api.get('/work-histories', { params: { page: 1, limit: WORK_HISTORY_PAGE_LIMIT } })
    workHistoryRows.value = whRes.data?.data ?? []
    workHistoryTotal.value = whRes.data?.total ?? workHistoryRows.value.length
    tableError.value = null
  } catch (err) {
    workHistoryRows.value = []
    workHistoryTotal.value = 0
    tableError.value = err.response?.data?.message || '작업 이력을 불러오지 못했습니다.'
    console.error('WorkHistoryView work-histories', err)
  }

  await nextTick()
  renderHourlyChart()
  renderAmrShareChart()
}

function handleWindowResize() {
  hourlyChartInstance.value?.resize()
  amrShareChartInstance.value?.resize()
}

let refreshTimer = null

onMounted(async () => {
  await loadWorkloadAndHistory()
  window.addEventListener('resize', handleWindowResize)
  refreshTimer = setInterval(loadWorkloadAndHistory, POLLING_INTERVAL_MS)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
  if (refreshTimer) clearInterval(refreshTimer)
  disposeCharts()
})
</script>

<style scoped>
.view-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.view-stack__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  flex: 0 0 auto;
}

.analysis-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.analysis-card { padding: 12px; }

.analysis-card__label {
  margin: 0 0 10px;
  font-size: 0.9rem;
  font-weight: 800;
}

.chart-host {
  min-height: 220px;
  width: 100%;
}

.table-error {
  margin-bottom: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff4f4;
  border: 1px solid #fbcaca;
  color: #b91c1c;
  font-size: 0.78rem;
  font-weight: 700;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
}

.simple-table th,
.simple-table td {
  padding: 10px 10px;
  border-bottom: 1px solid rgba(217, 228, 240, 0.95);
  text-align: left;
  font-size: 0.82rem;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
}

.view-stack > .section-panel { min-height: 0; }
.view-stack > .section-panel:last-of-type { flex: 1 1 auto; min-height: 0; }
.view-stack > .section-panel:last-of-type .section-panel__body { overflow: auto; }

.simple-table th {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

@media (max-width: 1440px) {
  .view-stack__stats,
  .analysis-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
