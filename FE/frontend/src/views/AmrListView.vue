<template>
  <div class="amr-manage-view">
    <aside class="amr-panel">
      <div class="amr-header">AMR 선택 목록</div>
      <div class="amr-filter-row">
        <label class="amr-filter-label" for="amr-status-filter">상태 필터</label>
        <select id="amr-status-filter" v-model="statusFilter" class="amr-filter-select" @change="loadAmrs">
          <option value="">전체</option>
          <option value="OPERATING">운행 중</option>
          <option value="IDLE">대기</option>
          <option value="CHARGING">충전 중</option>
          <option value="ERROR">오류</option>
          <option value="EMERGENCY_STOP">비상 정지</option>
          <option value="STOPPED">정지</option>
        </select>
      </div>
      <div class="amr-filter-row">
        <label class="amr-filter-label" for="amr-status-sort">상태 정렬 순서</label>
        <input
          id="amr-status-sort"
          v-model="statusSortOrder"
          class="amr-sort-input"
          type="text"
          placeholder="EMERGENCY_STOP,ERROR,STOPPED,..."
          @change="persistStatusSortOrder"
        />
        <button type="button" class="amr-sort-apply-btn" @click="applyStatusSort">적용</button>
      </div>
      <div class="amr-scroll">
        <div v-if="loadError" class="error-note">{{ loadError }}</div>
        <div v-else-if="isLoading" class="loading-note">로딩 중...</div>
        <div v-else class="amr-selection">
          <a v-for="robot in robots" :key="robot.rawId" href="#" class="amr-link" @click.prevent="openDetail(robot.rawId)">
            <div class="amr-select-card" :class="robot.class">
              <div class="amr-select-top">
                <div>
                  <div class="amr-select-id">{{ robot.id }}</div>
                  <div class="amr-select-meta">{{ robot.meta }}</div>
                </div>
                <span class="status-tag" :class="robot.tagClass">{{ robot.tag }}</span>
              </div>
              <div class="amr-select-foot">
                <div class="amr-select-loc">현재 위치: {{ robot.location }} → 목적지: {{ robot.destination }}</div>
                <div class="amr-select-btn">상세 보기</div>
              </div>
            </div>
          </a>
        </div>
        <div class="mini-note">AMR을 선택하면 상세 정보 모달이 열립니다.</div>
      </div>
    </aside>

    <Teleport to="body">
      <div
        v-if="detailModalOpen"
        class="amr-detail-modal-backdrop"
        role="presentation"
        @click.self="closeDetailModal"
      >
        <div
          class="amr-detail-modal-panel"
          role="dialog"
          aria-modal="true"
          aria-label="AMR 상세"
        >
          <AmrDetailModalPanel
            v-if="selectedAmrId"
            :amr-id="selectedAmrId"
            @close="closeDetailModal"
            @open-detail-page="openDetailPage"
            @updated="loadAmrs"
          />
        </div>
      </div>
    </Teleport>

    <section class="dashboard-panel">
      <div class="summary-row">
        <div class="sum-card" style="border-top: 3px solid #3b82f6;"><h4>운영 중 로봇</h4><div>{{ stats.operating }}/{{ stats.total }}</div></div>
        <div class="sum-card" style="border-top: 3px solid #10b981;"><h4>운행</h4><div>{{ stats.running }}/{{ stats.operating }}</div></div>
        <div class="sum-card" style="border-top: 3px solid #f59e0b;"><h4>대기</h4><div>{{ stats.waiting }}/{{ stats.operating }}</div></div>
        <div class="sum-card" style="border-top: 3px solid #ef4444;"><h4 style="color:#ef4444">오류</h4><div style="color:#ef4444">{{ stats.error }}/{{ stats.operating }}</div></div>
      </div>

      <div class="bottom-grid">
        <article class="widget">
          <div class="widget-title">AMR 현재 위치 및 상태 목록 <span style="font-size:0.65rem; font-weight:600; color:#64748b;">(운행 중: {{ stats.running }}대, 충전 중: {{ stats.charging }}대, 대기 중: {{ stats.waiting }}대 / 전체: {{ stats.total }}대)</span></div>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr><th>AMR ID</th><th>상태</th><th>배터리</th><th>현재 위치</th><th>목적지</th><th>현재 작업</th><th>이동 속도</th></tr>
              </thead>
              <tbody>
                <tr
                  v-for="r in robots"
                  :key="r.rawId"
                  class="amr-table-row"
                  @click="openDetail(r.rawId)"
                >
                  <td>{{ r.id }}</td>
                  <td><span class="badge" :class="r.tagClass">{{ r.tagDisplay }}</span></td>
                  <td>{{ r.battery }}%</td>
                  <td>{{ r.location }}</td>
                  <td>{{ r.destination }}</td>
                  <td>{{ r.task }}</td>
                  <td>{{ r.speed }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import api from '@/plugins/axios'
import { DEMO_REST_POLLING_INTERVAL_MS } from '@/config/demo-intervals'
import { useRouter } from 'vue-router'
import AmrDetailModalPanel from '@/components/amr/AmrDetailModalPanel.vue'

const router = useRouter()

const detailModalOpen = ref(false)
const selectedAmrId = ref('')

const AMR_LIST_LIMIT = 50

// 상태
const isLoading = ref(true)
const loadError = ref(null)
const robots = ref([])
const statusFilter = ref('')
const STATUS_SORT_STORAGE_KEY = 'amrStatusSortOrder'
const DEFAULT_STATUS_SORT_ORDER = 'EMERGENCY_STOP,ERROR,STOPPED,OPERATING,CHARGING,IDLE'
const statusSortOrder = ref(localStorage.getItem(STATUS_SORT_STORAGE_KEY) || DEFAULT_STATUS_SORT_ORDER)

// API 응답을 UI 모델로 변환
const STATUS_MAP = {
  running:  { tag: '운행', tagDisplay: '운행 중',  tagClass: 'status-running',  cardClass: 'running'  },
  charging: { tag: '충전', tagDisplay: '충전 중',  tagClass: 'status-charging', cardClass: 'charging' },
  waiting:  { tag: '대기', tagDisplay: '대기',     tagClass: 'status-waiting',  cardClass: 'waiting'  },
  error:    { tag: '오류', tagDisplay: '오류',     tagClass: 'status-error',    cardClass: 'error'    }
}

// Normalize backend status enums to UI keys
function normalizeStatus(rawStatus) {
  if (!rawStatus) return 'waiting'
  const s = String(rawStatus).trim().toUpperCase()
  // Backend may use OPERATING, IDLE, CHARGING, ERROR, etc.
  if (s === 'OPERATING' || s === 'RUNNING' || s === 'DRIVING' || s === 'EN_ROUTE_CHARGING') return 'running'
  if (s === 'CHARGING') return 'charging'
  if (s === 'IDLE' || s === 'PAUSED' || s === 'STANDBY') return 'waiting'
  if (s === 'STOPPED') return 'error'
  if (s === 'ERROR' || s === 'FAULT' || s === 'EMERGENCY_STOP') return 'error'
  // fallback
  return 'waiting'
}

function mapAmr(raw) {
  const uiKey = normalizeStatus(raw.status)
  const s = STATUS_MAP[uiKey] || STATUS_MAP.waiting
  return {
    id:          raw.name || raw.id,
    rawId:       raw.id,
    rawStatus:   String(raw.status || '').toUpperCase(),
    class:       s.cardClass,
    tag:         s.tag,
    tagDisplay:  raw.currentTask && String(raw.currentTask).includes('충전 스테이션')
      ? raw.currentTask
      : s.tagDisplay,
    tagClass:    s.tagClass,
    battery:     raw.batteryPercent ?? 0,
    meta:        `${s.tagDisplay}, 배터리 ${raw.batteryPercent ?? 0}%`,
    location:    raw.position?.zone || '-',
    destination: raw.destination?.zone || '-',
    task:        raw.currentTask || '-',
    speed:       raw.speed != null ? `${raw.speed} m/s` : '0 m/s',
  }
}

// KPI 집계 — robots 목록에서 실시간으로 계산
const stats = computed(() => {
  const list = robots.value
  const running  = list.filter(r => r.class === 'running').length
  const charging = list.filter(r => r.class === 'charging').length
  const waiting  = list.filter(r => r.class === 'waiting').length
  const errorAmt = list.filter(r => r.class === 'error').length
  const total    = list.length
  const operating = running + charging + waiting + errorAmt
  return { total, operating, running, charging, waiting, error: errorAmt, unhealthy: errorAmt, lastCheck: list.find(r => r.class === 'error')?.id || '-' }
})

function parseStatusSortOrder() {
  return statusSortOrder.value
    .split(',')
    .map((token) => token.trim().toUpperCase())
    .filter(Boolean)
}

function sortRobotsByStatusOrder(list) {
  const order = parseStatusSortOrder()
  const rankMap = new Map(order.map((status, index) => [status, index]))
  const fallbackRank = order.length + 100
  return [...list].sort((left, right) => {
    const leftRank = rankMap.get(String(left.rawStatus || '').toUpperCase()) ?? fallbackRank
    const rightRank = rankMap.get(String(right.rawStatus || '').toUpperCase()) ?? fallbackRank
    if (leftRank !== rightRank) {
      return leftRank - rightRank
    }
    return String(left.rawId).localeCompare(String(right.rawId))
  })
}

function persistStatusSortOrder() {
  localStorage.setItem(STATUS_SORT_STORAGE_KEY, statusSortOrder.value.trim() || DEFAULT_STATUS_SORT_ORDER)
}

function applyStatusSort() {
  persistStatusSortOrder()
  robots.value = sortRobotsByStatusOrder(robots.value)
}

// API 호출
async function loadAmrs() {
  try {
    const query = new URLSearchParams({ page: '1', limit: String(AMR_LIST_LIMIT) })
    if (statusFilter.value) {
      query.set('status', statusFilter.value)
    }
    const res = await api.get(`/amrs?${query.toString()}`)
    const mapped = (res.data.data || []).map(mapAmr)
    robots.value = sortRobotsByStatusOrder(mapped)
    loadError.value = null
  } catch (err) {
    loadError.value = err.response?.data?.message || 'AMR 목록 로드 실패'
    console.error('AmrListView error:', err)
  } finally {
    isLoading.value = false
  }
}

let refreshTimer = null

function handleDemoScenarioApplied() {
  loadAmrs()
}

onMounted(() => {
  loadAmrs()
  refreshTimer = setInterval(loadAmrs, DEMO_REST_POLLING_INTERVAL_MS)
  window.addEventListener('demo-scenario-applied', handleDemoScenarioApplied)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('demo-scenario-applied', handleDemoScenarioApplied)
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleEscapeKey)
})

function openDetail(amrId) {
  if (!amrId) return
  selectedAmrId.value = amrId
  detailModalOpen.value = true
}

function closeDetailModal() {
  detailModalOpen.value = false
  selectedAmrId.value = ''
  loadAmrs()
}

function openDetailPage() {
  const amrId = selectedAmrId.value
  if (!amrId) return
  closeDetailModal()
  router.push({ path: '/amr-detail', query: { amr: amrId } })
}

function handleEscapeKey(event) {
  if (event.key === 'Escape' && detailModalOpen.value) {
    closeDetailModal()
  }
}

watch(detailModalOpen, (isOpen) => {
  if (isOpen) {
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', handleEscapeKey)
  } else {
    document.body.style.overflow = ''
    window.removeEventListener('keydown', handleEscapeKey)
  }
})
</script>

<style scoped>
.amr-manage-view { display: flex; gap: 12px; height: 100%; min-height: 0; }
.amr-panel { width: 330px; background: #e6eef8; border-radius: 10px; display:flex; flex-direction:column; flex-shrink:0; }
.amr-header { padding: 12px; text-align:center; font-weight:800; font-size:0.92rem; background:#dbeaf7; border-radius:10px 10px 0 0; }
.amr-filter-row { padding: 8px 10px 0; display:flex; flex-direction:column; gap:4px; }
.amr-filter-label { font-size:0.62rem; color:#64748b; font-weight:700; }
.amr-filter-select { width:100%; font-size:0.68rem; padding:6px 8px; border:1px solid #cbd5e1; border-radius:6px; background:#fff; }
.amr-sort-input { width:100%; font-size:0.62rem; padding:6px 8px; border:1px solid #cbd5e1; border-radius:6px; background:#fff; }
.amr-sort-apply-btn { margin-top:4px; width:100%; padding:6px 8px; border:none; border-radius:6px; background:#64748b; color:#fff; font-size:0.65rem; font-weight:700; cursor:pointer; }
.amr-scroll { flex:1; overflow-y:auto; padding:10px; display:flex; flex-direction:column; gap:8px; }
.amr-selection { display:flex; flex-direction:column; gap:8px; }
.amr-link { text-decoration:none; color:inherit; display:block; }
.amr-select-card { background:white; border-radius:8px; padding:10px; border-left:6px solid #cbd5e1; box-shadow:0 2px 4px rgba(0,0,0,0.03); transition:transform .12s ease, border-color .12s ease; }
.amr-select-card:hover { transform: translateY(-2px); }
.amr-select-card.running { border-left-color: #10b981; }
.amr-select-card.charging { border-left-color: #3b82f6; }
.amr-select-card.waiting { border-left-color: #f59e0b; }
.amr-select-card.error { border-left-color: #ef4444; }
.amr-select-top { display:flex; justify-content:space-between; gap:10px; align-items:flex-start; }
.amr-select-id { font-weight:800; color:#0f172a; font-size:0.86rem; }
.amr-select-meta { font-size:0.62rem; color:#64748b; margin-top:4px; line-height:1.3; }
.amr-select-foot { display:flex; justify-content:space-between; align-items:center; gap:8px; margin-top:8px; font-size:0.64rem; color:#475569; }
.amr-select-loc { font-size:0.6rem; color:#64748b; }
.mini-note { font-size:0.58rem; color:#64748b; margin-top:6px; }

.amr-detail-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: rgba(15, 23, 42, 0.45);
}

.amr-detail-modal-panel {
  width: min(720px, 100%);
  max-height: min(88vh, 900px);
  overflow-y: auto;
  padding: 16px 18px 20px;
  border-radius: 12px;
  background: #f8fafc;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.25);
}

.amr-detail-modal-title {
  margin: 0 0 12px;
  font-size: 1rem;
  font-weight: 800;
  color: #0f172a;
}

.amr-table-row { cursor: pointer; }
.amr-table-row:hover td { background: #f8fafc; }

.dashboard-panel { flex:1; display:flex; flex-direction:column; gap:8px; min-width:0; }
.summary-row { display:grid; grid-template-columns: repeat(4,1fr); gap:10px; flex-shrink:0; }
.sum-card { background:white; padding:10px 12px; border-radius:8px; display:flex; justify-content:space-between; align-items:center; box-shadow:0 2px 4px rgba(0,0,0,0.05); }
.sum-card h4 { font-size:0.68rem; color:#64748b; }
.sum-card div { font-size:1.15rem; font-weight:800; }

.bottom-grid { display:grid; grid-template-columns: 1fr; gap:10px; flex:1; min-height:0; }
.table-wrapper { flex:1; overflow-y:auto; border:1px solid #e2e8f0; border-radius:6px; }
table { width:100%; border-collapse: collapse; font-size:0.72rem; }
th { background:#f8fafc; padding:8px; border-bottom:2px solid #e2e8f0; text-align:left; color:#64748b; position: sticky; top:0; }
td { padding:6px 8px; border-bottom:1px solid #f1f5f9; }
.badge { padding:2px 5px; border-radius:3px; font-size:0.6rem; font-weight:800; }
.status-running  { color:#10b981; background:#ecfdf5; }
.status-charging { color:#3b82f6; background:#eff6ff; }
.status-waiting  { color:#f59e0b; background:#fffbeb; }
.status-error    { color:#ef4444; background:#fef2f2; }

.error-note  { font-size:0.72rem; color:#ef4444; padding:8px; text-align:center; }
.loading-note { font-size:0.72rem; color:#64748b; padding:8px; text-align:center; }

@media (max-width: 1200px) {
  .amr-panel { width: 280px; }
}

</style>
