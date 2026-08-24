export interface DashboardResponse { currency: string; currentBalance: number; upcomingObligations: number; safetyReserve: number; availableLiquidity: number; remainingAfterObligations: number; safetyBuffer: number }
export interface Transaction { id: string; type: string; amount: number; description: string; timestamp: string; status: string }
export interface FinancialStateResponse { transactions: Transaction[] }
export interface Intent { actionType: string; amount: number; currency: string; target?: string; description: string }
export interface Snapshot { currentBalance: number; upcomingObligations: number; safetyReserve: number; availableLiquidity: number; remainingAfterObligations: number; safetyBuffer: number }
export interface Simulation { before: Snapshot; after: Snapshot; impact: { liquidityChange: number; obligationCoverageChange: number; safetyBufferChange: number; reserveBreached: boolean; obligationsCovered: boolean }; consequence: string }
export type Decision = 'SAFE' | 'REVIEW' | 'BLOCK'
export interface Explanation { status: string; decision: Decision; headline: string; explanation: string; keyFactors: string[]; recommendation: string; providerMode: string }
export interface AgentAnalysisResponse { message: string; status: 'VALID' | 'NEEDS_CLARIFICATION' | 'INVALID'; intent?: Intent; missingFields: string[]; clarificationMessage?: string; simulation?: Simulation; policy?: { decision: Decision; reason: string; recommendation: string }; explanation?: Explanation }
