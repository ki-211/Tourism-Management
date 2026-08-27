import { describe, expect, it } from 'vitest'
import { applyLocationEvent, distanceMeters, shouldUploadLocation, validCoordinates } from './location'

describe('location helpers', () => {
  it('按二十米位移或六十秒心跳决定是否上报', () => {
    const origin = { latitude: 39.9042, longitude: 116.4074, sentAt: 1000 }
    expect(shouldUploadLocation(origin, { latitude: 39.90421, longitude: 116.4074 }, 30000)).toBe(false)
    expect(shouldUploadLocation(origin, { latitude: 39.9045, longitude: 116.4074 }, 30000)).toBe(true)
    expect(shouldUploadLocation(origin, { latitude: 39.90421, longitude: 116.4074 }, 61000)).toBe(true)
    expect(distanceMeters(origin, { latitude: 39.9045, longitude: 116.4074 })).toBeGreaterThan(20)
  })

  it('校验经纬度范围', () => {
    expect(validCoordinates(39.9, 116.4)).toBe(true)
    expect(validCoordinates('', 116.4)).toBe(false)
    expect(validCoordinates(91, 116.4)).toBe(false)
    expect(validCoordinates(39.9, -181)).toBe(false)
  })

  it('按实时事件增量更新和移除成员位置', () => {
    const original: any[] = [{ userId: 1, nickname: '甲', latitude: 1, longitude: 1, updatedAt: '2026-01-01T00:00:00', expiresAt: '2026-01-01T00:01:30' }]
    const added = applyLocationEvent(original, { type: 'LOCATION_UPDATED', payload: { userId: 2, nickname: '乙', latitude: 2, longitude: 2, updatedAt: '2026-01-01T00:00:01', expiresAt: '2026-01-01T00:01:31' } })
    expect(added.map(item => item.userId)).toEqual([2, 1])
    expect(applyLocationEvent(added, { type: 'LOCATION_REMOVED', payload: { userId: 1 } }).map(item => item.userId)).toEqual([2])
  })
})
