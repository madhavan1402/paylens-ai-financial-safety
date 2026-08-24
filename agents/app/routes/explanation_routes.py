from fastapi import APIRouter

from app.models.explanation_models import ExplanationRequest, ExplanationResponse
from app.services.explanation_service import ExplanationService

router = APIRouter()
explanation_service = ExplanationService()


@router.post("/explain", response_model=ExplanationResponse)
def explain(request: ExplanationRequest) -> ExplanationResponse:
    return explanation_service.explain(request)
