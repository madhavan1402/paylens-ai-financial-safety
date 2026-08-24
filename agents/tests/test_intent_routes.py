from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_endpoint():
    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "paylens-intent-agent"}


def test_intent_endpoint_returns_a_valid_structured_intent():
    response = client.post("/api/intent", json={"message": "Refund INR 2.5 lakh to Rahul"})

    assert response.status_code == 200
    assert response.json() == {
        "status": "VALID",
        "intent": {
            "actionType": "REFUND",
            "amount": "250000.0",
            "currency": "INR",
            "target": "Rahul",
            "description": "Refund ₹250,000 to Rahul",
        },
        "confidence": "1.0",
        "missingFields": [],
        "message": None,
        "providerMode": "deterministic",
    }
