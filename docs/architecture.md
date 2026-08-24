# PayLens Phase 1 Architecture

PayLens is a financial safety layer that sits between an instruction and any eventual financial execution. In this phase, the backend provides deterministic financial-state reads only; it does not contain AI decision-making or payment execution.

```text
Client / future AI agent
          |
          v
 Express API controllers
          |
          v
 FinancialStateService (deterministic calculations)
          |
          v
 In-memory seeded financial state
```

The Express application exposes health, financial-state, and dashboard routes. Controllers only translate HTTP requests and responses; `FinancialStateService` owns the dashboard calculation. The state is seeded in memory for this phase and will be replaceable with persistent storage later.

Future phases may add simulation, policy enforcement, controlled execution, and audit persistence behind this boundary. They are intentionally out of scope for Phase 1.
