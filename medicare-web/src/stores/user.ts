import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, logout } from '../api/auth'
import type { SysUser } from '../types'

const STORAGE_KEY = 'medicare_user'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<SysUser | null>(null)
  const bootstrapping = ref(false)
  const lastSyncedAt = ref('')
  let bootstrapPromise: Promise<boolean> | null = null

  const isLoggedIn = computed(() => !!currentUser.value)

  function persistUser(user: SysUser | null) {
    if (!user) {
      sessionStorage.removeItem(STORAGE_KEY)
      return
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  }

  function loadFromStorage() {
    const saved = sessionStorage.getItem(STORAGE_KEY)
    if (!saved) {
      return false
    }
    try {
      currentUser.value = JSON.parse(saved)
      return true
    } catch {
      sessionStorage.removeItem(STORAGE_KEY)
      currentUser.value = null
      return false
    }
  }

  function setUser(user: SysUser) {
    currentUser.value = user
    lastSyncedAt.value = new Date().toISOString()
    persistUser(user)
  }

  function clearUser() {
    currentUser.value = null
    lastSyncedAt.value = ''
    persistUser(null)
  }

  async function bootstrap(force = false) {
    if (!force && currentUser.value) {
      return true
    }

    if (!force && bootstrapPromise) {
      return bootstrapPromise
    }

    bootstrapPromise = (async () => {
      bootstrapping.value = true
      try {
        if (!force && loadFromStorage()) {
          return true
        }
        const res = await getCurrentUser()
        setUser(res.data)
        return true
      } catch {
        clearUser()
        return false
      } finally {
        bootstrapping.value = false
        bootstrapPromise = null
      }
    })()

    return bootstrapPromise
  }

  async function logoutAndClear() {
    try {
      await logout()
    } catch {
      // Ignore logout network failures and clear client state anyway.
    } finally {
      clearUser()
    }
  }

  function hasRole(...roles: string[]) {
    if (!currentUser.value) {
      return false
    }
    return roles.includes(currentUser.value.role)
  }

  return {
    currentUser,
    bootstrapping,
    lastSyncedAt,
    isLoggedIn,
    setUser,
    clearUser,
    loadFromStorage,
    bootstrap,
    logoutAndClear,
    hasRole,
  }
})
