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

Example simulation request:

```json
{
  "actionType": "REFUND",
  "amount": 250000,
  "description": "Customer refund"
}
```

The simulation is side-effect free. It returns before/after financial snapshots, impacts, and a neutral consequence such as `OBLIGATION_SHORTFALL`; it does not execute the action or make a policy decision.
