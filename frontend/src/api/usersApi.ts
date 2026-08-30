import { api } from './client'
import type { CreateUserRequest, UserResponse, UserRole, UserStatus } from '../types/api'

export const getUsersApi = async (): Promise<UserResponse[]> =>
  (await api.get<UserResponse[]>('/api/users')).data

export const createUserApi = async (req: CreateUserRequest): Promise<UserResponse> =>
  (await api.post<UserResponse>('/api/users', req)).data

export const updateUserRoleApi = async (userId: string, role: UserRole): Promise<UserResponse> =>
  (await api.put<UserResponse>(`/api/users/${userId}/role`, { role })).data

export const updateUserStatusApi = async (userId: string, status: UserStatus): Promise<UserResponse> =>
  (await api.put<UserResponse>(`/api/users/${userId}/status`, { status })).data
