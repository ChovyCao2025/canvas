import type { AudienceStat } from '../../services/audienceApi'
import type { AsyncTask, AsyncTaskStatus } from '../../services/taskApi'

type AudienceTaskLike = Pick<AsyncTask, 'status'> & Partial<Pick<AsyncTask, 'taskId' | 'bizId'>>

export function isTerminalTaskStatus(status: AsyncTaskStatus) {
  return status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELED'
}

export function hasRunningAudienceTasks(tasks: AudienceTaskLike[]) {
  return tasks.some(task => !isTerminalTaskStatus(task.status))
}

export function getNextAudiencePollDelay(failureCount: number, pageHidden: boolean) {
  if (pageHidden) return 15000
  if (failureCount >= 4) return 10000
  if (failureCount >= 2) return 5000
  return 3000
}

export function getAudienceDisplayStatus(
  stat?: Pick<AudienceStat, 'status'>,
  task?: Pick<AsyncTask, 'status' | 'taskId' | 'bizId'>,
) {
  if (task && !isTerminalTaskStatus(task.status)) {
    return task.status
  }
  return stat?.status ?? 'PENDING'
}
