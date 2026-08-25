# PayLens — AI Financial Safety & Consequence Engine

PayLens is a financial safety layer for AI agents and automated financial workflows. Before any future execution step, its deterministic backend establishes the merchant's financial position—balance, obligations, protected reserve, and resulting liquidity—so an AI is never the authority that calculates whether money is safe to move.

## Phase 1 status

Phase 1 provides a deterministic in-memory Financial State Engine using Java 21 and Spring Boot. It has no AI integration, payment execution, database, authentication, or risk/policy decisions.

## Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The server runs on `http://localhost:8080`. Run tests with `./mvnw clean test` (or `.\mvnw.cmd clean test` on Windows).

## API endpoints

- `GET /api/health`
- `GET /api/financial-state`
- `GET /api/dashboard`
- `POST /api/simulations`
- `POST /api/policy/evaluate`
- `POST /api/agent/analyze`
- `POST /api/agent/explain` (runs the same authoritative analysis flow and includes an explanation)
- Python agent: `GET /api/health`, `POST /api/intent`, `POST /api/explain` (port 8000)

Example dashboard response:

```json
{
  "currency": "INR",
  "currentBalance": 840000,
  "upcomingObligations": 620000,
  "safetyReserve": 100000,
  "availableLiquidity": 740000,
  "remainingAfterObligations": 220000,
  "safetyBuffer": 120000
}
```

## AI Intent Agent

Run the isolated deterministic intent agent:

```bash
cd agents
python -m venv .venv
.venv\Scripts\pip install -r requirements.txt
.venv\Scripts\uvicorn app.main:app --reload --port 8000
```

`POST /api/intent` accepts `{"message":"Refund ₹2.5 lakh to Rahul"}` and returns validated structured intent. Without an AI API key, its deterministic fallback recognizes common refunds, vendor payments, payroll, tax payments, INR, lakh/lac, and k notation. Ambiguous text asks for clarification rather than guessing.

`POST /api/agent/analyze` sends natural language through that agent, validates the untrusted result in Spring Boot, then returns its simulation and `SAFE`/`REVIEW`/`BLOCK` policy result. It never executes the action.

## AI Consequence & Explanation Agent

Phase 5 adds a downstream explanation layer. Spring Boot alone constructs the structured explanation request from the validated intent, authoritative simulation, and authoritative policy result. The FastAPI endpoint never receives client-supplied financial facts.

The default provider is deterministic and works without an API key. An optional LLM provider is intentionally isolated behind a provider seam and must preserve the supplied decision and facts. Spring Boot checks the response decision; an unavailable or mismatched response falls back to its local deterministic explanation, without affecting `SAFE`, `REVIEW`, or `BLOCK`.

- `BLOCK`: “Refund blocked” — insufficient funds for upcoming obligations.
- `REVIEW`: “Human review required” — obligations are covered but the safety margin is reduced.
- `SAFE`: “Action appears financially safe” — obligation coverage and safety margin are preserved.

## Frontend command center

The React/Vite frontend is a responsive PayLens operations dashboard. It reads live metrics from Spring Boot, displays financial-state transaction data, and sends natural-language actions only to `POST /api/agent/analyze`.

```bash
cd frontend
npm install
npm run dev
```

Set `VITE_API_BASE_URL=http://localhost:8080` in `frontend/.env` (see `.env.example`). Main routes are `/` (Overview), `/safety` (AI Safety Center), `/transactions`, `/simulations`, `/decisions`, `/customers`, `/audit`, and `/settings`. The UI presents authoritative simulation, policy, and explanation data only; it contains no execution capability.

Example simulation request:

```json
{
  "actionType": "REFUND",
  "amount": 250000,
  "description": "Customer refund"
}
```

The simulation is side-effect free. It returns before/after financial snapshots, impacts, and a neutral consequence such as `OBLIGATION_SHORTFALL`; it does not execute the action or make a policy decision.

Policy evaluation uses the same request body and returns a deterministic policy result. A ₹20,000 refund is `SAFE`, a ₹50,000 refund is `REVIEW` because its ₹70,000 after-buffer is below the ₹100,000 required margin, and a ₹250,000 refund is `BLOCK` because it cannot cover ₹620,000 of upcoming obligations.


## Governance, Human Review & Append-Only Audit Trail (Phase 7)

Phase 7 introduces persistent decision recording, human governance workflows, and an immutable append-only audit trail.

- **Decision Persistence**: Every financial safety analysis (`POST /api/agent/analyze`) is recorded in Spring Data JPA (`DecisionRecord`). Decisions track original prompt, intent, simulation, policy results, explanation, and governance status (`SAFE`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `BLOCKED`).
- **State Machine Rules**:
  - `SAFE` is terminal.
  - `BLOCKED` is terminal.
  - `PENDING_REVIEW` can transition to `APPROVED` or `REJECTED`.
  - `APPROVED` and `REJECTED` are terminal.
  - Invalid state transitions return HTTP 409 Conflict.
- **BLOCK Protection**: A `BLOCKED` decision can NEVER be approved or rejected. Any attempt to approve/reject a non-`PENDING_REVIEW` decision returns HTTP 409 Conflict.
- **Approval & Rejection Semantics**:
  - `POST /api/decisions/{id}/approve`: Only `PENDING_REVIEW` decisions can be approved.
  - `POST /api/decisions/{id}/reject`: Only `PENDING_REVIEW` decisions can be rejected (requires a non-blank comment).
  - **IMPORTANT**: **APPROVED means human governance approval only. No financial transaction is executed in Phase 7.**
- **Append-Only Audit Trail**: `AuditEvent` records system actions, AI intent parsing, policy evaluation, review requests, approvals, rejections, and blocks. Audit events are strictly append-only (no `PUT`, `PATCH`, or `DELETE` endpoints exist).
- **APIs**:
  - `GET /api/decisions`: List decisions (supports optional `status` and `limit` parameters).
  - `GET /api/decisions/{id}`: Full decision detail with intent, simulation, policy, explanation, and governance status.
  - `POST /api/decisions/{id}/approve`: Human review approval.
  - `POST /api/decisions/{id}/reject`: Human review rejection.
  - `GET /api/audit`: Retrieve append-only audit trail (supports optional `decisionId` query parameter).

