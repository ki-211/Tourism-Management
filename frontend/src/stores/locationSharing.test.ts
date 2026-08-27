import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useLocationSharingStore } from './locationSharing'

const storage = {
  getStorageSync: vi.fn(),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn()
}

describe('location sharing lifecycle', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubGlobal('uni', storage)
    vi.stubGlobal('wx', { offLocationChange: vi.fn(), stopLocationUpdate: vi.fn() })
    setActivePinia(createPinia())
  })

  it('ignores a duplicate start for the same active activity', async () => {
    const store = useLocationSharingStore()
    store.status = 'active'
    store.activityId = 10
    store.activityEndTime = new Date(Date.now() + 60000).toISOString()
    const startRuntime = vi.spyOn(store, 'startRuntime')

    await store.start(10, store.activityEndTime)

    expect(startRuntime).not.toHaveBeenCalled()
  })

  it('pauses while H5 is hidden and uploads immediately after visibility returns', async () => {
    let listener: (() => void) | undefined
    const documentMock = {
      hidden: false,
      addEventListener: vi.fn((_name: string, callback: () => void) => { listener = callback }),
      removeEventListener: vi.fn()
    }
    vi.stubGlobal('document', documentMock)
    const store = useLocationSharingStore()
    store.status = 'active'
    store.activityId = 10
    store.activityEndTime = new Date(Date.now() + 60000).toISOString()
    const locateNow = vi.spyOn(store, 'locateNow').mockResolvedValue()

    store.startH5()
    documentMock.hidden = true
    listener?.()
    expect(store.status).toBe('paused')
    expect(store.message).toContain('已暂停')

    documentMock.hidden = false
    listener?.()
    expect(store.status).toBe('active')
    expect(locateNow).toHaveBeenLastCalledWith(true)
  })

  it('stops sharing when the activity end time arrives', async () => {
    const store = useLocationSharingStore()
    store.activityId = 10
    store.activityEndTime = new Date(Date.now() + 1000).toISOString()
    const stop = vi.spyOn(store, 'stop').mockResolvedValue()

    store.scheduleActivityEnd()
    await vi.advanceTimersByTimeAsync(1001)

    expect(stop).toHaveBeenCalledWith(true)
  })
})
