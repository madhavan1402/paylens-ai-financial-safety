import axios from 'axios';
import type {
  MonitoringCycleResponse,
  MonitoringStatusResponse,
  RiskEventResponse,
  RiskEventSource,
  RiskEventStatus,
  RiskSeverity,
  RiskSignalType
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const runMonitoringCycle = async (): Promise<MonitoringCycleResponse> => {
  const response = await axios.post<MonitoringCycleResponse>(`${API_BASE_URL}/risk-monitoring/run`);
  return response.data;
};

export const getMonitoringStatus = async (): Promise<MonitoringStatusResponse> => {
  const response = await axios.get<MonitoringStatusResponse>(`${API_BASE_URL}/risk-monitoring/status`);
  return response.data;
};

export const getRiskEvents = async (params?: {
  status?: RiskEventStatus;
  severity?: RiskSeverity;
  type?: RiskSignalType;
  source?: RiskEventSource;
}): Promise<RiskEventResponse[]> => {
  const response = await axios.get<RiskEventResponse[]>(`${API_BASE_URL}/risk-events`, { params });
  return response.data;
};

export const getRiskEventDetail = async (riskEventId: string): Promise<RiskEventResponse> => {
  const response = await axios.get<RiskEventResponse>(`${API_BASE_URL}/risk-events/${riskEventId}`);
  return response.data;
};

export const acknowledgeRiskEvent = async (riskEventId: string): Promise<RiskEventResponse> => {
  const response = await axios.post<RiskEventResponse>(`${API_BASE_URL}/risk-events/${riskEventId}/acknowledge`);
  return response.data;
};

export const dismissRiskEvent = async (riskEventId: string, reason?: string): Promise<RiskEventResponse> => {
  const response = await axios.post<RiskEventResponse>(`${API_BASE_URL}/risk-events/${riskEventId}/dismiss`, { reason });
  return response.data;
};

export const resolveRiskEvent = async (riskEventId: string, reason?: string): Promise<RiskEventResponse> => {
  const response = await axios.post<RiskEventResponse>(`${API_BASE_URL}/risk-events/${riskEventId}/resolve`, { reason });
  return response.data;
};
