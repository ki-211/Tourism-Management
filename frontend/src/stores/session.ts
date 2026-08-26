import { defineStore } from 'pinia'
import { clearSession, currentUser, hasSession, saveSession, type SessionUser } from '@/services/session'

export const useSessionStore = defineStore('session', {
  state: () => ({ user: currentUser() as SessionUser | null, loggedIn: hasSession() }),
  actions: {
    reload() { this.user = currentUser(); this.loggedIn = hasSession() },
    save(data: { accessToken: string; refreshToken: string; user: SessionUser }) {
      saveSession(data); this.user = data.user; this.loggedIn = true
    },
    clear() { clearSession(); this.user = null; this.loggedIn = false }
  }
})
