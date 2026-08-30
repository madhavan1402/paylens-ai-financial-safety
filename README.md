# PayLens — AI Financial Safety & Consequence Engine

PayLens is an AI-powered financial safety and governance platform for automated financial workflows.

Before an AI-assisted financial action can reach payment infrastructure, PayLens evaluates the merchant's financial position, simulates consequences, applies deterministic financial policies, supports human governance, controls execution, and reconciles provider outcomes.

> **AI can understand and explain financial actions. PayLens decides whether they are safe.**

---

## 🚀 Key Features

### 💰 Financial Intelligence
- Financial state and liquidity analysis
- Upcoming obligations and safety reserve tracking
- Safety buffer calculation
- Financial health scoring
- 7-day liquidity forecasting
- What-if scenario simulation

### 🤖 AI Safety
- Natural-language financial intent detection
- AI-powered consequence explanations
- Structured AI output validation
- Deterministic fallback without requiring an AI API key
- AI cannot override financial policy

### 🛡️ Deterministic Policy Engine
Every financial action is evaluated as:

- 🟢 `SAFE`
- 🟡 `REVIEW`
- 🔴 `BLOCK`

The deterministic policy engine remains the authoritative decision-maker.

### 👨‍⚖️ Human Governance
- Persistent decision records
- Human approval and rejection workflows
- Protected governance state transitions
- Append-only audit trail

### 💳 Controlled Execution
- Explicit execution confirmation
- Server-side eligibility validation
- Database idempotency protection
- Razorpay TEST mode integration
- No automatic retry for unknown outcomes

### 🔄 Reconciliation & Reliability
- Provider outcome reconciliation
- `UNKNOWN` ≠ `FAILED`
- Manual review for unresolved outcomes
- Execution reliability metrics

### ⚠️ Autonomous Risk Monitoring
- Scheduled financial monitoring
- Risk detection and prioritization
- Alert deduplication
- Change detection
- Automatic resolution when conditions clear

### 🔐 Production Security
- JWT authentication
- BCrypt password hashing
- Refresh token rotation
- Account lockout after failed attempts
- Authentication rate limiting
- Role-Based Access Control (RBAC)
- Merchant isolation
- Security headers
- Security audit logging

### 🧠 AI Fintech Copilot
The final PayLens Copilot can:

- Answer financial questions
- Analyze financial actions
- Run consequence simulations
- Explain policy decisions
- Provide financial intelligence
- Surface risk insights

**The Copilot is read-only and cannot execute payments, approve decisions, override policies, or bypass governance.**

---

## 🏗️ Architecture

```text
User / Frontend
       │
       ▼
AI Fintech Copilot
       │
       ▼
Intent Understanding
       │
       ▼
Financial State Engine
       │
       ▼
Consequence Simulation
       │
       ▼
Deterministic Policy Engine
       │
       ▼
AI Explanation
       │
       ▼
Human Governance
       │
       ▼
Controlled Execution Gateway
       │
       ▼
Razorpay TEST Mode
       │
       ▼
Reconciliation
       │
       ▼
Financial Intelligence + Risk Monitoring
