import axios from 'axios';
import type { CreateUserRequest, UserResponse, UserRole, UserStatus } from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const getUsersApi = async (): Promise<UserResponse[]> => {
  const response = await axios.get<UserResponse[]>(`${API_BASE_URL}/users`);
  return response.data;
};

export const createUserApi = async (req: CreateUserRequest): Promise<UserResponse> => {
  const response = await axios.post<UserResponse>(`${API_BASE_URL}/users`, req);
  return response.data;
};

export const updateUserRoleApi = async (userId: string, role: UserRole): Promise<UserResponse> => {
  const response = await axios.put<UserResponse>(`${API_BASE_URL}/users/${userId}/role`, { role });
  return response.data;
};

export const updateUserStatusApi = async (userId: string, status: UserStatus): Promise<UserResponse> => {
  const response = await axios.put<UserResponse>(`${API_BASE_URL}/users/${userId}/status`, { status });
  return response.data;
};
