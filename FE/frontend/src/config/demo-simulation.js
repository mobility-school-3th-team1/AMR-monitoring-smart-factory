/** 시연 시뮬레이션 상수 — BE `app.demo` 및 루트 `TODO.md`와 동일 */
export const DEMO_CHARGE_RATE_PCT_PER_SEC = 5
export const DEMO_BATTERY_FULL_PCT = 100
export const DEMO_PRIMARY_STATION_ID = 'station-1'

export function estimateSecondsToFullCharge(batteryPercent) {
  const battery = Number(batteryPercent) || 0
  const remaining = Math.max(0, DEMO_BATTERY_FULL_PCT - battery)
  if (remaining === 0) {
    return 0
  }
  return Math.ceil(remaining / DEMO_CHARGE_RATE_PCT_PER_SEC)
}

export function formatEtaLabel(secondsToFull) {
  if (secondsToFull <= 0) {
    return '완충'
  }
  if (secondsToFull < 60) {
    return `${secondsToFull}초`
  }
  const minutes = Math.floor(secondsToFull / 60)
  const seconds = secondsToFull % 60
  if (seconds === 0) {
    return `${minutes}분`
  }
  return `${minutes}분 ${seconds}초`
}

/** 0-30분 / 30-60분 / 60분+ 버킷별 대수 */
export function forecastBucketsFromChargingAmrs(chargingAmrs) {
  const buckets = [0, 0, 0]
  if (!Array.isArray(chargingAmrs)) {
    return buckets
  }
  for (const amr of chargingAmrs) {
    const secondsToFull = estimateSecondsToFullCharge(amr.batteryPercent)
    const minutesToFull = secondsToFull / 60
    if (minutesToFull <= 30) {
      buckets[0] += 1
    } else if (minutesToFull <= 60) {
      buckets[1] += 1
    } else {
      buckets[2] += 1
    }
  }
  return buckets
}

export function maxEtaFromChargingAmrs(chargingAmrs) {
  if (!Array.isArray(chargingAmrs) || chargingAmrs.length === 0) {
    return '—'
  }
  const maxSeconds = chargingAmrs.reduce((max, amr) => {
    const seconds = estimateSecondsToFullCharge(amr.batteryPercent)
    return Math.max(max, seconds)
  }, 0)
  return formatEtaLabel(maxSeconds)
}
