import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  AlertTriangle, Bell, Bot, CheckCircle2,
  CircleDollarSign, ClipboardList, CreditCard, Gauge, Landmark, LoaderCircle,
  Menu, RefreshCw, Search, Settings, ShieldCheck, Users, WalletCards, XCircle, X, Zap, Info
} from 'lucide-react'
import { analyzeAction } from './api/agentApi'
import { getDashboard, getFinancialState } from './api/dashboardApi'
import { getAuditEvents, getDecisionDetail, getDecisions } from './api/decisionsApi'
import { executeDecision, getDecisionExecution, getExecutionDetail, getExecutions } from './api/executionsApi'
import { getReconciliationHistory, getReliabilityMetrics, triggerReconciliation } from './api/reconciliationsApi'
import type {
  AgentAnalysisResponse, AuditEvent, DashboardResponse, Decision,
  DecisionDetail, DecisionSummary, ExecutionResponse, ExecutionStatus, ExecutionSummary, FinancialStateResponse, GovernanceStatus,
  ReconciliationStatus, ReconciliationSummary, ReliabilityMetrics, Transaction
} from './types/api'
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

function StatusBadge({ value }: { value: GovernanceStatus | Decision | ExecutionStatus | ReconciliationStatus }) {
  const isSafe = value === 'SAFE' || value === 'APPROVED' || value === 'SUCCEEDED' || value === 'CONFIRMED'
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
  const title = path === '/' ? 'Overview' : path === '/safety' ? 'AI Safety Center' : path === '/executions' ? 'Execution Gateway' : path.slice(1).replace(/^./, c => c.toUpperCase())

  return (
    <div className="app-shell">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand"><b>P</b>PAYLENS</div>
        <p className="brand-subtitle">Financial safety for autonomous actions.</p>
        <nav>
          {nav.map(([name, href, Icon]) =>
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
            <p className="eyebrow">PAYLENS OPERATIONS</p>
            <h1>{title}</h1>
          </div>
          <div className="top-actions">
            <button className="search"><Search size={16} />Search or command</button>
            <span className="test-mode">Razorpay Test Mode</span>
            <button className="icon-button" aria-label="Notifications"><Bell size={18} /></button>
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

  useEffect(() => {
    getDashboard().then(setData).catch(console.error)
    getFinancialState().then(setState).catch(console.error)
  }, [])

  if (!data || !state) return <Loading>Loading dashboard metrics...</Loading>

  return (
    <>
      <div className="metrics-grid">
        {Object.entries(data).filter(([k]) => k !== 'currency').map(([k, v]) => (
          <div className="metric-card" key={k}>
            <span>{labels[k] ?? k}</span>
            <strong>{money(v)}</strong>
          </div>
        ))}
      </div>
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
  return (
    <Shell>
      <Routes>
        <Route path="/" element={<OverviewPage />} />
        <Route path="/safety" element={<SafetyPage />} />
        <Route path="/simulations" element={<OverviewPage />} />
        <Route path="/decisions" element={<DecisionsPage />} />
        <Route path="/executions" element={<ExecutionsPage />} />
        <Route path="/reconciliations" element={<ReconciliationsPage />} />
        <Route path="/audit" element={<AuditPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Shell>
  )
}

export default App
