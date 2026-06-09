/** 当前正在协作查看或编辑画布的用户状态。 */
export interface CanvasPresence {
  /** 用户唯一 ID。 */
  userId: string

  /** 用户展示名。 */
  displayName: string

  /** 在线或编辑状态描述。 */
  state: string
}

/** 画布协同概览，聚合在线、锁、评论和通知数量。 */
export interface CanvasCollaborationSummary {
  /** 画布 ID。 */
  canvasId: number

  /** 当前在线协作者列表。 */
  presence: CanvasPresence[]

  /** 当前活动锁数量。 */
  activeLockCount: number

  /** 未关闭评论数量。 */
  openCommentCount: number

  /** 未读通知数量。 */
  unreadNotificationCount: number
}

/** 编辑器偏好设置存储结构。 */
export interface EditorPreference {
  /** 偏好所属模块，当前固定为画布编辑器。 */
  preferenceKey: 'canvas-editor'

  /** 偏好 JSON 内容。 */
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

/** 将协同摘要转换成顶部状态徽标文案。 */
export function collaborationSummaryBadge(summary: CanvasCollaborationSummary) {
  return {
    presenceText: `${summary.presence.length} active`,
    lockText: `${summary.activeLockCount} locks`,
    commentText: `${summary.openCommentCount} comments`,
    notificationText: `${summary.unreadNotificationCount} unread`,
  }
}

/** 过滤用户偏好 patch，只允许写入编辑器认可的偏好键。 */
export function editorPreferencePatch(input: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(input).filter(([key]) => allowedPreferenceKeys.has(key)),
  )
}

/** 把权限加载状态转换为展示文案。 */
export function permissionStateText(state: 'READY' | 'FORBIDDEN' | 'ERROR') {
  if (state === 'FORBIDDEN') return 'No permission'
  if (state === 'ERROR') return 'Unable to load'
  return 'Ready'
}
