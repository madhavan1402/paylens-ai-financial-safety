from fastapi import FastAPI

from app.routes.intent_routes import router as intent_router
from app.routes.explanation_routes import router as explanation_router

app = FastAPI(title="PayLens Intent Agent")
app.include_router(intent_router, prefix="/api")
app.include_router(explanation_router, prefix="/api")
