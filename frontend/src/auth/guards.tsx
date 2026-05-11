import { Navigate, Outlet } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuth } from '../context/AuthContext'

/** 未登录时重定向到 /login */
export function RequireAuth() {
  const { user, loading } = useAuth()
  if (loading) return <Spin fullscreen />
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

/** 仅 ADMIN 可访问，否则 403 */
export function RequireAdmin() {
  const { user, loading, isAdmin } = useAuth()
  if (loading) return <Spin fullscreen />
  if (!user) return <Navigate to="/login" replace />
  if (!isAdmin) return <div style={{ padding: 24 }}>无权限（需要 ADMIN 角色）</div>
  return <Outlet />
}
