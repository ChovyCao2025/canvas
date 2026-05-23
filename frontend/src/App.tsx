import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { RequireAuth, RequireAdmin } from './auth/guards'
import AppLayout from './components/layout/AppLayout'
import LoginPage from './pages/login'
import HomePage from './pages/home'
import CanvasListPage from './pages/canvas-list'
import CanvasEditorPage from './pages/canvas-editor'
import CanvasStatsPage from './pages/canvas-stats'
import AdminUsersPage from './pages/admin'
import ApiConfigPage from './pages/api-config'
import AbExperimentPage from './pages/ab-experiment'
import TagConfigPage from './pages/tag-config'
import MqConfigPage from './pages/mq-config'
import EventConfigPage from './pages/event-config'
import AudienceListPage from './pages/audience-list'
import AudienceEditPage from './pages/audience-edit'
import ApiDocsPage from './pages/api-docs'
import SystemOptionsPage from './pages/system-options'

/**
 * 全局路由入口。
 *
 * 路由分层约定：
 * 1) `/login` 为匿名访问。
 * 2) 需要登录的页面走 `RequireAuth`。
 * 3) 需要管理员权限的页面走 `RequireAdmin`。
 * 4) 编辑器与看板页使用全屏布局，不套用侧边栏。
 * 5) 权限守卫写在 Route 层，页面组件不再重复做登录判断。
 *
 * 阅读顺序建议（前端不熟悉时可按此看）：
 * - 先看 `/login`（匿名）；
 * - 再看 `RequireAuth`（登录后通用页）；
 * - 最后看 `RequireAdmin`（后台配置页）。
 */
export default function App() {
  // 入口组件本身不持有业务状态，只负责“路由装配”。
  // 业务状态放在页面/上下文内部，避免 App 变成巨型容器。
  // 后续新增页面时，优先考虑放在哪个权限域（匿名/登录/管理员）。
  return (
    // 第 1 层：认证状态容器（token/user/role）
    <AuthProvider>
      {/* 第 2 层：浏览器路由容器（URL <-> React 视图） */}
      <BrowserRouter>
        {/* 第 3 层：按权限分发到匿名、登录、管理员三类路由树 */}
        <Routes>
          {/* 路由表顺序按访问门槛递增：匿名 -> 登录态 -> 管理员 */}
          {/* 匿名可访问页面 */}
          <Route path="/login" element={<LoginPage />} />

          {/* 需要登录——带侧边栏布局 */}
          <Route element={<RequireAuth />}>
            <Route element={<AppLayout />}>
              {/* 根路径默认跳首页，避免空白页 */}
              <Route path="/" element={<Navigate to="/home" replace />} />

              {/* 普通登录用户可访问的工作台页面 */}
              {/* 这些页面默认共享 AppLayout（侧边栏 + 内容区）。 */}
              <Route path="/home"   element={<HomePage />} />
              <Route path="/canvas" element={<CanvasListPage />} />
            </Route>

            {/* 编辑器 / 统计保持全屏，无侧边栏 */}
            {/* 原因：编辑器画布区域需要更大可视空间 */}
            <Route path="/canvas/:id/edit"  element={<CanvasEditorPage />} />
            <Route path="/canvas/:id/stats" element={<CanvasStatsPage />} />
          </Route>

          {/* 需要 ADMIN——带侧边栏布局 */}
          <Route element={<RequireAdmin />}>
            <Route element={<AppLayout />}>
              {/* 系统配置页统一归于管理员权限域 */}
              <Route path="/admin/users"    element={<AdminUsersPage />} />

              {/* 配置中心：外部能力与元数据管理 */}
              {/* 管理页路由集中在这里，便于一次性审视权限边界。 */}
              <Route path="/api-config"     element={<ApiConfigPage />} />
              <Route path="/ab-experiments" element={<AbExperimentPage />} />
              <Route path="/tag-config"     element={<TagConfigPage />} />
              <Route path="/audiences"      element={<AudienceListPage />} />
              <Route path="/audiences/new"  element={<AudienceEditPage />} />
              <Route path="/audiences/:id/edit" element={<AudienceEditPage />} />
              <Route path="/mq-config"      element={<MqConfigPage />} />
              <Route path="/event-config"   element={<EventConfigPage />} />
              <Route path="/api-docs"       element={<ApiDocsPage />} />
              <Route path="/system-options" element={<SystemOptionsPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
