export type CanvasTriggerType = 'REALTIME' | 'SCHEDULED'

export interface CanvasSettingsLike {
  triggerType?: CanvasTriggerType
  cronExpression?: string
  validStart?: string
  validEnd?: string
  maxTotalExecutions?: number
  perUserDailyLimit?: number
  perUserTotalLimit?: number
  cooldownSeconds?: number
}

export function getTriggerTypeSummary(triggerType?: string): string {
  if (triggerType === 'SCHEDULED') {
    return '当前为定时'
  }

  if (triggerType == null || triggerType === 'REALTIME') {
    return '当前为实时'
  }

  return '触发方式未知'
}

export function countExecutionLimitFields(settings: CanvasSettingsLike): number {
  let count = 0

  if (settings.validStart || settings.validEnd) {
    count += 1
  }

  if (settings.maxTotalExecutions != null) {
    count += 1
  }

  if (settings.perUserDailyLimit != null) {
    count += 1
  }

  if (settings.perUserTotalLimit != null) {
    count += 1
  }

  if (settings.cooldownSeconds != null) {
    count += 1
  }

  return count
}

export function shouldExpandExecutionLimits(settings: CanvasSettingsLike): boolean {
  return countExecutionLimitFields(settings) > 0
}

export function getExecutionLimitsSummary(settings: CanvasSettingsLike): string {
  const configuredCount = countExecutionLimitFields(settings)
  return configuredCount === 0 ? '未设置限制' : `已配置 ${configuredCount} 项`
}
