/**
 * 页面职责：人群异步任务展示工具，统一任务状态颜色和可读文案。
 *
 * 维护说明：列表页和测试通过这些纯函数共享展示规则。
 */
import type { AudienceStat } from '../../services/audienceApi'
import type { AsyncTask, AsyncTaskStatus } from '../../services/taskApi'

/** 人群任务展示逻辑需要的最小任务字段集合。 */
type AudienceTaskLike = Pick<AsyncTask, 'status'> & Partial<Pick<AsyncTask, 'taskId' | 'bizId'>>

/** 判断任务是否已经结束；结束态不再触发高频轮询。 */
export function isTerminalTaskStatus(status: AsyncTaskStatus) {
  return status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELED'
}

/** 当前页是否存在仍在运行的人群任务。 */
export function hasRunningAudienceTasks(tasks: AudienceTaskLike[]) {
  return tasks.some(task => !isTerminalTaskStatus(task.status))
}

/** 计算人群任务轮询间隔，连续失败或页面隐藏时自动降频。 */
export function getNextAudiencePollDelay(failureCount: number, pageHidden: boolean) {
  if (pageHidden) return 15000
  if (failureCount >= 4) return 10000
  if (failureCount >= 2) return 5000
  return 3000
}

/** 合并静态人群统计和异步任务状态，优先展示正在运行的任务状态。 */
export function getAudienceDisplayStatus(
  stat?: Pick<AudienceStat, 'status'>,
  task?: Pick<AsyncTask, 'status' | 'taskId' | 'bizId'>,
) {
  if (task && !isTerminalTaskStatus(task.status)) {
    return task.status
  }
  return stat?.status ?? 'PENDING'
}
