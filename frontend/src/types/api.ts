export interface DashboardResponse {
  currency: string;
  currentBalance: number;
  upcomingObligations: number;
  safetyReserve: number;
  availableLiquidity: number;
  remainingAfterObligations: number;
  safetyBuffer: number;
}

export interface Transaction {
  id: string;
  type: string;
  amount: number;
  description: string;
  timestamp: string;
  status: string;
}

export interface FinancialStateResponse {
  transactions: Transaction[];
}

export interface Intent {
  actionType: string;
  amount: number;
  currency: string;
  target?: string;
  description: string;
}

export interface Snapshot {
  currentBalance: number;
  upcomingObligations: number;
  safetyReserve: number;
  availableLiquidity: number;
  remainingAfterObligations: number;
  safetyBuffer: number;
}

export interface Simulation {
  before: Snapshot;
  after: Snapshot;
  impact: {
    liquidityChange: number;
    obligationCoverageChange: number;
    safetyBufferChange: number;
    reserveBreached: boolean;
    obligationsCovered: boolean;
  };
  consequence: string;
}

export type Decision = 'SAFE' | 'REVIEW' | 'BLOCK';

export type GovernanceStatus = 'SAFE' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'BLOCKED';

export type ExecutionStatus =
  | 'REQUESTED'
  | 'ELIGIBILITY_REJECTED'
  | 'PROCESSING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'DUPLICATE'
  | 'UNKNOWN'
  | 'UNSUPPORTED_EXECUTION';

export type ExecutionProvider = 'RAZORPAY_TEST' | 'MOCK_TEST_PROVIDER';

export interface Explanation {
  status: string;
  decision: Decision;
  headline: string;
  explanation: string;
  keyFactors: string[];
  recommendation: string;
  providerMode: string;
}

export interface GovernanceResponse {
  decisionId: string;
  status: GovernanceStatus;
}

export interface AgentAnalysisResponse {
  message: string;
  status: 'VALID' | 'NEEDS_CLARIFICATION' | 'INVALID';
  intent?: Intent;
  missingFields: string[];
  clarificationMessage?: string;
  simulation?: Simulation;
  policy?: {
    decision: Decision;
    reason: string;
    recommendation: string;
  };
  explanation?: Explanation;
  governance?: GovernanceResponse;
}

export interface DecisionSummary {
  decisionId: string;
  actionType: string;
  amount: number;
  currency: string;
  target?: string;
  decision: Decision;
  status: GovernanceStatus;
  createdAt: string;
}

export interface DecisionDetail {
  decisionId: string;
  originalMessage: string;
  intent: Intent;
  simulation: Simulation;
  policy: {
    decision: Decision;
    reason: string;
    recommendation: string;
  };
  explanation?: Explanation;
  status: GovernanceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEvent {
  eventId: string;
  decisionId: string;
  eventType: string;
  actorType: 'SYSTEM' | 'AI_AGENT' | 'HUMAN';
  actorId: string;
  description: string;
  createdAt: string;
}

export interface ExecutionRequest {
  decisionId: string;
  idempotencyKey: string;
}

export interface ExecutionResponse {
  executionId: string;
  decisionId: string;
  idempotencyKey: string;
  provider: ExecutionProvider;
  providerReference?: string;
  actionType: string;
  amount: number;
  currency: string;
  target?: string;
  status: ExecutionStatus;
  failureCode?: string;
  failureMessage?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ExecutionSummary {
  executionId: string;
  decisionId: string;
  actionType: string;
  amount: number;
  currency: string;
  provider: ExecutionProvider;
  providerReference?: string;
  status: ExecutionStatus;
  createdAt: string;
}

export type ReconciliationStatus =
  | 'NOT_REQUIRED'
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'CONFIRMED'
  | 'FAILED'
  | 'UNKNOWN'
  | 'MANUAL_REVIEW_REQUIRED';

export type NormalizedReconciliationOutcome =
  | 'CONFIRMED_SUCCESS'
  | 'CONFIRMED_FAILURE'
  | 'STILL_PROCESSING'
  | 'NOT_FOUND'
  | 'UNKNOWN';

export type RetryDecision = 'NOT_SAFE' | 'SAFE_TO_RETRY' | 'MANUAL_REVIEW';

export interface ReconciliationRecord {
  reconciliationId: string;
  executionId: string;
  decisionId: string;
  provider: ExecutionProvider;
  providerReference?: string;
  previousExecutionStatus: ExecutionStatus;
  resolvedExecutionStatus: ExecutionStatus;
  status: ReconciliationStatus;
  providerOutcome: NormalizedReconciliationOutcome;
  retryDecision: RetryDecision;
  resolution?: string;
  failureCode?: string;
  failureMessage?: string;
  attemptNumber: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

export interface ReconciliationSummary {
  reconciliationId: string;
  executionId: string;
  decisionId: string;
  provider: ExecutionProvider;
  providerReference?: string;
  resolvedExecutionStatus: ExecutionStatus;
  status: ReconciliationStatus;
  providerOutcome: NormalizedReconciliationOutcome;
  retryDecision: RetryDecision;
  createdAt: string;
}

export interface ReliabilityMetrics {
  totalExecutions: number;
  confirmedSuccess: number;
  confirmedFailure: number;
  pending: number;
  unknownOrManualReview: number;
  successRate: number;
}

export type FinancialHealthStatus = 'HEALTHY' | 'CAUTION' | 'AT_RISK' | 'CRITICAL';

export type ObligationRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type RiskSignalType =
  | 'LOW_LIQUIDITY'
  | 'SAFETY_BUFFER_PRESSURE'
  | 'UPCOMING_OBLIGATION'
  | 'HIGH_OBLIGATION_CONCENTRATION'
  | 'EXECUTION_FAILURE'
  | 'RECONCILIATION_REQUIRED'
  | 'UNKNOWN_EXECUTION'
  | 'REVENUE_AT_RISK';

export type RiskSeverity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type ForecastConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

export type ForecastDataQuality = 'SUFFICIENT_HISTORY' | 'LIMITED_HISTORY' | 'INSUFFICIENT_HISTORY';

export interface FinancialHealthResponse {
  currentBalance: number;
  availableLiquidity: number;
  unpaidObligations: number;
  safetyReserve: number;
  remainingAfterObligations: number;
  safetyBuffer: number;
  healthStatus: FinancialHealthStatus;
  healthScore: number;
  calculatedAt: string;
}

export interface CashFlowPoint {
  date: string;
  inflow: number;
  outflow: number;
  netFlow: number;
  balance: number;
}

export interface CashFlowResponse {
  period: string;
  points: CashFlowPoint[];
  dataQualityMessage: string;
}

export interface ObligationRiskItem {
  id: string;
  type: string;
  description: string;
  amount: number;
  dueDate: string;
  status: string;
  daysUntilDue: number;
  riskLevel: ObligationRiskLevel;
}

export interface ObligationsRiskResponse {
  obligations: ObligationRiskItem[];
  totalUpcomingAmount: number;
}

export interface LiquidityForecastDay {
  date: string;
  dayIndex: number;
  projectedBalance: number;
  projectedSafetyBuffer: number;
  scheduledOutflows: number;
  projectedInflows: number;
}

export interface LiquidityForecastResponse {
  forecastDays: number;
  startingBalance: number;
  projectedInflows: number;
  projectedOutflows: number;
  projectedEndingBalance: number;
  projectedSafetyBuffer: number;
  confidence: ForecastConfidence;
  dataQuality: ForecastDataQuality;
  assumptions: string[];
  forecastDaysList: LiquidityForecastDay[];
}

export interface RiskSignal {
  signalId: string;
  type: RiskSignalType;
  severity: RiskSeverity;
  title: string;
  description: string;
  detectedAt: string;
  relatedEntityId?: string;
  recommendedAction: string;
}

export interface RiskSignalsResponse {
  signals: RiskSignal[];
  totalCount: number;
  criticalCount: number;
}

export interface RevenueAtRiskResponse {
  totalAmount: number;
  caseCount: number;
  highPriorityAmount: number;
  dataStatus: string;
  calculatedAt: string;
}

export interface ForecastScenarioRequest {
  actionType: string;
  amount: number;
  currency?: string;
  target?: string;
}

export interface ForecastScenarioResponse {
  actionType: string;
  amount: number;
  policyDecision: 'SAFE' | 'REVIEW' | 'BLOCK';
  currentSafetyBuffer: number;
  projectedSafetyBuffer: number;
  safetyBufferImpact: number;
  currentHealthStatus: FinancialHealthStatus;
  projectedHealthStatus: FinancialHealthStatus;
  consequenceSummary: string;
  preservesFullMargin: boolean;
}

export interface IntelligenceSummaryResponse {
  health: FinancialHealthResponse;
  forecast: LiquidityForecastResponse;
  topObligations: ObligationRiskItem[];
  activeSignals: RiskSignal[];
  revenueAtRisk: RevenueAtRiskResponse;
  calculatedAt: string;
}

export type RiskEventStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'DISMISSED';

export type RiskEventSource =
  | 'FINANCIAL_STATE'
  | 'FORECAST'
  | 'OBLIGATION'
  | 'EXECUTION'
  | 'RECONCILIATION'
  | 'REVENUE'
  | 'SYSTEM';

export type RiskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface MonitoringStatusResponse {
  lastRunAt?: string;
  nextRunAt?: string;
  monitoringEnabled: boolean;
  lastRunDurationMs: number;
  lastRunStatus: string;
  eventsDetected: number;
  eventsUpdated: number;
  eventsResolved: number;
  openCount: number;
  acknowledgedCount: number;
  resolvedCount: number;
  dismissedCount: number;
}

export interface MonitoringCycleResponse {
  cycleId: string;
  executedAt: string;
  durationMs: number;
  eventsDetected: number;
  eventsUpdated: number;
  eventsResolved: number;
  status: string;
}

export interface RiskEventResponse {
  riskEventId: string;
  fingerprint: string;
  riskSignalType: RiskSignalType;
  severity: RiskSeverity;
  priority: RiskPriority;
  title: string;
  description: string;
  status: RiskEventStatus;
  source: RiskEventSource;
  detectedAt: string;
  firstDetectedAt: string;
  lastDetectedAt: string;
  resolvedAt?: string;
  occurrenceCount: number;
  relatedEntityType?: string;
  relatedEntityId?: string;
  recommendedAction?: string;
  financialImpact?: number;
  dismissalReason?: string;
  resolutionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export type UserRole = 'OWNER' | 'ADMIN' | 'FINANCE_MANAGER' | 'REVIEWER' | 'OPERATOR' | 'VIEWER';
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED';

export interface UserResponse {
  userId: string;
  merchantId: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  lastLoginAt?: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
  merchantName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  merchantName?: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  displayName: string;
  role: UserRole;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}export type CopilotIntent =
  | 'FINANCIAL_STATUS'
  | 'ACTION_ANALYSIS'
  | 'RISK_EXPLANATION'
  | 'POLICY_EXPLANATION'
  | 'FORECAST_QUERY'
  | 'UNKNOWN'

export interface CopilotRequest {
  message: string
  actionType?: string
  amount?: number
}

export interface CopilotSimulation {
  action: {
    actionType: string
    amount: number
    description: string
  }
  before: Snapshot
  after: Snapshot
  impact: {
    liquidityChange: number
    obligationCoverageChange: number
    safetyBufferChange: number
    reserveBreached: boolean
    obligationsCovered: boolean
  }
  consequence: string
}

export interface CopilotResponse {
  intent: CopilotIntent
  headline: string
  explanation: string
  keyFactors: string[]
  financialImpact?: string
  recommendation: string
  requiresHumanReview: boolean
  policyDecision?: 'SAFE' | 'REVIEW' | 'BLOCK'
  simulation?: CopilotSimulation
  financialHealth: FinancialHealthResponse
  generatedAt: string
}

