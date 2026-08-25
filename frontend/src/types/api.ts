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
