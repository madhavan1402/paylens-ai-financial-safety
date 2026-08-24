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
