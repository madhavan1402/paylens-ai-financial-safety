from app.models.explanation_models import ExplanationDecision, ExplanationRequest, ExplanationResponse
from app.utils.parsing import format_inr


class ExplanationService:
    """Explains supplied backend facts; it never makes a financial decision."""

    def explain(self, request: ExplanationRequest) -> ExplanationResponse:
        policy = request.policy
        after = request.simulation.after
        impact = request.simulation.impact
        action = request.intent.actionType.value.replace("_", " ").lower()
        if policy.decision == ExplanationDecision.BLOCK:
            factors = []
            if not impact.obligationsCovered:
                factors.append("Upcoming obligations exceed the simulated remaining balance.")
            if after.safetyBuffer < 0:
                factors.append("The safety buffer would fall below zero.")
            if not factors:
                factors.append(policy.reason)
            explanation = (
                f"The proposed {action} would leave {format_inr(after.currentBalance)} while upcoming "
                f"obligations total {format_inr(after.upcomingObligations)}, leaving "
                f"{format_inr(after.remainingAfterObligations)} after obligations."
            )
            headline = f"{action.capitalize()} blocked"
        elif policy.decision == ExplanationDecision.REVIEW:
            headline = "Human review required"
            explanation = (
                f"The proposed {action} preserves obligation coverage, but the safety buffer would be "
                f"{format_inr(after.safetyBuffer)}, below the required safety reserve of "
                f"{format_inr(after.safetyReserve)}."
            )
            factors = ["Upcoming obligations remain covered.", "The configured safety margin would be materially reduced."]
        else:
            headline = "Action appears financially safe"
            explanation = (
                f"The proposed {action} leaves {format_inr(after.remainingAfterObligations)} after upcoming "
                f"obligations and preserves a {format_inr(after.safetyBuffer)} safety buffer."
            )
            factors = ["Upcoming obligations remain covered.", "The required safety margin is preserved."]
        return ExplanationResponse(decision=policy.decision, headline=headline, explanation=explanation,
                                   keyFactors=factors, recommendation=policy.recommendation,
                                   providerMode="deterministic")
