import { api } from './client'
import type {
  CashFlowResponse,
  FinancialHealthResponse,
  ForecastScenarioRequest,
  ForecastScenarioResponse,
  IntelligenceSummaryResponse,
  LiquidityForecastResponse,
  ObligationsRiskResponse,
  RevenueAtRiskResponse,
  RiskSignalsResponse,
} from '../types/api'

export const getFinancialHealth = async (): Promise<FinancialHealthResponse> =>
  (await api.get<FinancialHealthResponse>('/api/intelligence/financial-health')).data

export const getCashFlow = async (period?: string): Promise<CashFlowResponse> =>
  (await api.get<CashFlowResponse>('/api/intelligence/cash-flow', { params: period ? { period } : {} })).data

export const getObligationRisks = async (): Promise<ObligationsRiskResponse> =>
  (await api.get<ObligationsRiskResponse>('/api/intelligence/obligations')).data

export const getForecast = async (days = 7): Promise<LiquidityForecastResponse> =>
  (await api.get<LiquidityForecastResponse>('/api/intelligence/forecast', { params: { days } })).data

export const simulateForecastScenario = async (request: ForecastScenarioRequest): Promise<ForecastScenarioResponse> =>
  (await api.post<ForecastScenarioResponse>('/api/intelligence/forecast/scenario', request)).data

export const getRiskSignals = async (): Promise<RiskSignalsResponse> =>
  (await api.get<RiskSignalsResponse>('/api/intelligence/risk-signals')).data

export const getRevenueAtRisk = async (): Promise<RevenueAtRiskResponse> =>
  (await api.get<RevenueAtRiskResponse>('/api/intelligence/revenue-at-risk')).data

export const getIntelligenceSummary = async (): Promise<IntelligenceSummaryResponse> =>
  (await api.get<IntelligenceSummaryResponse>('/api/intelligence/summary')).data
