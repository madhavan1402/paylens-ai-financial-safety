# PayLens Phase 1 Architecture

PayLens is a financial safety and consequence layer placed before future financial execution. In Phase 1, Spring Boot is the authoritative financial core: it owns deterministic financial state and calculations, while no AI component may calculate financial safety.

```text
Client / future AI agent
          |
          v
Spring Boot REST controllers
          |
          v
FinancialStateService
          |
          v
InMemoryFinancialStateRepository
```

The repository contains fixed, realistic INR merchant data. Its domain models are `FinancialAccount`, `Transaction`, and `Obligation`; enums constrain transaction and obligation categories and lifecycle states. API DTO records define the public response contract. Controllers perform HTTP handling only, and `FinancialStateService` is the single location for financial calculations.

## Financial State Engine formulas

- Upcoming obligations = all obligations with status `UPCOMING` or `DUE`.
- Available liquidity = current balance − safety reserve.
- Remaining after obligations = current balance − upcoming obligations.
- Safety buffer = current balance − upcoming obligations − safety reserve.

All monetary values use `BigDecimal` and the seeded currency is INR. The deterministic seed has a ₹840,000 balance, ₹100,000 reserve, and ₹620,000 unpaid obligations.

## API

- `GET /api/health` returns service health.
- `GET /api/financial-state` returns account, transactions, obligations, and financial summary.
- `GET /api/dashboard` returns concise calculated metrics for a future frontend.

Simulation, policy decisions, risk scoring, execution, AI, databases, and authentication are deliberately outside Phase 1.

## Financial Consequence Simulation Engine

Phase 2 adds a separate, side-effect-free simulation layer. `SimulationService` obtains its before-state from `FinancialStateService`, then calculates a hypothetical after-state for outgoing refunds, vendor payments, payroll, or tax payments. It never writes to the repository or executes a payment.

The simulation returns before and after snapshots, changes to liquidity, obligation coverage, and safety buffer, plus neutral financial facts: `NORMAL`, `RESERVE_BREACH`, or `OBLIGATION_SHORTFALL`. These are not policy decisions. The backend, rather than an AI, remains responsible for all monetary calculations.

- After balance = current balance − proposed amount.
- After available liquidity = after balance − safety reserve.
- After remaining obligations = after balance − upcoming obligations.
- After safety buffer = after balance − upcoming obligations − safety reserve.

`POST /api/simulations` accepts a validated action and amount and returns this comparison without changing the authoritative financial state.

## Financial Policy & Risk Engine

Phase 3 separates policy from simulation. `PolicyController` delegates the proposed action to `SimulationService`, then passes the immutable simulation result to `PolicyService`. The policy layer performs no financial calculations, does not mutate state, and never uses an LLM.

Rules are evaluated in strict order: an obligation shortfall blocks first; a negative safety buffer then blocks; a non-negative safety buffer below the required margin requires review; otherwise the action is safe. The required margin is centrally defined by `PolicyThresholds` as one safety reserve. Each decision has deterministic reason and recommendation text.

`POST /api/policy/evaluate` returns `SAFE`, `REVIEW`, or `BLOCK` together with the full simulation that explains the decision. These decisions remain policy outcomes only; no execution occurs.

## AI Intent Agent

Phase 4 introduces an isolated FastAPI agent that extracts structured financial intent from natural language. It provides `POST /api/intent` and uses a deterministic parser whenever no external AI provider is configured, supporting INR, lakh/lac, and k notation. Ambiguous language produces a clarification request rather than invented financial data.

Spring Boot treats every agent response as untrusted. `POST /api/agent/analyze` validates the agent's action type, positive amount, INR currency, and description before passing it to the existing simulation and policy services. The Python agent cannot calculate financial safety, change state, or execute a payment; Spring Boot remains the financial authority.

## AI Consequence & Explanation Agent

## Frontend Architecture

The Phase 6 React application is a presentation layer with a persistent operations shell and routes for overview, AI Safety Center, transactions, and scoped empty states. Axios calls the Spring Boot API using `VITE_API_BASE_URL`; the browser never calls Python agents directly. The flow is React UI to Spring Boot API to intent/explanation agents and deterministic simulation/policy services. The Overview reads `/api/dashboard` and `/api/financial-state`; the Safety Center sends only a natural-language message to `/api/agent/analyze` and renders the returned authoritative intent, simulation, policy, and explanation. Local CORS is limited to the Vite development origin.


## Governance, Human Review Workflow, and Immutable Audit Architecture

Phase 7 completes the PayLens safety lifecycle: Intent → Financial State → Simulation → Policy → Explanation → Governance Persistence → Audit.

```text
POST /api/agent/analyze
        ↓
Intent Validation
        ↓
Financial Simulation
        ↓
Policy Evaluation (SAFE / REVIEW / BLOCK)
        ↓
AI Explanation
        ↓
GovernanceService (Persists DecisionRecord & Initial AuditEvents)
        ↓
Status: SAFE | PENDING_REVIEW | BLOCKED
        ↓
Human Review API (POST /api/decisions/{id}/approve | reject)
        ↓
Status: APPROVED | REJECTED
```

### State Machine Integrity
- `SAFE` (Terminal): Action satisfied financial safety policy.
- `BLOCKED` (Terminal): Action breached critical safety thresholds. **A BLOCKED decision can NEVER be approved or rejected by human review.**
- `PENDING_REVIEW`: Action requires human governance review.
- `APPROVED` (Terminal): Recorded human governance sign-off. **APPROVED means human governance approval only. No financial transaction or money movement is executed.**
- `REJECTED` (Terminal): Recorded human governance rejection.

### Audit Immutability
Audit records (`AuditEvent`) are created by backend services during evaluation, human review, and execution gateway events (`ACTION_ANALYZED`, `INTENT_PARSED`, `SIMULATION_COMPLETED`, `POLICY_EVALUATED`, `EXPLANATION_GENERATED`, `REVIEW_REQUESTED`, `REVIEW_APPROVED`, `REVIEW_REJECTED`, `ACTION_BLOCKED`, `EXECUTION_REQUESTED`, `EXECUTION_ELIGIBILITY_REJECTED`, `EXECUTION_STARTED`, `EXECUTION_SUCCEEDED`, `EXECUTION_FAILED`, `EXECUTION_DUPLICATE`, `EXECUTION_UNKNOWN`, `EXECUTION_UNSUPPORTED`). The audit trail is append-only with no modification or deletion endpoints.


## Controlled Financial Execution Gateway Architecture (Phase 8)

Phase 8 evolves PayLens from a safety & governance advisor to a controlled execution gateway.

```text
               EXECUTION REQUEST (POST /api/executions)
                                │
                                ▼
                        ExecutionService
                                │
                  1. Load persisted DecisionRecord
                  2. Validate Governance Status
                  3. Enforce Idempotency Key (DB Index)
                                │
                 +--------------+--------------+
                 │                             │
             ELIGIBLE                      DENIED
      (SAFE or APPROVED)        (PENDING_REVIEW / BLOCKED / REJECTED)
                 │                             │
                 ▼                             ▼
   PaymentExecutionProvider           ELIGIBILITY_REJECTED
   (Interface Abstraction)            (Audit log & return 422)
                 │
                 ▼
   RazorpayTestExecutionProvider
                 │
                 ▼
  Razorpay TEST API SDK (order / refund)
                 │
       +---------+---------+
       │         │         │
       ▼         ▼         ▼
   SUCCEEDED   FAILED   UNKNOWN
```

### Execution State Machine
- `REQUESTED` -> Initial execution request received.
- `ELIGIBILITY_REJECTED` -> Terminal state for governance denial (`PENDING_REVIEW`, `REJECTED`, `BLOCKED`).
- `PROCESSING` -> Execution submitted to provider.
- `SUCCEEDED` -> Provider confirmed test payment execution.
- `FAILED` -> Provider returned execution failure code/message.
- `DUPLICATE` -> Idempotency key match returned existing execution record.
- `UNKNOWN` -> Provider network timeout. **Requires manual reconciliation; automatic retry is disabled.**
- `UNSUPPORTED_EXECUTION` -> Action type (e.g. `PAYROLL`, `TAX_PAYMENT`) not supported by Razorpay TEST API.


## Reconciliation & Reliability Engine Architecture (Phase 9)

Phase 9 resolves execution uncertainties without ever automatically retrying the underlying financial operation.

```text
               UNKNOWN EXECUTION STATE
                          │
                          ▼
            POST /api/executions/{id}/reconcile
                          │
                          ▼
                ReconciliationService
                          │
            (Load Execution & Lock Execution ID)
                          │
                          ▼
            PaymentReconciliationProvider (Interface)
                          │
                          ▼
            RazorpayTestReconciliationProvider
            (SDK: razorpay.refunds.fetch(ref))
                          │
        +-----------------+-----------------+
        │                 │                 │
CONFIRMED_SUCCESS  CONFIRMED_FAILURE   NOT_FOUND / UNKNOWN
        │                 │                 │
        ▼                 ▼                 ▼
  Status: SUCCEEDED   Status: FAILED    Status: UNKNOWN
  Recon: CONFIRMED    Recon: FAILED     Recon: MANUAL_REVIEW
```

### Key Principles
1. **UNKNOWN ≠ FAILED**: An HTTP timeout does not imply transaction failure.
2. **Zero Automatic Financial Retries**: Uncertain financial actions are never re-executed automatically.
3. **Idempotency & Thread Safety**: Concurrency is locked per `executionId.intern()`. Duplicate calls return existing reconciliation state.
4. **Append-Only Reconciliation Audit**: Reconciliation lifecycle generates append-only events (`RECONCILIATION_REQUESTED`, `RECONCILIATION_STARTED`, `RECONCILIATION_CONFIRMED`, `RECONCILIATION_FAILED`, `RECONCILIATION_PENDING`, `RECONCILIATION_MANUAL_REVIEW`, `RECONCILIATION_NOT_FOUND`).


## Real-Time Financial Intelligence Engine (Phase 10)

Phase 10 transforms PayLens into a proactive financial intelligence platform.

- **Financial Health Score (0-100)**: Deterministically computed from `safetyBuffer`, `availableLiquidity`, and execution reliability penalties.
- **7-Day Liquidity Forecast**: Daily balance and safety buffer projections reusing Phase 2 formulas.
- **Scenario What-If Simulator**: Evaluates proposed action impact using `SimulationService` without mutating backend state.


## Autonomous Risk Monitoring Engine (Phase 11)

Phase 11 introduces autonomous risk monitoring (`MONITOR -> DETECT CHANGE -> PRIORITIZE -> EXPLAIN -> ALERT -> RECOMMEND`).

```text
Financial State & Phase 10 Intelligence
                ↓
RiskMonitoringService (5-min Scheduled & Manual API)
                ↓
Change Detection (RiskSnapshot Baseline & Delta Comparison)
                ↓
Deterministic Risk Detection Rules
(Buffer Drop, Health Degradation, Forecast Breach, Obligation Risk, Recon Required, Execution Failure, Revenue Surge, Liquidity Critical)
                ↓
Fingerprint Deduplication (prevent duplicate open alerts)
                ↓
RiskEvent Entity (OPEN → ACKNOWLEDGED → RESOLVED / DISMISSED)
                ↓
Deterministic Recommendation Engine
                ↓
Human Governance Review / Scenario Simulator Link
```

### Safety Principles & Rules
1. **No Automatic Execution**: PayLens autonomously monitors and alerts, but **never automatically executes payments, approves governance, or alters financial state**.
2. **Scheduled Monitoring**: Operates on a 5-minute Spring `@Scheduled` background cycle and `POST /api/risk-monitoring/run` manual trigger.
3. **Change Detection & Deduplication**: Baseline snapshots prevent false startup alerts. Stable fingerprints (`type:entityType:entityId`) update existing open risk events rather than creating alert spam.
4. **Backend-Authoritative State Machine**: Client requests cannot force active financial risks to `RESOLVED` unless the underlying financial condition has cleared.


## Production Security, Identity & Multi-Role Governance Architecture (Phase 12)

Phase 12 introduces secure identity, authentication, authorization, multi-role governance, and multi-tenant merchant isolation.

```text
User
 ↓
Authentication (BCrypt + JWT Access Tokens & Refresh Tokens)
 ↓
Authorization (RBAC: OWNER, ADMIN, FINANCE_MANAGER, REVIEWER, OPERATOR, VIEWER)
 ↓
Financial Policy (SAFE / REVIEW / BLOCK)
 ↓
Governance (PENDING_REVIEW → APPROVED / REJECTED)
 ↓
Execution Gateway (Razorpay TEST Provider)
 ↓
Reconciliation (Provider State Normalization)
```

### Security & Governance Principles
1. **Frontend Authorization is UX-Only**: Backend Spring Security rules are authoritative and return `403 FORBIDDEN` for unauthorized REST API attempts regardless of client buttons or parameters.
2. **Authentication Does Not Bypass Safety**: Passing authentication and role checks does not bypass financial policy rules (`SAFE`/`REVIEW`/`BLOCK`), governance approval (`PENDING_REVIEW` → `APPROVED`), or Razorpay TEST execution eligibility.
3. **Multi-Tenant Merchant Boundary**: Every authenticated user belongs to a `merchantId`. All financial records are isolated per merchant context.
4. **Credential Protection**: Passwords are saved strictly as BCrypt hashes. Passwords, JWT tokens, and Razorpay API secrets are never logged or exposed in audit events.





