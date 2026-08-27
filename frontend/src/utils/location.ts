import type { Location } from '@/services/types'

export type Coordinates = { latitude: number; longitude: number }

const EARTH_RADIUS_METERS = 6371000

function radians(value: number) { return value * Math.PI / 180 }

export function distanceMeters(first: Coordinates, second: Coordinates) {
  const latitudeDelta = radians(second.latitude - first.latitude)
  const longitudeDelta = radians(second.longitude - first.longitude)
  const firstLatitude = radians(first.latitude)
  const secondLatitude = radians(second.latitude)
  const value = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(firstLatitude) * Math.cos(secondLatitude) * Math.sin(longitudeDelta / 2) ** 2
  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(value)))
}

export function shouldUploadLocation(last: (Coordinates & { sentAt: number }) | null, next: Coordinates, now = Date.now()) {
  if (!last) return true
  return now - last.sentAt >= 60000 || distanceMeters(last, next) >= 20
}

export function validCoordinates(latitude: unknown, longitude: unknown) {
  if (latitude === '' || longitude === '' || latitude == null || longitude == null) return false
  const lat = Number(latitude)
  const lon = Number(longitude)
  return Number.isFinite(lat) && Number.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
}

export function applyLocationEvent(locations: Location[], event: any) {
  if (event?.type === 'LOCATION_REMOVED') return locations.filter(item => item.userId !== Number(event.payload?.userId))
  if (event?.type !== 'LOCATION_UPDATED' || !event.payload) return locations
  const incoming = event.payload as Location
  const existing = locations.findIndex(item => item.userId === incoming.userId)
  if (existing < 0) return [incoming, ...locations]
  const next = [...locations]
  next[existing] = incoming
  return next.sort((left, right) => new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime())
}
