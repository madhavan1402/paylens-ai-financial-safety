import React, { useEffect, useState } from 'react';
import { createUserApi, getUsersApi, updateUserRoleApi, updateUserStatusApi } from '../api/usersApi';
import type { UserResponse, UserRole, UserStatus } from '../types/api';
import { Users, UserPlus, CheckCircle2, XCircle, AlertTriangle, LoaderCircle, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const UserManagementPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);

  // New user form state
  const [newEmail, setNewEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newDisplayName, setNewDisplayName] = useState('');
  const [newRole, setNewRole] = useState<UserRole>('VIEWER');
  const [submitting, setSubmitting] = useState(false);

  const loadUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getUsersApi();
      setUsers(data);
    } catch (e: any) {
      setError(e.response?.data?.error || 'Failed to load users.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadUsers(); }, []);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await createUserApi({
        email: newEmail,
        password: newPassword,
        displayName: newDisplayName,
        role: newRole
      });
      setShowAddModal(false);
      setNewEmail('');
      setNewPassword('');
      setNewDisplayName('');
      setNewRole('VIEWER');
      await loadUsers();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create user.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRoleChange = async (userId: string, role: UserRole) => {
    setError(null);
    try {
      await updateUserRoleApi(userId, role);
      await loadUsers();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to update role.');
    }
  };

  const handleStatusToggle = async (userId: string, currentStatus: UserStatus) => {
    setError(null);
    const newStatus: UserStatus = currentStatus === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    try {
      await updateUserStatusApi(userId, newStatus);
      await loadUsers();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to update user status.');
    }
  };

  const rolesList: UserRole[] = ['OWNER', 'ADMIN', 'FINANCE_MANAGER', 'REVIEWER', 'OPERATOR', 'VIEWER'];

  return (
    <>
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users size={20} style={{ color: '#00f2fe' }} /> User & Identity Management
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.25rem' }}>
              Manage team access, RBAC permissions, and active operational accounts.
            </p>
          </div>
          <button className="btn-primary" onClick={() => setShowAddModal(true)}>
            <UserPlus size={16} /> Add Team Member
          </button>
        </div>
      </div>

      {error && (
        <div style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', padding: '0.75rem 1rem', borderRadius: '8px', marginBottom: '1rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertTriangle size={16} /> {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Merchant Users</h2>
          <span className="badge review">{users.length} Account(s)</span>
        </div>

        {loading ? (
          <div className="loading"><LoaderCircle className="spin" /> Loading team members...</div>
        ) : (
          <div className="table-container" style={{ marginTop: '1rem' }}>
            <table>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Last Login</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={u.userId}>
                    <td>
                      <strong style={{ color: '#f8fafc' }}>{u.displayName}</strong>
                      <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}><code>{u.userId}</code></div>
                    </td>
                    <td>{u.email}</td>
                    <td>
                      <select
                        value={u.role}
                        onChange={e => handleRoleChange(u.userId, e.target.value as UserRole)}
                        disabled={currentUser?.userId === u.userId || (u.role === 'OWNER' && currentUser?.role !== 'OWNER')}
                        style={{ padding: '0.35rem 0.5rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)', fontSize: '0.8rem' }}
                      >
                        {rolesList.map(r => (
                          <option key={r} value={r} disabled={r === 'OWNER' && currentUser?.role !== 'OWNER'}>
                            {r.replace('_', ' ')}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span className={`badge ${u.status === 'ACTIVE' ? 'safe' : 'blocked'}`}>
                        {u.status === 'ACTIVE' ? <CheckCircle2 size={12} /> : <XCircle size={12} />}
                        {u.status}
                      </span>
                    </td>
                    <td>{u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : 'Never'}</td>
                    <td>
                      <button
                        className="btn-secondary"
                        style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem' }}
                        disabled={currentUser?.userId === u.userId}
                        onClick={() => handleStatusToggle(u.userId, u.status)}
                      >
                        {u.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showAddModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Create User Account</h3>
              <button className="icon-button" onClick={() => setShowAddModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateUser}>
              <div className="modal-body">
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Full Name</label>
                  <input
                    type="text"
                    value={newDisplayName}
                    onChange={e => setNewDisplayName(e.target.value)}
                    placeholder="Jane Doe"
                    required
                    style={{ width: '100%', padding: '0.6rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
                  />
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Work Email</label>
                  <input
                    type="email"
                    value={newEmail}
                    onChange={e => setNewEmail(e.target.value)}
                    placeholder="jane@company.com"
                    required
                    style={{ width: '100%', padding: '0.6rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
                  />
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>Initial Password (min 8 chars)</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    minLength={8}
                    style={{ width: '100%', padding: '0.6rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
                  />
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: '0.25rem' }}>User Role</label>
                  <select
                    value={newRole}
                    onChange={e => setNewRole(e.target.value as UserRole)}
                    style={{ width: '100%', padding: '0.6rem', borderRadius: '6px', background: 'var(--panel-light)', border: '1px solid var(--border)', color: 'var(--text)' }}
                  >
                    {rolesList.map(r => (
                      <option key={r} value={r} disabled={r === 'OWNER' && currentUser?.role !== 'OWNER'}>
                        {r.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={() => setShowAddModal(false)}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={submitting}>
                  {submitting ? <LoaderCircle className="spin" size={16} /> : 'Create User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};
