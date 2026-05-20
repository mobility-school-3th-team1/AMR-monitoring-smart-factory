export const DEMO_EMERGENCY_SCENARIO_STORAGE_KEY = 'demoEmergencyScenario'

export const DEFAULT_DEMO_EMERGENCY_AMR_ID = 'amr-01'

export function readDemoEmergencyScenario() {
  try {
    const raw = sessionStorage.getItem(DEMO_EMERGENCY_SCENARIO_STORAGE_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    if (!parsed?.active) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export function writeDemoEmergencyScenario(scenario) {
  sessionStorage.setItem(DEMO_EMERGENCY_SCENARIO_STORAGE_KEY, JSON.stringify(scenario))
}

export function clearDemoEmergencyScenario() {
  sessionStorage.removeItem(DEMO_EMERGENCY_SCENARIO_STORAGE_KEY)
}

export function buildDemoEmergencyScenario(amrId = DEFAULT_DEMO_EMERGENCY_AMR_ID) {
  return {
    active: true,
    amrId,
    title: '비상 상황 감지',
    message: `${amrId} 기능 고장(ERROR)이 발생했습니다. 개별 관제에서 상태를 확인하세요.`,
    occurredAt: new Date().toISOString()
  }
}
