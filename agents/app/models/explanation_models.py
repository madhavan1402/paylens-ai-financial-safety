from decimal import Decimal
from enum import StrEnum

from pydantic import BaseModel, ConfigDict, Field

from app.models.intent_models import FinancialIntent


class ExplanationDecision(StrEnum):
    SAFE = "SAFE"
    REVIEW = "REVIEW"
    BLOCK = "BLOCK"


class FinancialFacts(BaseModel):
    model_config = ConfigDict(extra="forbid")
    currentBalance: Decimal
    upcomingObligations: Decimal
    safetyReserve: Decimal
    availableLiquidity: Decimal
    remainingAfterObligations: Decimal
    safetyBuffer: Decimal


class ImpactFacts(BaseModel):
    model_config = ConfigDict(extra="forbid")
    liquidityChange: Decimal
    obligationCoverageChange: Decimal
    safetyBufferChange: Decimal
    reserveBreached: bool
    obligationsCovered: bool


class SimulationFacts(BaseModel):
    model_config = ConfigDict(extra="forbid")
    before: FinancialFacts
    after: FinancialFacts
    impact: ImpactFacts
    consequence: str


class PolicyFacts(BaseModel):
    model_config = ConfigDict(extra="forbid")
    decision: ExplanationDecision
    reason: str
    recommendation: str


class ExplanationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    originalMessage: str = Field(min_length=1)
    intent: FinancialIntent
    simulation: SimulationFacts
    policy: PolicyFacts


class ExplanationResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")
    status: str = "SUCCESS"
    decision: ExplanationDecision
    headline: str = Field(min_length=1)
    explanation: str = Field(min_length=1)
    keyFactors: list[str] = Field(min_length=1)
    recommendation: str = Field(min_length=1)
    providerMode: str
