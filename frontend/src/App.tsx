import { Suspense, lazy } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import { RequireAuth, RequireAdmin, RequireSuperAdmin } from './auth/guards'

const AppLayout = lazy(() => import('./components/layout/AppLayout'))
const LoginPage = lazy(() => import('./pages/login'))
const HomePage = lazy(() => import('./pages/home'))
const CanvasListPage = lazy(() => import('./pages/canvas-list'))
const CanvasEditorPage = lazy(() => import('./pages/canvas-editor'))
const CanvasStatsPage = lazy(() => import('./pages/canvas-stats'))
const AdminUsersPage = lazy(() => import('./pages/admin'))
const TenantAdminPage = lazy(() => import('./pages/tenant-admin'))
const ApiConfigPage = lazy(() => import('./pages/api-config'))
const DataSourceConfigPage = lazy(() => import('./pages/data-source-config'))
const AbExperimentPage = lazy(() => import('./pages/ab-experiment'))
const TagConfigPage = lazy(() => import('./pages/tag-config'))
const IdentityTypesPage = lazy(() => import('./pages/identity-types'))
const TagImportPage = lazy(() => import('./pages/tag-import'))
const MqConfigPage = lazy(() => import('./pages/mq-config'))
const EventConfigPage = lazy(() => import('./pages/event-config'))
const AudienceListPage = lazy(() => import('./pages/audience-list'))
const AudienceEditPage = lazy(() => import('./pages/audience-edit'))
const ApiDocsPage = lazy(() => import('./pages/api-docs'))
const SystemOptionsPage = lazy(() => import('./pages/system-options'))
const CdpUsersPage = lazy(() => import('./pages/cdp-users'))
const CdpUserDetailPage = lazy(() => import('./pages/cdp-user-detail'))
const CanvasUsersPage = lazy(() => import('./pages/canvas-users'))

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

              <Route element={<RequireAuth />}>
                <Route element={<AppLayout />}>
                  <Route path="/" element={<Navigate to="/home" replace />} />
                  <Route path="/home" element={<HomePage />} />
                  <Route path="/canvas" element={<CanvasListPage />} />
                  <Route path="/cdp/users" element={<CdpUsersPage />} />
                  <Route path="/cdp/users/:userId" element={<CdpUserDetailPage />} />
                </Route>

                <Route path="/canvas/:id/edit" element={<CanvasEditorPage />} />
                <Route path="/canvas/:id/stats" element={<CanvasStatsPage />} />
                <Route path="/canvas/:id/users" element={<CanvasUsersPage />} />
              </Route>

              <Route element={<RequireAdmin />}>
                <Route element={<AppLayout />}>
                  <Route path="/admin/users" element={<AdminUsersPage />} />
                  <Route path="/api-config" element={<ApiConfigPage />} />
                  <Route path="/data-source-config" element={<DataSourceConfigPage />} />
                  <Route path="/ab-experiments" element={<AbExperimentPage />} />
                  <Route path="/tag-config" element={<TagConfigPage />} />
                  <Route path="/identity-types" element={<IdentityTypesPage />} />
                  <Route path="/tag-import" element={<TagImportPage />} />
                  <Route path="/audiences" element={<AudienceListPage />} />
                  <Route path="/audiences/new" element={<AudienceEditPage />} />
                  <Route path="/audiences/:id/edit" element={<AudienceEditPage />} />
                  <Route path="/mq-config" element={<MqConfigPage />} />
                  <Route path="/event-config" element={<EventConfigPage />} />
                  <Route path="/api-docs" element={<ApiDocsPage />} />
                  <Route path="/system-options" element={<SystemOptionsPage />} />
                </Route>
              </Route>

              <Route element={<RequireSuperAdmin />}>
                <Route element={<AppLayout />}>
                  <Route path="/admin/tenants" element={<TenantAdminPage />} />
                </Route>
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
