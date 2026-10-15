import { beforeEach, describe, expect, it, vi } from 'vitest'

const { apiMock } = vi.hoisted(() => ({ apiMock: vi.fn() }))
vi.mock('./api', () => ({
  api: apiMock,
  API_BASE: 'https://example.com/api/v1'
}))

import { ActivitySocket } from './realtime'

type Handler = (...args: any[]) => void
function socketTask() {
  const handlers: Record<string, Handler> = {}
  return {
    handlers,
    onOpen: vi.fn((handler: Handler) => { handlers.open = handler }),
    onMessage: vi.fn((handler: Handler) => { handlers.message = handler }),
    onClose: vi.fn((handler: Handler) => { handlers.close = handler }),
    onError: vi.fn((handler: Handler) => { handlers.error = handler }),
    send: vi.fn((options: any) => options.success?.()),
    close: vi.fn()
  }
}

describe('activity socket reconnect', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    apiMock.mockResolvedValue({ ticket: 'ticket' })
  })

  it('emits a reconciliation event only after a reconnect subscription succeeds', async () => {
    const first = socketTask()
    const second = socketTask()
    const connectSocket = vi.fn()
      .mockReturnValueOnce(first)
      .mockReturnValueOnce(second)
    vi.stubGlobal('uni', { connectSocket })
    const events: any[] = []
    const activitySocket = new ActivitySocket(12)
    activitySocket.on(event => events.push(event))

    await activitySocket.connect()
    first.handlers.open()
    expect(events).toEqual([])

    first.handlers.close()
    await vi.advanceTimersByTimeAsync(1000)
    await vi.waitFor(() => expect(connectSocket).toHaveBeenCalledTimes(2))
    second.handlers.open()

    expect(events).toContainEqual({ type: 'SOCKET_RECONNECTED', activityId: 12 })
    expect(second.send).toHaveBeenCalledWith(expect.objectContaining({
      data: JSON.stringify({ type: 'SUBSCRIBE_ACTIVITY', activityId: 12 })
    }))
    activitySocket.close()
  })

  it('stops reconnecting and reports revocation when the server closes with 4403', async () => {
    const task = socketTask()
    const connectSocket = vi.fn().mockReturnValue(task)
    vi.stubGlobal('uni', { connectSocket })
    const events: any[] = []
    const activitySocket = new ActivitySocket(12)
    activitySocket.on(event => events.push(event))

    await activitySocket.connect()
    task.handlers.open()
    task.handlers.close({ code: 4403, reason: 'ACCESS_REVOKED' })
    await vi.advanceTimersByTimeAsync(60000)

    expect(connectSocket).toHaveBeenCalledTimes(1)
    expect(events).toEqual([{ type: 'ACCESS_REVOKED', activityId: 12 }])
  })
})
