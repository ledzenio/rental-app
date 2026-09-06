import { create } from 'zustand'
import type { AuthPayload, Role } from './types'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  email: string | null
  role: Role | null
  virtualBalance: number | null
  setAuth: (payload: AuthPayload) => void
  setEmail: (email: string) => void
  setBalance: (balance: number | null) => void
  clear: () => void
}

const initialToken = localStorage.getItem('accessToken')
const initialRefresh = localStorage.getItem('refreshToken')
const initialEmail = localStorage.getItem('email')
const initialRoleRaw = localStorage.getItem('role')
const initialRole: Role | null = initialRoleRaw ? (initialRoleRaw as Role) : null
const initialBalanceRaw = localStorage.getItem('virtualBalance')
const initialBalance = initialBalanceRaw !== null ? Number(initialBalanceRaw) : null

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: initialToken,
  refreshToken: initialRefresh,
  email: initialEmail,
  role: initialRole,
  virtualBalance: Number.isFinite(initialBalance) ? initialBalance : null,
  setAuth: (payload) => {
    localStorage.setItem('accessToken', payload.accessToken)
    localStorage.setItem('refreshToken', payload.refreshToken)
    localStorage.setItem('email', payload.email)
    localStorage.setItem('role', payload.role)
    localStorage.removeItem('virtualBalance')
    set({
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      email: payload.email,
      role: payload.role,
      virtualBalance: null,
    })
  },
  setEmail: (email) => {
    localStorage.setItem('email', email)
    set({ email })
  },
  setBalance: (balance) => {
    if (balance === null) {
      localStorage.removeItem('virtualBalance')
    } else {
      localStorage.setItem('virtualBalance', String(balance))
    }
    set({ virtualBalance: balance })
  },
  clear: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('email')
    localStorage.removeItem('role')
    localStorage.removeItem('virtualBalance')
    set({ accessToken: null, refreshToken: null, email: null, role: null, virtualBalance: null })
  },
}))

export function useRoles() {
  const role = useAuthStore((s) => s.role)
  return {
    isUser: role === 'USER',
    isManager: role === 'MANAGER',
    isSpecialist: role === 'SERVICE_SPECIALIST',
  }
}
