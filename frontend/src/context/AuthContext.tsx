import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import type { LoginResp } from '../services/api'

interface AuthState {
  user: LoginResp | null
  isAdmin: boolean
  loading: boolean
  login: (resp: LoginResp) => void
  logout: () => void
}

const AuthContext = createContext<AuthState>({
  user: null,
  isAdmin: false,
  loading: true,
  login: () => {},
  logout: () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<LoginResp | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const saved = localStorage.getItem('canvas_user')
    if (saved) {
      try { setUser(JSON.parse(saved)) } catch { /* ignore */ }
    }
    setLoading(false)
  }, [])

  const login = (resp: LoginResp) => {
    localStorage.setItem('canvas_token', resp.token)
    localStorage.setItem('canvas_user', JSON.stringify(resp))
    setUser(resp)
  }

  const logout = () => {
    localStorage.removeItem('canvas_token')
    localStorage.removeItem('canvas_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isAdmin: user?.role === 'ADMIN', loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
