export const displayTime = (value?: string, now = new Date()) => {
  const date = parseTime(value)
  if (!date) return '--'
  const clock = clockText(date)
  if (sameDay(date, now)) return `今天 ${clock}`
  if (sameDay(date, shiftDay(now, 1))) return `明天 ${clock}`
  if (sameDay(date, shiftDay(now, -1))) return `昨天 ${clock}`
  if (date.getFullYear() === now.getFullYear()) return `${date.getMonth() + 1}月${date.getDate()}日 ${clock}`
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${clock}`
}

export const displayTimeRange = (start?: string, end?: string, now = new Date()) => {
  const from = parseTime(start)
  const to = parseTime(end)
  if (!from || !to) return `${displayTime(start, now)} – ${displayTime(end, now)}`
  if (sameDay(from, to)) return `${dayLabel(from, now)} ${clockText(from)} – ${clockText(to)}`
  return `${displayTime(start, now)} – ${displayTime(end, now)}`
}

export const apiTime = (value: string) => value.length === 16 ? value + ':00' : value
export const isSignupOpen = (start: string, end: string, now = new Date()) => now >= new Date(start) && now <= new Date(end)

export type ActivityLife = 'upcoming' | 'signup' | 'closed' | 'ongoing' | 'ended'

export const activityLife = (activity: { signupStart: string; signupEnd: string; startTime: string; endTime: string }, now = new Date()): ActivityLife => {
  const end = parseTime(activity.endTime)
  const start = parseTime(activity.startTime)
  const signupStart = parseTime(activity.signupStart)
  const signupEnd = parseTime(activity.signupEnd)
  if (end && now >= end) return 'ended'
  if (start && now >= start) return 'ongoing'
  if (signupEnd && now > signupEnd) return 'closed'
  if (signupStart && now >= signupStart) return 'signup'
  return 'upcoming'
}

export const activityLifeLabel: Record<ActivityLife, string> = {
  upcoming: '报名未开始',
  signup: '报名中',
  closed: '报名已截止',
  ongoing: '进行中',
  ended: '已结束'
}

function parseTime(value?: string) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function clockText(date: Date) {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function sameDay(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate()
}

function shiftDay(date: Date, days: number) {
  const next = new Date(date)
  next.setDate(date.getDate() + days)
  return next
}

function dayLabel(date: Date, now: Date) {
  if (sameDay(date, now)) return '今天'
  if (sameDay(date, shiftDay(now, 1))) return '明天'
  if (sameDay(date, shiftDay(now, -1))) return '昨天'
  if (date.getFullYear() === now.getFullYear()) return `${date.getMonth() + 1}月${date.getDate()}日`
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}
