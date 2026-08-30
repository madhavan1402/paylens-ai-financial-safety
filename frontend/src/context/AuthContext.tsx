import React, { createContext, useContext, useEffect, useState } from 'react'
import { loginApi, logoutApi } from '../api/authApi'
import type { LoginRequest, UserResponse, UserRole } from '../types/api'

/**
 * AuthContext — provides the authenticated user, JWT state, and auth operations.
 *
 * JWT is attached to all API requests by the centralized api client (client.ts).
 * AuthContext is only responsible for storing / restoring auth state and calling
 * login / logout / refresh endpoints.
 */

interface AuthContextType {
  user: UserResponse | null
  merchantName: string
  accessToken: string | null
  authenticated: boolean
  loading: boolean
  login: (req: LoginRequest) => Promise<void>
  logout: () => Promise<void>
  hasRole: (roles: UserRole[]) => boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [merchantName, setMerchantName] = useState<string>('Acme Commerce Pvt Ltd')
  const [accessToken, setAccessToken] = useState<string | null>(localStorage.getItem('paylens_access_token'))
  const [refreshToken, setRefreshToken] = useState<string | null>(localStorage.getItem('paylens_refresh_token'))
  const [loading, setLoading] = useState<boolean>(true)

  // Restore session from localStorage on mount
  useEffect(() => {
    const storedToken = localStorage.getItem('paylens_access_token')
    const savedUser = localStorage.getItem('paylens_user')
    const savedMerchant = localStorage.getItem('paylens_merchant')

    if (storedToken && savedUser) {
      try {
        setUser(JSON.parse(savedUser))
        setAccessToken(storedToken)
        if (savedMerchant) setMerchantName(savedMerchant)
      } catch {
        clearAuth()
      }
    } else if (storedToken && !savedUser) {
      // Token present but no user data — clear to force re-login
      clearAuth()
    }
    setLoading(false)
  }, [])

  const clearAuth = () => {
    setUser(null)
    setAccessToken(null)
    setRefreshToken(null)
    localStorage.removeItem('paylens_access_token')
    localStorage.removeItem('paylens_refresh_token')
    localStorage.removeItem('paylens_user')
    localStorage.removeItem('paylens_merchant')
  }

  const login = async (req: LoginRequest) => {
    const data = await loginApi(req)
    setUser(data.user)
    setMerchantName(data.merchantName)
    setAccessToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    localStorage.setItem('paylens_access_token', data.accessToken)
    localStorage.setItem('paylens_refresh_token', data.refreshToken)
    localStorage.setItem('paylens_user', JSON.stringify(data.user))
    localStorage.setItem('paylens_merchant', data.merchantName)
  }

  const logout = async () => {
    if (refreshToken) {
      try { await logoutApi(refreshToken) } catch { /* ignore */ }
    }
    clearAuth()
  }

  const hasRole = (roles: UserRole[]): boolean => {
    if (!user) return false
    return roles.includes(user.role)
  }

  return (
    <AuthContext.Provider value={{
      user,
      merchantName,
      accessToken,
      authenticated: !!user && !!accessToken,
      loading,
      login,
      logout,
      hasRole,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
