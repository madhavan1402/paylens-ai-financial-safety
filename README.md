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

## Controlled Financial Execution Gateway (Phase 8)

Phase 8 introduces a controlled financial execution gateway between PayLens governance decisions and the Razorpay TEST API, proving that an AI-generated action cannot reach payment infrastructure unless PayLens governance explicitly permits it.

```text
Decision (SAFE / APPROVED)
           │
           ▼
    ExecutionService
           │
  (Eligibility & DB Idempotency Check)
           │
           ▼
PaymentExecutionProvider (Interface)
           │
           ▼
RazorpayTestExecutionProvider
           │
           ▼
  Razorpay TEST API (order / payment / refund)
```

- **Server-Side Governance Validation**: The execution gateway never trusts client or AI input. It loads the persisted `DecisionRecord` and independently validates eligibility.
- **Eligibility Rules**:
  - `SAFE`: Eligible for execution upon explicit user confirmation in TEST mode.
  - `APPROVED`: Eligible for execution after recorded human governance sign-off.
  - `PENDING_REVIEW`: **DENIED** (`ELIGIBILITY_REJECTED`).
  - `REJECTED`: **DENIED** (`ELIGIBILITY_REJECTED`).
  - `BLOCKED`: **DENIED** (`ELIGIBILITY_REJECTED`). **BLOCKED decisions can NEVER reach payment infrastructure.**
- **Provider Abstraction**: Business logic depends on `PaymentExecutionProvider` interface. `RazorpayTestExecutionProvider` encapsulates official Razorpay Java SDK integration for test-mode actions (`REFUND`, `VENDOR_PAYMENT`). Unsupported actions (`PAYROLL`, `TAX_PAYMENT`) return `UNSUPPORTED_EXECUTION` without fabricating payment references.
- **Database-Level Idempotency**: `ExecutionRecord` enforces unique index constraint on `idempotencyKey`. Duplicate execution requests return the existing execution result without invoking the provider twice.
- **Outcome Safety & Timeout Handling**: Network timeouts yield `UNKNOWN` status. Outcome `UNKNOWN` does NOT trigger an automatic retry; the UI indicates manual reconciliation is required.
- **Credential Protection**: `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` remain strictly server-side in environment variables. Secrets are never exposed to React, API responses, logs, or source control.
- **Execution APIs**:
  - `POST /api/executions`: Triggers controlled execution for `decisionId` with `idempotencyKey`.
  - `GET /api/executions`: Lists execution history (supports optional `status` filter).
  - `GET /api/executions/{id}`: Returns complete detail of an execution record.
  - `GET /api/decisions/{id}/execution`: Returns execution record associated with a decision.

## Reconciliation & Reliability Engine (Phase 9)

Phase 9 implements a real-world financial execution reconciliation and reliability layer to safely answer: *"PayLens requested an execution from the provider. What actually happened?"*

```text
               EXECUTION GATEWAY (STATUS: UNKNOWN)
                                │
                                ▼
                       ReconciliationService
                                │
                 1. Load persisted ExecutionRecord
                 2. Query Razorpay TEST Status API
                 3. Normalize Provider Outcome
                                │
         +----------------------+----------------------+
         │                      │                      │
 CONFIRMED_SUCCESS      CONFIRMED_FAILURE          NOT_FOUND /
   (Status: SUCCEEDED)    (Status: FAILED)          STILL_PROCESSING
         │                      │                      │
         ▼                      ▼                      ▼
  Reconciliation:        Reconciliation:        Reconciliation:
    CONFIRMED              FAILED             MANUAL_REVIEW_REQUIRED
```

- **UNKNOWN ≠ FAILED**: PayLens never assumes a network timeout means payment failure. `UNKNOWN` status is never automatically converted to `FAILED`.
- **Zero Automatic Financial Retries**: `UNKNOWN` outcomes **never** trigger an automatic re-execution of the financial action. PayLens reconciles provider state first.
- **Provider Outcome Normalization**:
  - `CONFIRMED_SUCCESS` -> Execution status updated to `SUCCEEDED`, reconciliation `CONFIRMED`.
  - `CONFIRMED_FAILURE` -> Execution status updated to `FAILED`, reconciliation `FAILED`.
  - `STILL_PROCESSING` -> Reconciliation status `PENDING`.
  - `NOT_FOUND` -> Preserved as `NOT_FOUND` provider outcome; reconciliation transitions to `MANUAL_REVIEW_REQUIRED`.
  - `UNKNOWN` -> Reconciliation transitions to `MANUAL_REVIEW_REQUIRED`.
- **Reconciliation Idempotency & Concurrency**: Duplicate or concurrent reconciliation requests for an execution ID do not make redundant provider API calls.
- **Reliability Metrics**: Calculates resolved success rate `(confirmedSuccess / (confirmedSuccess + confirmedFailure))`, explicitly excluding unresolved `UNKNOWN` or `PENDING` states.
- **Reconciliation APIs**:
  - `POST /api/executions/{id}/reconcile`: Triggers server-side provider status reconciliation.
  - `GET /api/executions/{id}/reconciliation`: Returns latest reconciliation record.
  - `GET /api/reconciliations`: Lists reconciliation history with optional filters.
  - `GET /api/reconciliations/{id}`: Returns complete reconciliation detail.
  - `GET /api/reconciliations/metrics`: Returns system execution reliability metrics.

## Real-Time Financial Intelligence Engine (Phase 10)

Phase 10 transforms PayLens from a reactive financial action analyzer into a proactive financial intelligence platform.

- **Financial Health Score (0-100)**: Authoritative score calculated from `safetyBuffer`, `availableLiquidity`, and execution reliability penalties.
- **7-Day Liquidity Forecast**: Daily balance and safety buffer projections reusing Phase 2 deterministic formulas.
- **Scenario What-If Simulator**: Evaluates proposed action impact using `SimulationService` without mutating backend state.

## Autonomous Risk Monitoring Engine (Phase 11)

Phase 11 introduces an autonomous risk-monitoring engine (`MONITOR -> DETECT CHANGE -> PRIORITIZE -> EXPLAIN -> ALERT -> RECOMMEND`).

- **Safety Guarantee**: PayLens autonomously detects, prioritizes, and explains financial risks, but **never automatically executes financial actions, approves governance decisions, or mutates state**.
- **Change Detection & Deduplication**: Captures `RiskSnapshot` states to compute actual deltas (safety buffer drop, health status degradation, forecast breach). Deduplicates alerts via stable fingerprints (`riskSignalType:entityType:entityId`) to prevent alert spam.
- **Automatic Risk Resolution**: Automatically transitions open risk events to `RESOLVED` when underlying financial conditions clear.
- **Risk Event State Machine**: `OPEN` → `ACKNOWLEDGED` → `RESOLVED` or `DISMISSED`. Manual resolution validates backend conditions first (returns HTTP 409 Conflict if condition remains active).
- **Scheduled & Manual Monitoring**: Operates on a 5-minute Spring `@Scheduled` background cycle and `POST /api/risk-monitoring/run` manual trigger.
- **Risk Monitoring APIs**:
  - `POST /api/risk-monitoring/run`: Executes a single monitoring cycle.
  - `GET /api/risk-monitoring/status`: Returns monitoring status and telemetry metrics.
  - `GET /api/risk-events`: Retrieves filtered risk events (`status`, `severity`, `type`).
  - `GET /api/risk-events/{id}`: Returns complete risk event detail.
  - `POST /api/risk-events/{id}/acknowledge`: Acknowledges an open risk event.
  - `POST /api/risk-events/{id}/dismiss`: Dismisses a risk event with optional reason.
  - `POST /api/risk-events/{id}/resolve`: Manually resolves a risk event if backend condition has cleared.




