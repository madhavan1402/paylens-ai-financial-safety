import axios from 'axios'

/**
 * Centralized API client for PayLens.
 *
 * - Single axios instance with the correct base URL.
 * - JWT Bearer token attached from localStorage on every request.
 * - 401 handling: clears token and redirects to login.
 * - All API modules import this client — no duplicated base URLs.
 */
const BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const api = axios.create({
  baseURL: BASE,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

// ── Request interceptor: attach JWT ──────────────────────────────────────────
api.interceptors.request.use(config => {
  const token = localStorage.getItem('paylens_access_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor: handle 401 gracefully ──────────────────────────────
api.interceptors.response.use(
  res => res,
  async error => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      const refreshToken = localStorage.getItem('paylens_refresh_token')
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${BASE}/api/auth/refresh`, { refreshToken })
          localStorage.setItem('paylens_access_token', data.accessToken)
          localStorage.setItem('paylens_refresh_token', data.refreshToken)
          localStorage.setItem('paylens_user', JSON.stringify(data.user))
          localStorage.setItem('paylens_merchant', data.merchantName)
          original.headers.Authorization = `Bearer ${data.accessToken}`
          return api(original)
        } catch {
          // refresh failed — clear auth and redirect
          localStorage.removeItem('paylens_access_token')
          localStorage.removeItem('paylens_refresh_token')
          localStorage.removeItem('paylens_user')
          localStorage.removeItem('paylens_merchant')
          window.location.href = '/login'
        }
      } else {
        localStorage.removeItem('paylens_access_token')
        localStorage.removeItem('paylens_refresh_token')
        localStorage.removeItem('paylens_user')
        localStorage.removeItem('paylens_merchant')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)
