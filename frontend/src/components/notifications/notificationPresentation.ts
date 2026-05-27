/**
 * 组件职责：通知展示工具，统一状态颜色、分类名称和操作按钮文案。
 *
 * 维护说明：保持为纯函数，便于通知列表和测试复用。
 */
import type { UserNotification } from '../../services/notificationApi'

/** 根据通知类型或严重程度返回 antd Tag/Badge 可用的状态色。 */
export function getNotificationStatusColor(type: string, severity?: string) {
  if (severity === 'SUCCESS' || type === 'TASK_SUCCEEDED') return 'success'
  if (severity === 'ERROR' || type === 'TASK_FAILED') return 'error'
  if (severity === 'WARNING') return 'warning'
  if (severity === 'INFO') return 'processing'
  return 'default'
}

/** 未读数为正数时才显示角标。 */
export function shouldShowUnreadBadge(count: number) {
  return count > 0
}

/** 通知分类转中文展示文案。 */
export function getNotificationCategoryLabel(category: string) {
  if (category === 'TASK') return '任务'
  if (category === 'APPROVAL') return '审批'
  if (category === 'ALERT') return '告警'
  if (category === 'CHANGE') return '变更'
  return '通知'
}

/** 解析通知操作按钮文案，优先使用后端 actionLabel。 */
export function getNotificationActionLabel(notification: UserNotification) {
  return notification.actionLabel || (notification.archivedAt ? '已归档' : '查看')
}
