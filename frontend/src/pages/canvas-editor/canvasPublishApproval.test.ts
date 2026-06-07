import { describe, expect, it } from 'vitest'
import { shouldSubmitPublishReview } from './canvasPublishApproval'
import type { CanvasPublishApprovalStatusView } from '../../services/api'

describe('canvasPublishApproval', () => {
  it('submits review when approval is required and no current approval exists', () => {
    expect(shouldSubmitPublishReview(status({ approvalRequired: true, latestApproved: null }))).toBe(true)
  })

  it('publishes directly when approval is not required or already approved', () => {
    expect(shouldSubmitPublishReview(status({ approvalRequired: false, latestApproved: null }))).toBe(false)
    expect(shouldSubmitPublishReview(status({
      approvalRequired: true,
      latestApproved: { id: 101, status: 'APPROVED' } as any,
    }))).toBe(false)
  })

  function status(overrides: Partial<CanvasPublishApprovalStatusView>): CanvasPublishApprovalStatusView {
    return {
      canvasId: 62,
      draftVersionId: 91,
      approvalRequired: false,
      riskLevel: 'LOW',
      riskReasons: [],
      latestApproved: null,
      ...overrides,
    }
  }
})
