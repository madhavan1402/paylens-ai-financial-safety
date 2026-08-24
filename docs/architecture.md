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
