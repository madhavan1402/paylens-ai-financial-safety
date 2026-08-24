from decimal import Decimal

import pytest

from app.models.intent_models import ActionType, IntentStatus
from app.services.intent_service import IntentService


@pytest.mark.parametrize(
    ("message", "action", "amount", "target"),
    [
        ("Refund ₹250000 to Rahul", ActionType.REFUND, "250000", "Rahul"),
        ("Refund ₹2.5 lakh to Rahul", ActionType.REFUND, "250000", "Rahul"),
        ("Refund 50k to customer", ActionType.REFUND, "50000", "customer"),
        ("Pay ₹80000 to ABC Suppliers", ActionType.VENDOR_PAYMENT, "80000", "ABC Suppliers"),
        ("Process payroll ₹300000", ActionType.PAYROLL, "300000", None),
        ("Pay ₹100000 tax", ActionType.TAX_PAYMENT, "100000", None),
    ],
)
def test_parses_supported_demo_commands(message, action, amount, target):
    result = IntentService().parse(message)

    assert result.status is IntentStatus.VALID
    assert result.intent.actionType is action
    assert result.intent.amount == Decimal(amount)
    assert result.intent.target == target
    assert result.providerMode == "deterministic"


@pytest.mark.parametrize("message", ["Refund the customer", "Pay Rahul"])
def test_requests_missing_amount(message):
    result = IntentService().parse(message)

    assert result.status is IntentStatus.NEEDS_CLARIFICATION
    assert result.missingFields == ["amount"]


@pytest.mark.parametrize("message", ["Tell me a joke", "Refund -50 to Rahul"])
def test_rejects_invalid_requests(message):
    result = IntentService().parse(message)
    assert result.status is IntentStatus.INVALID
