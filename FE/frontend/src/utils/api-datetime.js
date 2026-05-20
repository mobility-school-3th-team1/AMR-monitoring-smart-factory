import dayjs from 'dayjs'

/**
 * BE LocalDateTime(JSON 배열·타임존 없는 ISO)을 브라우저 로컬 시각으로 해석한다.
 * 서버 JVM TZ=Asia/Seoul 기준으로 저장된 시각 값을 그대로 로컬 시계에 맞춘다.
 */
export function parseApiDateTime(raw) {
  if (!raw) return null

  if (typeof raw === 'string') {
    const trimmed = raw.trim()
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(trimmed) && !/[zZ]|[+-]\d{2}:\d{2}$/.test(trimmed)) {
      const [datePart, timePart = '00:00:00'] = trimmed.split('T')
      const [year, month, day] = datePart.split('-').map(Number)
      const timeSegments = timePart.split(':').map(Number)
      const hour = timeSegments[0] ?? 0
      const minute = timeSegments[1] ?? 0
      const second = timeSegments[2] ?? 0
      return dayjs(new Date(year, month - 1, day, hour, minute, second))
    }
    return dayjs(trimmed)
  }

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

export function formatApiDateTime(raw, pattern = 'YYYY-MM-DD HH:mm') {
  const parsed = parseApiDateTime(raw)
  if (!parsed || !parsed.isValid()) return '—'
  return parsed.format(pattern)
}
