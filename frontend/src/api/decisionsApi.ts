import { api } from './client'
import type { AuditEvent, DecisionDetail, DecisionSummary, GovernanceResponse } from '../types/api'

export const getDecisions = async (status?: string, limit: number = 50): Promise<DecisionSummary[]> => {
  const params = new URLSearchParams()
  if (status) params.append('status', status)
  params.append('limit', limit.toString())
  const res = await api.get<{ decisions: DecisionSummary[] }>(`/api/decisions?${params.toString()}`)
  return res.data.decisions
}

export const getDecisionDetail = async (id: string): Promise<DecisionDetail> => {
  const res = await api.get<DecisionDetail>(`/api/decisions/${id}`)
  return res.data
}

export const approveDecision = async (id: string, actorId: string = 'demo-user', comment: string = 'Reviewed and approved.'): Promise<GovernanceResponse> => {
  const res = await api.post<GovernanceResponse>(`/api/decisions/${id}/approve`, { actorId, comment })
  return res.data
}

export const rejectDecision = async (id: string, actorId: string = 'demo-user', comment: string = 'Rejected after review.'): Promise<GovernanceResponse> => {
  const res = await api.post<GovernanceResponse>(`/api/decisions/${id}/reject`, { actorId, comment })
  return res.data
}

export const getAuditEvents = async (decisionId?: string): Promise<AuditEvent[]> => {
  const params = new URLSearchParams()
  if (decisionId) params.append('decisionId', decisionId)
  const res = await api.get<{ events: AuditEvent[] }>(`/api/audit?${params.toString()}`)
  return res.data.events
}
