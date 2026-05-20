<template>
  <div class="amr-detail-page">
    <div class="amr-detail-page__toolbar">
      <RouterLink to="/amr-list" class="back-link">← AMR 목록으로</RouterLink>
    </div>

    <div v-if="loadError" class="error-banner">{{ loadError }}</div>
    <div v-else-if="demoScenarioBanner" class="scenario-banner">{{ demoScenarioBanner }}</div>

    <header class="detail-header">
      <div>
        <strong class="amr-title">{{ displayName }}</strong>
        <span class="amr-id-chip">{{ resolvedAmrId }}</span>
        <small v-if="lastSeenLabel" class="amr-meta">{{ lastSeenLabel }}</small>
      </div>
      <div class="detail-header__actions">
        <span v-if="detailErrorBanner" class="error-msg">{{ detailErrorBanner }}</span>
        <button type="button" class="btn-emergency" @click="triggerEmergency">비상 정지</button>
      </div>
    </header>

    <section class="summary-row">
      <BaseStatCard label="상태" :value="statusLabel" :description="currentTaskLine" tone="blue" />
      <BaseStatCard label="배터리" :value="batteryLabel" description="실시간 잔량" tone="green" />
      <BaseStatCard label="총 주행 거리" :value="`${mileageDisplay} km`" description="누적 주행" tone="orange" />
      <BaseStatCard label="현재 구역" :value="positionZone" description="위치 구역" tone="purple" />
    </section>

    <section class="summary-row">
      <BaseStatCard label="작업 건수" :value="`${workHistoryTotal}건`" description="조회 기간 내 전체" tone="blue" />
      <BaseStatCard label="정상 완료" :value="`${successTaskCount}건`" description="성공 처리" tone="green" />
      <BaseStatCard label="주의/실패" :value="`${failedTaskCount}건`" description="비정상 종료" tone="orange" />
      <BaseStatCard label="최근 작업" :value="latestWorkTime" description="마지막 수행 시각" tone="purple" />
    </section>

    <SectionPanel class="chart-section" title="시간대별 작업 건수" eyebrow="통계">
      <BaseCard class="chart-card">
        <div ref="hourlyChartRef" class="chart-host"></div>
      </BaseCard>
    </SectionPanel>

    <SectionPanel class="table-section" title="작업 이력" eyebrow="이력">
      <div v-if="tableError" class="table-error">{{ tableError }}</div>
      <div class="table-scroll">
        <table class="simple-table">
          <thead>
            <tr>
              <th>작업 시간</th>
              <th>출발지</th>
              <th>도착지</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="workRows.length === 0">
              <td colspan="4" class="empty-cell">표시할 작업 이력이 없습니다.</td>
            </tr>
            <tr v-for="row in workRows" :key="row.id">
              <td>{{ row.time }}</td>
              <td>{{ row.from }}</td>
              <td>{{ row.to }}</td>
              <td>{{ row.status }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionPanel>
  </div>
</template>


<script setup>
import { ref, shallowRef, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import BaseCard from '../components/atoms/BaseCard.vue'
import BaseStatCard from '../components/atoms/BaseStatCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'
import { useAmrDetail } from '@/composables/useAmrDetail'
import api from '@/plugins/axios'
import { parseApiDateTime } from '@/utils/api-datetime'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'

const WORK_HISTORY_PAGE_LIMIT = 50

const route = useRoute()
const amrIdFromRoute = computed(() => String(route.query.amr || '').trim())

const {
  loadError,
  resolvedAmrId,
  demoScenarioBanner,
  displayName,
  statusLabel,
  detailErrorBanner,
  currentTaskLine,
  batteryLabel,
  mileageDisplay,
  positionZone,
  lastSeenLabel,
  loadAmrDetail,
  triggerEmergency
} = useAmrDetail(() => amrIdFromRoute.value)

const hourlyChartRef = ref(null)
const hourlyChartInstance = shallowRef(null)
const workHistoryRows = ref([])
const workHistoryTotal = ref(0)
const tableError = ref(null)

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
      from: row.from || '—',
      to: row.to || '—',
      status: mapResultLabel(row.result)
    }
  })
)

const successTaskCount = computed(
  () => workHistoryRows.value.filter((row) => String(row.result || '').toLowerCase() === 'success').length
)

const failedTaskCount = computed(
  () =>
    workHistoryRows.value.filter((row) => {
      const key = String(row.result || '').toLowerCase()
      return key === 'failed' || key === 'fail' || key === 'warning'
    }).length
)

const latestWorkTime = computed(() => {
  if (workRows.value.length === 0) return '—'
  return workRows.value[0].time
})

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
    const parsed = parseApiDateTime(row.startTime)
    if (!parsed?.isValid()) continue
    const key = parsed.startOf('minute').format('YYYY-MM-DDTHH:mm:ss')
    bucketMap.set(key, (bucketMap.get(key) ?? 0) + 1)
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

function formatWorkloadTimestamp(raw) {
  const parsed = parseApiDateTime(raw)
  if (!parsed?.isValid()) return ''
  return parsed.format('HH:mm')
}

function renderHourlyChart(workloadRows) {
  if (!hourlyChartRef.value) return
  if (!hourlyChartInstance.value) {
    hourlyChartInstance.value = echarts.init(hourlyChartRef.value)
  }
  const sorted = [...workloadRows].sort((a, b) => {
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

async function loadWorkHistoryAndChart() {
  const amrId = amrIdFromRoute.value
  if (!amrId) {
    workHistoryRows.value = []
    workHistoryTotal.value = 0
    tableError.value = null
    return
  }

  const minuteRange = buildMinuteWorkloadRange()

  try {
    const whRes = await api.get('/work-histories', {
      params: {
        amrId,
        page: 1,
        limit: WORK_HISTORY_PAGE_LIMIT,
        from: minuteRange.from,
        to: minuteRange.to
      }
    })
    workHistoryRows.value = whRes.data?.data ?? []
    workHistoryTotal.value = whRes.data?.total ?? workHistoryRows.value.length
    tableError.value = null
    const workloadRows = fillMinuteWorkloadGaps(workHistoryRows.value)
    await nextTick()
    renderHourlyChart(workloadRows)
  } catch (err) {
    workHistoryRows.value = []
    workHistoryTotal.value = 0
    tableError.value = err.response?.data?.message || '작업 이력을 불러오지 못했습니다.'
    console.error('AmrDetailView work-histories', err)
  }
}

async function refreshPageData() {
  await Promise.all([loadAmrDetail(), loadWorkHistoryAndChart()])
}

function handleWindowResize() {
  hourlyChartInstance.value?.resize()
}

function handleDemoScenarioApplied() {
  refreshPageData()
}

let refreshTimer = null

onMounted(async () => {
  await refreshPageData()
  window.addEventListener('resize', handleWindowResize)
  window.addEventListener('demo-scenario-applied', handleDemoScenarioApplied)
  refreshTimer = setInterval(refreshPageData, DEMO_REST_POLLING_INTERVAL_MS)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
  window.removeEventListener('demo-scenario-applied', handleDemoScenarioApplied)
  if (refreshTimer) clearInterval(refreshTimer)
  if (hourlyChartInstance.value) {
    hourlyChartInstance.value.dispose()
    hourlyChartInstance.value = null
  }
})

watch(amrIdFromRoute, () => {
  refreshPageData()
})
</script>

<style scoped>
.amr-detail-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  font-size: 0.92rem;
}

.amr-detail-page__toolbar {
  flex: 0 0 auto;
}

.back-link {
  font-size: 0.76rem;
  font-weight: 700;
  color: #2563eb;
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}

.scenario-banner {
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  font-size: 0.76rem;
  font-weight: 700;
  flex: 0 0 auto;
}

.error-banner {
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff4f4;
  border: 1px solid #fbcaca;
  color: #b91c1c;
  font-size: 0.76rem;
  font-weight: 700;
  flex: 0 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 0 0 auto;
}

.detail-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.amr-title {
  font-size: 1.02rem;
}

.amr-id-chip {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  font-size: 0.68rem;
  font-weight: 700;
  color: #475569;
}

.amr-meta {
  display: block;
  margin-top: 6px;
  color: #94a3b8;
  font-size: 0.7rem;
}

.error-msg {
  color: #ef4444;
  font-weight: 800;
  font-size: 0.82rem;
}

.btn-emergency {
  padding: 6px 12px;
  font-size: 0.78rem;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-weight: 800;
  cursor: pointer;
}

.btn-emergency:hover {
  background: #dc2626;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  flex: 0 0 auto;
}

.amr-detail-page :deep(.stat-card) {
  padding: 10px 12px;
}

.amr-detail-page :deep(.stat-label) {
  margin-bottom: 6px;
  font-size: 0.74rem;
}

.amr-detail-page :deep(.stat-value) {
  font-size: 1.35rem;
}

.amr-detail-page :deep(.stat-description) {
  margin-top: 4px;
  font-size: 0.7rem;
}

.amr-detail-page :deep(.section-panel) {
  padding: 12px 14px;
}

.amr-detail-page :deep(.section-panel__header) {
  margin-bottom: 10px;
}

.amr-detail-page :deep(.section-panel__eyebrow) {
  margin-bottom: 4px;
  font-size: 0.68rem;
}

.amr-detail-page :deep(.section-panel__title) {
  font-size: 0.9rem;
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

.chart-card {
  padding: 8px;
}

.chart-host {
  width: 100%;
  height: 150px;
}

.table-scroll {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.table-error {
  margin-bottom: 6px;
  color: #b91c1c;
  font-size: 0.74rem;
  font-weight: 700;
  flex: 0 0 auto;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.76rem;
}

.simple-table th,
.simple-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.simple-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  font-weight: 800;
  color: #475569;
  font-size: 0.72rem;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
  padding: 14px;
}

@media (max-width: 960px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
