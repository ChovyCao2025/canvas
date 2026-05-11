import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { RequireAuth } from './auth/guards'
import LoginPage from './pages/login'
import CanvasListPage from './pages/canvas-list'
import CanvasEditorPage from './pages/canvas-editor'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RequireAuth />}>
            <Route path="/" element={<Navigate to="/canvas" replace />} />
            <Route path="/canvas" element={<CanvasListPage />} />
            <Route path="/canvas/:id/edit" element={<CanvasEditorPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
