import { Buffer } from 'node:buffer'
import { describe, expect, it } from 'vitest'
import { base64Ascii, iconDataUri, iconNames, iconSvg } from './icons'

describe('icons', () => {
  it('encodes base64 exactly like the platform encoder, including padding', () => {
    for (const text of ['', 'a', 'ab', 'abc', 'abcd', iconSvg('pin', '#0f766e')]) {
      expect(base64Ascii(text)).toBe(Buffer.from(text, 'ascii').toString('base64'))
    }
  })

  it('builds a valid, colored SVG data URI for every icon', () => {
    for (const name of iconNames) {
      const uri = iconDataUri(name, '#dc4c4c')
      expect(uri.startsWith('data:image/svg+xml;base64,')).toBe(true)
      const svg = Buffer.from(uri.split(',')[1], 'base64').toString('ascii')
      expect(svg).toMatch(/^<svg [^>]*viewBox="0 0 24 24"[^>]*>.*<\/svg>$/)
      expect(svg).toContain('stroke="#dc4c4c"')
      expect(/[^\x20-\x7e]/.test(svg)).toBe(false)
    }
  })
})
