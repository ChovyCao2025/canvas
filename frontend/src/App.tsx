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

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* 需要登录——带侧边栏布局 */}
          <Route element={<RequireAuth />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/home" replace />} />
              <Route path="/home"   element={<HomePage />} />
              <Route path="/canvas" element={<CanvasListPage />} />
            </Route>

            {/* 编辑器 / 统计保持全屏，无侧边栏 */}
            <Route path="/canvas/:id/edit"  element={<CanvasEditorPage />} />
            <Route path="/canvas/:id/stats" element={<CanvasStatsPage />} />
          </Route>

          {/* 需要 ADMIN——带侧边栏布局 */}
          <Route element={<RequireAdmin />}>
            <Route element={<AppLayout />}>
              <Route path="/admin/users"    element={<AdminUsersPage />} />
              <Route path="/api-config"     element={<ApiConfigPage />} />
              <Route path="/ab-experiments" element={<AbExperimentPage />} />
              <Route path="/tag-config"     element={<TagConfigPage />} />
              <Route path="/audiences"      element={<AudienceListPage />} />
              <Route path="/audiences/new"  element={<AudienceEditPage />} />
              <Route path="/audiences/:id/edit" element={<AudienceEditPage />} />
              <Route path="/mq-config"      element={<MqConfigPage />} />
              <Route path="/event-config"   element={<EventConfigPage />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
