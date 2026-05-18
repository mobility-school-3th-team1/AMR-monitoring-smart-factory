<template>
  <div class="amr-manage-view">
    <aside class="amr-panel">
      <div class="amr-header">AMR 선택 목록</div>
      <div class="amr-scroll">
        <div class="amr-selection">
          <a v-for="robot in robots" :key="robot.id" href="#" class="amr-link" @click.prevent="openDetail(robot.id)">
            <div class="amr-select-card" :class="robot.class">
              <div class="amr-select-top">
                <div>
                  <div class="amr-select-id">{{ robot.id }}</div>
                  <div class="amr-select-meta">배터리 {{ robot.battery }}% · {{ robot.meta }}</div>
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
        <div class="mini-note">스크롤해서 AMR을 선택하면 상세 화면으로 이동합니다.</div>
      </div>
    </aside>

    <section class="dashboard-panel">
      <div class="summary-row">
        <div class="sum-card" style="border-top: 3px solid #3b82f6;"><h4>운영 중 로봇</h4><div>{{ stats.operating }}/{{ stats.total }}</div></div>
        <div class="sum-card" style="border-top: 3px solid #10b981;"><h4>운행</h4><div>{{ stats.running }}/{{ stats.operating }}</div></div>
        <div class="sum-card" style="border-top: 3px solid #f59e0b;"><h4>대기</h4><div>{{ stats.waiting }}/{{ stats.operating }}</div></div>
        <div class="sum-card" style="border-top: 3px solid #ef4444;"><h4 style="color:#ef4444">오류</h4><div style="color:#ef4444">{{ stats.error }}/{{ stats.operating }}</div></div>
      </div>

      <div class="grid-layout">
        <article class="widget">
          <div class="widget-title">공정별 평균 대기 시간 (분)</div>
          <div class="chart-box">차트 영역 (샘플)</div>
        </article>

        <article class="widget span-2">
          <div class="widget-title">일별 오류 발생 건수</div>
          <div class="chart-box">차트 영역 (샘플)</div>
        </article>

        <article class="widget span-2">
          <div class="widget-title">평균 시간 준수율 (%)</div>
          <div class="chart-box">차트 영역 (샘플)</div>
        </article>

        <article class="widget">
          <div class="widget-title">건전성 기준 미달 기체</div>
          <div class="compact-stat"><div class="big">{{ stats.unhealthy }}<span style="font-size:0.9rem; color:#64748b; margin-left:6px;">/ {{ stats.total }} 대</span></div><div class="muted">최근 점검 대상: <strong>{{ stats.lastCheck }}</strong></div></div>
        </article>
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
                <tr v-for="r in robots" :key="r.id">
                  <td>{{ r.id }}</td>
                  <td><span class="badge" :class="r.tagClass">{{ r.tagDisplay }}</span></td>
                  <td>{{ r.battery }}%</td>
                  <td>{{ r.location }}</td>
                  <td>{{ r.destination }}</td>
                  <td>{{ r.task }}</td>
                  <td>{{ r.speed || '0 m/s' }}</td>
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
import { useRouter } from 'vue-router'

const router = useRouter()

const stats = {
  operating: 9,
  total: 12,
  running: 6,
  waiting: 2,
  error: 1,
  charging: 2,
  unhealthy: 0,
  lastCheck: 'AMR-01'
}

const robots = [
  { id: 'AMR-01', class: 'error', tag: '오류', tagDisplay: '오류', tagClass: 'status-error', battery: 86, meta: '오류 상태 · 최종 점검일 2026.05.12', location: 'Zone A', destination: 'Zone B', task: '진단 대기', speed: '0 m/s' },
  { id: 'AMR-02', class: 'waiting', tag: '대기', tagDisplay: '대기', tagClass: 'status-waiting', battery: 90, meta: '대기 상태 · 충전 대기 중', location: 'Zone B', destination: 'Zone C', task: '충전 대기', speed: '0 m/s' },
  { id: 'AMR-04', class: 'running', tag: '운행', tagDisplay: '운행 중', tagClass: 'status-running', battery: 87, meta: '운행 중 · 정상 주행', location: 'Zone C', destination: '충전 스테이션 1', task: '이송', speed: '1.2 m/s' },
  { id: 'AMR-05', class: 'running', tag: '운행', tagDisplay: '운행 중', tagClass: 'status-running', battery: 72, meta: '운행 중 · 작업 이송 수행', location: 'Zone D', destination: 'Zone E', task: '이송', speed: '1.1 m/s' },
  { id: 'AMR-08', class: 'charging', tag: '충전', tagDisplay: '충전 중', tagClass: 'status-charging', battery: 58, meta: '충전 중 · 보조 스테이션 연결', location: '충전 스테이션 2', destination: '-', task: '충전', speed: '0 m/s' }
]

function openDetail(amrId) {
  router.push({ path: '/amr-detail', query: { amr: amrId } })
}
</script>

<style scoped>
.amr-manage-view { display: flex; gap: 12px; height: 100%; min-height: 0; }
.amr-panel { width: 330px; background: #e6eef8; border-radius: 10px; display:flex; flex-direction:column; flex-shrink:0; }
.amr-header { padding: 12px; text-align:center; font-weight:800; font-size:0.92rem; background:#dbeaf7; border-radius:10px 10px 0 0; }
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

.dashboard-panel { flex:1; display:flex; flex-direction:column; gap:8px; min-width:0; }
.summary-row { display:grid; grid-template-columns: repeat(4,1fr); gap:10px; flex-shrink:0; }
.sum-card { background:white; padding:10px 12px; border-radius:8px; display:flex; justify-content:space-between; align-items:center; box-shadow:0 2px 4px rgba(0,0,0,0.05); }
.sum-card h4 { font-size:0.68rem; color:#64748b; }
.sum-card div { font-size:1.15rem; font-weight:800; }

.grid-layout { flex:0.56; display:grid; grid-template-columns: repeat(3,1fr); grid-template-rows: 0.72fr 0.72fr; gap:8px; min-height:0; }
.widget { background:white; border-radius:8px; padding:10px; box-shadow:0 2px 5px rgba(0,0,0,0.08); display:flex; flex-direction:column; min-height:0; }
.widget-title { font-size:0.78rem; font-weight:700; margin-bottom:8px; border-left:4px solid #3b82f6; padding-left:10px; }
.span-2 { grid-column: span 2; }
.compact-stat { font-size:0.72rem; }
.compact-stat .big { font-size:2rem; font-weight:800; color:#1e293b; }
.compact-stat .muted { font-size:0.62rem; color:#94a3b8; }

.bottom-grid { display:grid; grid-template-columns: 1fr; gap:10px; flex:1; min-height:0; }
.table-wrapper { flex:1; overflow-y:auto; border:1px solid #e2e8f0; border-radius:6px; }
table { width:100%; border-collapse: collapse; font-size:0.72rem; }
th { background:#f8fafc; padding:8px; border-bottom:2px solid #e2e8f0; text-align:left; color:#64748b; position: sticky; top:0; }
td { padding:6px 8px; border-bottom:1px solid #f1f5f9; }
.badge { padding:2px 5px; border-radius:3px; font-size:0.6rem; font-weight:800; }
.status-driving { color:#10b981; background:#ecfdf5; }
.status-charging { color:#3b82f6; background:#eff6ff; }
.status-waiting { color:#f59e0b; background:#fffbeb; }
.status-error { color:#ef4444; background:#fef2f2; }

@media (max-width: 1200px) {
  .amr-panel { width: 280px; }
  .grid-layout { grid-template-columns: repeat(2,1fr); }
}

</style>