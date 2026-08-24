import re
from decimal import Decimal


AMOUNT_PATTERN = re.compile(
    r"(?:₹|inr\s*)?\s*(-?\d+(?:\.\d+)?)\s*(lakh|lac|k)?\b", re.IGNORECASE
)


def parse_inr_amount(message: str) -> Decimal | None:
    match = AMOUNT_PATTERN.search(message)
    if not match:
        return None
    amount = Decimal(match.group(1))
    unit = (match.group(2) or "").lower()
    if unit in {"lakh", "lac"}:
        amount *= Decimal("100000")
    elif unit == "k":
        amount *= Decimal("1000")
    return amount


def target_after_to(message: str) -> str | None:
    match = re.search(r"\bto\s+(.+)$", message, re.IGNORECASE)
    return match.group(1).strip(" .") if match else None


def format_inr(amount: Decimal) -> str:
    return f"₹{amount:,.0f}" if amount == amount.to_integral() else f"₹{amount:,.2f}"
