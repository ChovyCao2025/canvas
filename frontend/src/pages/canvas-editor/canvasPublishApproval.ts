import type { CanvasPublishApprovalStatusView } from '../../services/api'

export function shouldSubmitPublishReview(status: CanvasPublishApprovalStatusView): boolean {
  return status.approvalRequired && !status.latestApproved
}
