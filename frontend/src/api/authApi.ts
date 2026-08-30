import { api } from './client'
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RegisterRequest, UserResponse } from '../types/api'

export const loginApi = async (req: LoginRequest): Promise<AuthResponse> =>
  (await api.post<AuthResponse>('/api/auth/login', req)).data

export const registerApi = async (req: RegisterRequest): Promise<AuthResponse> =>
  (await api.post<AuthResponse>('/api/auth/register', req)).data

export const refreshTokenApi = async (refreshToken: string): Promise<AuthResponse> =>
  (await api.post<AuthResponse>('/api/auth/refresh', { refreshToken })).data

export const logoutApi = async (refreshToken?: string): Promise<void> => {
  await api.post('/api/auth/logout', { refreshToken })
}

export const getCurrentUserApi = async (): Promise<UserResponse> =>
  (await api.get<UserResponse>('/api/auth/me')).data

export const changePasswordApi = async (req: ChangePasswordRequest): Promise<{ message: string }> =>
  (await api.post<{ message: string }>('/api/auth/change-password', req)).data
