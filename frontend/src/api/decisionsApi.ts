import { api } from './client'
import type { AuditEvent, DecisionDetail, DecisionSummary, GovernanceResponse } from '../types/api'

export const getDecisions = async (status?: string, limit = 50): Promise<DecisionSummary[]> => {
  const params = new URLSearchParams()
  if (status) params.append('status', status)
  params.append('limit', limit.toString())
  return (await api.get<{ decisions: DecisionSummary[] }>(`/api/decisions?${params.toString()}`)).data.decisions
}

export const getDecisionDetail = async (id: string): Promise<DecisionDetail> =>
  (await api.get<DecisionDetail>(`/api/decisions/${id}`)).data

export const approveDecision = async (
  id: string,
  actorId = 'demo-user',
  comment = 'Reviewed and approved.'
): Promise<GovernanceResponse> =>
  (await api.post<GovernanceResponse>(`/api/decisions/${id}/approve`, { actorId, comment })).data

export const rejectDecision = async (
  id: string,
  actorId = 'demo-user',
  comment = 'Rejected after review.'
): Promise<GovernanceResponse> =>
  (await api.post<GovernanceResponse>(`/api/decisions/${id}/reject`, { actorId, comment })).data

export const getAuditEvents = async (decisionId?: string): Promise<AuditEvent[]> => {
  const params = new URLSearchParams()
  if (decisionId) params.append('decisionId', decisionId)
  return (await api.get<{ events: AuditEvent[] }>(`/api/audit?${params.toString()}`)).data.events
}
