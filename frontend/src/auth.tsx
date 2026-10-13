import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { clearAuth, loadAuth, post, saveAuth, setUnauthorizedHandler } from './api';
import type { AuthState } from './types';

interface AuthContextValue {
  auth: AuthState | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => loadAuth());

  useEffect(() => {
    setUnauthorizedHandler(() => setAuth(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const result = await post<AuthState>('/api/auth/login', { email, password });
    saveAuth(result);
    setAuth(result);
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setAuth(null);
  }, []);

  return <AuthContext.Provider value={{ auth, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
