import { describe, expect, it } from 'vitest'
import { displayTime, isSignupOpen } from './time'

describe('time helpers', () => {
  it('判断报名窗口边界', () => {
    expect(isSignupOpen('2026-08-26T09:00:00', '2026-08-26T10:00:00', new Date('2026-08-26T09:30:00'))).toBe(true)
    expect(isSignupOpen('2026-08-26T09:00:00', '2026-08-26T10:00:00', new Date('2026-08-26T10:00:01'))).toBe(false)
  })
  it('空时间显示占位符', () => expect(displayTime('')).toBe('--'))
})
