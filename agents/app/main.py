from fastapi import FastAPI

from app.routes.intent_routes import router as intent_router

app = FastAPI(title="PayLens Intent Agent")
app.include_router(intent_router, prefix="/api")
