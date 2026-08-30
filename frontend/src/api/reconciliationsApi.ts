import { api } from './client'
import type { ReconciliationRecord, ReconciliationSummary, ReliabilityMetrics } from '../types/api'

export const triggerReconciliation = async (executionId: string): Promise<ReconciliationRecord> =>
  (await api.post<ReconciliationRecord>(`/api/executions/${executionId}/reconcile`)).data

export const getLatestReconciliation = async (executionId: string): Promise<ReconciliationRecord> =>
  (await api.get<ReconciliationRecord>(`/api/executions/${executionId}/reconciliation`)).data

export const getReconciliationHistory = async (executionId?: string, status?: string): Promise<ReconciliationSummary[]> => {
  const params: Record<string, string> = {}
  if (executionId) params.executionId = executionId
  if (status && status !== 'ALL') params.status = status
  return (await api.get<{ reconciliations: ReconciliationSummary[] }>('/api/reconciliations', { params })).data.reconciliations
}

export const getReliabilityMetrics = async (): Promise<ReliabilityMetrics> =>
  (await api.get<ReliabilityMetrics>('/api/reconciliations/metrics')).data
