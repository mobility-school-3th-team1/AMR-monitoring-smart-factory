<template>
  <div class="detail-layout">
    <SectionPanel
      eyebrow="AMR 개별 관제"
      title="선택 장비 상태"
      subtitle="선택한 AMR의 상세 정보 및 상태 모니터링"
    >
      <div class="summary-row">
        <div class="sum-card" style="border-left-color: #3b82f6;"><h4>운영 중 로봇</h4><div class="val">{{ stats.operating }}/{{ stats.total }}</div></div>
        <div class="sum-card" style="border-left-color: #10b981;"><h4>운행</h4><div class="val">{{ stats.running }}/{{ stats.operating }}</div></div>
        <div class="sum-card" style="border-left-color: #f59e0b;"><h4>대기</h4><div class="val">{{ stats.waiting }}/{{ stats.operating }}</div></div>
        <div class="sum-card" style="border-left-color: #ef4444;"><h4 style="color:#ef4444">오류</h4><div class="val" style="color:#ef4444">{{ stats.error }}/{{ stats.operating }}</div></div>
      </div>

      <div class="detail-header">
        <div>
          <strong id="amr-title" style="font-size:1.1rem;">{{ amrId }}</strong>
          <small id="amr-last-check" style="color:#94a3b8; margin-left:8px;">{{ amrData.last }}</small>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <div v-if="amrData.error" class="error-msg">{{ amrData.error }}</div>
          <button class="btn-emergency" @click="triggerEmergency">🚨 비상 정지</button>
        </div>
      </div>

      <div class="info-grid">
        <BaseCard class="info-card" style="display:flex; align-items:center; gap:12px; padding: 10px 15px;">
          <div style="width:100px; height:70px; background:#334155; border-radius:6px; flex-shrink:0;"></div>
          <div style="font-size:0.88rem; line-height:1.6;">
            <strong id="amr-current-task">{{ amrData.task }}</strong><br>
            <span id="amr-status-line">{{ amrData.line }}</span>
          </div>
        </BaseCard>

        <BaseCard class="info-card" style="text-align:center;">
          <p style="color:#64748b; font-size:0.75rem; margin-bottom:5px;">총 주행 거리</p>
          <div style="font-size:1.6rem; font-weight:800;">{{ amrData.mileage }} <small style="font-size:0.9rem;">km</small></div>
        </BaseCard>

        <BaseCard class="info-card" style="text-align:center;">
          <p style="color:#64748b; font-size:0.75rem; margin-bottom:5px;">총 가동 시간</p>
          <div style="font-size:1.6rem; font-weight:800;">{{ amrData.uptime }} <small style="font-size:0.9rem;">시간</small></div>
        </BaseCard>
      </div>

      <div class="health-section">
        <div class="status-group" style="margin-bottom:10px; padding-bottom:10px; border-bottom:1px solid #f1f5f9;">
          <div class="status-item" v-for="s in sensors" :key="s.name"><div class="dot"></div>{{ s.name }}</div>
        </div>
        <div class="status-group">
          <span style="font-weight:bold; color:#475569; margin-right:5px;">구동부:</span>
          <div class="status-item">앞 왼쪽</div>
          <div class="status-item">앞 오른쪽</div>
          <div class="status-item">뒤 왼쪽</div>
          <div class="status-item">뒤 오른쪽</div>
        </div>
      </div>

      <div class="bottom-grid">
        <article class="info-card" style="display:flex; flex-direction:column;">
          <p style="font-weight:bold; margin-bottom:10px; font-size:0.86rem;">시간 준수율 추이 (%)</p>
          <div class="chart-box">
            <svg class="chart-svg" viewBox="0 0 500 130">
              <polyline :points="chartPoints" class="line-path" />
              <g>
                <circle v-for="(pt, i) in chartNodes" :cx="pt.x" :cy="pt.y" r="4" class="node" :key="i" />
              </g>
            </svg>
          </div>
        </article>
      </div>

    </SectionPanel>
  </div>
</template>

<script setup>
import BaseCard from '../components/atoms/BaseCard.vue'
import SectionPanel from '../components/molecules/SectionPanel.vue'
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/plugins/axios'
import { publish } from '@/plugins/ws'

const route = useRoute()

const sensors = ref([
  { name: 'LiDAR', state: '정상' },
  { name: '전방 카메라', state: '정상' },
  { name: '후방 카메라', state: '정상' },
  { name: 'IMU', state: '주의' },
  { name: '로드셀', state: '정상' }
])

const stats = ref({ operating: 9, total: 12, running: 6, waiting: 2, error: 1 })

const amrId = ref('AMR-01')
const amrData = ref({ status: '', error: '', task: '', line: '', mileage: 0, uptime: 0, last: '' })

const sample = {
  'AMR-01': { status: '오류', error: '오류: 전방 장애물로 인한 주행 불가', task: '▶ 현재 작업: 부품 운송', line: '🔋 86% | Temp: 20°C | Humid: 30%', mileage: 128, uptime: 86, last: '최종 점검일: 2026.05.12' },
  'AMR-02': { status: '대기', error: '대기: 다음 작업 지시 대기 중', task: '▶ 현재 작업: 대기 상태', line: '🔋 90% | Temp: 20°C | Humid: 30%', mileage: 102, uptime: 74, last: '최종 점검일: 2026.05.12' },
  'AMR-04': { status: '운행', error: '운행: 정상 주행 중', task: '▶ 현재 작업: 배터리 팩 운반', line: '🔋 87% | Temp: 20°C | Humid: 30%', mileage: 210, uptime: 190, last: '최종 점검일: 2026.05.12' }
}

onMounted(() => {
  const q = route.query.amr || route.params.amr || 'AMR-01'
  amrId.value = q
  amrData.value = sample[q] || sample['AMR-01']
})

async function triggerEmergency() {
  const confirmAction = window.confirm(`비상 정지 명령을 전송하시겠습니까? (AMR: ${amrId.value})`)
  if (!confirmAction) return

  try {
    // Send REST command
    const resp = await api.post(`/amrs/${encodeURIComponent(amrId.value)}/commands`, { command: 'emergencyStop' })
    const accepted = resp?.data?.accepted === true

    if (accepted) {
      amrData.value.error = 'EMERGENCY_STOP'
      amrData.value.status = 'EMERGENCY_STOP'
      // notify user
      alert('비상 정지 명령이 수락되었습니다.')
    } else {
      alert('비상 정지 요청이 전송되었으나 서버에서 수락 응답을 받지 못했습니다.')
    }

    // Publish MQTT command (best-effort)
    try {
      publish('factory/amr/command', { amrId: amrId.value, command: 'emergencyStop', timestamp: new Date().toISOString() })
    } catch (pubErr) {
      console.warn('MQTT publish failed (best-effort):', pubErr)
    }
  } catch (err) {
    console.error('triggerEmergency error', err)
    const msg = err?.response?.data?.message || '비상 정지 요청 중 오류가 발생했습니다.'
    alert(msg)
  }
}

// small chart mock
const chartNodes = computed(() => {
  const points = [80,85,84,82,81,80,82,83,85,86]
  const baseX = 25
  const step = 45
  return points.map((v,i) => ({ x: baseX + i*step, y: 120 - (v-60) }))
})
const chartPoints = computed(() => chartNodes.value.map(p => `${p.x},${p.y}`).join(' '))
</script>

<style scoped>
.detail-layout {
  display: grid;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.detail-card {
  padding: 20px;
  border-radius: var(--radius-md);
  background: var(--color-surface-soft);
}

.detail-card__label {
  margin: 0 0 10px;
  font-size: 0.76rem;
  font-weight: 800;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.detail-card h3 {
  margin: 0 0 10px;
  font-size: 1.12rem;
}

.detail-card p {
  margin: 0 0 14px;
  font-size: 0.86rem;
  color: var(--color-text-muted);
}

.detail-card--action {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(254, 242, 242, 0.75));
}

.danger-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  background: var(--color-danger);
  color: #fff;
  font-weight: 800;
}

.sensor-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.sensor-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.76);
}

.sensor-list span,
.sensor-list strong {
  font-size: 0.84rem;
}

@media (max-width: 1280px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

/* Additional styles from mockup-driven layout */
.summary-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 12px; }
.sum-card { background: white; padding: 12px; border-radius: 8px; border-left: 4px solid #cbd5e1; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.sum-card h4 { font-size: 0.7rem; color: #64748b; margin: 0; }
.sum-card .val { font-size: 1.3rem; font-weight: 800; }

.detail-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
.error-msg { color: #ef4444; font-weight:800; font-size:0.9rem; }

.btn-emergency { padding: 8px 14px; background: #ef4444; color:#fff; border:none; border-radius:6px; font-weight:800; cursor:pointer; box-shadow: 0 4px 6px rgba(239,68,68,0.2); }
.btn-emergency:hover { background:#dc2626; transform: translateY(-1px); }

.info-grid { display: grid; grid-template-columns: 1.5fr 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.info-card { background:white; border-radius:8px; padding:12px; border:1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }

.health-section { background:white; border-radius:8px; padding:12px 15px; border:1px solid #e2e8f0; margin-bottom:12px; }
.status-group { display:flex; gap:20px; flex-wrap:wrap; font-size:0.78rem; }
.status-item { display:flex; align-items:center; gap:6px; font-weight:600; }
.status-item .dot, .dot { width:8px; height:8px; border-radius:50%; background:#10b981; }

.bottom-grid { display:grid; grid-template-columns: 1fr; gap:12px; min-height:0; }
.chart-box { flex:1; position:relative; min-height:0; width:100%; }
.chart-svg { width:100%; height:100%; }
.line-path { fill:none; stroke:#3b82f6; stroke-width:2.5; }
.node { fill:#3b82f6; stroke:white; stroke-width:1.5; }
</style>