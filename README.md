# PayLens — AI Financial Safety & Consequence Engine

PayLens is an AI-powered financial safety platform for merchants, AI agents, and automated financial workflows.

Before money can move, PayLens establishes the merchant's authoritative financial position, simulates the consequences of a proposed action, evaluates deterministic financial policy, applies human governance when required, controls TEST-mode execution, reconciles provider outcomes, monitors financial risk, and exposes the complete system through a secure AI Fintech Copilot.

The core principle is:

> **AI can understand, recommend, and explain financial actions — but AI is never the authority that decides whether money is safe to move or directly executes money movement.**

```text
User / AI Request
        │
        ▼
   Intent Detection
        │
        ▼
 Financial State Engine
        │
        ▼
 Consequence Simulation
        │
        ▼
 Deterministic Policy
        │
        ▼
 AI Explanation
        │
        ▼
 Human Governance
        │
        ▼
 Controlled TEST Execution
        │
        ▼
   Reconciliation
        │
        ▼
 Financial Intelligence
        │
        ▼
 Autonomous Risk Monitoring

 Technology Stack
Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT Authentication
- BCrypt Password Hashing
- Maven
- H2 Development Database
- Razorpay Java SDK
- REST APIs
AI Agent Layer
- Python
- FastAPI
- Deterministic AI fallback
- Structured intent extraction
- Structured financial explanation
Frontend
- React
- TypeScript
- Vite
- Modern fintech dashboard UI
Run the Backend
cd backend
./mvnw spring-boot:run
On Windows PowerShell:
cd backend
.\mvnw.cmd spring-boot:run
The backend runs on:
http://localhost:8080
Run backend tests:
./mvnw clean test
Windows:
.\mvnw.cmd clean test
Run the AI Agent
cd agents
python -m venv .venv
Activate the environment on Windows:
.venv\Scripts\activate
Install dependencies:
pip install -r requirements.txt
Run the FastAPI agent:
uvicorn app.main:app --reload --port 8000
The AI agent runs on:
http://localhost:8000
Run the Frontend
cd frontend
npm install
npm run dev
The frontend normally runs on:
http://localhost:5173
Configure the backend URL in:
frontend/.env
Example:
VITE_API_BASE_URL=http://localhost:8080
Phase 1 — Financial State Engine
Phase 1 establishes the deterministic financial foundation of PayLens.
The backend calculates the merchant's financial position using authoritative financial values.
Current Balance
        │
        ▼
Upcoming Obligations
        │
        ▼
Protected Safety Reserve
        │
        ▼
Available Liquidity
        │
        ▼
Safety Buffer
Example financial state:
{
  "currency": "INR",
  "currentBalance": 840000,
  "upcomingObligations": 620000,
  "safetyReserve": 100000,
  "availableLiquidity": 740000,
  "remainingAfterObligations": 220000,
  "safetyBuffer": 120000
}
Core APIs:
- GET /api/health
- GET /api/financial-state
- GET /api/dashboard
Phase 2 — Consequence Simulation
PayLens can simulate the financial consequences of a proposed action without executing it.
Example request:
{
  "actionType": "REFUND",
  "amount": 250000,
  "description": "Customer refund"
}
The simulation calculates:
- Financial position before the action
- Financial position after the action
- Liquidity impact
- Safety buffer impact
- Obligation coverage
- Neutral financial consequences
The simulation is completely side-effect free.
Simulation does not execute money movement.

API:
POST /api/simulations
Phase 3 — Deterministic Policy Engine
The Policy Engine is the authoritative source of truth for financial safety decisions.
AI does not determine whether an action is financially safe.
The deterministic backend evaluates actions and returns:
SAFE
REVIEW
BLOCK
Example:
- ₹20,000 refund → SAFE
- ₹50,000 refund → REVIEW
- ₹250,000 refund → BLOCK
A typical policy flow:
Simulation Result
       │
       ▼
Can obligations be covered?
       │
   ┌───┴────┐
   │        │
  NO       YES
   │        │
 BLOCK      ▼
       Safety margin preserved?
              │
         ┌────┴────┐
         │         │
        NO        YES
         │         │
      REVIEW      SAFE
API:
POST /api/policy/evaluate
Phase 4 — AI Intent Agent
The AI Intent Agent converts natural language into structured financial intent.
Example:
Refund ₹2.5 lakh to Rahul
The agent produces validated structured information such as:
- Action type
- Amount
- Currency
- Description
- Target information
The deterministic fallback supports common actions including:
- Refund
- Vendor payment
- Payroll
- Tax payment
It also understands:
- INR
- Lakh / Lac
- K notation
Ambiguous requests are not guessed.
PayLens asks for clarification instead of inventing financial information.

AI Agent API:
POST /api/intent
Phase 5 — AI Consequence & Explanation Agent
Phase 5 adds an explanation layer after deterministic analysis.
The flow is:
Natural Language
       │
       ▼
AI Intent
       │
       ▼
Validated by Spring Boot
       │
       ▼
Financial Simulation
       │
       ▼
Policy Decision
       │
       ▼
AI Explanation
The AI explanation receives authoritative structured facts from the backend.
It does not receive arbitrary client-provided financial values.
Example decisions:
- BLOCK: Insufficient funds for upcoming obligations.
- REVIEW: Obligations are covered, but the safety margin is reduced.
- SAFE: Obligation coverage and safety margin are preserved.
If an AI provider is unavailable or produces an invalid response, PayLens falls back to deterministic local explanations.
The AI explanation layer cannot change:
SAFE
REVIEW
BLOCK
APIs:
POST /api/agent/analyze
POST /api/agent/explain
Phase 6 — Fintech Command Center
PayLens provides a React-based fintech operations dashboard.
The frontend displays authoritative backend information including:
- Financial health
- Transactions
- Payments
- Refunds
- Customers
- Obligations
- Risk signals
- Simulations
- Decisions
- Executions
- Audit history
- AI Safety Center
- AI Copilot
The frontend does not independently calculate financial policy.
The backend remains authoritative.

Phase 7 — Governance, Human Review & Append-Only Audit Trail
Phase 7 introduces persistent decisions and human governance.
Every financial analysis is stored as a DecisionRecord.
A decision contains:
- Original user request
- Structured intent
- Simulation result
- Policy decision
- AI explanation
- Governance status
Decision states:
SAFE
BLOCKED
PENDING_REVIEW
APPROVED
REJECTED
State transitions:
PENDING_REVIEW
      │
 ┌────┴─────┐
 ▼          ▼
APPROVED   REJECTED
Rules:
- SAFE is terminal.
- BLOCKED is terminal.
- PENDING_REVIEW can become APPROVED.
- PENDING_REVIEW can become REJECTED.
- Invalid transitions return HTTP 409 Conflict.
- A BLOCKED decision can never be approved.
Important:
APPROVED means governance approval. Approval alone does not bypass execution controls.

Append-Only Audit Trail
Audit events record important system activity.
Examples:
ACTION_ANALYZED
POLICY_EVALUATED
REVIEW_REQUESTED
REVIEW_APPROVED
REVIEW_REJECTED
ACTION_BLOCKED
Audit records are append-only.
There are no update or delete APIs for audit history.
APIs:
- GET /api/decisions
- GET /api/decisions/{id}
- POST /api/decisions/{id}/approve
- POST /api/decisions/{id}/reject
- GET /api/audit
Phase 8 — Controlled Financial Execution Gateway
Phase 8 introduces controlled execution through Razorpay TEST mode.
Architecture:
Decision (SAFE / APPROVED)
           │
           ▼
    ExecutionService
           │
           ▼
Eligibility Validation
           │
           ▼
Idempotency Validation
           │
           ▼
PaymentExecutionProvider
           │
           ▼
Razorpay TEST Provider
           │
           ▼
     Razorpay TEST API
The execution service never trusts client or AI input.
It independently loads the persisted DecisionRecord.
Eligibility Rules
SAFE
   │
   └── Eligible after explicit user confirmation

APPROVED
   │
   └── Eligible after human governance approval

PENDING_REVIEW
   │
   └── DENIED

REJECTED
   │
   └── DENIED

BLOCKED
   │
   └── DENIED
A BLOCKED decision can never reach payment infrastructure.
Idempotency
ExecutionRecord uses an idempotency key.
Duplicate execution requests return the existing result instead of executing the provider twice.
Timeout Safety
Network uncertainty produces:
UNKNOWN
UNKNOWN does not automatically trigger another financial execution.
Manual reconciliation is required.
Provider Support
The provider abstraction supports controlled TEST-mode execution.
Unsupported actions return:
UNSUPPORTED_EXECUTION
PayLens never fabricates payment references.
Execution APIs:
- POST /api/executions
- GET /api/executions
- GET /api/executions/{id}
- GET /api/decisions/{id}/execution
Phase 9 — Reconciliation & Reliability Engine
Phase 9 answers a critical financial question:
PayLens requested an execution. What actually happened at the provider?

Architecture:
EXECUTION
   │
   ▼
Provider Response
   │
   ▼
Execution Status
   │
   ▼
ReconciliationService
   │
   ▼
Provider Status Check
   │
   ▼
Normalized Outcome
Possible outcomes:
CONFIRMED_SUCCESS
CONFIRMED_FAILURE
STILL_PROCESSING
NOT_FOUND
UNKNOWN
Normalization:
CONFIRMED_SUCCESS
        │
        ▼
Execution → SUCCEEDED
Reconciliation → CONFIRMED


CONFIRMED_FAILURE
        │
        ▼
Execution → FAILED
Reconciliation → FAILED


STILL_PROCESSING
        │
        ▼
Reconciliation → PENDING


NOT_FOUND / UNKNOWN
        │
        ▼
MANUAL_REVIEW_REQUIRED
Important principle:
UNKNOWN does not mean FAILED.

PayLens never automatically retries uncertain financial executions.
It reconciles provider state first.
Reliability Metrics
PayLens calculates resolved execution reliability:
confirmedSuccess
────────────────────────────────
confirmedSuccess + confirmedFailure
Unresolved UNKNOWN and PENDING outcomes are excluded.
APIs:
- POST /api/executions/{id}/reconcile
- GET /api/executions/{id}/reconciliation
- GET /api/reconciliations
- GET /api/reconciliations/{id}
- GET /api/reconciliations/metrics
Phase 10 — Real-Time Financial Intelligence
Phase 10 transforms PayLens from a reactive analyzer into a proactive financial intelligence platform.
PayLens continuously evaluates financial health.
Transactions
      │
      ▼
Financial Events
      │
      ▼
Financial State
      │
      ▼
Financial Intelligence
      │
      ▼
Risk Signals
Features include:
Financial Health Score
A deterministic score from 0–100 based on factors such as:
- Safety buffer
- Available liquidity
- Financial pressure
- Execution reliability
7-Day Liquidity Forecast
PayLens projects:
- Daily balances
- Upcoming financial pressure
- Projected safety buffers
- Forecast breaches
Scenario What-If Simulator
Users can test hypothetical financial actions without mutating the backend state.
Examples:
What happens if I refund ₹2,00,000?

What happens if vendor payment increases?

What happens if multiple obligations occur together?
All calculations reuse the authoritative deterministic financial logic.
Phase 11 — Autonomous Risk Monitoring
Phase 11 introduces autonomous financial risk monitoring.
Architecture:
MONITOR
   │
   ▼
DETECT CHANGE
   │
   ▼
PRIORITIZE
   │
   ▼
EXPLAIN
   │
   ▼
ALERT
   │
   ▼
RECOMMEND
PayLens can proactively detect financial risk instead of waiting for the user to ask.
Examples:
⚠ Projected safety buffer will fall below threshold.

🔴 Multiple large obligations are approaching.

🟡 Failed-payment trends have increased.

⚠ Financial health has degraded.
Safety Guarantee
Autonomous monitoring can:
- Detect risk
- Prioritize risk
- Explain risk
- Create alerts
- Recommend actions
It cannot:
- Execute financial actions
- Approve governance decisions
- Override policy
- Move money automatically
Risk Change Detection
PayLens stores RiskSnapshot states and compares changes over time.
It can detect:
- Safety buffer drops
- Health degradation
- Forecast breaches
- Financial pressure changes
Alert Deduplication
Risk events use stable fingerprints:
riskSignalType:entityType:entityId
This prevents duplicate alert spam.
Risk Event State Machine
OPEN
 │
 ▼
ACKNOWLEDGED
 │
 ├──────────────► RESOLVED
 │
 └──────────────► DISMISSED
Automatic resolution occurs when the underlying risk condition clears.
Manual resolution validates backend conditions first.
If the risk still exists:
HTTP 409 Conflict
Monitoring Modes
PayLens supports:
- Scheduled monitoring every 5 minutes
- Manual monitoring trigger
APIs:
- POST /api/risk-monitoring/run
- GET /api/risk-monitoring/status
- GET /api/risk-events
- GET /api/risk-events/{id}
- POST /api/risk-events/{id}/acknowledge
- POST /api/risk-events/{id}/dismiss
- POST /api/risk-events/{id}/resolve
Phase 12 — Production Security, Identity & Multi-Role Governance
Phase 12 adds authentication, authorization, merchant isolation, and production-oriented security controls.
Core principle:
Authentication and authorization happen before financial policy, governance approval, and execution eligibility.

User
 │
 ▼
Authentication
 │
 ▼
Authorization
 │
 ▼
Merchant Isolation
 │
 ▼
Financial Policy
 │
 ▼
Governance
 │
 ▼
Execution Eligibility
Authentication
PayLens uses:
- JWT access tokens
- 15-minute access token expiry
- Refresh token rotation
- Refresh token revocation
- BCrypt password hashing
Passwords are never stored in plaintext.
Account Protection
Security features include:
- Account lockout after 5 failed login attempts
- 15-minute account lock period
- Login failure persistence
- Security audit logging
- Authentication rate limiting
Role-Based Access Control
Supported roles:
OWNER
Full merchant operations and administration.
ADMIN
Operational administration and user management.
FINANCE_MANAGER
Can perform financial operations and governance actions.
REVIEWER
Can approve or reject governance decisions.
Cannot execute financial actions.
OPERATOR
Can execute authorized TEST-mode financial actions.
Cannot approve governance decisions.
VIEWER
Read-only access to:
- Dashboard
- Financial intelligence
- Risks
- Decisions
- Executions
- Audit history
Security Controls
- JWT authentication
- BCrypt passwords
- Account lockout
- Authentication rate limiting
- Restricted CORS
- Security headers
- Merchant isolation
- RBAC authorization
- Security audit events
Security audit examples:
LOGIN_SUCCESS
LOGIN_FAILURE
LOGOUT
USER_CREATED
USER_DISABLED
ROLE_CHANGED
ACCESS_DENIED
SESSION_REVOKED
Phase 13 — Final AI Fintech Copilot
Phase 13 combines the PayLens architecture into a single secure AI Fintech Copilot.
Instead of a generic chatbot, the Copilot orchestrates the existing PayLens financial safety system.
The Copilot flow:
User Question
      │
      ▼
Copilot Intent
      │
      ▼
Financial State
      │
      ▼
Simulation
      │
      ▼
Policy Evaluation
      │
      ▼
Risk / Governance Context
      │
      ▼
Structured Explanation
      │
      ▼
Copilot Response
Example user request:
Can I refund ₹80,000?
The Copilot can explain:
Decision: SAFE

Current Liquidity: ₹8.4L
After Action: ₹7.6L
Safety Buffer: ₹4.5L

Policy Result:
SAFE

Execution:
Available only through the controlled TEST execution flow.
Copilot Intent Categories
The Copilot supports six intent categories for financial intelligence and safety queries.
It can route requests to the appropriate deterministic PayLens capabilities instead of inventing financial answers.
Structured Responses
The Copilot returns structured responses containing financial context such as:
- User intent
- Financial situation
- Simulation result
- Policy decision
- Explanation
- Recommended next step
Core Safety Guarantee
The Copilot is read-only.
It does not call ExecutionService.
It cannot:
❌ Approve a governance decision
❌ Override SAFE / REVIEW / BLOCK
❌ Execute a payment
❌ Move money
❌ Bypass human governance
❌ Bypass Razorpay execution controls
The Copilot can:
✅ Understand the request
✅ Retrieve financial context
✅ Run deterministic analysis
✅ Explain consequences
✅ Explain policy decisions
✅ Recommend the next safe step
Deterministic Policy Remains Authoritative
The Copilot never decides whether an action is financially safe.
The authoritative source remains:
Deterministic Policy Engine
The Copilot explains the result.
Policy Engine → Decision
Copilot → Explanation
Copilot Audit Trail
Every Copilot query creates an audit event:
COPILOT_QUERY
This ensures AI interactions remain traceable.
Security
Copilot queries require JWT authentication.
API:
POST /api/copilot/query
Complete Security Architecture
                USER
                  │
                  ▼
        JWT Authentication
                  │
                  ▼
          Role Authorization
                  │
                  ▼
         Merchant Isolation
                  │
                  ▼
             AI Copilot
                  │
                  ▼
          Intent Detection
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
          ┌───────┼────────┐
          ▼       ▼        ▼
        SAFE    REVIEW    BLOCK
          │       │        │
          │       ▼        │
          │ Human Review   │
          │       │        │
          │    APPROVED    │
          │       │        │
          └───────┼────────┘
                  ▼
     Controlled TEST Execution
                  │
                  ▼
        Provider Reconciliation
                  │
                  ▼
        Reliability Intelligence
                  │
                  ▼
        Autonomous Risk Monitoring
Frontend Command Center
The React/Vite frontend provides a fintech operations interface for PayLens.
Major areas include:
Overview
Payments
Transactions
Customers
Refunds

Financial Intelligence
├── Obligations
├── Risk Center
└── Scenario Simulator

AI Safety
├── AI Copilot
├── Safety Center
├── Simulations
├── Decisions
└── Executions

Audit
Settings
The frontend presents authoritative backend results.
It does not independently determine:
SAFE
REVIEW
BLOCK
Important Safety Principles
AI Never Has Direct Money Access
AI
 │
 ▼
Analysis / Recommendation
 │
 ✘
No Direct Payment Access
Policy Is Deterministic
AI cannot override:
SAFE
REVIEW
BLOCK
Governance Is Enforced
Actions requiring review must pass through human governance.
Execution Is Controlled
Execution happens only through the backend eligibility gateway.
TEST Mode
Razorpay integration remains restricted to TEST mode.
UNKNOWN Is Not Retried Automatically
Financial uncertainty is reconciled before any further action.
Audit Is Append-Only
Important system and security events remain traceable.
Production Limitations
The current project uses some development-oriented components.
For full production deployment:
- Replace H2 with managed PostgreSQL.
- Replace in-memory rate limiting with distributed rate limiting such as Redis.
- Store secrets in a secure secret manager.
- Configure production HTTPS.
- Add production observability and centralized logging.
- Deploy with distributed monitoring infrastructure.
Razorpay execution remains:
TEST MODE ONLY
Real-money autonomous execution is intentionally not enabled.
Testing
The complete project has been verified with backend and frontend builds.
Backend:
mvn clean test
Result:
90 tests
0 failures
0 errors
BUILD SUCCESS
Frontend:
npm run build
Result:
SUCCESS
Final Architecture Principle
PayLens does not give AI direct control over financial infrastructure.
Instead, it places multiple deterministic and governed layers between AI and money movement:
AI Intelligence
       │
       ▼
Financial State
       │
       ▼
Simulation
       │
       ▼
Deterministic Policy
       │
       ▼
Human Governance
       │
       ▼
Controlled Execution
       │
       ▼
Reconciliation
       │
       ▼
Risk Monitoring
       │
       ▼
Audit & Security
Final Statement
PayLens doesn't give AI direct access to money. It puts financial intelligence, consequence simulation, deterministic policy, human governance, controlled execution, reconciliation, autonomous risk monitoring, and security controls between AI and financial infrastructure.
        │
        ▼
   AI Fintech Copilot
