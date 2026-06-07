export interface CanvasPresence {
  userId: string
  displayName: string
  state: string
}

export interface CanvasCollaborationSummary {
  canvasId: number
  presence: CanvasPresence[]
  activeLockCount: number
  openCommentCount: number
  unreadNotificationCount: number
}

export interface EditorPreference {
  preferenceKey: 'canvas-editor'
  preferenceJson: Record<string, unknown>
}

const allowedPreferenceKeys = new Set([
  'theme',
  'sidebarCollapsed',
  'notificationLevel',
  'recentNodeTypes',
  'editorLayout',
  'listDefaults',
])

export function collaborationSummaryBadge(summary: CanvasCollaborationSummary) {
  return {
    presenceText: `${summary.presence.length} active`,
    lockText: `${summary.activeLockCount} locks`,
    commentText: `${summary.openCommentCount} comments`,
    notificationText: `${summary.unreadNotificationCount} unread`,
  }
}

export function editorPreferencePatch(input: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(input).filter(([key]) => allowedPreferenceKeys.has(key)),
  )
}

export function permissionStateText(state: 'READY' | 'FORBIDDEN' | 'ERROR') {
  if (state === 'FORBIDDEN') return 'No permission'
  if (state === 'ERROR') return 'Unable to load'
  return 'Ready'
}
