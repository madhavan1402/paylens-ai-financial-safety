import axios from 'axios';
import type {
  CashFlowResponse,
  FinancialHealthResponse,
  ForecastScenarioRequest,
  ForecastScenarioResponse,
  IntelligenceSummaryResponse,
  LiquidityForecastResponse,
  ObligationsRiskResponse,
  RevenueAtRiskResponse,
  RiskSignalsResponse
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const getFinancialHealth = async (): Promise<FinancialHealthResponse> => {
  const response = await axios.get<FinancialHealthResponse>(`${API_BASE_URL}/intelligence/financial-health`);
  return response.data;
};

export const getCashFlow = async (period?: string): Promise<CashFlowResponse> => {
  const response = await axios.get<CashFlowResponse>(`${API_BASE_URL}/intelligence/cash-flow`, {
    params: period ? { period } : {}
  });
  return response.data;
};

export const getObligationRisks = async (): Promise<ObligationsRiskResponse> => {
  const response = await axios.get<ObligationsRiskResponse>(`${API_BASE_URL}/intelligence/obligations`);
  return response.data;
};

export const getForecast = async (days: number = 7): Promise<LiquidityForecastResponse> => {
  const response = await axios.get<LiquidityForecastResponse>(`${API_BASE_URL}/intelligence/forecast`, {
    params: { days }
  });
  return response.data;
};

export const simulateForecastScenario = async (request: ForecastScenarioRequest): Promise<ForecastScenarioResponse> => {
  const response = await axios.post<ForecastScenarioResponse>(`${API_BASE_URL}/intelligence/forecast/scenario`, request);
  return response.data;
};

export const getRiskSignals = async (): Promise<RiskSignalsResponse> => {
  const response = await axios.get<RiskSignalsResponse>(`${API_BASE_URL}/intelligence/risk-signals`);
  return response.data;
};

export const getRevenueAtRisk = async (): Promise<RevenueAtRiskResponse> => {
  const response = await axios.get<RevenueAtRiskResponse>(`${API_BASE_URL}/intelligence/revenue-at-risk`);
  return response.data;
};

export const getIntelligenceSummary = async (): Promise<IntelligenceSummaryResponse> => {
  const response = await axios.get<IntelligenceSummaryResponse>(`${API_BASE_URL}/intelligence/summary`);
  return response.data;
};
