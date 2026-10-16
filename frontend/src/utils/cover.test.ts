import { describe, expect, it } from 'vitest'
import { coverInitial, coverTheme } from './cover'

describe('cover placeholder', () => {
  it('is stable per activity and varies between neighbours', () => {
    expect(coverTheme(5)).toEqual(coverTheme(5))
    const backgrounds = new Set([1, 2, 3, 4, 5, 6].map(id => coverTheme(id).background))
    expect(backgrounds.size).toBe(6)
    expect(coverTheme(0).background).toMatch(/^linear-gradient\(135deg, #[0-9a-f]{6} 0%, #[0-9a-f]{6} 100%\)$/)
  })

  it('uses the first visible character, keeping emoji intact', () => {
    expect(coverInitial('  周末西湖骑行')).toBe('周')
    expect(coverInitial('🚲 骑行')).toBe('🚲')
    expect(coverInitial('')).toBe('行')
  })
})
