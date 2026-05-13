import { createContext, useContext, useState, type ReactNode } from 'react'
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
  loading: false,
  login: () => {},
  logout: () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<LoginResp | null>(() => {
    try {
      const saved = localStorage.getItem('canvas_user')
      const token = localStorage.getItem('canvas_token')
      console.log('[AUTH] init token:', token?.slice(0,20), 'saved:', !!saved)
      return saved ? JSON.parse(saved) : null
    } catch {
      return null
    }
  })

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
    <AuthContext.Provider value={{ user, isAdmin: user?.role === 'ADMIN', loading: false, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
