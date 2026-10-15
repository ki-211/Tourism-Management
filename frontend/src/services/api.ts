import { accessToken, clearSession, refreshToken, saveSession } from './session'
import type { ApiEnvelope } from './types'

export const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '')
let refreshing: Promise<string> | null = null
type ApiMethod = NonNullable<UniApp.RequestOptions['method']> | 'PATCH'
type ApiOptions = { silent?: boolean }

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
  uni.$emit('session-expired'); clearSession(); uni.reLaunch({ url: '/pages/login/login' }); throw error
}

// Only a 401 means the session is really gone; network failures or business errors on the retried
// request must surface as ordinary errors instead of logging the user out.
async function withRefresh<T>(attempt: (token?: string) => Promise<T>): Promise<T> {
  try { return await attempt() }
  catch (error: any) {
    if (error?.statusCode !== 401 || !refreshToken()) throw error
    let token: string
    try { token = await renew() }
    catch (refreshError: any) { if (refreshError?.statusCode === 401) expireSession(refreshError); throw refreshError }
    try { return await attempt(token) }
    catch (retryError: any) { if (retryError?.statusCode === 401) expireSession(retryError); throw retryError }
  }
}

export async function api<T>(path: string, method: ApiMethod = 'GET', data?: unknown, options: ApiOptions = {}): Promise<T> {
  const attempt = (token?: string) => raw<T>(path, method, data, token ?? accessToken())
  try { return path.startsWith('/auth/') ? await attempt() : await withRefresh(attempt) }
  catch (error: any) {
    if (error?.statusCode !== 401 && !options.silent) uni.showToast({ title: error?.message || '请求失败', icon: 'none' })
    throw error
  }
}

export type UploadProgress = (progress: number) => void

function uploadRaw<T>(path: string, filePath: string, fields: Record<string, string>, token: string, onProgress?: UploadProgress): Promise<T> {
  return new Promise((resolve, reject) => {
    const task = uni.uploadFile({
    url: API_BASE + path, filePath, name: 'file', formData: fields, header: { Authorization: `Bearer ${token}` },
    success: response => {
      let body: ApiEnvelope<T> | null = null
      try { body = JSON.parse(response.data) as ApiEnvelope<T> } catch { body = null }
      if (response.statusCode < 300 && body?.code === 'OK') resolve(body.data)
      else reject({ statusCode: response.statusCode, code: body?.code, message: body?.message || (response.statusCode === 413 ? '图片不能超过 10MB' : '上传失败') })
    }, fail: error => reject({ statusCode: 0, message: error.errMsg || '上传失败' })
    })
    task.onProgressUpdate?.(result => onProgress?.(result.progress))
  })
}

export async function upload<T>(path: string, filePath: string, fields: Record<string, string> = {}, onProgress?: UploadProgress): Promise<T> {
  try { return await withRefresh(token => uploadRaw<T>(path, filePath, fields, token ?? accessToken(), onProgress)) }
  catch (error: any) {
    if (error?.statusCode !== 401) uni.showToast({ title: error?.message || '上传失败', icon: 'none' })
    throw error
  }
}
