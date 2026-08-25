import axios from 'axios';
import type {
  ReconciliationRecord,
  ReconciliationSummary,
  ReliabilityMetrics
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const triggerReconciliation = async (executionId: string): Promise<ReconciliationRecord> => {
  const response = await axios.post<ReconciliationRecord>(`${API_BASE_URL}/executions/${executionId}/reconcile`);
  return response.data;
};

export const getLatestReconciliation = async (executionId: string): Promise<ReconciliationRecord> => {
  const response = await axios.get<ReconciliationRecord>(`${API_BASE_URL}/executions/${executionId}/reconciliation`);
  return response.data;
};

export const getReconciliationHistory = async (executionId?: string, status?: string): Promise<ReconciliationSummary[]> => {
  const params: Record<string, string> = {};
  if (executionId) params.executionId = executionId;
  if (status && status !== 'ALL') params.status = status;

  const response = await axios.get<{ reconciliations: ReconciliationSummary[] }>(`${API_BASE_URL}/reconciliations`, { params });
  return response.data.reconciliations;
};

export const getReliabilityMetrics = async (): Promise<ReliabilityMetrics> => {
  const response = await axios.get<ReliabilityMetrics>(`${API_BASE_URL}/reconciliations/metrics`);
  return response.data;
};
