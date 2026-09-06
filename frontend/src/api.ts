import axios from 'axios'
import { useAuthStore } from './authStore'
import type { AuthPayload } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8088/api/v1'

export const api = axios.create({
  baseURL: API_BASE_URL,
})

const authApi = axios.create({
  baseURL: API_BASE_URL,
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshPromise: Promise<string | null> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as (typeof error.config & { _retry?: boolean })

    if (!error.response || error.response.status !== 401 || originalRequest?._retry) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    const { refreshToken, setAuth, clear } = useAuthStore.getState()
    if (!refreshToken) {
      clear()
      return Promise.reject(error)
    }

    if (!refreshPromise) {
      refreshPromise = authApi
        .post<AuthPayload>('/auth/refresh', { refreshToken })
        .then((response) => {
          setAuth(response.data)
          return response.data.accessToken
        })
        .catch(() => {
          clear()
          return null
        })
        .finally(() => {
          refreshPromise = null
        })
    }

    const newAccessToken = await refreshPromise
    if (!newAccessToken) return Promise.reject(error)

    originalRequest.headers = originalRequest.headers ?? {}
    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`

    return api(originalRequest)
  }
)
