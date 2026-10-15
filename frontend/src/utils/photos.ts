import type { Photo } from '@/services/types'

/**
 * Keeps the already-signed URL of photos the client has seen, so <image> elements keep their cached
 * copy instead of downloading every photo again whenever the list is refetched.
 */
export function reuseSignedUrls(previous: Photo[], fresh: Photo[]): Photo[] {
  const known = new Map(previous.map(photo => [photo.id, photo]))
  return fresh.map(photo => {
    const seen = known.get(photo.id)
    return seen ? { ...photo, url: seen.url, thumbnailUrl: seen.thumbnailUrl } : photo
  })
}
