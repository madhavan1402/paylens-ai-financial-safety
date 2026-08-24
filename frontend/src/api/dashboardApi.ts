import { api } from './client'
import type { DashboardResponse, FinancialStateResponse } from '../types/api'
export const getDashboard = async () => (await api.get<DashboardResponse>('/api/dashboard')).data
export const getFinancialState = async () => (await api.get<FinancialStateResponse>('/api/financial-state')).data
