import { api } from './client'
import type {
  MonitoringCycleResponse,
  MonitoringStatusResponse,
  RiskEventResponse,
  RiskEventSource,
  RiskEventStatus,
  RiskSeverity,
  RiskSignalType,
} from '../types/api'

export const runMonitoringCycle = async (): Promise<MonitoringCycleResponse> =>
  (await api.post<MonitoringCycleResponse>('/api/risk-monitoring/run')).data

export const getMonitoringStatus = async (): Promise<MonitoringStatusResponse> =>
  (await api.get<MonitoringStatusResponse>('/api/risk-monitoring/status')).data

export const getRiskEvents = async (params?: {
  status?: RiskEventStatus
  severity?: RiskSeverity
  type?: RiskSignalType
  source?: RiskEventSource
}): Promise<RiskEventResponse[]> =>
  (await api.get<RiskEventResponse[]>('/api/risk-events', { params })).data

export const getRiskEventDetail = async (riskEventId: string): Promise<RiskEventResponse> =>
  (await api.get<RiskEventResponse>(`/api/risk-events/${riskEventId}`)).data

export const acknowledgeRiskEvent = async (riskEventId: string): Promise<RiskEventResponse> =>
  (await api.post<RiskEventResponse>(`/api/risk-events/${riskEventId}/acknowledge`)).data

export const dismissRiskEvent = async (riskEventId: string, reason?: string): Promise<RiskEventResponse> =>
  (await api.post<RiskEventResponse>(`/api/risk-events/${riskEventId}/dismiss`, { reason })).data

export const resolveRiskEvent = async (riskEventId: string, reason?: string): Promise<RiskEventResponse> =>
  (await api.post<RiskEventResponse>(`/api/risk-events/${riskEventId}/resolve`, { reason })).data
