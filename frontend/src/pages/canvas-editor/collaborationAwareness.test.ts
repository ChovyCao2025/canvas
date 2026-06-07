import { describe, expect, it } from 'vitest'
import {
  collaborationSummaryBadge,
  editorPreferencePatch,
  permissionStateText,
  type CanvasCollaborationSummary,
} from './collaborationAwareness'

describe('collaboration awareness helpers', () => {
  it('formats collaboration counters for editor chrome', () => {
    const summary: CanvasCollaborationSummary = {
      canvasId: 42,
      presence: [{ userId: 'u1', displayName: 'Alice', state: 'editing' }],
      activeLockCount: 2,
      openCommentCount: 3,
      unreadNotificationCount: 1,
    }

    expect(collaborationSummaryBadge(summary)).toEqual({
      presenceText: '1 active',
      lockText: '2 locks',
      commentText: '3 comments',
      notificationText: '1 unread',
    })
  })

  it('keeps only supported editor preference keys in update payloads', () => {
    expect(editorPreferencePatch({
      theme: 'dark',
      sidebarCollapsed: true,
      unsafe: 'ignored',
    })).toEqual({
      theme: 'dark',
      sidebarCollapsed: true,
    })
  })

  it('maps permission states to stable labels', () => {
    expect(permissionStateText('FORBIDDEN')).toBe('No permission')
    expect(permissionStateText('ERROR')).toBe('Unable to load')
    expect(permissionStateText('READY')).toBe('Ready')
  })
})
