import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import CanvasListPage from './pages/canvas-list'
import CanvasEditorPage from './pages/canvas-editor'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/canvas" replace />} />
        <Route path="/canvas" element={<CanvasListPage />} />
        <Route path="/canvas/:id/edit" element={<CanvasEditorPage />} />
      </Routes>
    </BrowserRouter>
  )
}
