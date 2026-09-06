import { create } from 'zustand'

export type NoticeVariant = 'success' | 'error' | 'info' | 'warning'

export type Notice = {
  id: string
  variant: NoticeVariant
  message: string
}

export const NOTICE_TITLE: Record<NoticeVariant, string> = {
  success: 'Готово',
  error: 'Ошибка',
  info: 'Подсказка',
  warning: 'Внимание',
}

const AUTO_DISMISS_MS: Record<NoticeVariant, number> = {
  success: 5200,
  info: 6800,
  warning: 9000,
  error: 15000,
}

const MAX_NOTICES = 5

type NoticeState = {
  notices: Notice[]
  push: (variant: NoticeVariant, message: string) => void
  dismiss: (id: string) => void
}

const timers = new Map<string, ReturnType<typeof setTimeout>>()

function clearTimer(id: string) {
  const t = timers.get(id)
  if (t) {
    clearTimeout(t)
    timers.delete(id)
  }
}

export const useNoticeStore = create<NoticeState>((set, get) => ({
  notices: [],
  dismiss: (id) => {
    clearTimer(id)
    set((s) => ({ notices: s.notices.filter((n) => n.id !== id) }))
  },
  push: (variant, message) => {
    const text = message.trim() || ' '
    const id =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `n-${Date.now()}-${Math.random().toString(16).slice(2)}`
    const notice: Notice = { id, variant, message: text }
    set((s) => {
      const combined = [...s.notices, notice]
      if (combined.length > MAX_NOTICES) {
        combined.slice(0, combined.length - MAX_NOTICES).forEach((n) => clearTimer(n.id))
      }
      return { notices: combined.slice(-MAX_NOTICES) }
    })
    const ms = AUTO_DISMISS_MS[variant]
    clearTimer(id)
    timers.set(
      id,
      setTimeout(() => {
        get().dismiss(id)
      }, ms),
    )
  },
}))

export function notifySuccess(message: string) {
  useNoticeStore.getState().push('success', message)
}

export function notifyError(message: string) {
  useNoticeStore.getState().push('error', message)
}

export function notifyInfo(message: string) {
  useNoticeStore.getState().push('info', message)
}

export function notifyWarning(message: string) {
  useNoticeStore.getState().push('warning', message)
}
