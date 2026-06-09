/** 单条发布前检查结果。 */
export interface PrePublishCheckItem {
  /** 检查项编码，便于定位具体规则。 */
  code: string

  /** 严重程度；ERROR 会阻断发布。 */
  severity: 'ERROR' | 'WARNING'

  /** 面向用户展示的检查消息。 */
  message: string
}

/** 发布前检查聚合结果。 */
export interface PrePublishCheckResult {
  /** 后端判定是否存在阻断发布的问题。 */
  blocking: boolean

  /** 全量检查项列表。 */
  items: PrePublishCheckItem[]
}

/** 判断发布前检查是否允许继续发布。 */
export function canPublishFromChecks(result: PrePublishCheckResult) {
  // 双重判断：兼容后端 blocking 标记和逐项 ERROR 严重级别。
  return !result.blocking && result.items.every(item => item.severity !== 'ERROR')
}

/** 统计发布前检查错误和警告数量，供摘要 UI 使用。 */
export function summarizePrePublishChecks(result: PrePublishCheckResult) {
  return {
    errors: result.items.filter(item => item.severity === 'ERROR').length,
    warnings: result.items.filter(item => item.severity === 'WARNING').length,
  }
}

/** 提取阻断发布的错误消息。 */
export function blockingPrePublishMessages(result: PrePublishCheckResult) {
  return result.items
    .filter(item => item.severity === 'ERROR')
    .map(item => `${item.code}: ${item.message}`)
}
