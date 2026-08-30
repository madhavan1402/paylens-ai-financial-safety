import axios from 'axios';
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RegisterRequest, UserResponse } from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const loginApi = async (req: LoginRequest): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(`${API_BASE_URL}/auth/login`, req);
  return response.data;
};

export const registerApi = async (req: RegisterRequest): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(`${API_BASE_URL}/auth/register`, req);
  return response.data;
};

export const refreshTokenApi = async (refreshToken: string): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(`${API_BASE_URL}/auth/refresh`, { refreshToken });
  return response.data;
};

export const logoutApi = async (refreshToken?: string): Promise<void> => {
  await axios.post(`${API_BASE_URL}/auth/logout`, { refreshToken });
};

export const getCurrentUserApi = async (): Promise<UserResponse> => {
  const response = await axios.get<UserResponse>(`${API_BASE_URL}/auth/me`);
  return response.data;
};

export const changePasswordApi = async (req: ChangePasswordRequest): Promise<{ message: string }> => {
  const response = await axios.post<{ message: string }>(`${API_BASE_URL}/auth/change-password`, req);
  return response.data;
};
