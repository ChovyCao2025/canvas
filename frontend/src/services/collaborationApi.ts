import http from './api'
import type { R } from '../types'
import type {
  CanvasCollaborationSummary,
  EditorPreference,
} from '../pages/canvas-editor/collaborationAwareness'

type CollaborationHttpClient = Pick<typeof http, 'get' | 'put'>

export function createCollaborationApi(client: CollaborationHttpClient = http) {
  return {
    summary: (canvasId: number) =>
      client.get<R<CanvasCollaborationSummary>, R<CanvasCollaborationSummary>>(
        `/canvas/${canvasId}/collaboration/summary`,
      ),
    editorPreference: () =>
      client.get<R<EditorPreference>, R<EditorPreference>>('/canvas/preferences/editor'),
    updateEditorPreference: (patch: Partial<EditorPreference['preferenceJson']>) =>
      client.put<R<EditorPreference>, R<EditorPreference>>('/canvas/preferences/editor', patch),
  }
}

export const collaborationApi = createCollaborationApi()
