// Gradients for activities without a cover, so a list of them does not look like copies of one card.
const THEMES = [
  ['#0f766e', '#3fb5a6'], // teal (brand)
  ['#1d5f8a', '#4aa3c7'], // lake
  ['#2f6b3f', '#7cb36a'], // forest
  ['#b4533a', '#ee9f5c'], // sunset
  ['#5b4b8a', '#a284c9'], // dusk
  ['#334e5c', '#6d93a3']  // slate
] as const

/** Stable per activity: the same activity always gets the same colors. */
export function coverTheme(id: number) {
  const [from, to] = THEMES[Math.abs(Math.trunc(id || 0) * 7) % THEMES.length]
  return { from, to, background: `linear-gradient(135deg, ${from} 0%, ${to} 100%)` }
}

/** First visible character of the title (emoji-safe), used as the placeholder's monogram. */
export function coverInitial(title?: string) {
  return Array.from((title || '').trim())[0] || '行'
}
