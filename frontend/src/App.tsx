import { Suspense, lazy } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import { RequireAuth, RequireAdmin } from './auth/guards'

const AppLayout = lazy(() => import('./components/layout/AppLayout'))
const LoginPage = lazy(() => import('./pages/login'))
const HomePage = lazy(() => import('./pages/home'))
const CanvasListPage = lazy(() => import('./pages/canvas-list'))
const CanvasEditorPage = lazy(() => import('./pages/canvas-editor'))
const CanvasStatsPage = lazy(() => import('./pages/canvas-stats'))
const AdminUsersPage = lazy(() => import('./pages/admin'))
const ApiConfigPage = lazy(() => import('./pages/api-config'))
const AbExperimentPage = lazy(() => import('./pages/ab-experiment'))
const TagConfigPage = lazy(() => import('./pages/tag-config'))
const MqConfigPage = lazy(() => import('./pages/mq-config'))
const EventConfigPage = lazy(() => import('./pages/event-config'))
const AudienceListPage = lazy(() => import('./pages/audience-list'))
const AudienceEditPage = lazy(() => import('./pages/audience-edit'))

function RouteFallback() {
  return <Spin fullscreen />
}

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BrowserRouter>
          <Suspense fallback={<RouteFallback />}>
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
          </Suspense>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
