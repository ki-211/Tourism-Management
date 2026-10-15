import { beforeEach, describe, expect, it, vi } from 'vitest'

type Reply = { statusCode: number; data?: unknown } | { fail: string }
const storage: Record<string, unknown> = {}
const replies: Reply[] = []
const uniMock = {
  getStorageSync: vi.fn((key: string) => storage[key]),
  setStorageSync: vi.fn((key: string, value: unknown) => { storage[key] = value }),
  removeStorageSync: vi.fn((key: string) => { delete storage[key] }),
  request: vi.fn((options: any) => {
    const reply = replies.shift()
    if (!reply) throw new Error(`unexpected request ${options.url}`)
    if ('fail' in reply) options.fail({ errMsg: reply.fail })
    else options.success(reply)
  }),
  showToast: vi.fn(),
  reLaunch: vi.fn(),
  $emit: vi.fn()
}

const ok = (data: unknown) => ({ statusCode: 200, data: { code: 'OK', message: 'ok', data } })
const fail = (statusCode: number, code = 'ERR') => ({ statusCode, data: { code, message: code } })

describe('api token refresh', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    replies.length = 0
    Object.keys(storage).forEach(key => delete storage[key])
    Object.assign(storage, { 'tourism.accessToken': 'old', 'tourism.refreshToken': 'refresh', 'tourism.user': { id: 1 } })
    vi.stubGlobal('uni', uniMock)
  })

  it('retries with the renewed token', async () => {
    replies.push(fail(401), ok({ accessToken: 'new', refreshToken: 'r2', user: { id: 1 } }), ok({ value: 1 }))
    const { api } = await import('./api')
    await expect(api('/things')).resolves.toEqual({ value: 1 })
    expect(uniMock.request.mock.calls[2][0].header.Authorization).toBe('Bearer new')
    expect(uniMock.reLaunch).not.toHaveBeenCalled()
  })

  it('keeps the session when the refresh request hits a network error', async () => {
    replies.push(fail(401), { fail: 'request:fail timeout' })
    const { api } = await import('./api')
    await expect(api('/things')).rejects.toMatchObject({ statusCode: 0 })
    expect(uniMock.reLaunch).not.toHaveBeenCalled()
    expect(storage['tourism.refreshToken']).toBe('refresh')
  })

  it('keeps the session when the retried request fails with a business error', async () => {
    replies.push(fail(401), ok({ accessToken: 'new', refreshToken: 'r2', user: { id: 1 } }), fail(409, 'ALREADY_JOINED'))
    const { api } = await import('./api')
    await expect(api('/things', 'POST')).rejects.toMatchObject({ statusCode: 409 })
    expect(uniMock.reLaunch).not.toHaveBeenCalled()
  })

  it('logs out when the refresh token itself is rejected', async () => {
    replies.push(fail(401), fail(401, 'INVALID_REFRESH_TOKEN'))
    const { api } = await import('./api')
    await expect(api('/things')).rejects.toMatchObject({ statusCode: 401 })
    expect(uniMock.reLaunch).toHaveBeenCalledWith({ url: '/pages/login/login' })
    expect(storage['tourism.refreshToken']).toBeUndefined()
  })
})
