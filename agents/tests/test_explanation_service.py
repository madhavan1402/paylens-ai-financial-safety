from decimal import Decimal

from app.models.explanation_models import (
    ExplanationDecision, ExplanationRequest, FinancialFacts, ImpactFacts, PolicyFacts, SimulationFacts,
)
from app.models.intent_models import ActionType, FinancialIntent
from app.services.explanation_service import ExplanationService


def request(decision: ExplanationDecision) -> ExplanationRequest:
    before = FinancialFacts(currentBalance=Decimal("840000"), upcomingObligations=Decimal("620000"),
                            safetyReserve=Decimal("100000"), availableLiquidity=Decimal("740000"),
                            remainingAfterObligations=Decimal("220000"), safetyBuffer=Decimal("120000"))
    after = FinancialFacts(currentBalance=Decimal("590000"), upcomingObligations=Decimal("620000"),
                           safetyReserve=Decimal("100000"), availableLiquidity=Decimal("490000"),
                           remainingAfterObligations=Decimal("-30000"), safetyBuffer=Decimal("-130000"))
    return ExplanationRequest(originalMessage="Refund ₹2.5 lakh to Rahul",
        intent=FinancialIntent(actionType=ActionType.REFUND, amount=Decimal("250000"), target="Rahul", description="Refund"),
        simulation=SimulationFacts(before=before, after=after,
            impact=ImpactFacts(liquidityChange=Decimal("-250000"), obligationCoverageChange=Decimal("-250000"),
                               safetyBufferChange=Decimal("-250000"), reserveBreached=True, obligationsCovered=False),
            consequence="OBLIGATION_SHORTFALL"),
        policy=PolicyFacts(decision=decision, reason="Authoritative reason", recommendation="Authoritative recommendation"))


def test_block_uses_authoritative_values_and_decision():
    response = ExplanationService().explain(request(ExplanationDecision.BLOCK))
    assert response.status == "SUCCESS" and response.decision == ExplanationDecision.BLOCK
    assert response.headline and response.keyFactors and response.recommendation
    assert "590,000" in response.explanation and "620,000" in response.explanation


def test_block_response_contains_required_fields():
    response = ExplanationService().explain(request(ExplanationDecision.BLOCK))
    assert response.headline and response.explanation and response.keyFactors and response.recommendation


def test_review_decision_integrity():
    assert ExplanationService().explain(request(ExplanationDecision.REVIEW)).decision == ExplanationDecision.REVIEW


def test_safe_decision_integrity():
    assert ExplanationService().explain(request(ExplanationDecision.SAFE)).decision == ExplanationDecision.SAFE


def test_only_structured_facts_are_accepted():
    payload = request(ExplanationDecision.BLOCK).model_dump()
    payload["untrustedBalance"] = "999999999"
    from pydantic import ValidationError
    import pytest
    with pytest.raises(ValidationError):
        ExplanationRequest.model_validate(payload)
