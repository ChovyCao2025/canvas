/**
 * 页面职责：画布设置展示工具，生成触发方式和执行限制摘要。
 *
 * 维护说明：纯函数供编辑器页面和测试复用。
 */
export type CanvasTriggerType = 'REALTIME' | 'SCHEDULED'

/**
 * 画布“设置抽屉”视图模型。
 * 所有字段均为可选，便于兼容历史配置和增量字段扩展。
 */
export interface CanvasSettingsLike {
  /** 触发类型。 */
  triggerType?: CanvasTriggerType

  /** 定时触发表达式。 */
  cronExpression?: string

  /** 生效开始时间。 */
  validStart?: string

  /** 生效结束时间。 */
  validEnd?: string

  /** 总触发上限。 */
  maxTotalExecutions?: number

  /** 单用户每日上限。 */
  perUserDailyLimit?: number

  /** 单用户累计上限。 */
  perUserTotalLimit?: number

  /** 单用户冷却秒数。 */
  cooldownSeconds?: number
}

/** 触发类型转可读摘要文案。 */
export function getTriggerTypeSummary(triggerType?: string): string {
  if (triggerType === 'SCHEDULED') {
    return '当前为定时'
  }

  if (triggerType == null || triggerType === 'REALTIME') {
    return '当前为实时'
  }

  return '触发方式未知'
}

/**
 * 统计已配置的“执行限制项”数量。
 * 这里按“配置项维度”计数，不按字段是否有默认值计数。
 */
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

/** 有任一限制项时默认展开“执行限制”面板，帮助用户快速发现已配置项。 */
export function shouldExpandExecutionLimits(settings: CanvasSettingsLike): boolean {
  return countExecutionLimitFields(settings) > 0
}

/** 折叠态下的执行限制摘要。 */
export function getExecutionLimitsSummary(settings: CanvasSettingsLike): string {
  const configuredCount = countExecutionLimitFields(settings)
  return configuredCount === 0 ? '未设置限制' : `已配置 ${configuredCount} 项`
}
