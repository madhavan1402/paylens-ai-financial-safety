import React, { createContext, useContext, useEffect, useState } from 'react';
import axios from 'axios';
import { loginApi, logoutApi, refreshTokenApi } from '../api/authApi';
import type { LoginRequest, UserResponse, UserRole } from '../types/api';

interface AuthContextType {
  user: UserResponse | null;
  merchantName: string;
  accessToken: string | null;
  authenticated: boolean;
  loading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (roles: UserRole[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [merchantName, setMerchantName] = useState<string>('Acme Commerce Pvt Ltd');
  const [accessToken, setAccessToken] = useState<string | null>(localStorage.getItem('paylens_access_token'));
  const [refreshToken, setRefreshToken] = useState<string | null>(localStorage.getItem('paylens_refresh_token'));
  const [loading, setLoading] = useState<boolean>(true);

  // Setup Axios interceptor to attach Bearer token to all outgoing requests
  useEffect(() => {
    const reqInterceptor = axios.interceptors.request.use(config => {
      const token = localStorage.getItem('paylens_access_token');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    const resInterceptor = axios.interceptors.response.use(
      res => res,
      async error => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;
          const storedRefreshToken = localStorage.getItem('paylens_refresh_token');
          if (storedRefreshToken) {
            try {
              const data = await refreshTokenApi(storedRefreshToken);
              setAccessToken(data.accessToken);
              setRefreshToken(data.refreshToken);
              setUser(data.user);
              setMerchantName(data.merchantName);
              localStorage.setItem('paylens_access_token', data.accessToken);
              localStorage.setItem('paylens_refresh_token', data.refreshToken);
              localStorage.setItem('paylens_user', JSON.stringify(data.user));
              localStorage.setItem('paylens_merchant', data.merchantName);

              originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
              return axios(originalRequest);
            } catch (e) {
              clearAuth();
            }
          } else {
            clearAuth();
          }
        }
        return Promise.reject(error);
      }
    );

    // Initial auth restore from localStorage
    const savedUser = localStorage.getItem('paylens_user');
    const savedMerchant = localStorage.getItem('paylens_merchant');
    if (accessToken && savedUser) {
      try {
        setUser(JSON.parse(savedUser));
        if (savedMerchant) setMerchantName(savedMerchant);
      } catch (e) {
        clearAuth();
      }
    }
    setLoading(false);

    return () => {
      axios.interceptors.request.eject(reqInterceptor);
      axios.interceptors.response.eject(resInterceptor);
    };
  }, []);

  const clearAuth = () => {
    setUser(null);
    setAccessToken(null);
    setRefreshToken(null);
    localStorage.removeItem('paylens_access_token');
    localStorage.removeItem('paylens_refresh_token');
    localStorage.removeItem('paylens_user');
    localStorage.removeItem('paylens_merchant');
  };

  const login = async (req: LoginRequest) => {
    const data = await loginApi(req);
    setUser(data.user);
    setMerchantName(data.merchantName);
    setAccessToken(data.accessToken);
    setRefreshToken(data.refreshToken);
    localStorage.setItem('paylens_access_token', data.accessToken);
    localStorage.setItem('paylens_refresh_token', data.refreshToken);
    localStorage.setItem('paylens_user', JSON.stringify(data.user));
    localStorage.setItem('paylens_merchant', data.merchantName);
  };

  const logout = async () => {
    if (refreshToken) {
      try { await logoutApi(refreshToken); } catch (e) {}
    }
    clearAuth();
  };

  const hasRole = (roles: UserRole[]): boolean => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  return (
    <AuthContext.Provider value={{
      user,
      merchantName,
      accessToken,
      authenticated: !!user && !!accessToken,
      loading,
      login,
      logout,
      hasRole
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
