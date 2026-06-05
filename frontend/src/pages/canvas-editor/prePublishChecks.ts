export interface PrePublishCheckItem {
  code: string
  severity: 'ERROR' | 'WARNING'
  message: string
}

export interface PrePublishCheckResult {
  blocking: boolean
  items: PrePublishCheckItem[]
}

export function canPublishFromChecks(result: PrePublishCheckResult) {
  return !result.blocking && result.items.every(item => item.severity !== 'ERROR')
}

export function summarizePrePublishChecks(result: PrePublishCheckResult) {
  return {
    errors: result.items.filter(item => item.severity === 'ERROR').length,
    warnings: result.items.filter(item => item.severity === 'WARNING').length,
  }
}

export function blockingPrePublishMessages(result: PrePublishCheckResult) {
  return result.items
    .filter(item => item.severity === 'ERROR')
    .map(item => `${item.code}: ${item.message}`)
}
