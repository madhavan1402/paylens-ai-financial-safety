from decimal import Decimal

from app.models.intent_models import ActionType, FinancialIntent, IntentResponse, IntentStatus
from app.utils.parsing import format_inr, parse_inr_amount, target_after_to


class IntentService:
    """Offline deterministic parser for the narrow Phase 4 demo grammar."""

    def parse(self, message: str) -> IntentResponse:
        normalized = message.strip()
        action = self._action_type(normalized.lower())
        if action is None:
            return IntentResponse(
                status=IntentStatus.INVALID,
                message="The requested financial action could not be understood.",
            )

        amount = parse_inr_amount(normalized)
        if amount is None:
            return IntentResponse(
                status=IntentStatus.NEEDS_CLARIFICATION,
                missingFields=["amount"],
                message=f"Please provide the {self._action_label(action)} amount.",
            )
        if amount <= Decimal("0"):
            return IntentResponse(status=IntentStatus.INVALID, message="Amount must be greater than zero.")

        target = target_after_to(normalized)
        description = f"{self._action_label(action).capitalize()} {format_inr(amount)}"
        if target:
            description += f" to {target}"
        return IntentResponse(
            status=IntentStatus.VALID,
            intent=FinancialIntent(
                actionType=action,
                amount=amount,
                currency="INR",
                target=target,
                description=description,
            ),
            confidence=Decimal("1.0"),
        )

    def _action_type(self, message: str) -> ActionType | None:
        if "refund" in message:
            return ActionType.REFUND
        if "payroll" in message:
            return ActionType.PAYROLL
        if "tax" in message:
            return ActionType.TAX_PAYMENT
        if "pay" in message or "vendor" in message or "supplier" in message:
            return ActionType.VENDOR_PAYMENT
        return None

    def _action_label(self, action: ActionType) -> str:
        return {
            ActionType.REFUND: "refund",
            ActionType.VENDOR_PAYMENT: "vendor payment",
            ActionType.PAYROLL: "payroll",
            ActionType.TAX_PAYMENT: "tax payment",
        }[action]
