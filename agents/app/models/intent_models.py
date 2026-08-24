from decimal import Decimal
from enum import StrEnum

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ActionType(StrEnum):
    REFUND = "REFUND"
    VENDOR_PAYMENT = "VENDOR_PAYMENT"
    PAYROLL = "PAYROLL"
    TAX_PAYMENT = "TAX_PAYMENT"


class IntentStatus(StrEnum):
    VALID = "VALID"
    NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION"
    INVALID = "INVALID"


class FinancialIntent(BaseModel):
    model_config = ConfigDict(extra="forbid")
    actionType: ActionType
    amount: Decimal = Field(gt=Decimal("0"))
    currency: str = "INR"
    target: str | None = None
    description: str

    @field_validator("currency")
    @classmethod
    def only_inr(cls, value: str) -> str:
        if value.upper() != "INR":
            raise ValueError("Only INR is supported")
        return "INR"


class IntentRequest(BaseModel):
    message: str = Field(min_length=1)


class IntentResponse(BaseModel):
    status: IntentStatus
    intent: FinancialIntent | None = None
    confidence: Decimal = Decimal("0")
    missingFields: list[str] = Field(default_factory=list)
    message: str | None = None
    providerMode: str = "deterministic"
