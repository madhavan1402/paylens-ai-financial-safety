import { useState, useRef, useEffect, type FormEvent, type KeyboardEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Bot, Send, Sparkles, TrendingUp, AlertTriangle, ShieldCheck, BarChart3,
  Brain, CheckCircle2, XCircle, Clock, Zap, Info, ChevronDown, ChevronUp,
  DollarSign, Activity
} from 'lucide-react'
import { queryCopilot } from '../api/copilotApi'
import type { CopilotIntent, CopilotResponse } from '../types/api'

/* ─────────────────────────────────────── types ─────────────────────────────────────── */

interface ChatMessage {
  id: string
  role: 'user' | 'copilot'
  text?: string
  response?: CopilotResponse
  timestamp: Date
  loading?: boolean
}

/* ──────────────────────────────────── constants ─────────────────────────────────────── */

const SUGGESTED_PROMPTS = [
  { label: 'Financial Status', icon: Activity, text: 'What is my current financial health status?' },
  { label: 'Risk Signals', icon: AlertTriangle, text: 'What are the active risk signals I should know about?' },
  { label: 'Liquidity Forecast', icon: TrendingUp, text: 'What will my liquidity look like over the next 7 days?' },
  { label: 'Policy Explained', icon: ShieldCheck, text: 'Why was a payment blocked by the policy engine?' },
  { label: 'Refund Analysis', icon: DollarSign, text: 'Should I refund ₹50,000?', actionType: 'REFUND', amount: 50000 },
  { label: 'Payout Safety', icon: BarChart3, text: 'Is it safe to pay ₹2,00,000 in vendor bills?', actionType: 'PAYOUT', amount: 200000 },
]

const INTENT_META: Record<CopilotIntent, { label: string; icon: React.ReactNode; color: string }> = {
  FINANCIAL_STATUS: { label: 'Financial Status', icon: <Activity size={14} />, color: '#4ade80' },
  ACTION_ANALYSIS: { label: 'Action Analysis', icon: <Zap size={14} />, color: '#f59e0b' },
  RISK_EXPLANATION: { label: 'Risk Explanation', icon: <AlertTriangle size={14} />, color: '#ef4444' },
  POLICY_EXPLANATION: { label: 'Policy Explanation', icon: <ShieldCheck size={14} />, color: '#818cf8' },
  FORECAST_QUERY: { label: 'Forecast Query', icon: <TrendingUp size={14} />, color: '#22d3ee' },
  UNKNOWN: { label: 'Unknown', icon: <Brain size={14} />, color: '#9ca3af' },
}

const POLICY_COLOR = { SAFE: '#4ade80', REVIEW: '#f59e0b', BLOCK: '#ef4444' }
const POLICY_ICON = {
  SAFE: <CheckCircle2 size={16} />,
  REVIEW: <Clock size={16} />,
  BLOCK: <XCircle size={16} />,
}

const money = (v: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v)

/* ──────────────────────────────────────────────────────────────────────────────────────── */

function IntentBadge({ intent }: { intent: CopilotIntent }) {
  const meta = INTENT_META[intent]
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      padding: '2px 8px', borderRadius: 20, fontSize: 11, fontWeight: 600,
      background: meta.color + '20', color: meta.color, border: `1px solid ${meta.color}40`
    }}>
      {meta.icon} {meta.label}
    </span>
  )
}

function PolicyBadge({ decision }: { decision: string }) {
  const color = POLICY_COLOR[decision as keyof typeof POLICY_COLOR] ?? '#9ca3af'
  const icon = POLICY_ICON[decision as keyof typeof POLICY_ICON] ?? <Info size={16} />
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      padding: '4px 12px', borderRadius: 20, fontSize: 12, fontWeight: 700,
      background: color + '20', color, border: `1px solid ${color}40`
    }}>
      {icon} {decision}
    </span>
  )
}

function CopilotCard({ response }: { response: CopilotResponse }) {
  const [showSim, setShowSim] = useState(false)

  return (
    <div style={{
      background: 'linear-gradient(135deg, rgba(30,30,50,0.95) 0%, rgba(20,20,40,0.98) 100%)',
      border: '1px solid rgba(129,140,248,0.2)',
      borderRadius: 16, padding: '20px 24px',
      boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
      maxWidth: 680
    }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
        <IntentBadge intent={response.intent} />
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {response.policyDecision && <PolicyBadge decision={response.policyDecision} />}
          {response.requiresHumanReview && (
            <span style={{
              display: 'inline-flex', alignItems: 'center', gap: 4,
              padding: '2px 8px', borderRadius: 20, fontSize: 11, fontWeight: 600,
              background: '#f59e0b20', color: '#f59e0b', border: '1px solid #f59e0b40'
            }}>
              <AlertTriangle size={12} /> Human Review Required
            </span>
          )}
        </div>
      </div>

      {/* Headline */}
      <h3 style={{ margin: '0 0 10px', fontSize: 16, fontWeight: 700, color: '#f1f5f9', lineHeight: 1.5 }}>
        {response.headline}
      </h3>

      {/* Explanation */}
      <p style={{ margin: '0 0 16px', fontSize: 14, color: '#94a3b8', lineHeight: 1.7 }}>
        {response.explanation}
      </p>

      {/* Key Factors */}
      {response.keyFactors && response.keyFactors.length > 0 && (
        <div style={{
          background: 'rgba(255,255,255,0.04)', borderRadius: 10,
          padding: '12px 16px', marginBottom: 16
        }}>
          <p style={{ margin: '0 0 8px', fontSize: 11, fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: 1 }}>
            Key Facts
          </p>
          <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
            {response.keyFactors.map((fact, i) => (
              <li key={i} style={{
                display: 'flex', alignItems: 'flex-start', gap: 8,
                padding: '4px 0', fontSize: 13, color: '#cbd5e1'
              }}>
                <span style={{ color: '#6366f1', marginTop: 2, flexShrink: 0 }}>›</span>
                {fact}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Financial Impact */}
      {response.financialImpact && (
        <div style={{
          background: 'rgba(99,102,241,0.08)', border: '1px solid rgba(99,102,241,0.2)',
          borderRadius: 10, padding: '10px 14px', marginBottom: 14
        }}>
          <p style={{ margin: 0, fontSize: 11, fontWeight: 700, color: '#818cf8', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4 }}>
            Financial Impact
          </p>
          <p style={{ margin: 0, fontSize: 13, color: '#c7d2fe' }}>{response.financialImpact}</p>
        </div>
      )}

      {/* Recommendation */}
      <div style={{
        background: response.requiresHumanReview
          ? 'rgba(245,158,11,0.08)' : 'rgba(74,222,128,0.06)',
        border: `1px solid ${response.requiresHumanReview ? 'rgba(245,158,11,0.25)' : 'rgba(74,222,128,0.2)'}`,
        borderRadius: 10, padding: '10px 14px', marginBottom: 14
      }}>
        <p style={{
          margin: 0, fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4,
          color: response.requiresHumanReview ? '#f59e0b' : '#4ade80'
        }}>
          PayLens Recommends
        </p>
        <p style={{ margin: 0, fontSize: 13, color: response.requiresHumanReview ? '#fde68a' : '#86efac' }}>
          {response.recommendation}
        </p>
      </div>

      {/* Simulation detail (expandable) */}
      {response.simulation && (
        <div>
          <button
            onClick={() => setShowSim(v => !v)}
            style={{
              display: 'flex', alignItems: 'center', gap: 6,
              background: 'none', border: 'none', cursor: 'pointer',
              color: '#6366f1', fontSize: 12, fontWeight: 600, padding: '4px 0'
            }}
          >
            {showSim ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
            {showSim ? 'Hide' : 'Show'} Simulation Detail
          </button>
          <AnimatePresence>
            {showSim && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                style={{ overflow: 'hidden' }}
              >
                <div style={{
                  display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12,
                  marginTop: 12, padding: '12px 0'
                }}>
                  {[
                    { label: 'Balance Before', value: money(response.simulation.before.currentBalance) },
                    { label: 'Balance After', value: money(response.simulation.after.currentBalance) },
                    { label: 'Buffer Before', value: money(response.simulation.before.safetyBuffer) },
                    { label: 'Buffer After', value: money(response.simulation.after.safetyBuffer) },
                  ].map(({ label, value }) => (
                    <div key={label} style={{
                      background: 'rgba(255,255,255,0.04)', borderRadius: 8,
                      padding: '10px 14px'
                    }}>
                      <p style={{ margin: 0, fontSize: 11, color: '#64748b', marginBottom: 4 }}>{label}</p>
                      <p style={{ margin: 0, fontSize: 15, fontWeight: 700, color: '#f1f5f9' }}>{value}</p>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}

      {/* Footer */}
      <div style={{
        marginTop: 14, paddingTop: 10, borderTop: '1px solid rgba(255,255,255,0.06)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between'
      }}>
        <span style={{ fontSize: 11, color: '#374151' }}>
          Deterministic · {new Date(response.generatedAt).toLocaleTimeString()}
        </span>
        <span style={{ fontSize: 10, color: '#374151', fontStyle: 'italic' }}>
          Copilot cannot execute or authorise transactions
        </span>
      </div>
    </div>
  )
}

/* ──────────────────────────────────── main page ─────────────────────────────────────── */

export function CopilotPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const sendMessage = async (text: string, actionType?: string, amount?: number) => {
    if (!text.trim() || loading) return
    setInput('')

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      text: text.trim(),
      timestamp: new Date()
    }
    const loadingMsg: ChatMessage = {
      id: Date.now().toString() + '_loading',
      role: 'copilot',
      loading: true,
      timestamp: new Date()
    }
    setMessages(prev => [...prev, userMsg, loadingMsg])
    setLoading(true)

    try {
      const response = await queryCopilot({
        message: text.trim(),
        actionType: actionType ?? undefined,
        amount: amount ?? undefined
      })
      setMessages(prev =>
        prev.map(m => m.id === loadingMsg.id
          ? { ...m, loading: false, response }
          : m
        )
      )
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : 'Unknown error'
      setMessages(prev =>
        prev.map(m => m.id === loadingMsg.id
          ? { ...m, loading: false, text: `⚠️ Error: ${errMsg}` }
          : m
        )
      )
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    sendMessage(input)
  }

  const handleKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage(input)
    }
  }

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', height: '100%',
      background: 'transparent', position: 'relative', overflow: 'hidden'
    }}>
      {/* ── Header ── */}
      <div style={{
        padding: '20px 28px 16px',
        borderBottom: '1px solid rgba(255,255,255,0.07)',
        background: 'rgba(10,10,25,0.8)',
        backdropFilter: 'blur(20px)',
        flexShrink: 0
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 6 }}>
          <div style={{
            width: 40, height: 40, borderRadius: 12,
            background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 0 20px rgba(99,102,241,0.4)'
          }}>
            <Bot size={22} color="white" />
          </div>
          <div>
            <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: '#f1f5f9' }}>
              PayLens Copilot
            </h1>
            <p style={{ margin: 0, fontSize: 12, color: '#64748b' }}>
              AI-powered fintech intelligence · Deterministic · Cannot execute transactions
            </p>
          </div>
        </div>
      </div>

      {/* ── Chat Area ── */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: '24px 28px',
        display: 'flex', flexDirection: 'column', gap: 20
      }}>
        {/* Welcome state */}
        {messages.length === 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            style={{ textAlign: 'center', padding: '40px 0' }}
          >
            <div style={{
              width: 72, height: 72, borderRadius: 24, margin: '0 auto 20px',
              background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 0 40px rgba(99,102,241,0.35)'
            }}>
              <Sparkles size={34} color="white" />
            </div>
            <h2 style={{ margin: '0 0 8px', color: '#f1f5f9', fontSize: 22, fontWeight: 700 }}>
              How can I help you today?
            </h2>
            <p style={{ margin: '0 0 32px', color: '#64748b', fontSize: 14, maxWidth: 480, marginInline: 'auto' }}>
              Ask me anything about your financial health, risk signals, upcoming obligations,
              or whether a payment is safe to execute. I use deterministic data — not guesses.
            </p>

            {/* Suggested prompts */}
            <div style={{
              display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
              gap: 12, maxWidth: 680, margin: '0 auto'
            }}>
              {SUGGESTED_PROMPTS.map(({ label, icon: Icon, text, actionType, amount }) => (
                <motion.button
                  key={label}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => sendMessage(text, actionType, amount)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    padding: '12px 16px', borderRadius: 12, cursor: 'pointer',
                    background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)',
                    color: '#cbd5e1', fontSize: 13, fontWeight: 500,
                    textAlign: 'left', transition: 'all 0.2s'
                  }}
                >
                  <Icon size={16} style={{ color: '#6366f1', flexShrink: 0 }} />
                  {label}: <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>{text}</span>
                </motion.button>
              ))}
            </div>
          </motion.div>
        )}

        {/* Messages */}
        <AnimatePresence mode="popLayout">
          {messages.map(msg => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              style={{
                display: 'flex',
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                gap: 12, alignItems: 'flex-start'
              }}
            >
              {/* Avatar */}
              <div style={{
                width: 36, height: 36, borderRadius: 10, flexShrink: 0,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: msg.role === 'user'
                  ? 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)'
                  : 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
                boxShadow: `0 4px 12px ${msg.role === 'user' ? 'rgba(59,130,246,0.3)' : 'rgba(99,102,241,0.3)'}`
              }}>
                {msg.role === 'user' ? '👤' : <Bot size={18} color="white" />}
              </div>

              {/* Content */}
              <div style={{ maxWidth: '80%' }}>
                {msg.loading ? (
                  <div style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    padding: '14px 18px', borderRadius: 14,
                    background: 'rgba(99,102,241,0.1)',
                    border: '1px solid rgba(99,102,241,0.2)'
                  }}>
                    {[0, 1, 2].map(i => (
                      <motion.div
                        key={i}
                        animate={{ opacity: [0.4, 1, 0.4] }}
                        transition={{ duration: 1.2, repeat: Infinity, delay: i * 0.2 }}
                        style={{ width: 8, height: 8, borderRadius: '50%', background: '#6366f1' }}
                      />
                    ))}
                    <span style={{ color: '#64748b', fontSize: 13 }}>Analysing…</span>
                  </div>
                ) : msg.role === 'copilot' && msg.response ? (
                  <CopilotCard response={msg.response} />
                ) : (
                  <div style={{
                    padding: '12px 16px', borderRadius: 14, fontSize: 14,
                    background: msg.role === 'user'
                      ? 'linear-gradient(135deg, rgba(59,130,246,0.2) 0%, rgba(29,78,216,0.2) 100%)'
                      : 'rgba(255,255,255,0.04)',
                    border: `1px solid ${msg.role === 'user' ? 'rgba(59,130,246,0.25)' : 'rgba(255,255,255,0.08)'}`,
                    color: '#e2e8f0', lineHeight: 1.6
                  }}>
                    {msg.text}
                  </div>
                )}
                <div style={{
                  marginTop: 4, fontSize: 11, color: '#374151', textAlign: msg.role === 'user' ? 'right' : 'left'
                }}>
                  {msg.timestamp.toLocaleTimeString()}
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        <div ref={bottomRef} />
      </div>

      {/* ── Input Area ── */}
      <div style={{
        padding: '16px 28px 24px', flexShrink: 0,
        borderTop: '1px solid rgba(255,255,255,0.06)',
        background: 'rgba(10,10,25,0.9)',
        backdropFilter: 'blur(20px)'
      }}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
          <div style={{
            flex: 1, position: 'relative',
            background: 'rgba(255,255,255,0.05)',
            border: '1px solid rgba(99,102,241,0.3)',
            borderRadius: 14,
            transition: 'border-color 0.2s'
          }}>
            <textarea
              ref={inputRef}
              id="copilot-input"
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKey}
              placeholder="Ask about financial health, risks, policy decisions, or payment safety…"
              rows={1}
              style={{
                width: '100%', padding: '14px 16px', fontSize: 14,
                background: 'transparent', border: 'none', outline: 'none',
                color: '#f1f5f9', resize: 'none', lineHeight: 1.6,
                fontFamily: 'inherit', boxSizing: 'border-box'
              }}
              onInput={e => {
                const el = e.currentTarget
                el.style.height = 'auto'
                el.style.height = Math.min(el.scrollHeight, 140) + 'px'
              }}
            />
          </div>
          <motion.button
            type="submit"
            disabled={loading || !input.trim()}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            style={{
              width: 48, height: 48, borderRadius: 12, border: 'none', cursor: 'pointer',
              background: loading || !input.trim()
                ? 'rgba(99,102,241,0.2)'
                : 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0, boxShadow: loading || !input.trim()
                ? 'none'
                : '0 4px 16px rgba(99,102,241,0.4)',
              transition: 'all 0.2s'
            }}
          >
            <Send size={18} color={loading || !input.trim() ? '#4b5563' : 'white'} />
          </motion.button>
        </form>
        <p style={{ margin: '8px 0 0', fontSize: 11, color: '#374151', textAlign: 'center' }}>
          Powered by deterministic financial intelligence · Press Enter to send · Shift+Enter for newline
        </p>
      </div>
    </div>
  )
}
