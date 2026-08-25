import axios from 'axios';
import type { ExecutionRequest, ExecutionResponse, ExecutionSummary } from '../types/api';

const API_BASE_URL = 'http://localhost:8080/api';

export const executeDecision = async (request: ExecutionRequest): Promise<ExecutionResponse> => {
  const response = await axios.post<ExecutionResponse>(`${API_BASE_URL}/executions`, request);
  return response.data;
};

export const getExecutions = async (status?: string): Promise<{ executions: ExecutionSummary[] }> => {
  const response = await axios.get<{ executions: ExecutionSummary[] }>(`${API_BASE_URL}/executions`, {
    params: status ? { status } : {},
  });
  return response.data;
};

export const getExecutionDetail = async (executionId: string): Promise<ExecutionResponse> => {
  const response = await axios.get<ExecutionResponse>(`${API_BASE_URL}/executions/${executionId}`);
  return response.data;
};

export const getDecisionExecution = async (decisionId: string): Promise<ExecutionResponse | null> => {
  try {
    const response = await axios.get<ExecutionResponse>(`${API_BASE_URL}/decisions/${decisionId}/execution`);
    return response.data;
  } catch (err: any) {
    if (err.response && err.response.status === 404) {
      return null;
    }
    throw err;
  }
};
