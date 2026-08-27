import { defineStore } from 'pinia'
import { api } from '@/services/api'
import { hasSession } from '@/services/session'
import type { Location } from '@/services/types'
import { distanceMeters, shouldUploadLocation, type Coordinates } from '@/utils/location'

export type SharingStatus = 'idle' | 'starting' | 'active' | 'paused' | 'stopping' | 'error'
type PersistedSharing = { activityId: number; activityEndTime: string; startedAt: string }
type SentPosition = Coordinates & { sentAt: number }

const STORAGE_KEY = 'tourism.locationSharing.v1'
const POLL_INTERVAL = 15000
let pollTimer: ReturnType<typeof setInterval> | null = null
let endTimer: ReturnType<typeof setTimeout> | null = null
let locationListener: ((result: any) => void) | null = null
let visibilityListener: (() => void) | null = null

function persisted(): PersistedSharing | null {
  const value = uni.getStorageSync(STORAGE_KEY)
  return value && typeof value === 'object' ? value as PersistedSharing : null
}

export const useLocationSharingStore = defineStore('locationSharing', {
  state: () => ({
    status: 'idle' as SharingStatus,
    activityId: 0,
    activityEndTime: '',
    startedAt: '',
    current: null as Location | null,
    lastSent: null as SentPosition | null,
    lastGeocoded: null as (Coordinates & { resolvedAt: number; address: string }) | null,
    lastGeocodeAttemptAt: 0,
    message: '',
    canOpenSettings: false,
    permissionDenials: 0,
    uploadBusy: false,
    mode: 'live' as 'live' | 'fixed',
    initialized: false
  }),
  getters: {
    sharing: state => ['starting', 'active', 'paused', 'error'].includes(state.status) && state.activityId > 0
  },
  actions: {
    async initialize() {
      if (this.initialized) return
      this.initialized = true
      const saved = persisted()
      if (!saved || !hasSession() || new Date(saved.activityEndTime).getTime() <= Date.now()) {
        uni.removeStorageSync(STORAGE_KEY)
        return
      }
      Object.assign(this, saved)
      this.status = 'starting'
      this.mode = 'live'
      try { await this.startRuntime(true) }
      catch (reason: any) { this.fail(reason, '位置共享恢复失败，请重新开启') }
    },
    async start(activityId: number, activityEndTime: string) {
      if (this.status === 'starting' || this.status === 'stopping') return
      if (this.activityId === activityId && (this.status === 'active' || this.status === 'paused')) return
      if (new Date(activityEndTime).getTime() <= Date.now()) {
        this.fail(null, '活动已结束，无法共享位置')
        return
      }
      if (this.sharing && this.activityId !== activityId) await this.stop(true)
      this.status = 'starting'
      this.message = ''
      this.canOpenSettings = false
      this.activityId = activityId
      this.activityEndTime = activityEndTime
      this.startedAt = new Date().toISOString()
      uni.setStorageSync(STORAGE_KEY, { activityId, activityEndTime, startedAt: this.startedAt } satisfies PersistedSharing)
      try { await this.startRuntime(false) }
      catch (reason: any) { this.fail(reason, this.permissionMessage(reason)) }
    },
    async startAt(activityId: number, activityEndTime: string, coordinates: Coordinates, address: string) {
      if (this.status === 'starting' || this.status === 'stopping') return
      if (new Date(activityEndTime).getTime() <= Date.now()) {
        this.fail(null, '活动已结束，无法共享位置')
        throw new Error('ACTIVITY_ENDED')
      }
      if (this.sharing && this.activityId !== activityId) await this.stop(true)
      this.teardownRuntime()
      this.activityId = activityId
      this.activityEndTime = activityEndTime
      this.startedAt = new Date().toISOString()
      this.status = 'active'
      this.mode = 'fixed'
      this.message = '当前共享的是固定位置，不会自动跟随移动'
      uni.setStorageSync(STORAGE_KEY, { activityId, activityEndTime, startedAt: this.startedAt } satisfies PersistedSharing)
      this.scheduleActivityEnd()
      try { await this.handlePosition({ ...coordinates, address }, true) }
      catch (reason: any) { this.fail(reason, '固定位置共享失败，请重试'); throw reason }
      this.clearPolling()
      pollTimer = setInterval(() => {
        if (this.current) this.handlePosition(this.current, true).catch((reason: any) => this.fail(reason, '固定位置续期失败，正在等待重试'))
      }, 60000)
    },
    async startRuntime(restoring: boolean) {
      this.scheduleActivityEnd()
      // #ifdef MP-WEIXIN
      await this.startWeixin(restoring)
      // #endif
      // #ifdef H5
      this.startH5()
      // #endif
      // #ifndef MP-WEIXIN
      // #ifndef H5
      await this.locateNow(true)
      this.startPolling()
      // #endif
      // #endif
    },
    async startWeixin(restoring: boolean) {
      const client = wx as any
      if (!restoring) await new Promise<void>((resolve, reject) => client.authorize({ scope: 'scope.userLocationBackground', success: resolve, fail: reject }))
      await new Promise<void>((resolve, reject) => client.startLocationUpdateBackground({ success: resolve, fail: reject }))
      if (locationListener) client.offLocationChange?.(locationListener)
      locationListener = (result: any) => this.handlePosition(result).catch((reason: any) => this.fail(reason, '位置上报失败，正在等待重试'))
      client.onLocationChange(locationListener)
      this.status = 'active'
      this.startPolling()
      await this.locateNow(true)
    },
    startH5() {
      if (!visibilityListener) {
        visibilityListener = () => {
          if (document.hidden) this.pauseH5()
          else this.resumeH5()
        }
        document.addEventListener('visibilitychange', visibilityListener)
      }
      if (document.hidden) this.pauseH5()
      else this.resumeH5()
    },
    pauseH5() {
      this.clearPolling()
      if (this.sharing) {
        this.status = 'paused'
        this.message = '页面处于后台，H5 位置共享已暂停；返回后会自动恢复'
      }
    },
    resumeH5() {
      if (!this.activityId || new Date(this.activityEndTime).getTime() <= Date.now()) return void this.stop(true)
      this.status = 'active'
      this.message = ''
      this.startPolling()
      this.locateNow(true).catch((reason: any) => this.fail(reason, this.permissionMessage(reason)))
    },
    startPolling() {
      this.clearPolling()
      pollTimer = setInterval(() => this.locateNow(false).catch(() => undefined), POLL_INTERVAL)
    },
    clearPolling() {
      if (pollTimer) clearInterval(pollTimer)
      pollTimer = null
    },
    async locateNow(force: boolean) {
      const result = await new Promise<any>((resolve, reject) => uni.getLocation({ type: 'gcj02', isHighAccuracy: true, success: resolve, fail: reject }))
      await this.handlePosition(result, force)
    },
    async handlePosition(result: any, force = false) {
      if (!this.activityId || this.uploadBusy) return
      if (new Date(this.activityEndTime).getTime() <= Date.now()) return void this.stop(true)
      const coordinates = { latitude: Number(result.latitude), longitude: Number(result.longitude) }
      if (!force && !shouldUploadLocation(this.lastSent, coordinates)) return
      this.uploadBusy = true
      try {
        const address = await this.resolveAddress(coordinates, String(result.address || ''))
        const uploaded = await api<Location>(`/activities/${this.activityId}/locations/me`, 'PUT', { ...coordinates, address })
        this.current = uploaded
        this.lastSent = { ...coordinates, sentAt: Date.now() }
        this.status = 'active'
        this.message = this.mode === 'fixed' ? '当前共享的是固定位置，不会自动跟随移动' : ''
      } catch (reason: any) {
        if (reason?.code === 'ACTIVITY_ENDED') await this.stop(true)
        else if (reason?.statusCode === 403) await this.stop(false)
        else throw reason
      } finally { this.uploadBusy = false }
    },
    async resolveAddress(coordinates: Coordinates, supplied: string) {
      if (supplied) return supplied
      const cached = this.lastGeocoded
      if (cached && Date.now() - cached.resolvedAt < 600000 && distanceMeters(cached, coordinates) < 200) return cached.address
      if (Date.now() - this.lastGeocodeAttemptAt < 600000) return cached?.address || ''
      this.lastGeocodeAttemptAt = Date.now()
      try {
        const place = await api<{ address: string }>(`/geocoding/reverse?latitude=${coordinates.latitude}&longitude=${coordinates.longitude}`, 'GET', undefined, { silent: true })
        this.lastGeocoded = { ...coordinates, resolvedAt: Date.now(), address: place.address }
        return place.address
      } catch { return cached?.address || '' }
    },
    async stop(removeRemote = true) {
      if (this.status === 'stopping') return
      const activityId = this.activityId
      this.status = 'stopping'
      this.teardownRuntime()
      if (removeRemote && activityId && hasSession()) {
        try { await api(`/activities/${activityId}/locations/me`, 'DELETE', undefined, { silent: true }) } catch { /* TTL provides the final privacy fallback. */ }
      }
      uni.removeStorageSync(STORAGE_KEY)
      this.$reset()
      this.initialized = true
    },
    teardownRuntime() {
      this.clearPolling()
      if (endTimer) clearTimeout(endTimer)
      endTimer = null
      // #ifdef MP-WEIXIN
      const client = wx as any
      if (locationListener) client.offLocationChange?.(locationListener)
      locationListener = null
      client.stopLocationUpdate?.({ fail: () => undefined })
      // #endif
      // #ifdef H5
      if (visibilityListener) document.removeEventListener('visibilitychange', visibilityListener)
      visibilityListener = null
      // #endif
    },
    scheduleActivityEnd() {
      if (endTimer) clearTimeout(endTimer)
      const delay = new Date(this.activityEndTime).getTime() - Date.now()
      if (delay <= 0) return void this.stop(true)
      endTimer = setTimeout(() => this.scheduleActivityEnd(), Math.min(delay, 2147483647))
    },
    permissionMessage(reason: any) {
      const text = String(reason?.errMsg || reason?.message || '')
      const denied = reason?.code === 1 || /deny|denied|auth|authorize|permission/i.test(text)
      this.canOpenSettings = false
      // #ifdef MP-WEIXIN
      this.canOpenSettings = denied
      // #endif
      if (denied) {
        this.permissionDenials += 1
        return this.permissionDenials === 1
          ? '首次定位授权未通过，可重新开启或进入设置授权'
          : '定位权限已被拒绝，请在设置中允许位置权限'
      }
      if (/network|timeout|offline/i.test(text)) return '网络异常，暂时无法获取位置，请联网后重试'
      if (/system|service|location/i.test(text)) return '系统定位服务未开启，请开启系统定位后重试'
      return '暂时无法获取位置，请检查网络和定位服务'
    },
    openSettings() { uni.openSetting({}) },
    fail(reason: any, message: string) {
      this.status = 'error'
      this.message = message
      if (reason) console.warn('Location sharing error', { activityId: this.activityId, message: reason?.errMsg || reason?.message || String(reason) })
    }
  }
})
