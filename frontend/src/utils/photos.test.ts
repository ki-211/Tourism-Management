import { describe, expect, it } from 'vitest'
import { reuseSignedUrls } from './photos'

const photo = (id: number, url: string, deletable = false) =>
  ({ id, url, thumbnailUrl: `${url}&variant=thumb`, uploaderId: 1, uploaderName: 'a', createdAt: '2026-01-01T00:00:00', deletable })

describe('reuseSignedUrls', () => {
  it('keeps known URLs, adopts new photos and fresh metadata, and drops removed photos', () => {
    const previous = [photo(1, 'old-1'), photo(2, 'old-2')]
    const fresh = [photo(3, 'new-3'), photo(1, 'new-1', true)]
    expect(reuseSignedUrls(previous, fresh)).toEqual([photo(3, 'new-3'), photo(1, 'old-1', true)])
  })
})
