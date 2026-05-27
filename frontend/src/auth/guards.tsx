import { Navigate, Outlet } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuth } from '../context/AuthContext'

/**
 * 认证守卫：
 * 1) 首次加载先等待 AuthContext 完成本地 token/user 恢复；
 * 2) 未登录统一跳到 `/login`；
 * 3) 已登录则渲染子路由。
 *
 * 设计意图：
 * - 把“是否可访问路由”的判断集中在路由层；
 * - 页面组件只关心业务渲染，不再重复写登录判断。
 */
export function RequireAuth() {
  const { user, loading } = useAuth()
  // 加载中先占位，避免“先渲染页面再跳登录”的闪烁
  if (loading) return <Spin fullscreen />
  // replace=true: 避免未登录用户回退历史时又回到受保护页
  // Outlet 表示“继续渲染当前嵌套路由”。
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

/**
 * 管理员守卫：
 * 仅 ADMIN 可访问系统管理类页面，普通运营角色在此被拦截。
 * 这里返回的“无权限”提示是兜底文案，可按产品设计替换为独立 403 页面。
 *
 * 执行顺序：
 * 1) 先处理 loading；
 * 2) 再校验是否已登录；
 * 3) 最后校验角色。
 */
export function RequireAdmin() {
  const { user, loading, isAdmin } = useAuth()
  // 与 RequireAuth 保持同样的加载态处理逻辑
  if (loading) return <Spin fullscreen />
  // 未登录优先走登录页；已登录但非管理员才给 403 文案
  // 这样可以明确区分“未登录”和“无权限”两类状态。
  if (!user) return <Navigate to="/login" replace />
  if (!isAdmin) return <div style={{ padding: 24 }}>无权限（需要管理员角色）</div>
  return <Outlet />
}

export function RequireSuperAdmin() {
  const { user, loading, isSuperAdmin } = useAuth()
  if (loading) return <Spin fullscreen />
  if (!user) return <Navigate to="/login" replace />
  if (!isSuperAdmin) return <div style={{ padding: 24 }}>无权限（需要超级管理员角色）</div>
  return <Outlet />
}
