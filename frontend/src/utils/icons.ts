// One consistent stroke-icon set (24×24, round caps) replacing ad-hoc Unicode glyphs, whose size and shape
// differ between fonts and platforms. Icons are rendered through <image> with a base64 SVG data URI, which works
// in both H5 and WeChat mini programs (inline <svg> elements are not supported in mini programs).
const PATHS = {
  pin: '<path d="M12 21s-6.5-5.7-6.5-11.2a6.5 6.5 0 0 1 13 0C18.5 15.3 12 21 12 21z"/><circle cx="12" cy="9.8" r="2.3"/>',
  clock: '<circle cx="12" cy="12" r="8.6"/><path d="M12 7.4V12l3.1 2"/>',
  user: '<circle cx="12" cy="8.2" r="3.7"/><path d="M4.8 20c.9-3.7 3.7-5.8 7.2-5.8s6.3 2.1 7.2 5.8"/>',
  users: '<circle cx="9.2" cy="8.6" r="3.3"/><path d="M3.4 19.2c.7-3.3 2.9-5.1 5.8-5.1s5.1 1.8 5.8 5.1"/><path d="M15.6 5.5a3.3 3.3 0 0 1 0 6.2"/><path d="M17.3 14.4c2 .6 3.1 2.2 3.4 4.8"/>',
  check: '<path d="M5 12.6l4.3 4.3L19 7.2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  inbox: '<path d="M4 13.2 6.3 5.8h11.4L20 13.2V18a1.2 1.2 0 0 1-1.2 1.2H5.2A1.2 1.2 0 0 1 4 18z"/><path d="M4 13.2h4.6l1.3 2.6h4.2l1.3-2.6H20"/>',
  calendar: '<rect x="4" y="5.6" width="16" height="14.2" rx="2.2"/><path d="M4 10.2h16M8.4 3.6v3.8M15.6 3.6v3.8"/>',
  chevronDown: '<path d="m6.5 9.5 5.5 5.5 5.5-5.5"/>',
  chevronRight: '<path d="m9.5 6.5 5.5 5.5-5.5 5.5"/>',
  arrowUpRight: '<path d="M7 17 17 7M8.6 7H17v8.4"/>',
  pen: '<path d="m5 19 1-4.3 9.6-9.6a2 2 0 0 1 2.8 0l.5.5a2 2 0 0 1 0 2.8L9.3 18 5 19z"/><path d="m13.9 6.9 3.2 3.2"/>',
  shield: '<path d="M12 3.4 19 6v5.5c0 4.4-2.9 7.8-7 9.1-4.1-1.3-7-4.7-7-9.1V6z"/><path d="m9 12 2.2 2.2 3.8-3.9"/>',
  ticket: '<path d="M4 7.2A1.6 1.6 0 0 1 5.6 5.6h12.8A1.6 1.6 0 0 1 20 7.2v2.4a2.4 2.4 0 0 0 0 4.8v2.4a1.6 1.6 0 0 1-1.6 1.6H5.6A1.6 1.6 0 0 1 4 16.8v-2.4a2.4 2.4 0 0 0 0-4.8z"/><path d="M14 5.6v12.8" stroke-dasharray="2 2.2"/>',
  car: '<path d="M5 16.5V12l1.8-4.6A1.8 1.8 0 0 1 8.5 6.2h7a1.8 1.8 0 0 1 1.7 1.2L19 12v4.5"/><path d="M3.8 16.5h16.4M5 12h14"/><circle cx="8" cy="16.6" r="1.5"/><circle cx="16" cy="16.6" r="1.5"/>'
} as const

export type IconName = keyof typeof PATHS
export const iconNames = Object.keys(PATHS) as IconName[]

const BASE64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

/** Base64 for ASCII text; mini programs have no btoa(). */
export function base64Ascii(text: string): string {
  let out = ''
  for (let i = 0; i < text.length; i += 3) {
    const a = text.charCodeAt(i), b = text.charCodeAt(i + 1), c = text.charCodeAt(i + 2)
    const triple = (a << 16) | ((b || 0) << 8) | (c || 0)
    out += BASE64[(triple >> 18) & 63] + BASE64[(triple >> 12) & 63]
    out += i + 1 < text.length ? BASE64[(triple >> 6) & 63] : '='
    out += i + 2 < text.length ? BASE64[triple & 63] : '='
  }
  return out
}

export function iconSvg(name: IconName, color: string, strokeWidth = 1.8): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="${strokeWidth}" stroke-linecap="round" stroke-linejoin="round">${PATHS[name]}</svg>`
}

const cache = new Map<string, string>()
export function iconDataUri(name: IconName, color = '#0f766e', strokeWidth = 1.8): string {
  const key = `${name}|${color}|${strokeWidth}`
  let uri = cache.get(key)
  if (!uri) { uri = `data:image/svg+xml;base64,${base64Ascii(iconSvg(name, color, strokeWidth))}`; cache.set(key, uri) }
  return uri
}
