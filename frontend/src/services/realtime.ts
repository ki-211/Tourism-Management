import { api, API_BASE } from './api'

type Listener = (event: any) => void

export class ActivitySocket {
  private socket: UniApp.SocketTask | null = null
  private retries = 0
  private closed = false
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private listeners = new Set<Listener>()

  constructor(private activityId: number) {}
  on(listener: Listener) { this.listeners.add(listener); return () => this.listeners.delete(listener) }

  async connect() {
    if (this.closed) return
    const { ticket } = await api<{ ticket: string }>('/auth/ws-ticket', 'POST')
    if (this.closed) return
    const url = API_BASE.replace(/^http/, 'ws').replace(/\/api\/v1$/, '') + `/ws?ticket=${encodeURIComponent(ticket)}`
    this.socket = uni.connectSocket({ url, complete: () => undefined })
    this.socket.onOpen(() => {
      const reconnected = this.retries > 0
      this.retries = 0
      if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
      this.socket?.send({
        data: JSON.stringify({ type: 'SUBSCRIBE_ACTIVITY', activityId: this.activityId }),
        success: () => {
          if (reconnected) this.listeners.forEach(listener => listener({ type: 'SOCKET_RECONNECTED', activityId: this.activityId }))
        }
      })
    })
    this.socket.onMessage(message => {
      try {
        const event = JSON.parse(String(message.data))
        this.listeners.forEach(listener => listener(event))
      } catch {
        return
      }
    })
    this.socket.onClose(() => this.reconnect())
    this.socket.onError(() => this.reconnect())
  }

  private reconnect() {
    if (this.closed || this.reconnectTimer) return
    const delay = Math.min(30000, 1000 * 2 ** this.retries++)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect().catch(() => this.reconnect())
    }, delay)
  }

  close() {
    this.closed = true
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
    this.socket?.close({})
    this.socket = null
    this.listeners.clear()
  }
}
