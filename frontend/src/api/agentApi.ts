import { api } from './client'
import type { AgentAnalysisResponse } from '../types/api'

export const analyzeAction = async (message: string): Promise<AgentAnalysisResponse> =>
  (await api.post<AgentAnalysisResponse>('/api/agent/analyze', { message })).data
