import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import type { AuthResponse, AuthUser } from '../types/auth'
import { clearSession, getStoredToken, getStoredUser, setSession } from '../services/api'

interface AuthContextValue {
  user: AuthUser | null
  token: string | null
  isAuthenticated: boolean
  isBuyer: boolean
  isBroker: boolean
  isAdmin: boolean
  signIn: (authResponse: AuthResponse) => void
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser())
  const [token, setToken] = useState<string | null>(() => getStoredToken())

  const signIn = (authResponse: AuthResponse) => {
    setSession(authResponse)
    setUser(authResponse.user)
    setToken(authResponse.accessToken)
  }

  const signOut = () => {
    clearSession()
    setUser(null)
    setToken(null)
  }

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    isAuthenticated: Boolean(user && token),
    isBuyer: user?.role === 'BUYER',
    isBroker: user?.role === 'BROKER',
    isAdmin: user?.role === 'ADMIN',
    signIn,
    signOut,
  }), [token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
