/**
 * 上下文职责：认证上下文，维护当前用户、管理员判断和本地持久化登录态。
 *
 * 维护说明：Provider 负责 localStorage 与 React 状态的同步，调用方通过 useAuth 读取和更新。
 */
import { createContext, useContext, useState, type ReactNode } from 'react'
import type { LoginResp } from '../services/api'

/**
 * 全局认证态：负责 token/user 持久化及登录态切换。
 * 注：loading 预留给后续接入 `/auth/me` 的异步初始化流程。
 */
interface AuthState {
  /** 当前登录用户。 */
  user: LoginResp | null

  /** 是否管理员。 */
  isAdmin: boolean

  /** 初始化/校验登录态时的加载标记。 */
  loading: boolean

  /** 登录并写入本地会话。 */
  login: (resp: LoginResp) => void

  /** 退出并清理本地会话。 */
  logout: () => void
}

/** 认证上下文默认值；真实登录态由 AuthProvider 在运行时注入。 */
const AuthContext = createContext<AuthState>({
  user: null,
  isAdmin: false,
  loading: false,
  login: () => {},
  logout: () => {},
})

/** 认证状态 Provider，负责 localStorage 恢复和登录态写入。 */
export function AuthProvider({ children }: { children: ReactNode }) {
  // 首次渲染从 localStorage 恢复，保证刷新后不丢失会话
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

  /** 登录成功后写入 token、用户快照和 React 状态。 */
  const login = (resp: LoginResp) => {
    // token 与用户快照同时写入，避免页面切换期间状态不一致
    localStorage.setItem('canvas_token', resp.token)
    localStorage.setItem('canvas_user', JSON.stringify(resp))
    setUser(resp)
  }

  /** 退出登录时清理本地会话并重置认证态。 */
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

/** 在业务组件中读取认证态的唯一入口。 */
export const useAuth = () => useContext(AuthContext)
