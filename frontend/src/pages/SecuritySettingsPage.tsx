import React, { useState } from 'react';
import { changePasswordApi } from '../api/authApi';
import { useAuth } from '../context/AuthContext';
import { KeyRound, CheckCircle2, AlertTriangle, LoaderCircle, Building, User as UserIcon } from 'lucide-react';

export const SecuritySettingsPage: React.FC = () => {
  const { user, merchantName } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      const res = await changePasswordApi({ currentPassword, newPassword });
      setSuccess(res.message || 'Password changed successfully. Active sessions revoked.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to change password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
      <div className="card">
        <div className="card-header">
          <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <UserIcon size={20} style={{ color: '#00f2fe' }} /> Identity & User Profile
          </h2>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem', fontSize: '0.9rem' }}>
          <div style={{ background: 'var(--panel-light)', padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
            <span style={{ color: 'var(--muted)', fontSize: '0.8rem', display: 'block' }}>Display Name</span>
            <strong style={{ color: '#f8fafc', fontSize: '1.05rem' }}>{user?.displayName}</strong>
          </div>

          <div style={{ background: 'var(--panel-light)', padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
            <span style={{ color: 'var(--muted)', fontSize: '0.8rem', display: 'block' }}>Work Email</span>
            <strong>{user?.email}</strong>
          </div>

          <div style={{ background: 'var(--panel-light)', padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
            <span style={{ color: 'var(--muted)', fontSize: '0.8rem', display: 'block' }}>Assigned RBAC Role</span>
            <span className="badge safe" style={{ marginTop: '0.25rem' }}>{user?.role}</span>
          </div>

          <div style={{ background: 'var(--panel-light)', padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
            <span style={{ color: 'var(--muted)', fontSize: '0.8rem', display: 'block' }}>Merchant Account</span>
            <strong style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.2rem' }}>
              <Building size={16} style={{ color: '#00f2fe' }} /> {merchantName} (<code>{user?.merchantId}</code>)
            </strong>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <KeyRound size={20} style={{ color: '#00f2fe' }} /> Change Account Password
          </h2>
        </div>

        {error && (
          <div style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', padding: '0.75rem', borderRadius: '8px', marginTop: '1rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertTriangle size={16} /> {error}
          </div>
        )}

        {success && (
          <div style={{ background: 'rgba(34, 197, 94, 0.15)', color: '#22c55e', padding: '0.75rem', borderRadius: '8px', marginTop: '1rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CheckCircle2 size={16} /> {success}
          </div>
        )}

        <form onSubmit={handleChangePassword} style={{ marginTop: '1rem' }}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Current Password</label>
            <input
              type="password"
              value={currentPassword}
              onChange={e => setCurrentPassword(e.target.value)}
              required
              style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
            />
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>New Password (min 8 chars)</label>
            <input
              type="password"
              value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
              required
              minLength={8}
              style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
            />
          </div>

          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Confirm New Password</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              required
              minLength={8}
              style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
            />
          </div>

          <button type="submit" className="btn-primary" disabled={loading} style={{ width: '100%' }}>
            {loading ? <LoaderCircle className="spin" size={16} /> : 'Update Password & Revoke Sessions'}
          </button>
        </form>
      </div>
    </div>
  );
};
