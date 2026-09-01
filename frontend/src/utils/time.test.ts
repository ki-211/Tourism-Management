import { describe, expect, it } from 'vitest'
import { activityLife, displayTime, displayTimeRange, isSignupOpen } from './time'

const now = new Date('2026-09-01T12:00:00')

describe('time helpers', () => {
  it('判断报名窗口边界', () => {
    expect(isSignupOpen('2026-08-26T09:00:00', '2026-08-26T10:00:00', new Date('2026-08-26T09:30:00'))).toBe(true)
    expect(isSignupOpen('2026-08-26T09:00:00', '2026-08-26T10:00:00', new Date('2026-08-26T10:00:01'))).toBe(false)
  })

  it('空时间显示占位符', () => expect(displayTime('')).toBe('--'))

  it('把邻近日期显示成今天明天昨天', () => {
    expect(displayTime('2026-09-01T14:30:00', now)).toBe('今天 14:30')
    expect(displayTime('2026-09-02T09:05:00', now)).toBe('明天 09:05')
    expect(displayTime('2026-08-31T23:00:00', now)).toBe('昨天 23:00')
    expect(displayTime('2026-08-15T08:05:00', now)).toBe('8月15日 08:05')
    expect(displayTime('2025-12-31T23:00:00', now)).toBe('2025年12月31日 23:00')
  })

  it('同一天的活动显示为时间段', () => {
    expect(displayTimeRange('2026-09-01T14:00:00', '2026-09-01T18:30:00', now)).toBe('今天 14:00 – 18:30')
    expect(displayTimeRange('2026-09-01T22:00:00', '2026-09-02T06:00:00', now)).toBe('今天 22:00 – 明天 06:00')
  })

  it('按时间判断活动状态', () => {
    const activity = {
      signupStart: '2026-08-20T09:00:00',
      signupEnd: '2026-08-31T18:00:00',
      startTime: '2026-09-01T09:00:00',
      endTime: '2026-09-03T18:00:00'
    }
    expect(activityLife(activity, new Date('2026-08-10T12:00:00'))).toBe('upcoming')
    expect(activityLife(activity, new Date('2026-08-25T12:00:00'))).toBe('signup')
    expect(activityLife(activity, new Date('2026-08-31T18:00:01'))).toBe('closed')
    expect(activityLife(activity, new Date('2026-09-01T12:00:00'))).toBe('ongoing')
    expect(activityLife(activity, new Date('2026-09-03T18:00:00'))).toBe('ended')
  })

  it('活动状态边界与后端时间窗口一致', () => {
    const activity = {
      signupStart: '2026-09-01T09:00:00',
      signupEnd: '2026-09-01T10:00:00',
      startTime: '2026-09-01T11:00:00',
      endTime: '2026-09-01T12:00:00'
    }
    expect(activityLife(activity, new Date('2026-09-01T08:59:59'))).toBe('upcoming')
    expect(activityLife(activity, new Date('2026-09-01T09:00:00'))).toBe('signup')
    expect(activityLife(activity, new Date('2026-09-01T10:00:00'))).toBe('signup')
    expect(activityLife(activity, new Date('2026-09-01T10:00:01'))).toBe('closed')
    expect(activityLife(activity, new Date('2026-09-01T11:00:00'))).toBe('ongoing')
    expect(activityLife(activity, new Date('2026-09-01T12:00:00'))).toBe('ended')
  })
})
