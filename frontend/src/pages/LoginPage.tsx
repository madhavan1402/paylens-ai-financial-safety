import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Lock, Mail, LoaderCircle, AlertTriangle } from 'lucide-react';
import type { UserRole } from '../types/api';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await login({ email, password });
    } catch (err: any) {
      setError(err.response?.data?.error || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = async (roleEmail: string, rolePass: string) => {
    setEmail(roleEmail);
    setPassword(rolePass);
    setLoading(true);
    setError(null);
    try {
      await login({ email: roleEmail, password: rolePass });
    } catch (err: any) {
      setError(err.response?.data?.error || 'Quick login failed.');
    } finally {
      setLoading(false);
    }
  };

  const demoAccounts: { role: UserRole; name: string; email: string }[] = [
    { role: 'OWNER', name: 'Alexis Vance', email: 'owner@paylens.io' },
    { role: 'ADMIN', name: 'Morgan Reid', email: 'admin@paylens.io' },
    { role: 'FINANCE_MANAGER', name: 'Jordan Taylor', email: 'finance@paylens.io' },
    { role: 'REVIEWER', name: 'Sam Mercer', email: 'reviewer@paylens.io' },
    { role: 'OPERATOR', name: 'Taylor Brooke', email: 'operator@paylens.io' },
    { role: 'VIEWER', name: 'Casey Quinn', email: 'viewer@paylens.io' },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'radial-gradient(circle at top right, #1e293b, #0f172a)', padding: '1rem' }}>
      <div style={{ width: '100%', maxWidth: '440px', background: 'rgba(30, 41, 59, 0.85)', backdropFilter: 'blur(16px)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '16px', padding: '2.5rem', boxShadow: '0 20px 50px rgba(0,0,0,0.5)' }}>
        
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '56px', height: '56px', borderRadius: '14px', background: 'linear-gradient(135deg, #00f2fe, #4facfe)', marginBottom: '1rem', color: '#0f172a', fontWeight: 'bold' }}>
            <ShieldCheck size={32} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#f8fafc', margin: 0 }}>PAYLENS</h1>
          <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginTop: '0.35rem' }}>Production Financial Safety & Multi-Role Governance</p>
        </div>

        {error && (
          <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#ef4444', padding: '0.75rem 1rem', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertTriangle size={18} /> {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '0.4rem' }}>Work Email</label>
            <div style={{ position: 'relative' }}>
              <Mail size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="name@company.com"
                required
                style={{ width: '100%', padding: '0.75rem 0.75rem 0.75rem 2.5rem', background: '#0f172a', border: '1px solid #334155', borderRadius: '8px', color: '#f8fafc', fontSize: '0.9rem', outline: 'none' }}
              />
            </div>
          </div>

          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '0.4rem' }}>Password</label>
            <div style={{ position: 'relative' }}>
              <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                style={{ width: '100%', padding: '0.75rem 0.75rem 0.75rem 2.5rem', background: '#0f172a', border: '1px solid #334155', borderRadius: '8px', color: '#f8fafc', fontSize: '0.9rem', outline: 'none' }}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{ width: '100%', padding: '0.85rem', background: 'linear-gradient(135deg, #00f2fe, #4facfe)', border: 'none', borderRadius: '8px', color: '#0f172a', fontWeight: 'bold', fontSize: '0.95rem', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
          >
            {loading ? <LoaderCircle className="spin" size={18} /> : 'Sign In to Operations Console'}
          </button>
        </form>

        <div style={{ marginTop: '2rem', paddingTop: '1.5rem', borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
          <p style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem', textAlign: 'center' }}>
            Quick Demo Role Switcher
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            {demoAccounts.map(acc => (
              <button
                key={acc.role}
                type="button"
                onClick={() => handleQuickLogin(acc.email, 'Paylens123!')}
                style={{ padding: '0.5rem 0.65rem', background: 'rgba(15, 23, 42, 0.6)', border: '1px solid #334155', borderRadius: '6px', color: '#e2e8f0', fontSize: '0.75rem', textAlign: 'left', cursor: 'pointer', display: 'flex', flexDirection: 'column' }}
              >
                <strong style={{ color: '#00f2fe', fontSize: '0.7rem' }}>{acc.role.replace('_', ' ')}</strong>
                <span style={{ fontSize: '0.7rem', color: '#94a3b8' }}>{acc.name.split(' ')[0]}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
