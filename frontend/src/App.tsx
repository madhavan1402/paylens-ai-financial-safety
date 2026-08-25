import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  AlertTriangle, ArrowRight, Bell, Bot, CheckCircle2, ChevronRight,
  CircleDollarSign, ClipboardList, CreditCard, Gauge, Landmark, LoaderCircle,
  Menu, Search, Settings, ShieldCheck, Users, WalletCards, XCircle, X, Check, Ban
} from 'lucide-react'
import { analyzeAction } from './api/agentApi'
import { getDashboard, getFinancialState } from './api/dashboardApi'
import { approveDecision, getAuditEvents, getDecisionDetail, getDecisions, rejectDecision } from './api/decisionsApi'
import type {
  AgentAnalysisResponse, AuditEvent, DashboardResponse, Decision,
  DecisionDetail, DecisionSummary, GovernanceStatus, Snapshot, Transaction
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
  ['Audit Log', '/audit', ClipboardList],
  ['Settings', '/settings', Settings]
] as const

const prompts = ['Refund ₹2.5 lakh to Rahul', 'Pay ₹80,000 to ABC Suppliers', 'Process payroll ₹3 lakh', 'Pay ₹1 lakh tax']

function StatusBadge({ value }: { value: GovernanceStatus | Decision }) {
  const isSafe = value === 'SAFE' || value === 'APPROVED'
  const isReview = value === 'PENDING_REVIEW' || value === 'REVIEW'
  const Icon = isSafe ? CheckCircle2 : isReview ? AlertTriangle : XCircle
  const displayLabel = value === 'REVIEW' ? 'PENDING REVIEW' : value === 'PENDING_REVIEW' ? 'PENDING REVIEW' : value === 'BLOCK' ? 'BLOCKED' : value
  const className = value.toLowerCase().replace('_', '-')
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
  const title = path === '/' ? 'Overview' : path === '/safety' ? 'AI Safety Center' : path.slice(1).replace(/^./, c => c.toUpperCase())

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
            <span className="test-mode">Test mode</span>
            <button className="icon-button" aria-label="Notifications"><Bell size={18} /></button>
            <button className="profile">M <span>Merchant</span></button>
          </div>
        </header>
        <div className="page-content">{children}</div>
      </main>
    </div>
  )
}

function Overview() {
  const [data, setData] = useState<DashboardResponse>()
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getDashboard(), getFinancialState()])
      .then(([dashboard, state]) => { setData(dashboard); setTransactions(state.transactions) })
      .catch(() => setError('We could not load the financial overview. Check that the PayLens backend is running.'))
  }, [])

  if (error) return <div className="notice error"><XCircle size={18} />{error}</div>
  if (!data) return <Loading>Loading financial overview…</Loading>

  return (
    <>
      <section className="page-intro">
        <div>
          <h2>Financial overview</h2>
          <p>A live view of liquidity, obligations, and your protected safety margin.</p>
        </div>
        <NavLink to="/safety" className="primary">Analyze an action <ArrowRight size={16} /></NavLink>
      </section>
      <section className="metric-grid">
        {Object.entries(labels).map(([key, label]) => {
          const value = data[key as keyof DashboardResponse] as number
          return (
            <article className="metric-card" key={key}>
              <span>{label}</span>
              <strong className={key === 'safetyBuffer' && value < 0 ? 'negative' : ''}>{money(value)}</strong>
              <small>{key === 'safetyBuffer' ? 'After obligations and reserve' : 'Authoritative backend figure'}</small>
            </article>
          )
        })}
      </section>
      <section className="two-col">
        <article className="panel health">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">LIQUIDITY POSITION</p>
              <h2>Financial health</h2>
            </div>
            <span className="live-dot">Live data</span>
          </div>
          {[
            ['Current balance', data.currentBalance],
            ['Upcoming obligations', data.upcomingObligations],
            ['Safety reserve', data.safetyReserve],
            ['Safety buffer', data.safetyBuffer]
          ].map(([name, value]) => (
            <div className="health-row" key={String(name)}>
              <span>{name}</span>
              <i><b style={{ width: `${Math.min(100, Math.abs(Number(value)) / data.currentBalance * 100)}%` }} /></i>
              <strong>{money(Number(value))}</strong>
            </div>
          ))}
        </article>
        <Activity rows={transactions} />
      </section>
    </>
  )
}

function Activity({ rows }: { rows: Transaction[] }) {
  return (
    <article className="panel activity">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">LEDGER</p>
          <h2>Recent financial activity</h2>
        </div>
        <NavLink to="/transactions">View all <ChevronRight size={15} /></NavLink>
      </div>
      {rows.length ? (
        <table>
          <thead>
            <tr><th>Type</th><th>Amount</th><th>Status</th></tr>
          </thead>
          <tbody>
            {rows.slice(0, 5).map(t => (
              <tr key={t.id}>
                <td><b>{t.type.replaceAll('_', ' ')}</b><small>{t.description}</small></td>
                <td>{money(t.amount)}</td>
                <td><span className="table-status">{t.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p className="muted">No transaction activity is available.</p>
      )}
    </article>
  )
}

function Safety() {
  const [message, setMessage] = useState(prompts[0])
  const [result, setResult] = useState<AgentAnalysisResponse>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!message.trim()) return
    setLoading(true)
    setError('')
    setResult(undefined)
    try {
      setResult(await analyzeAction(message))
    } catch {
      setError('Analysis is temporarily unavailable. Your financial state has not been changed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <section className="safety-hero">
        <p className="eyebrow">AI SAFETY CENTER</p>
        <h2>Analyze financial actions before they happen.</h2>
        <p>Validate intent, simulate impact, and apply your configured safety policy.</p>
        <form className="command" onSubmit={submit}>
          <label htmlFor="command">What would you like to do?</label>
          <textarea id="command" value={message} onChange={e => setMessage(e.target.value)} />
          <button className="primary" disabled={loading}>
            {loading ? <LoaderCircle className="spin" size={17} /> : <Bot size={17} />}
            Analyze action
          </button>
        </form>
        <div className="chips">
          {prompts.map(x => <button key={x} onClick={() => setMessage(x)}>{x}</button>)}
        </div>
      </section>
      {loading && <Loading>Analyzing your request…</Loading>}
      {error && <div className="notice error"><XCircle size={18} />{error}</div>}
      {result && <Result value={result} />}
    </>
  )
}

function SnapshotCard({ title, data }: { title: string; data: Snapshot }) {
  return (
    <article className="snapshot">
      <p className="eyebrow">{title}</p>
      {(['currentBalance', 'upcomingObligations', 'safetyBuffer'] as const).map(k => (
        <div key={k}>
          <span>{labels[k]}</span>
          <b className={k === 'safetyBuffer' && data[k] < 0 ? 'negative' : ''}>{money(data[k])}</b>
        </div>
      ))}
    </article>
  )
}

function Result({ value }: { value: AgentAnalysisResponse }) {
  if (value.status === 'NEEDS_CLARIFICATION') {
    return (
      <div className="notice clarification">
        <AlertTriangle size={22} />
        <div>
          <h2>More information needed</h2>
          <p>{value.clarificationMessage ?? `Please provide: ${value.missingFields.join(', ')}`}</p>
        </div>
      </div>
    )
  }

  if (value.status === 'INVALID' || !value.intent || !value.simulation || !value.policy) {
    return (
      <div className="notice error">
        <XCircle size={22} />
        <div>
          <h2>PayLens couldn't understand that financial action.</h2>
          <p>{value.clarificationMessage ?? 'Try one of the examples above.'}</p>
        </div>
      </div>
    )
  }

  const { intent, simulation, policy, explanation, governance } = value
  return (
    <motion.section initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="analysis-result">
      <div className="result-grid">
        <article className="panel">
          <p className="eyebrow">AI INTERPRETATION</p>
          <h2>{intent.actionType.replaceAll('_', ' ')}</h2>
          <dl>
            <div><dt>Amount</dt><dd>{money(intent.amount)}</dd></div>
            <div><dt>Target</dt><dd>{intent.target ?? 'Not specified'}</dd></div>
            <div><dt>Currency</dt><dd>{intent.currency}</dd></div>
          </dl>
        </article>
        <article className={`decision-panel ${policy.decision.toLowerCase()}`}>
          <p className="eyebrow">SAFETY DECISION</p>
          <StatusBadge value={governance?.status ?? policy.decision} />
          <h2>
            {policy.decision === 'SAFE'
              ? 'Action satisfies the current safety policy.'
              : policy.decision === 'REVIEW'
              ? 'Human review required'
              : 'Action cannot proceed safely.'}
          </h2>
          <p>{policy.reason}</p>
          {governance && (
            <div style={{ marginTop: '12px', fontSize: '12px', color: '#64748b' }}>
              Persisted Decision ID: <code>{governance.decisionId}</code>
            </div>
          )}
        </article>
      </div>

      <article className="panel">
        <p className="eyebrow">AUTHORITATIVE SIMULATION</p>
        <h2>Before and after</h2>
        <div className="comparison">
          <SnapshotCard title="Before" data={simulation.before} />
          <ArrowRight className="comparison-arrow" />
          <SnapshotCard title="After" data={simulation.after} />
        </div>
      </article>

      <div className="result-grid lower">
        <article className="panel">
          <p className="eyebrow">WHY PAYLENS DECIDED THIS</p>
          <h2>Policy evaluation</h2>
          <p className="policy-reason">{policy.reason}</p>
          <h3>Recommendation</h3>
          <p>{policy.recommendation}</p>
        </article>
        <article className="panel">
          <p className="eyebrow">FINANCIAL IMPACT</p>
          <h2>Simulated change</h2>
          <dl>
            <div><dt>Liquidity change</dt><dd>{money(simulation.impact.liquidityChange)}</dd></div>
            <div><dt>Safety buffer change</dt><dd>{money(simulation.impact.safetyBufferChange)}</dd></div>
            <div><dt>Reserve breached</dt><dd>{simulation.impact.reserveBreached ? 'Yes' : 'No'}</dd></div>
            <div><dt>Obligations covered</dt><dd>{simulation.impact.obligationsCovered ? 'Yes' : 'No'}</dd></div>
          </dl>
        </article>
      </div>

      {explanation && (
        <article className="panel explanation">
          <div>
            <p className="eyebrow">AI EXPLANATION</p>
            <h2>{explanation.headline}</h2>
            <p>{explanation.explanation}</p>
          </div>
          <div>
            <h3>Key factors</h3>
            <ul>{explanation.keyFactors.map(x => <li key={x}>{x}</li>)}</ul>
            <h3>Recommendation</h3>
            <p>{explanation.recommendation}</p>
            <span className="provider">Provider: {explanation.providerMode}</span>
          </div>
        </article>
      )}
    </motion.section>
  )
}

function Transactions() {
  const [rows, setRows] = useState<Transaction[]>()
  useEffect(() => { getFinancialState().then(x => setRows(x.transactions)).catch(() => setRows([])) }, [])
  if (!rows) return <Loading>Loading transactions…</Loading>

  return (
    <article className="panel full-table">
      <p className="eyebrow">LEDGER</p>
      <h2>Transactions</h2>
      {rows.length ? (
        <table>
          <thead>
            <tr><th>ID</th><th>Type</th><th>Description</th><th>Amount</th><th>Timestamp</th><th>Status</th></tr>
          </thead>
          <tbody>
            {rows.map(t => (
              <tr key={t.id}>
                <td>{t.id}</td>
                <td>{t.type}</td>
                <td>{t.description}</td>
                <td>{money(t.amount)}</td>
                <td>{new Date(t.timestamp).toLocaleDateString('en-IN')}</td>
                <td><span className="table-status">{t.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <Empty title="No transactions" text="No transaction data is available from the financial state service." />
      )}
    </article>
  )
}

function DecisionsPage() {
  const [decisions, setDecisions] = useState<DecisionSummary[]>([])
  const [filter, setFilter] = useState<string>('ALL')
  const [loading, setLoading] = useState(true)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [error, setError] = useState('')

  const loadDecisions = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getDecisions(filter === 'ALL' ? undefined : filter)
      setDecisions(data)
    } catch {
      setError('Could not load decisions from governance service.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadDecisions() }, [filter])

  // Derive summary metrics from backend decisions list
  const metrics = {
    total: decisions.length,
    safe: decisions.filter(d => d.status === 'SAFE').length,
    pending: decisions.filter(d => d.status === 'PENDING_REVIEW').length,
    approved: decisions.filter(d => d.status === 'APPROVED').length,
    blocked: decisions.filter(d => d.status === 'BLOCKED').length
  }

  return (
    <>
      <section className="page-intro">
        <div>
          <h2>Financial Governance Decisions</h2>
          <p>Persisted financial safety decisions and human review controls.</p>
        </div>
      </section>

      <section className="metric-grid" style={{ marginBottom: '20px' }}>
        <article className="metric-card">
          <span>Total Decisions</span>
          <strong>{metrics.total}</strong>
          <small>Backend records</small>
        </article>
        <article className="metric-card">
          <span>Pending Review</span>
          <strong style={{ color: '#a86a08' }}>{metrics.pending}</strong>
          <small>Requires human approval</small>
        </article>
        <article className="metric-card">
          <span>Blocked</span>
          <strong className="negative">{metrics.blocked}</strong>
          <small>Policy violations</small>
        </article>
      </section>

      <div className="status-tabs">
        {['ALL', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SAFE', 'BLOCKED'].map(s => (
          <button
            key={s}
            className={`status-tab ${filter === s ? 'active' : ''}`}
            onClick={() => setFilter(s)}
          >
            {s.replaceAll('_', ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <Loading>Loading governance decisions…</Loading>
      ) : error ? (
        <div className="notice error"><XCircle size={18} />{error}</div>
      ) : (
        <article className="panel full-table">
          {decisions.length ? (
            <table>
              <thead>
                <tr>
                  <th>Decision ID</th>
                  <th>Action</th>
                  <th>Amount</th>
                  <th>Target</th>
                  <th>Policy Decision</th>
                  <th>Governance Status</th>
                  <th>Created</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {decisions.map(d => (
                  <tr key={d.decisionId}>
                    <td><code>{d.decisionId}</code></td>
                    <td><b>{d.actionType.replaceAll('_', ' ')}</b></td>
                    <td>{money(d.amount)}</td>
                    <td>{d.target ?? 'N/A'}</td>
                    <td><StatusBadge value={d.decision} /></td>
                    <td><StatusBadge value={d.status} /></td>
                    <td>{new Date(d.createdAt).toLocaleString('en-IN')}</td>
                    <td>
                      <button className="btn-view" onClick={() => setSelectedId(d.decisionId)}>
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <Empty title="No decisions found" text="No safety decisions match the selected status filter." />
          )}
        </article>
      )}

      {selectedId && (
        <DecisionDetailDrawer
          decisionId={selectedId}
          onClose={() => setSelectedId(null)}
          onStatusUpdated={() => { loadDecisions() }}
        />
      )}
    </>
  )
}

function DecisionDetailDrawer({
  decisionId,
  onClose,
  onStatusUpdated
}: {
  decisionId: string
  onClose: () => void
  onStatusUpdated: () => void
}) {
  const [detail, setDetail] = useState<DecisionDetail | null>(null)
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showApproveModal, setShowApproveModal] = useState(false)
  const [showRejectModal, setShowRejectModal] = useState(false)

  const loadData = async () => {
    setLoading(true)
    try {
      const [d, events] = await Promise.all([
        getDecisionDetail(decisionId),
        getAuditEvents(decisionId)
      ])
      setDetail(d)
      setAuditEvents(events)
    } catch {
      setError('Failed to load decision detail or audit trail.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [decisionId])

  const handleApprove = async () => {
    try {
      await approveDecision(decisionId, 'demo-user', 'Reviewed and approved by human governance.')
      setShowApproveModal(false)
      await loadData()
      onStatusUpdated()
    } catch (err: any) {
      alert(err.response?.data?.error ?? 'Approval failed.')
    }
  }

  const handleReject = async (comment: string) => {
    try {
      await rejectDecision(decisionId, 'demo-user', comment)
      setShowRejectModal(false)
      await loadData()
      onStatusUpdated()
    } catch (err: any) {
      alert(err.response?.data?.error ?? 'Rejection failed.')
    }
  }

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer-content" onClick={e => e.stopPropagation()}>
        <div className="drawer-header">
          <div>
            <p className="eyebrow">DECISION DETAIL</p>
            <h2 style={{ margin: 0 }}><code>{decisionId}</code></h2>
          </div>
          <button className="modal-close" onClick={onClose}><X size={22} /></button>
        </div>

        {loading ? (
          <Loading>Fetching complete decision record…</Loading>
        ) : error || !detail ? (
          <div className="notice error"><XCircle size={18} />{error}</div>
        ) : (
          <>
            {/* Status Header Banner */}
            <article className={`decision-panel ${detail.policy.decision.toLowerCase()}`}>
              <p className="eyebrow">GOVERNANCE STATUS</p>
              <StatusBadge value={detail.status} />

              {detail.status === 'PENDING_REVIEW' && (
                <div className="notice clarification" style={{ marginTop: '14px' }}>
                  <AlertTriangle size={20} />
                  <div>
                    <h2>Human review required</h2>
                    <p>This action triggered policy review rules and requires manual sign-off before proceeding.</p>
                    <div className="action-bar">
                      <button className="btn-approve" onClick={() => setShowApproveModal(true)}>
                        <Check size={16} /> Approve
                      </button>
                      <button className="btn-reject" onClick={() => setShowRejectModal(true)}>
                        <Ban size={16} /> Reject
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {detail.status === 'BLOCKED' && (
                <div className="notice blocked-notice" style={{ marginTop: '14px' }}>
                  <XCircle size={20} />
                  <div>
                    <h2>BLOCKED — Action cannot be approved</h2>
                    <p>This action severely breaches financial safety thresholds and cannot be approved by human review. A new analysis request with different terms is required.</p>
                  </div>
                </div>
              )}

              {detail.status === 'APPROVED' && (
                <div className="notice approved-notice" style={{ marginTop: '14px' }}>
                  <CheckCircle2 size={20} />
                  <div>
                    <h2>Governance Approval Recorded</h2>
                    <p>Human governance approval has been recorded for this action. <strong>No money movement or payment execution was performed.</strong></p>
                  </div>
                </div>
              )}

              {detail.status === 'REJECTED' && (
                <div className="notice error" style={{ marginTop: '14px' }}>
                  <XCircle size={20} />
                  <div>
                    <h2>Action Rejected</h2>
                    <p>Human review rejected this financial action.</p>
                  </div>
                </div>
              )}
            </article>

            {/* Original Prompt & Intent */}
            <article className="panel">
              <p className="eyebrow">ORIGINAL REQUEST & INTENT</p>
              <p style={{ fontStyle: 'italic', background: '#f1f5f9', padding: '10px 14px', borderRadius: '6px' }}>
                "{detail.originalMessage}"
              </p>
              <dl>
                <div><dt>Action Type</dt><dd>{detail.intent.actionType}</dd></div>
                <div><dt>Amount</dt><dd>{money(detail.intent.amount)}</dd></div>
                <div><dt>Target</dt><dd>{detail.intent.target ?? 'Not specified'}</dd></div>
                <div><dt>Currency</dt><dd>{detail.intent.currency}</dd></div>
              </dl>
            </article>

            {/* Simulation Comparison */}
            <article className="panel">
              <p className="eyebrow">SIMULATION RESULTS</p>
              <div className="comparison">
                <SnapshotCard title="Before" data={detail.simulation.before} />
                <ArrowRight className="comparison-arrow" />
                <SnapshotCard title="After" data={detail.simulation.after} />
              </div>
            </article>

            {/* Policy Evaluation */}
            <article className="panel">
              <p className="eyebrow">POLICY EVALUATION</p>
              <h2>{detail.policy.reason}</h2>
              <p>{detail.policy.recommendation}</p>
            </article>

            {/* AI Explanation if present */}
            {detail.explanation && (
              <article className="panel explanation">
                <div>
                  <p className="eyebrow">AI EXPLANATION</p>
                  <h2>{detail.explanation.headline}</h2>
                  <p>{detail.explanation.explanation}</p>
                </div>
                <div>
                  <h3>Key factors</h3>
                  <ul>{detail.explanation.keyFactors.map(x => <li key={x}>{x}</li>)}</ul>
                </div>
              </article>
            )}

            {/* Immutable Audit Timeline */}
            <article className="panel">
              <p className="eyebrow">IMMUTABLE AUDIT TIMELINE</p>
              <h2>Event History</h2>
              <div className="timeline">
                {auditEvents.map(e => (
                  <div key={e.eventId} className="timeline-item">
                    <div className={`timeline-dot ${e.actorType.toLowerCase()}`} />
                    <div className="timeline-content">
                      <div className="timeline-meta">
                        <span className={`actor-badge ${e.actorType.toLowerCase()}`}>{e.actorType}: {e.actorId}</span>
                        <span>{new Date(e.createdAt).toLocaleTimeString('en-IN')}</span>
                      </div>
                      <strong style={{ display: 'block', fontSize: '12px', color: '#1e293b' }}>
                        {e.eventType.replaceAll('_', ' ')}
                      </strong>
                      <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#475569' }}>
                        {e.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </article>
          </>
        )}
      </div>

      {/* Approve Modal */}
      {showApproveModal && detail && (
        <ApproveModal
          detail={detail}
          onConfirm={handleApprove}
          onCancel={() => setShowApproveModal(false)}
        />
      )}

      {/* Reject Modal */}
      {showRejectModal && (
        <RejectModal
          onConfirm={handleReject}
          onCancel={() => setShowRejectModal(false)}
        />
      )}
    </div>
  )
}

function ApproveModal({
  detail,
  onConfirm,
  onCancel
}: {
  detail: DecisionDetail
  onConfirm: () => void
  onCancel: () => void
}) {
  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal-card" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Approve Financial Action?</h3>
          <button className="modal-close" onClick={onCancel}><X size={20} /></button>
        </div>
        <div className="modal-body">
          <p style={{ color: '#475569', fontSize: '14px', marginBottom: '16px' }}>
            You are approving human governance for this pending action:
          </p>
          <dl>
            <div><dt>Action</dt><dd>{detail.intent.actionType}</dd></div>
            <div><dt>Amount</dt><dd>{money(detail.intent.amount)}</dd></div>
            <div><dt>Target</dt><dd>{detail.intent.target ?? 'Not specified'}</dd></div>
            <div><dt>Policy Reason</dt><dd style={{ fontWeight: 400 }}>{detail.policy.reason}</dd></div>
          </dl>
          <div className="notice clarification" style={{ marginTop: '16px', fontSize: '12px' }}>
            <AlertTriangle size={16} />
            <p><strong>Note:</strong> Approving records human governance approval. <strong>No payment or money movement is executed.</strong></p>
          </div>
        </div>
        <div className="modal-actions">
          <button className="btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="btn-approve" onClick={onConfirm}>Confirm Approval</button>
        </div>
      </div>
    </div>
  )
}

function RejectModal({
  onConfirm,
  onCancel
}: {
  onConfirm: (comment: string) => void
  onCancel: () => void
}) {
  const [comment, setComment] = useState('')

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div className="modal-card" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Reject Financial Action</h3>
          <button className="modal-close" onClick={onCancel}><X size={20} /></button>
        </div>
        <div className="modal-body">
          <label htmlFor="reject-comment">Reason for Rejection (Required)</label>
          <textarea
            id="reject-comment"
            placeholder="e.g. Rejected because liquidity should be preserved for upcoming obligations."
            value={comment}
            onChange={e => setComment(e.target.value)}
          />
        </div>
        <div className="modal-actions">
          <button className="btn-secondary" onClick={onCancel}>Cancel</button>
          <button
            className="btn-reject"
            disabled={!comment.trim()}
            onClick={() => onConfirm(comment.trim())}
          >
            Confirm Rejection
          </button>
        </div>
      </div>
    </div>
  )
}

function AuditLogPage() {
  const [events, setEvents] = useState<AuditEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getAuditEvents()
      .then(data => setEvents(data))
      .catch(() => setError('Failed to retrieve append-only audit events.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <>
      <section className="page-intro">
        <div>
          <h2>Append-Only Audit Log</h2>
          <p>Immutable system, AI agent, and human governance audit timeline.</p>
        </div>
      </section>

      {loading ? (
        <Loading>Loading audit log events…</Loading>
      ) : error ? (
        <div className="notice error"><XCircle size={18} />{error}</div>
      ) : (
        <article className="panel full-table">
          {events.length ? (
            <table>
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Decision ID</th>
                  <th>Event Type</th>
                  <th>Actor</th>
                  <th>Description</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {events.map(e => (
                  <tr key={e.eventId}>
                    <td><code>{e.eventId}</code></td>
                    <td><code>{e.decisionId}</code></td>
                    <td><b>{e.eventType.replaceAll('_', ' ')}</b></td>
                    <td>
                      <span className={`actor-badge ${e.actorType.toLowerCase()}`}>
                        {e.actorType}: {e.actorId}
                      </span>
                    </td>
                    <td>{e.description}</td>
                    <td>{new Date(e.createdAt).toLocaleString('en-IN')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <Empty title="No audit events" text="No audit records have been generated yet." />
          )}
        </article>
      )}
    </>
  )
}

function Placeholder({ type }: { type: string }) {
  const copy: Record<string, [string, string]> = {
    simulations: ['No simulation history yet', 'Run an analysis from the AI Safety Center to see financial simulations here.'],
    customers: ['Customer intelligence will appear here when connected.', 'PayLens does not currently have a customer data source.'],
    settings: ['System settings', 'Environment and provider status are managed by the backend.']
  }
  return <Empty title={copy[type][0]} text={copy[type][1]} />
}

export default function App() {
  return (
    <Shell>
      <Routes>
        <Route path="/" element={<Overview />} />
        <Route path="/safety" element={<Safety />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/decisions" element={<DecisionsPage />} />
        <Route path="/audit" element={<AuditLogPage />} />
        {['simulations', 'customers', 'settings'].map(x => (
          <Route key={x} path={`/${x}`} element={<Placeholder type={x} />} />
        ))}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Shell>
  )
}
