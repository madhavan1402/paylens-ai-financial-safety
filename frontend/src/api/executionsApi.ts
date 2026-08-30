import { api } from './client'
import type { ExecutionRequest, ExecutionResponse, ExecutionSummary } from '../types/api'

export const executeDecision = async (request: ExecutionRequest): Promise<ExecutionResponse> =>
  (await api.post<ExecutionResponse>('/api/executions', request)).data

export const getExecutions = async (status?: string): Promise<{ executions: ExecutionSummary[] }> =>
  (await api.get<{ executions: ExecutionSummary[] }>('/api/executions', {
    params: status ? { status } : {}
  })).data

export const getExecutionDetail = async (executionId: string): Promise<ExecutionResponse> =>
  (await api.get<ExecutionResponse>(`/api/executions/${executionId}`)).data

export const getDecisionExecution = async (decisionId: string): Promise<ExecutionResponse | null> => {
  try {
    return (await api.get<ExecutionResponse>(`/api/decisions/${decisionId}/execution`)).data
  } catch (err: any) {
    if (err.response?.status === 404) return null
    throw err
  }
}
