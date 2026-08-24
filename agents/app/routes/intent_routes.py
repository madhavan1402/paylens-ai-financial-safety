from fastapi import APIRouter

from app.models.intent_models import IntentRequest, IntentResponse
from app.services.intent_service import IntentService

router = APIRouter()
intent_service = IntentService()


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "paylens-intent-agent"}


@router.post("/intent", response_model=IntentResponse)
def parse_intent(request: IntentRequest) -> IntentResponse:
    return intent_service.parse(request.message)
