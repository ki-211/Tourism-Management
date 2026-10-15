import type { SignTask } from '@/services/types'
import { displayTime } from './time'
import { distanceMeters, type Coordinates } from './location'

type Rules = Pick<SignTask, 'deadline' | 'closed' | 'latitude' | 'longitude' | 'radiusMeters' | 'locationName' | 'photoRequired'>

/** Short labels describing a task's sign-in requirements, shown as chips. */
export function signRuleLabels(task: Rules, now = new Date()): string[] {
  const labels: string[] = []
  if (task.deadline) labels.push(task.closed ? '已截止' : `${displayTime(task.deadline, now)} 截止`)
  if (task.radiusMeters) labels.push(`${task.locationName ? task.locationName + ' ' : ''}${task.radiusMeters} 米内`)
  if (task.photoRequired) labels.push('需拍照')
  return labels
}

export function distanceToTask(task: Rules, position?: Coordinates | null): number | null {
  if (!task.radiusMeters || task.latitude == null || task.longitude == null || !position) return null
  return Math.round(distanceMeters({ latitude: Number(task.latitude), longitude: Number(task.longitude) }, position))
}

/** Mirrors the server-side checks so users see why they cannot sign in before submitting. */
export function signInBlocker(task: Rules, position: Coordinates | null | undefined, hasPhoto: boolean, now = new Date()): string | null {
  if (task.closed || (task.deadline && now > new Date(task.deadline))) return '签到已截止'
  if (task.radiusMeters) {
    if (!position) return '该签到需要定位，请先获取当前位置'
    const distance = distanceToTask(task, position)
    if (distance != null && distance > task.radiusMeters) return `距签到点约 ${distance} 米，需在 ${task.radiusMeters} 米范围内`
  }
  if (task.photoRequired && !hasPhoto) return '该签到需要上传现场照片'
  return null
}
