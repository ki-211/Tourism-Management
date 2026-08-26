import { accessToken, clearSession, refreshToken, saveSession } from './session'
import type { ApiEnvelope } from './types'

export const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '')
let refreshing: Promise<string> | null = null
type ApiMethod = NonNullable<UniApp.RequestOptions['method']> | 'PATCH'

function raw<T>(path: string, method: ApiMethod, data?: unknown, token = accessToken()): Promise<T> {
  return new Promise((resolve, reject) => uni.request({
    url: API_BASE + path, method: method as UniApp.RequestOptions['method'], data: data as UniApp.RequestOptions['data'],
    header: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    success: response => {
      const body = response.data as ApiEnvelope<T>
      if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 'OK') resolve(body.data)
      else reject({ statusCode: response.statusCode, code: body?.code, message: body?.message || '请求失败' })
    },
    fail: error => reject({ statusCode: 0, message: error.errMsg || '网络连接失败' })
  }))
}

async function renew() {
  if (!refreshing) refreshing = raw<any>('/auth/refresh', 'POST', { refreshToken: refreshToken() }, '')
    .then(data => { saveSession(data); return data.accessToken as string }).finally(() => { refreshing = null })
  return refreshing
}

function expireSession(error: unknown): never {
  clearSession(); uni.reLaunch({ url: '/pages/login/login' }); throw error
}

export async function api<T>(path: string, method: ApiMethod = 'GET', data?: unknown): Promise<T> {
  try { return await raw<T>(path, method, data) }
  catch (error: any) {
    if (error.statusCode === 401 && refreshToken() && !path.startsWith('/auth/')) {
      try { return await raw<T>(path, method, data, await renew()) } catch (retryError) { expireSession(retryError) }
    }
    if (error.statusCode !== 401) uni.showToast({ title: error.message || '请求失败', icon: 'none' })
    throw error
  }
}

function uploadRaw<T>(path: string, filePath: string, fields: Record<string, string>, token: string): Promise<T> {
  return new Promise((resolve, reject) => uni.uploadFile({
    url: API_BASE + path, filePath, name: 'file', formData: fields, header: { Authorization: `Bearer ${token}` },
    success: response => {
      const body = JSON.parse(response.data) as ApiEnvelope<T>
      if (response.statusCode < 300 && body.code === 'OK') resolve(body.data)
      else reject({ statusCode: response.statusCode, code: body.code, message: body.message || '上传失败' })
    }, fail: error => reject({ statusCode: 0, message: error.errMsg || '上传失败' })
  }))
}

export async function upload<T>(path: string, filePath: string, fields: Record<string, string> = {}): Promise<T> {
  try { return await uploadRaw(path, filePath, fields, accessToken()) }
  catch (error: any) {
    if (error.statusCode === 401 && refreshToken()) {
      try { return await uploadRaw(path, filePath, fields, await renew()) } catch (retryError) { expireSession(retryError) }
    }
    uni.showToast({ title: error.message || '上传失败', icon: 'none' }); throw error
  }
}
