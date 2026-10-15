import { describe, expect, it } from 'vitest'
import { distanceToTask, signInBlocker, signRuleLabels } from './sign'

const base = { deadline: null, closed: false, latitude: null, longitude: null, radiusMeters: null, locationName: null, photoRequired: false }
const fenced = { ...base, latitude: 39.9087, longitude: 116.3975, radiusMeters: 200, locationName: '集合点' }
const now = new Date('2026-09-01T10:00:00')

describe('sign rules', () => {
  it('describes the requirements', () => {
    expect(signRuleLabels(base, now)).toEqual([])
    expect(signRuleLabels({ ...fenced, deadline: '2026-09-01T12:00:00', photoRequired: true }, now))
      .toEqual(['今天 12:00 截止', '集合点 200 米内', '需拍照'])
    expect(signRuleLabels({ ...base, deadline: '2026-09-01T09:00:00', closed: true }, now)).toEqual(['已截止'])
  })

  it('blocks sign-in the same way the server does', () => {
    expect(signInBlocker(base, null, false, now)).toBeNull()
    expect(signInBlocker({ ...base, deadline: '2026-09-01T09:59:00' }, null, false, now)).toBe('签到已截止')
    expect(signInBlocker(fenced, null, false, now)).toContain('需要定位')
    expect(signInBlocker(fenced, { latitude: 39.92, longitude: 116.40 }, false, now)).toMatch(/距签到点约 \d+ 米，需在 200 米范围内/)
    expect(signInBlocker(fenced, { latitude: 39.9095, longitude: 116.3980 }, false, now)).toBeNull()
    expect(signInBlocker({ ...base, photoRequired: true }, null, false, now)).toContain('照片')
    expect(signInBlocker({ ...base, photoRequired: true }, null, true, now)).toBeNull()
  })

  it('computes distance only for fenced tasks', () => {
    expect(distanceToTask(base, { latitude: 1, longitude: 1 })).toBeNull()
    expect(distanceToTask(fenced, { latitude: 39.9095, longitude: 116.3980 })).toBeGreaterThan(90)
  })
})
