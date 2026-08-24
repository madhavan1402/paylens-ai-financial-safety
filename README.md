# PayLens — AI Financial Safety & Consequence Engine

PayLens is a financial safety layer for AI agents and automated financial workflows. It helps turn a financial instruction into a deterministic view of balances, upcoming obligations, and protected reserves before a later policy engine decides whether an action is safe, needs review, or should be blocked. AI will explain intent in future phases; it will not be the authority that approves moving money.

## Current architecture

The Phase 1 backend is an Express and TypeScript API. Thin controllers delegate to an in-memory `FinancialStateService`, which supplies seeded state and deterministic dashboard metrics. No AI, payment provider integration, database, authentication, or execution capability is present.

## Local development

```bash
cd backend
npm install
npm run dev
```

The API starts on `http://localhost:4000` by default. Copy `.env.example` to `.env` to change configuration.

Other backend commands:

```bash
npm run typecheck
npm run build
npm run start
```

## Backend endpoints

- `GET /api/health` — service health status.
- `GET /api/financial-state` — seeded account, transactions, obligations, and reserve.
- `GET /api/dashboard` — deterministic current balance, obligations total, reserve, and available liquidity.
