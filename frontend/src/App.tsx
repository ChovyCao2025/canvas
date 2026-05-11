import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { RequireAuth, RequireAdmin } from './auth/guards'
import LoginPage from './pages/login'
import CanvasListPage from './pages/canvas-list'
import CanvasEditorPage from './pages/canvas-editor'
import CanvasStatsPage from './pages/canvas-stats'
import AdminUsersPage from './pages/admin'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* 需要登录 */}
          <Route element={<RequireAuth />}>
            <Route path="/" element={<Navigate to="/canvas" replace />} />
            <Route path="/canvas" element={<CanvasListPage />} />
            <Route path="/canvas/:id/edit" element={<CanvasEditorPage />} />
            <Route path="/canvas/:id/stats" element={<CanvasStatsPage />} />
          </Route>

          {/* 需要 ADMIN */}
          <Route element={<RequireAdmin />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
