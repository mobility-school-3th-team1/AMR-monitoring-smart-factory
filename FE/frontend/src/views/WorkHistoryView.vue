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
      class="chart-section"
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
      class="table-section"
      eyebrow="작업 내역"
      title="세부 기록 테이블"
      subtitle="페이지 단위 조회 (기본 20건)"
    >
      <div v-if="tableError" class="table-error">{{ tableError }}</div>
      <div class="table-scroll">
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
      </div>
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
import { parseApiDateTime } from '@/utils/api-datetime'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'
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

function formatWorkloadTimestamp(raw) {
  const parsed = parseApiDateTime(raw)
  if (!parsed || !parsed.isValid()) return ''
  return parsed.format('HH:mm')
}

function buildMinuteWorkloadRange() {
  const rangeEnd = dayjs()
  const rangeStart = rangeEnd.subtract(59, 'minute').startOf('minute')
  return {
    from: rangeStart.format('YYYY-MM-DDTHH:mm:ss'),
    to: rangeEnd.format('YYYY-MM-DDTHH:mm:ss')
  }
}

function fillMinuteWorkloadGaps(rows) {
  const rangeEnd = dayjs().startOf('minute')
  const bucketMap = new Map()

  for (const row of rows) {
    const parsed = parseApiDateTime(row.timestamp)
    if (!parsed?.isValid()) continue
    const key = parsed.startOf('minute').format('YYYY-MM-DDTHH:mm:ss')
    bucketMap.set(key, row.taskCount ?? 0)
  }

  const filled = []
  for (let offset = 59; offset >= 0; offset -= 1) {
    const bucketTime = rangeEnd.subtract(offset, 'minute')
    const key = bucketTime.format('YYYY-MM-DDTHH:mm:ss')
    filled.push({
      timestamp: key,
      taskCount: bucketMap.get(key) ?? 0
    })
  }
  return filled
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
    grid: { left: 36, right: 12, top: 16, bottom: 28 },
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
    const minuteRange = buildMinuteWorkloadRange()
    const [hourRes, amrRes] = await Promise.all([
      api.get('/analytics/workload', {
        params: { groupBy: 'minute', from: minuteRange.from, to: minuteRange.to }
      }),
      api.get('/analytics/workload', { params: { groupBy: 'amr' } })
    ])
    const minuteRows = Array.isArray(hourRes.data) ? hourRes.data : []
    workloadByHour.value = fillMinuteWorkloadGaps(minuteRows)
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
  refreshTimer = setInterval(loadWorkloadAndHistory, DEMO_REST_POLLING_INTERVAL_MS)
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
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  font-size: 0.92rem;
}

.view-stack__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  flex: 0 0 auto;
}

.view-stack :deep(.stat-card) {
  padding: 10px 12px;
}

.view-stack :deep(.stat-label) {
  margin-bottom: 6px;
  font-size: 0.74rem;
}

.view-stack :deep(.stat-value) {
  font-size: 1.35rem;
}

.view-stack :deep(.stat-description) {
  margin-top: 4px;
  font-size: 0.7rem;
}

.view-stack :deep(.section-panel) {
  padding: 12px 14px;
}

.view-stack :deep(.section-panel__header) {
  margin-bottom: 10px;
}

.view-stack :deep(.section-panel__eyebrow) {
  margin-bottom: 4px;
  font-size: 0.68rem;
}

.view-stack :deep(.section-panel__title) {
  font-size: 0.9rem;
}

.view-stack :deep(.section-panel__subtitle) {
  font-size: 0.74rem;
}

.chart-section {
  flex: 0 0 auto;
  min-height: 0;
}

.table-section {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-section :deep(.section-panel) {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-section :deep(.section-panel__body) {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.analysis-card {
  padding: 8px;
}

.analysis-card__label {
  margin: 0 0 6px;
  font-size: 0.78rem;
  font-weight: 800;
}

.chart-host {
  height: 150px;
  width: 100%;
}

.table-error {
  margin-bottom: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  background: #fff4f4;
  border: 1px solid #fbcaca;
  color: #b91c1c;
  font-size: 0.72rem;
  font-weight: 700;
  flex: 0 0 auto;
}

.table-scroll {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  overflow: auto;
  border: 1px solid rgba(217, 228, 240, 0.95);
  border-radius: 6px;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
}

.simple-table th,
.simple-table td {
  padding: 6px 8px;
  border-bottom: 1px solid rgba(217, 228, 240, 0.95);
  text-align: left;
  font-size: 0.76rem;
}

.simple-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
  padding: 14px;
}

@media (max-width: 1440px) {
  .view-stack__stats,
  .analysis-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
