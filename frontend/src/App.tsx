import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  Activity, AlertTriangle, Bot, Calendar, Check, CheckCheck, CheckCircle2,
  CircleDollarSign, ClipboardList, CreditCard, Gauge, Landmark, LoaderCircle,
  LogOut, Menu, Play, RefreshCw, Settings, ShieldCheck, Sliders, TrendingUp, Users, WalletCards, XCircle, X, Zap, Info
} from 'lucide-react'
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { analyzeAction } from './api/agentApi'
import { getDashboard, getFinancialState } from './api/dashboardApi'
import { getAuditEvents, getDecisionDetail, getDecisions } from './api/decisionsApi'
import { executeDecision, getDecisionExecution, getExecutionDetail, getExecutions } from './api/executionsApi'
import { getReconciliationHistory, getReliabilityMetrics, triggerReconciliation } from './api/reconciliationsApi'
import { getIntelligenceSummary, getObligationRisks, simulateForecastScenario } from './api/intelligenceApi'
import { acknowledgeRiskEvent, dismissRiskEvent, getMonitoringStatus, getRiskEvents, resolveRiskEvent, runMonitoringCycle } from './api/riskMonitoringApi'
import type {
  AgentAnalysisResponse, AuditEvent, DashboardResponse,
  DecisionDetail, DecisionSummary, ExecutionResponse, ExecutionSummary, FinancialStateResponse, ForecastScenarioResponse, GovernanceStatus,
  IntelligenceSummaryResponse, MonitoringStatusResponse, ObligationRiskItem, ReconciliationSummary, ReliabilityMetrics, RiskEventResponse, RiskEventStatus, Transaction
} from './types/api'
import { useAuth } from './context/AuthContext'
import { LoginPage } from './pages/LoginPage'
import { UserManagementPage } from './pages/UserManagementPage'
import { SecuritySettingsPage } from './pages/SecuritySettingsPage'
import './App.css'

const money = (value: number | undefined) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value ?? 0)

const labels: Record<string, string> = {
  currentBalance: 'Current balance',
  upcomingObligations: 'Upcoming obligations',
  safetyReserve: 'Safety reserve',
  availableLiquidity: 'Available liquidity',
  remainingAfterObligations: 'Remaining after obligations',
  safetyBuffer: 'Safety buffer'
}

const nav = [
  ['Overview', '/', Gauge],
  ['Payments', '#', WalletCards],
  ['Transactions', '/transactions', CreditCard],
  ['Customers', '/customers', Users],
  ['Refunds', '#', CircleDollarSign],
  ['FINANCIAL INTELLIGENCE', '', null],
  ['Obligations', '/obligations', Calendar],
  ['Risk Center', '/risk', AlertTriangle],
  ['Scenario Simulator', '/simulator', Sliders],
  ['AI SAFETY', '', null],
  ['Safety Center', '/safety', ShieldCheck],
  ['Simulations', '/simulations', Landmark],
  ['Decisions', '/decisions', CheckCircle2],
  ['Executions', '/executions', Zap],
  ['Reconciliations', '/reconciliations', RefreshCw],
  ['Audit Log', '/audit', ClipboardList],
  ['Settings', '/settings', Settings]
] as const

const prompts = ['Refund ₹2.5 lakh to Rahul', 'Pay ₹80,000 to ABC Suppliers', 'Process payroll ₹3 lakh', 'Pay ₹1 lakh tax']

function StatusBadge({ value }: { value: string }) {
  const isSafe = value === 'SAFE' || value === 'APPROVED' || value === 'SUCCEEDED' || value === 'CONFIRMED' || value === 'HEALTHY'
  const isReview = value === 'PENDING_REVIEW' || value === 'REVIEW' || value === 'REQUESTED' || value === 'PROCESSING' || value === 'PENDING' || value === 'MANUAL_REVIEW_REQUIRED'
  const Icon = isSafe ? CheckCircle2 : isReview ? AlertTriangle : XCircle
  const displayLabel = value === 'REVIEW' ? 'PENDING REVIEW' : value.replace(/_/g, ' ')
  const className = value.toLowerCase().replace(/_/g, '-')
  return (
    <span className={`badge ${className}`}>
      <Icon size={14} />
      {displayLabel}
    </span>
  )
}

function Loading({ children }: { children: ReactNode }) {
  return <div className="loading"><LoaderCircle className="spin" /> {children}</div>
}

function Empty({ title, text }: { title: string; text: string }) {
  return <section className="empty"><ClipboardList size={28} /><h2>{title}</h2><p>{text}</p></section>
}

function Shell({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const path = useLocation().pathname
  const { user, merchantName, logout, hasRole } = useAuth()
  const title = path === '/' ? 'Overview' : path === '/safety' ? 'AI Safety Center' : path === '/executions' ? 'Execution Gateway' : path === '/users' ? 'User Management' : path.slice(1).replace(/^./, c => c.toUpperCase())

  const sidebarNav = [
    ...nav,
    ...(hasRole(['OWNER', 'ADMIN']) ? [['USER GOVERNANCE', '', null], ['User Management', '/users', Users]] as const : [])
  ]

  return (
    <div className="app-shell">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand"><b>P</b>PAYLENS</div>
        <p className="brand-subtitle">{merchantName}</p>
        <nav>
          {sidebarNav.map(([name, href, Icon]) =>
            Icon ? href === '#' ? (
              <span className="nav-disabled" key={name}><Icon size={18} />{name}</span>
            ) : (
              <NavLink key={name} to={href} onClick={() => setOpen(false)}><Icon size={18} />{name}</NavLink>
            ) : (
              <span className="nav-group" key={name}>{name}</span>
            )
          )}
        </nav>
      </aside>
      <main>
        <header>
          <button className="menu-button" onClick={() => setOpen(!open)} aria-label="Toggle navigation"><Menu size={20} /></button>
          <div>
            <p className="eyebrow">PAYLENS OPERATIONS — {merchantName.toUpperCase()}</p>
            <h1>{title}</h1>
          </div>
          <div className="top-actions">
            <span className="test-mode">Razorpay Test Mode</span>
            {user && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', background: 'var(--panel-light)', padding: '0.4rem 0.75rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', fontSize: '0.75rem' }}>
                  <strong style={{ color: '#f8fafc' }}>{user.displayName}</strong>
                  <span style={{ color: '#00f2fe', fontWeight: 600, fontSize: '0.65rem' }}>{user.role.replace('_', ' ')}</span>
                </div>
                <button
                  className="icon-button"
                  onClick={logout}
                  title="Sign Out"
                  style={{ color: '#ef4444', background: 'rgba(239, 68, 68, 0.1)' }}
                >
                  <LogOut size={16} />
                </button>
              </div>
            )}
          </div>
        </header>
        {children}
      </main>
    </div>
  )
}

function ExecutionModal({
  decisionId,
  actionType,
  amount,
  currency,
  target,
  status: govStatus,
  onClose,
  onSuccess
}: {
  decisionId: string
  actionType: string
  amount: number
  currency: string
  target?: string
  status: GovernanceStatus
  onClose: () => void
  onSuccess: (result: ExecutionResponse) => void
}) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<ExecutionResponse | null>(null)

  const handleConfirm = async () => {
    setLoading(true)
    setError(null)
    const idempotencyKey = `exec_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`
    try {
      const res = await executeDecision({ decisionId, idempotencyKey })
      setResult(res)
      onSuccess(res)
    } catch (err: any) {
      if (err.response?.data?.failureMessage) {
        setError(err.response.data.failureMessage)
      } else if (err.response?.data?.error) {
        setError(err.response.data.error)
      } else {
        setError('Execution failed. Provider communication error.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h3>Execute Test Payment</h3>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="modal-body">
          <div className="warning-box">
            <Info size={18} />
            <span>TEST MODE — NO REAL MONEY MOVED. Controlled execution via Razorpay TEST provider.</span>
          </div>

          {!result ? (
            <>
              <p style={{ fontSize: '0.9rem', color: 'var(--muted)' }}>
                You are about to trigger a test-mode payment execution for decision <code style={{ color: 'var(--primary)' }}>{decisionId}</code>.
              </p>

              <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.85rem' }}>
                  <div><span style={{ color: 'var(--muted)' }}>Action:</span> <strong>{actionType}</strong></div>
                  <div><span style={{ color: 'var(--muted)' }}>Amount:</span> <strong>{money(amount)} {currency}</strong></div>
                  <div><span style={{ color: 'var(--muted)' }}>Target:</span> <strong>{target || 'N/A'}</strong></div>
                  <div><span style={{ color: 'var(--muted)' }}>Governance:</span> <StatusBadge value={govStatus} /></div>
                  <div><span style={{ color: 'var(--muted)' }}>Provider:</span> <strong>Razorpay TEST</strong></div>
                </div>
              </div>

              {error && (
                <div style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', padding: '0.75rem', borderRadius: '8px', fontSize: '0.85rem' }}>
                  {error}
                </div>
              )}
            </>
          ) : (
            <div className={`execution-result-card ${result.status.toLowerCase()}`}>
              <div className="exec-header">
                <h3>
                  {result.status === 'SUCCEEDED' ? (
                    <><CheckCircle2 style={{ color: 'var(--safe)' }} /> TEST EXECUTION SUCCEEDED</>
                  ) : result.status === 'UNKNOWN' ? (
                    <><AlertTriangle style={{ color: 'var(--review)' }} /> OUTCOME UNKNOWN</>
                  ) : (
                    <><XCircle style={{ color: 'var(--block)' }} /> EXECUTION FAILED</>
                  )}
                </h3>
                <StatusBadge value={result.status} />
              </div>

              {result.status === 'UNKNOWN' && (
                <p style={{ fontSize: '0.85rem', color: 'var(--review)', margin: '0.5rem 0' }}>
                  Execution outcome could not be confirmed. Provider timed out. Manual reconciliation required.
                </p>
              )}

              <div className="exec-details-grid">
                <div className="exec-field"><label>Execution ID</label><span>{result.executionId}</span></div>
                <div className="exec-field"><label>Provider</label><span>{result.provider}</span></div>
                <div className="exec-field"><label>Reference</label><span>{result.providerReference || 'N/A'}</span></div>
                <div className="exec-field"><label>Status</label><span>{result.status}</span></div>
              </div>

              {result.failureMessage && (
                <p style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.75rem' }}>
                  Reason: {result.failureMessage}
                </p>
              )}
            </div>
          )}
        </div>
        <div className="modal-footer">
          {!result ? (
            <>
              <button className="btn-secondary" onClick={onClose} disabled={loading}>Cancel</button>
              <button className="btn-execute" onClick={handleConfirm} disabled={loading}>
                {loading ? <LoaderCircle className="spin" size={16} /> : <Zap size={16} />}
                Confirm & Execute Test Payment
              </button>
            </>
          ) : (
            <button className="btn-primary" onClick={onClose}>Close</button>
          )}
        </div>
      </div>
    </div>
  )
}

function OverviewPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [state, setState] = useState<FinancialStateResponse | null>(null)
  const [intel, setIntel] = useState<IntelligenceSummaryResponse | null>(null)

  useEffect(() => {
    getDashboard().then(setData).catch(console.error)
    getFinancialState().then(setState).catch(console.error)
    getIntelligenceSummary().then(setIntel).catch(console.error)
  }, [])

  if (!data || !state) return <Loading>Loading dashboard metrics...</Loading>

  const forecastChartData = intel?.forecast.forecastDaysList.map(d => ({
    date: d.date.slice(5),
    balance: d.projectedBalance,
    buffer: d.projectedSafetyBuffer
  })) || []

  return (
    <>
      {intel && (
        <div className="card" style={{ marginBottom: '1.5rem', background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.8), rgba(15, 23, 42, 0.95))', border: '1px solid var(--border)' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', alignItems: 'center' }}>
            <div>
              <p className="eyebrow">FINANCIAL HEALTH SCORE</p>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.75rem', marginTop: '0.25rem' }}>
                <span style={{ fontSize: '2.5rem', fontWeight: 800, color: intel.health.healthScore >= 80 ? '#22c55e' : intel.health.healthScore >= 60 ? '#eab308' : '#ef4444' }}>
                  {intel.health.healthScore}
                </span>
                <span style={{ color: 'var(--muted)', fontSize: '1.1rem' }}>/ 100</span>
                <StatusBadge value={intel.health.healthStatus} />
              </div>
            </div>
            <div>
              <p className="eyebrow">SAFETY BUFFER</p>
              <h3 style={{ fontSize: '1.4rem', color: 'var(--primary)', margin: '0.25rem 0' }}>{money(intel.health.safetyBuffer)}</h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>Reserve margin: {money(intel.health.safetyReserve)}</p>
            </div>
            <div>
              <p className="eyebrow">REVENUE AT RISK</p>
              <h3 style={{ fontSize: '1.4rem', color: intel.revenueAtRisk.totalAmount > 0 ? '#ef4444' : '#22c55e', margin: '0.25rem 0' }}>
                {money(intel.revenueAtRisk.totalAmount)}
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>{intel.revenueAtRisk.caseCount} unconfirmed / failed execution(s)</p>
            </div>
          </div>
        </div>
      )}

      {intel && intel.activeSignals.length > 0 && (
        <div className="card" style={{ marginBottom: '1.5rem', borderLeft: '4px solid #eab308' }}>
          <div className="card-header" style={{ marginBottom: '0.5rem' }}>
            <h3 style={{ fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#eab308' }}>
              <AlertTriangle size={18} /> ACTIVE FINANCIAL RISK SIGNALS ({intel.activeSignals.length})
            </h3>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {intel.activeSignals.slice(0, 2).map(s => (
              <div key={s.signalId} style={{ background: 'var(--panel-light)', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.85rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                  <strong>{s.title}</strong>
                  <span className={`badge ${s.severity === 'HIGH' || s.severity === 'CRITICAL' ? 'blocked' : 'review'}`}>{s.severity}</span>
                </div>
                <p style={{ margin: 0, color: 'var(--muted)' }}>{s.description}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="metrics-grid">
        {Object.entries(data).filter(([k]) => k !== 'currency').map(([k, v]) => (
          <div className="metric-card" key={k}>
            <span>{labels[k] ?? k}</span>
            <strong>{money(v)}</strong>
          </div>
        ))}
      </div>

      {forecastChartData.length > 0 && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">
            <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <TrendingUp size={18} style={{ color: 'var(--primary)' }} /> 7-Day Liquidity & Safety Buffer Forecast
            </h2>
            <span className="badge" style={{ fontSize: '0.7rem' }}>Confidence: {intel?.forecast.confidence}</span>
          </div>
          <div style={{ width: '100%', height: 220, marginTop: '1rem' }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={forecastChartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="date" stroke="var(--muted)" fontSize={12} />
                <YAxis stroke="var(--muted)" fontSize={12} tickFormatter={val => `₹${val/1000}k`} />
                <Tooltip formatter={(value: any) => money(Number(value))} contentStyle={{ background: '#0f172a', border: '1px solid var(--border)', borderRadius: '8px' }} />
                <Area type="monotone" dataKey="balance" name="Projected Balance" stroke="#00f2fe" fill="rgba(0, 242, 254, 0.15)" />
                <Area type="monotone" dataKey="buffer" name="Projected Safety Buffer" stroke="#3b82f6" fill="rgba(59, 130, 246, 0.15)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <p style={{ fontSize: '0.75rem', color: 'var(--muted)', marginTop: '0.5rem', fontStyle: 'italic' }}>
            Note: {intel?.forecast.assumptions[0]} {intel?.forecast.assumptions[1]}
          </p>
        </div>
      )}

      <div className="card">
        <div className="card-header"><h2 className="card-title">Recent Financial State Transactions</h2></div>
        <div className="table-container">
          <table>
            <thead>
              <tr><th>ID</th><th>Type</th><th>Amount</th><th>Description</th><th>Status</th></tr>
            </thead>
            <tbody>
              {state.transactions.map((t: Transaction) => (
                <tr key={t.id}>
                  <td><code>{t.id}</code></td>
                  <td>{t.type}</td>
                  <td><strong>{money(t.amount)}</strong></td>
                  <td>{t.description}</td>
                  <td><span className="badge safe">{t.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}

function SafetyPage() {
  const [msg, setMsg] = useState('Pay ₹1000 to vendor for supplies')
  const [loading, setLoading] = useState(false)
  const [res, setRes] = useState<AgentAnalysisResponse | null>(null)
  const [execModalOpen, setExecModalOpen] = useState(false)
  const [execResult, setExecResult] = useState<ExecutionResponse | null>(null)

  const handleAnalyze = async (text: string) => {
    setLoading(true)
    setExecResult(null)
    try {
      const data = await analyzeAction(text)
      setRes(data)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="card">
        <h2 className="card-title" style={{ marginBottom: '1rem' }}>Analyze Financial Action Intent</h2>
        <div className="prompt-bar">
          {prompts.map(p => (
            <button className="prompt-chip" key={p} onClick={() => { setMsg(p); handleAnalyze(p); }}>{p}</button>
          ))}
        </div>
        <div className="analysis-input">
          <input value={msg} onChange={e => setMsg(e.target.value)} placeholder="Type natural language financial intent..." />
          <button className="btn-primary" onClick={() => handleAnalyze(msg)} disabled={loading}>
            {loading ? <LoaderCircle className="spin" size={16} /> : <Bot size={16} />}
            Analyze Safety
          </button>
        </div>
      </div>

      {res && (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
          {res.status === 'NEEDS_CLARIFICATION' ? (
            <div className="card"><AlertTriangle size={24} color="#f59e0b" /><h3>Clarification Needed</h3><p>{res.clarificationMessage}</p></div>
          ) : (
            <div className="card">
              <div className="card-header">
                <div>
                  <p className="eyebrow">DETERMINISTIC SAFETY ANALYSIS</p>
                  <h2 className="card-title">{res.message}</h2>
                </div>
                {res.governance && <StatusBadge value={res.governance.status} />}
              </div>

              {res.simulation && res.policy && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
                  <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
                    <h3 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--muted)' }}>FINANCIAL IMPACT SIMULATION</h3>
                    <p>Liquidity Change: <strong>{money(res.simulation.impact.liquidityChange)}</strong></p>
                    <p>Buffer Change: <strong>{money(res.simulation.impact.safetyBufferChange)}</strong></p>
                    <p style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>{res.simulation.consequence}</p>
                  </div>
                  <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
                    <h3 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', color: 'var(--muted)' }}>DETERMINISTIC POLICY EVALUATION</h3>
                    <p style={{ fontSize: '1rem', fontWeight: '700', margin: '0.25rem 0' }}>{res.policy.decision}</p>
                    <p style={{ fontSize: '0.85rem' }}>{res.policy.reason}</p>
                  </div>
                </div>
              )}

              {res.governance && (
                <div className="gateway-banner">
                  <div className="gateway-info">
                    <h4><Zap size={18} style={{ color: 'var(--primary)' }} /> Controlled Financial Execution Gateway</h4>
                    <p>
                      {res.governance.status === 'SAFE' && 'Decision is SAFE. Eligible for controlled Razorpay TEST execution upon user confirmation.'}
                      {res.governance.status === 'APPROVED' && 'Decision is APPROVED by governance. Eligible for controlled Razorpay TEST execution.'}
                      {res.governance.status === 'PENDING_REVIEW' && 'Decision requires human governance review. Execution is DENIED until approved.'}
                      {res.governance.status === 'REJECTED' && 'Decision was REJECTED by governance. Execution is DENIED.'}
                      {res.governance.status === 'BLOCKED' && 'Decision was BLOCKED by PayLens policy. Execution is strictly DENIED.'}
                    </p>
                  </div>

                  {(res.governance.status === 'SAFE' || res.governance.status === 'APPROVED') && !execResult && (
                    <button className="btn-execute" onClick={() => setExecModalOpen(true)}>
                      <Zap size={16} /> Execute Test Payment
                    </button>
                  )}
                </div>
              )}

              {execResult && (
                <div className={`execution-result-card ${execResult.status.toLowerCase()}`}>
                  <div className="exec-header">
                    <h3>
                      {execResult.status === 'SUCCEEDED' ? (
                        <><CheckCircle2 style={{ color: 'var(--safe)' }} /> TEST EXECUTION SUCCEEDED</>
                      ) : (
                        <><XCircle style={{ color: 'var(--block)' }} /> EXECUTION FAILED</>
                      )}
                    </h3>
                    <StatusBadge value={execResult.status} />
                  </div>
                  <div className="exec-details-grid">
                    <div className="exec-field"><label>Execution ID</label><span>{execResult.executionId}</span></div>
                    <div className="exec-field"><label>Provider</label><span>{execResult.provider}</span></div>
                    <div className="exec-field"><label>Reference</label><span>{execResult.providerReference || 'N/A'}</span></div>
                  </div>
                </div>
              )}

              {execModalOpen && res.governance && res.intent && (
                <ExecutionModal
                  decisionId={res.governance.decisionId}
                  actionType={res.intent.actionType}
                  amount={res.intent.amount}
                  currency={res.intent.currency}
                  target={res.intent.target}
                  status={res.governance.status}
                  onClose={() => setExecModalOpen(false)}
                  onSuccess={setExecResult}
                />
              )}
            </div>
          )}
        </motion.div>
      )}
    </>
  )
}

function DecisionsPage() {
  const [decisions, setDecisions] = useState<DecisionSummary[]>([])
  const [filter, setFilter] = useState<string>('ALL')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<DecisionDetail | null>(null)
  const [events, setEvents] = useState<AuditEvent[]>([])
  const [execDetail, setExecDetail] = useState<ExecutionResponse | null>(null)
  const [execModalOpen, setExecModalOpen] = useState(false)
  const [loading, setLoading] = useState(true)

  const loadDecisions = () => {
    setLoading(true)
    getDecisions(filter === 'ALL' ? undefined : filter)
      .then(setDecisions)
      .catch(console.error)
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadDecisions() }, [filter])

  useEffect(() => {
    if (selectedId) {
      getDecisionDetail(selectedId).then(setDetail).catch(console.error)
      getAuditEvents(selectedId).then(setEvents).catch(console.error)
      getDecisionExecution(selectedId).then(setExecDetail).catch(() => setExecDetail(null))
    } else {
      setDetail(null)
      setEvents([])
      setExecDetail(null)
    }
  }, [selectedId])

  return (
    <>
      <div className="tabs">
        {['ALL', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SAFE', 'BLOCKED'].map(t => (
          <button key={t} className={`tab ${filter === t ? 'active' : ''}`} onClick={() => setFilter(t)}>
            {t.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="card">
        <div className="card-header"><h2 className="card-title">Governance Decisions</h2></div>
        {loading ? (
          <Loading>Loading governance decisions...</Loading>
        ) : decisions.length === 0 ? (
          <Empty title="No decisions found" text="No governance decisions match the selected status filter." />
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Decision ID</th><th>Action</th><th>Amount</th><th>Target</th><th>Policy</th><th>Status</th><th>Created</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {decisions.map(d => (
                  <tr key={d.decisionId}>
                    <td><code>{d.decisionId}</code></td>
                    <td>{d.actionType}</td>
                    <td><strong>{money(d.amount)}</strong></td>
                    <td>{d.target || 'N/A'}</td>
                    <td><span className="badge">{d.decision}</span></td>
                    <td><StatusBadge value={d.status} /></td>
                    <td>{new Date(d.createdAt).toLocaleTimeString()}</td>
                    <td>
                      <button className="btn-secondary" onClick={() => setSelectedId(d.decisionId)}>Inspect</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {detail && (
        <div className="drawer-overlay" onClick={() => setSelectedId(null)}>
          <div className="drawer" onClick={e => e.stopPropagation()}>
            <div className="card-header">
              <h2>Decision Detail: <code>{detail.decisionId}</code></h2>
              <button className="icon-button" onClick={() => setSelectedId(null)}><X size={18} /></button>
            </div>

            <StatusBadge value={detail.status} />

            <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
              <p className="eyebrow">ORIGINAL INTENT PROMPT</p>
              <p style={{ fontSize: '0.95rem', fontStyle: 'italic', margin: '0.25rem 0' }}>"{detail.originalMessage}"</p>
              <p style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>
                Action: {detail.intent.actionType} | Amount: {money(detail.intent.amount)} | Target: {detail.intent.target || 'N/A'}
              </p>
            </div>

            <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
              <h3 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.4rem' }}>
                <Zap size={16} style={{ color: 'var(--primary)' }} /> EXECUTION GATEWAY STATUS
              </h3>
              {execDetail ? (
                <div>
                  <p style={{ fontSize: '0.85rem' }}>Status: <StatusBadge value={execDetail.status} /></p>
                  <p style={{ fontSize: '0.85rem' }}>Provider: <strong>{execDetail.provider}</strong></p>
                  <p style={{ fontSize: '0.85rem' }}>Reference: <code>{execDetail.providerReference || 'N/A'}</code></p>
                </div>
              ) : (detail.status === 'SAFE' || detail.status === 'APPROVED') ? (
                <div>
                  <p style={{ fontSize: '0.85rem', color: 'var(--muted)', marginBottom: '0.75rem' }}>Eligible for controlled Razorpay TEST execution.</p>
                  <button className="btn-execute" onClick={() => setExecModalOpen(true)}>
                    <Zap size={16} /> Execute Test Payment
                  </button>
                </div>
              ) : (
                <p style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Execution is denied for status: {detail.status}.</p>
              )}
            </div>

            <div className="card-header"><h3>Audit Timeline</h3></div>
            <div className="timeline">
              {events.map(e => (
                <div className="timeline-item" key={e.eventId}>
                  <div className="timeline-dot" />
                  <div className="timeline-content">
                    <div className="timeline-meta">
                      <span>{e.eventType}</span>
                      <span>{new Date(e.createdAt).toLocaleTimeString()}</span>
                    </div>
                    <p style={{ fontSize: '0.85rem' }}>{e.description}</p>
                    <span className="badge safe" style={{ marginTop: '0.35rem', fontSize: '0.65rem' }}>{e.actorType}: {e.actorId}</span>
                  </div>
                </div>
              ))}
            </div>

            {execModalOpen && (
              <ExecutionModal
                decisionId={detail.decisionId}
                actionType={detail.intent.actionType}
                amount={detail.intent.amount}
                currency={detail.intent.currency}
                target={detail.intent.target}
                status={detail.status}
                onClose={() => setExecModalOpen(false)}
                onSuccess={(res) => {
                  setExecDetail(res)
                  loadDecisions()
                }}
              />
            )}
          </div>
        </div>
      )}
    </>
  )
}

function ExecutionsPage() {
  const [executions, setExecutions] = useState<ExecutionSummary[]>([])
  const [filter, setFilter] = useState<string>('ALL')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<ExecutionResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [reconciling, setReconciling] = useState(false)

  const loadExecutions = () => {
    setLoading(true)
    getExecutions(filter === 'ALL' ? undefined : filter)
      .then(res => setExecutions(res.executions))
      .catch(console.error)
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadExecutions() }, [filter])

  useEffect(() => {
    if (selectedId) {
      getExecutionDetail(selectedId).then(setDetail).catch(console.error)
    } else {
      setDetail(null)
    }
  }, [selectedId])

  const handleReconcile = async () => {
    if (!detail) return
    setReconciling(true)
    try {
      await triggerReconciliation(detail.executionId)
      const updated = await getExecutionDetail(detail.executionId)
      setDetail(updated)
      loadExecutions()
    } catch (err) {
      console.error(err)
    } finally {
      setReconciling(false)
    }
  }

  return (
    <>
      <div className="tabs">
        {['ALL', 'SUCCEEDED', 'FAILED', 'ELIGIBILITY_REJECTED', 'UNSUPPORTED_EXECUTION', 'UNKNOWN'].map(t => (
          <button key={t} className={`tab ${filter === t ? 'active' : ''}`} onClick={() => setFilter(t)}>
            {t.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="card">
        <div className="card-header"><h2 className="card-title">Controlled Execution Gateway History</h2></div>
        {loading ? (
          <Loading>Loading execution history...</Loading>
        ) : executions.length === 0 ? (
          <Empty title="No execution records" text="No payment executions match the selected filter." />
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Execution ID</th><th>Decision ID</th><th>Action</th><th>Amount</th><th>Provider</th><th>Reference</th><th>Status</th><th>Timestamp</th><th>Inspect</th></tr>
              </thead>
              <tbody>
                {executions.map(e => (
                  <tr key={e.executionId}>
                    <td><code>{e.executionId}</code></td>
                    <td><code>{e.decisionId}</code></td>
                    <td>{e.actionType}</td>
                    <td><strong>{money(e.amount)}</strong></td>
                    <td>{e.provider}</td>
                    <td><code>{e.providerReference || 'N/A'}</code></td>
                    <td><StatusBadge value={e.status} /></td>
                    <td>{new Date(e.createdAt).toLocaleTimeString()}</td>
                    <td>
                      <button className="btn-secondary" onClick={() => setSelectedId(e.executionId)}>Detail</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {detail && (
        <div className="drawer-overlay" onClick={() => setSelectedId(null)}>
          <div className="drawer" onClick={e => e.stopPropagation()}>
            <div className="card-header">
              <h2>Execution Detail: <code>{detail.executionId}</code></h2>
              <button className="icon-button" onClick={() => setSelectedId(null)}><X size={18} /></button>
            </div>

            <StatusBadge value={detail.status} />

            <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
              <div className="exec-details-grid">
                <div className="exec-field"><label>Decision ID</label><span>{detail.decisionId}</span></div>
                <div className="exec-field"><label>Idempotency Key</label><span>{detail.idempotencyKey}</span></div>
                <div className="exec-field"><label>Provider</label><span>{detail.provider}</span></div>
                <div className="exec-field"><label>Provider Reference</label><span>{detail.providerReference || 'N/A'}</span></div>
                <div className="exec-field"><label>Action</label><span>{detail.actionType}</span></div>
                <div className="exec-field"><label>Amount</label><span>{money(detail.amount)} {detail.currency}</span></div>
                <div className="exec-field"><label>Target</label><span>{detail.target || 'N/A'}</span></div>
                <div className="exec-field"><label>Created At</label><span>{new Date(detail.createdAt).toLocaleString()}</span></div>
              </div>
            </div>

            {detail.status === 'UNKNOWN' && (
              <div style={{ background: 'rgba(234, 179, 8, 0.15)', border: '1px solid rgba(234, 179, 8, 0.4)', color: '#eab308', padding: '1rem', borderRadius: '8px', marginTop: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.35rem' }}>
                  <AlertTriangle size={18} /> Execution outcome uncertain
                </div>
                <p style={{ fontSize: '0.85rem', margin: 0, color: 'var(--text)' }}>
                  The provider request could not be confirmed. PayLens will not automatically retry this financial action.
                </p>
                <button className="btn-execute" style={{ marginTop: '0.75rem', background: '#eab308', color: '#000' }} onClick={handleReconcile} disabled={reconciling}>
                  <RefreshCw size={16} className={reconciling ? 'spin' : ''} /> {reconciling ? 'Reconciling...' : 'Reconcile Provider State'}
                </button>
              </div>
            )}

            {detail.failureMessage && (
              <div style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', padding: '1rem', borderRadius: '8px', fontSize: '0.85rem', marginTop: '1rem' }}>
                <strong>Failure Details ({detail.failureCode || 'ERROR'}):</strong>
                <p style={{ marginTop: '0.25rem' }}>{detail.failureMessage}</p>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  )
}

function ReconciliationsPage() {
  const [reconciliations, setReconciliations] = useState<ReconciliationSummary[]>([])
  const [metrics, setMetrics] = useState<ReliabilityMetrics | null>(null)
  const [filter, setFilter] = useState<string>('ALL')
  const [loading, setLoading] = useState(true)

  const loadData = () => {
    setLoading(true)
    Promise.all([
      getReconciliationHistory(undefined, filter === 'ALL' ? undefined : filter),
      getReliabilityMetrics()
    ])
      .then(([recons, m]) => {
        setReconciliations(recons)
        setMetrics(m)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadData() }, [filter])

  return (
    <>
      {metrics && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">
            <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Zap size={18} style={{ color: 'var(--primary)' }} /> EXECUTION RELIABILITY DASHBOARD
            </h2>
          </div>
          <div className="stats-grid">
            <div className="stat-card">
              <span className="stat-label">Total Executions</span>
              <span className="stat-value">{metrics.totalExecutions}</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Confirmed Success</span>
              <span className="stat-value" style={{ color: '#22c55e' }}>{metrics.confirmedSuccess}</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Confirmed Failure</span>
              <span className="stat-value" style={{ color: '#ef4444' }}>{metrics.confirmedFailure}</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Manual Review Required</span>
              <span className="stat-value" style={{ color: '#eab308' }}>{metrics.unknownOrManualReview}</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Success Rate</span>
              <span className="stat-value" style={{ color: 'var(--primary)' }}>{metrics.successRate.toFixed(1)}%</span>
            </div>
          </div>
        </div>
      )}

      <div className="tabs">
        {['ALL', 'CONFIRMED', 'FAILED', 'PENDING', 'MANUAL_REVIEW_REQUIRED'].map(t => (
          <button key={t} className={`tab ${filter === t ? 'active' : ''}`} onClick={() => setFilter(t)}>
            {t.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="card">
        <div className="card-header"><h2 className="card-title">Reconciliation Engine Audit History</h2></div>
        {loading ? (
          <Loading>Loading reconciliation history...</Loading>
        ) : reconciliations.length === 0 ? (
          <Empty title="No reconciliation records" text="No reconciliation events match the selected filter." />
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr><th>Reconciliation ID</th><th>Execution ID</th><th>Provider</th><th>Reference</th><th>Resolved Status</th><th>Reconciliation Status</th><th>Outcome</th><th>Retry Safety</th><th>Timestamp</th></tr>
              </thead>
              <tbody>
                {reconciliations.map(r => (
                  <tr key={r.reconciliationId}>
                    <td><code>{r.reconciliationId}</code></td>
                    <td><code>{r.executionId}</code></td>
                    <td>{r.provider}</td>
                    <td><code>{r.providerReference || 'N/A'}</code></td>
                    <td><StatusBadge value={r.resolvedExecutionStatus} /></td>
                    <td><StatusBadge value={r.status} /></td>
                    <td><span className="badge">{r.providerOutcome}</span></td>
                    <td><span className={`badge ${r.retryDecision === 'SAFE_TO_RETRY' ? 'safe' : 'blocked'}`}>{r.retryDecision}</span></td>
                    <td>{new Date(r.createdAt).toLocaleTimeString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  )
}

function ObligationsPage() {
  const [data, setData] = useState<ObligationRiskItem[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getObligationRisks()
      .then(res => {
        setData(res.obligations)
        setTotal(res.totalUpcomingAmount)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading>Loading obligations risk analysis...</Loading>

  return (
    <>
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="card-header">
          <h2 className="card-title">Merchant Unpaid Obligations Summary</h2>
          <span className="badge" style={{ fontSize: '0.85rem', background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444' }}>
            Total Upcoming: {money(total)}
          </span>
        </div>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr><th>ID</th><th>Category</th><th>Description</th><th>Amount</th><th>Due Date</th><th>Days Remaining</th><th>Risk Level</th><th>Status</th></tr>
            </thead>
            <tbody>
              {data.map(o => (
                <tr key={o.id}>
                  <td><code>{o.id}</code></td>
                  <td>{o.type}</td>
                  <td>{o.description}</td>
                  <td><strong>{money(o.amount)}</strong></td>
                  <td>{o.dueDate}</td>
                  <td><strong>{o.daysUntilDue} day(s)</strong></td>
                  <td>
                    <span className={`badge ${o.riskLevel === 'CRITICAL' || o.riskLevel === 'HIGH' ? 'blocked' : o.riskLevel === 'MEDIUM' ? 'review' : 'safe'}`}>
                      {o.riskLevel}
                    </span>
                  </td>
                  <td><span className="badge">{o.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}

function RiskCenterPage() {
  const [events, setEvents] = useState<RiskEventResponse[]>([])
  const [status, setStatus] = useState<MonitoringStatusResponse | null>(null)
  const [filter, setFilter] = useState<string>('ALL')
  const [loading, setLoading] = useState(true)
  const [running, setRunning] = useState(false)
  const [dismissModalEventId, setDismissModalEventId] = useState<string | null>(null)
  const [dismissReason, setDismissReason] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)
  const navigate = useNavigate()

  const loadData = async () => {
    setLoading(true)
    try {
      const [evts, st] = await Promise.all([
        getRiskEvents(filter === 'ALL' ? undefined : { status: filter as RiskEventStatus }),
        getMonitoringStatus()
      ])
      setEvents(evts)
      setStatus(st)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [filter])

  const handleRunMonitoring = async () => {
    setRunning(true)
    setActionError(null)
    try {
      await runMonitoringCycle()
      await loadData()
    } catch (e: any) {
      setActionError('Monitoring run failed.')
    } finally {
      setRunning(false)
    }
  }

  const handleAcknowledge = async (id: string) => {
    setActionError(null)
    try {
      await acknowledgeRiskEvent(id)
      await loadData()
    } catch (e: any) {
      setActionError(e.response?.data?.error || 'Acknowledge failed.')
    }
  }

  const handleConfirmDismiss = async () => {
    if (!dismissModalEventId) return
    setActionError(null)
    try {
      await dismissRiskEvent(dismissModalEventId, dismissReason)
      setDismissModalEventId(null)
      setDismissReason('')
      await loadData()
    } catch (e: any) {
      setActionError(e.response?.data?.error || 'Dismiss failed.')
    }
  }

  const handleResolve = async (id: string) => {
    setActionError(null)
    try {
      await resolveRiskEvent(id)
      await loadData()
    } catch (e: any) {
      setActionError(e.response?.data?.error || 'Cannot resolve active risk.')
    }
  }

  return (
    <>
      <div className="card" style={{ marginBottom: '1.5rem', background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.95))' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Activity size={20} style={{ color: '#00f2fe' }} /> Autonomous Risk Monitoring Status
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.25rem' }}>
              Monitoring Status: <strong style={{ color: '#22c55e' }}>ACTIVE</strong> • Scheduled Interval: 5 mins •
              Last run: {status?.lastRunAt ? new Date(status.lastRunAt).toLocaleTimeString() : 'Baseline Initialized'} ({status?.lastRunStatus})
            </p>
          </div>
          <button className="btn-primary" onClick={handleRunMonitoring} disabled={running}>
            {running ? <LoaderCircle className="spin" size={16} /> : <Play size={16} />}
            Run Risk Check Now
          </button>
        </div>

        {status && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.75rem', marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)', fontSize: '0.8rem' }}>
            <div><span style={{ color: 'var(--muted)' }}>Open:</span> <strong style={{ color: '#ef4444' }}>{status.openCount}</strong></div>
            <div><span style={{ color: 'var(--muted)' }}>Acknowledged:</span> <strong style={{ color: '#eab308' }}>{status.acknowledgedCount}</strong></div>
            <div><span style={{ color: 'var(--muted)' }}>Resolved:</span> <strong style={{ color: '#22c55e' }}>{status.resolvedCount}</strong></div>
            <div><span style={{ color: 'var(--muted)' }}>Dismissed:</span> <strong style={{ color: 'var(--muted)' }}>{status.dismissedCount}</strong></div>
            <div><span style={{ color: 'var(--muted)' }}>Last Duration:</span> <strong>{status.lastRunDurationMs} ms</strong></div>
          </div>
        )}
      </div>

      {actionError && (
        <div style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', padding: '0.75rem 1rem', borderRadius: '8px', marginBottom: '1rem', fontSize: '0.85rem' }}>
          <AlertTriangle size={16} style={{ marginRight: '0.5rem', display: 'inline-block' }} />
          {actionError}
        </div>
      )}

      <div className="tabs">
        {['ALL', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED'].map(t => (
          <button key={t} className={`tab ${filter === t ? 'active' : ''}`} onClick={() => setFilter(t)}>
            {t}
          </button>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Operational Risk Events</h2>
          <span className="badge review">{events.length} Event(s)</span>
        </div>

        {loading ? (
          <Loading>Loading risk events...</Loading>
        ) : events.length === 0 ? (
          <Empty title="No risk events found" text="No active or historical risk events match the selected status filter." />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
            {events.map(ev => (
              <div key={ev.riskEventId} style={{ background: 'var(--panel-light)', border: '1px solid var(--border)', borderRadius: '8px', padding: '1.25rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <h3 style={{ fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <AlertTriangle size={18} style={{ color: ev.severity === 'CRITICAL' || ev.severity === 'HIGH' ? '#ef4444' : '#eab308' }} />
                    {ev.title}
                  </h3>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span className={`badge ${ev.severity === 'CRITICAL' || ev.severity === 'HIGH' ? 'blocked' : 'review'}`}>{ev.severity}</span>
                    <span className="badge">{ev.priority} PRIORITY</span>
                    <StatusBadge value={ev.status} />
                  </div>
                </div>

                <p style={{ fontSize: '0.9rem', color: 'var(--text)', margin: '0.5rem 0' }}>{ev.description}</p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem', background: 'var(--bg)', padding: '0.75rem', borderRadius: '6px', fontSize: '0.8rem', margin: '0.75rem 0' }}>
                  <div><span style={{ color: 'var(--muted)' }}>Detected:</span> <strong>{new Date(ev.firstDetectedAt).toLocaleTimeString()}</strong></div>
                  <div><span style={{ color: 'var(--muted)' }}>Occurrences:</span> <strong>{ev.occurrenceCount} cycle(s)</strong></div>
                  <div><span style={{ color: 'var(--muted)' }}>Financial Impact:</span> <strong>{money(ev.financialImpact)}</strong></div>
                  <div><span style={{ color: 'var(--muted)' }}>Source:</span> <strong>{ev.source}</strong></div>
                </div>

                {ev.recommendedAction && (
                  <div style={{ fontSize: '0.85rem', color: 'var(--primary)', background: 'rgba(0, 242, 254, 0.05)', padding: '0.65rem 0.85rem', borderRadius: '6px', border: '1px solid rgba(0, 242, 254, 0.2)', marginBottom: '0.75rem' }}>
                    <strong>Deterministic Recommendation:</strong> {ev.recommendedAction}
                  </div>
                )}

                {ev.dismissalReason && (
                  <p style={{ fontSize: '0.8rem', color: 'var(--muted)', fontStyle: 'italic' }}>Dismissal Reason: {ev.dismissalReason}</p>
                )}

                {ev.resolutionReason && (
                  <p style={{ fontSize: '0.8rem', color: 'var(--safe)', fontStyle: 'italic' }}>Resolution Details: {ev.resolutionReason}</p>
                )}

                <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end', marginTop: '0.75rem', flexWrap: 'wrap' }}>
                  <button className="btn-secondary" style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem' }} onClick={() => navigate('/simulator')}>
                    <Sliders size={14} /> Simulate Impact
                  </button>
                  {ev.status === 'OPEN' && (
                    <button className="btn-secondary" style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem' }} onClick={() => handleAcknowledge(ev.riskEventId)}>
                      <Check size={14} /> Acknowledge
                    </button>
                  )}
                  {(ev.status === 'OPEN' || ev.status === 'ACKNOWLEDGED') && (
                    <>
                      <button className="btn-secondary" style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem' }} onClick={() => setDismissModalEventId(ev.riskEventId)}>
                        <X size={14} /> Dismiss
                      </button>
                      <button className="btn-primary" style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem' }} onClick={() => handleResolve(ev.riskEventId)}>
                        <CheckCheck size={14} /> Resolve Risk
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {dismissModalEventId && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Dismiss Risk Alert</h3>
              <button className="icon-button" onClick={() => setDismissModalEventId(null)}><X size={18} /></button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>
                Please provide a justification for dismissing risk alert <code style={{ color: 'var(--primary)' }}>{dismissModalEventId}</code>:
              </p>
              <textarea
                value={dismissReason}
                onChange={e => setDismissReason(e.target.value)}
                placeholder="Reason for dismissal..."
                style={{ width: '100%', height: '80px', padding: '0.65rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)', marginTop: '0.75rem' }}
              />
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setDismissModalEventId(null)}>Cancel</button>
              <button className="btn-primary" onClick={handleConfirmDismiss}>Confirm Dismissal</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function ScenarioSimulatorPage() {
  const [actionType, setActionType] = useState('REFUND')
  const [amount, setAmount] = useState<number>(50000)
  const [result, setResult] = useState<ForecastScenarioResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSimulate = async () => {
    setLoading(true)
    try {
      const res = await simulateForecastScenario({ actionType, amount })
      setResult(res)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { handleSimulate() }, [])

  return (
    <>
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h2 className="card-title" style={{ marginBottom: '1rem' }}>Financial Scenario What-If Simulator</h2>
        <p style={{ fontSize: '0.85rem', color: 'var(--muted)', marginBottom: '1.25rem' }}>
          Simulate prospective financial actions against real merchant liquidity and safety reserve bounds without executing any payment.
        </p>

        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div style={{ flex: '1', minWidth: '180px' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--muted)', display: 'block', marginBottom: '0.35rem' }}>Action Type</label>
            <select value={actionType} onChange={e => setActionType(e.target.value)} style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}>
              <option value="REFUND">REFUND</option>
              <option value="VENDOR_PAYMENT">VENDOR_PAYMENT</option>
              <option value="PAYROLL">PAYROLL</option>
              <option value="TAX_PAYMENT">TAX_PAYMENT</option>
            </select>
          </div>

          <div style={{ flex: '1', minWidth: '180px' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--muted)', display: 'block', marginBottom: '0.35rem' }}>Proposed Amount (INR)</label>
            <input type="number" value={amount} onChange={e => setAmount(Number(e.target.value))} style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }} />
          </div>

          <button className="btn-primary" onClick={handleSimulate} disabled={loading} style={{ height: '42px' }}>
            {loading ? <LoaderCircle className="spin" size={16} /> : <Sliders size={16} />}
            Simulate Impact
          </button>
        </div>
      </div>

      {result && (
        <div className="card">
          <div className="card-header">
            <div>
              <p className="eyebrow">SIMULATION CONSEQUENCE IMPACT</p>
              <h2 className="card-title">Scenario Result for {result.actionType} of {money(result.amount)}</h2>
            </div>
            <StatusBadge value={result.policyDecision} />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginTop: '1rem' }}>
            <div className="metric-card">
              <span>Current Safety Buffer</span>
              <strong>{money(result.currentSafetyBuffer)}</strong>
            </div>
            <div className="metric-card">
              <span>Projected Safety Buffer</span>
              <strong style={{ color: result.projectedSafetyBuffer < 100000 ? '#ef4444' : '#22c55e' }}>
                {money(result.projectedSafetyBuffer)}
              </strong>
            </div>
            <div className="metric-card">
              <span>Safety Buffer Impact</span>
              <strong style={{ color: result.safetyBufferImpact < 0 ? '#ef4444' : '#22c55e' }}>
                {money(result.safetyBufferImpact)}
              </strong>
            </div>
            <div className="metric-card">
              <span>Projected Health Status</span>
              <StatusBadge value={result.projectedHealthStatus} />
            </div>
          </div>

          <div style={{ background: 'var(--panel-light)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)', marginTop: '1.25rem', fontSize: '0.85rem' }}>
            <strong>Consequence Fact Summary:</strong>
            <p style={{ marginTop: '0.25rem', color: 'var(--muted)' }}>{result.consequenceSummary}</p>
          </div>
        </div>
      )}
    </>
  )
}

function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAuditEvents()
      .then(setEvents)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading>Loading append-only audit trail...</Loading>

  return (
    <div className="card">
      <div className="card-header"><h2 className="card-title">Append-Only Immutable Audit Log</h2></div>
      <div className="table-container">
        <table>
          <thead>
            <tr><th>Event ID</th><th>Decision ID</th><th>Event Type</th><th>Actor</th><th>Description</th><th>Timestamp</th></tr>
          </thead>
          <tbody>
            {events.map(e => (
              <tr key={e.eventId}>
                <td><code>{e.eventId}</code></td>
                <td><code>{e.decisionId}</code></td>
                <td><strong>{e.eventType}</strong></td>
                <td><span className="badge safe">{e.actorType}: {e.actorId}</span></td>
                <td>{e.description}</td>
                <td>{new Date(e.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export function App() {
  const { authenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <Loading>Authenticating user session...</Loading>
  }

  if (!authenticated && location.pathname !== '/login') {
    return <Navigate to="/login" replace />
  }

  if (authenticated && location.pathname === '/login') {
    return <Navigate to="/" replace />
  }

  if (location.pathname === '/login') {
    return <LoginPage />
  }

  return (
    <Shell>
      <Routes>
        <Route path="/" element={<OverviewPage />} />
        <Route path="/obligations" element={<ObligationsPage />} />
        <Route path="/risk" element={<RiskCenterPage />} />
        <Route path="/simulator" element={<ScenarioSimulatorPage />} />
        <Route path="/safety" element={<SafetyPage />} />
        <Route path="/simulations" element={<OverviewPage />} />
        <Route path="/decisions" element={<DecisionsPage />} />
        <Route path="/executions" element={<ExecutionsPage />} />
        <Route path="/reconciliations" element={<ReconciliationsPage />} />
        <Route path="/audit" element={<AuditPage />} />
        <Route path="/users" element={<UserManagementPage />} />
        <Route path="/settings/security" element={<SecuritySettingsPage />} />
        <Route path="/settings" element={<SecuritySettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Shell>
  )
}

export default App

