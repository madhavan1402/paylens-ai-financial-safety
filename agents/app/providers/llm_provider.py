from typing import Protocol


class LLMProvider(Protocol):
    """Optional provider boundary; provider output must still be Pydantic-validated."""

    def parse_intent(self, user_text: str) -> dict: ...
