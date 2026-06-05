import type { ChannelRow, PreferenceSummary } from '../../services/marketingPreferencesApi'

export function consentStatusView(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'OPT_IN':
      return { text: '已同意', color: 'green' }
    case 'OPT_OUT':
      return { text: '已退订', color: 'red' }
    default:
      return { text: status || '未知', color: 'default' }
  }
}

export function suppressionStateView(state?: string | null) {
  switch ((state ?? '').toUpperCase()) {
    case 'ACTIVE':
      return { text: '生效中', color: 'red' }
    case 'EXPIRED':
      return { text: '已过期', color: 'default' }
    case 'INACTIVE':
      return { text: '已停用', color: 'default' }
    default:
      return { text: state || '未知', color: 'default' }
  }
}

export function booleanStatusView(value: boolean, activeText: string, inactiveText: string) {
  return value
    ? { text: activeText, color: 'green' }
    : { text: inactiveText, color: 'default' }
}

export function channelReachabilityView(row: Pick<ChannelRow, 'enabled' | 'reachable'>) {
  if (row.reachable) return { text: '可达', color: 'green' }
  if (!row.enabled) return { text: '已关闭', color: 'default' }
  return { text: '缺少地址', color: 'orange' }
}

export function formatPreferenceDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function formatPreferenceSummary(summary?: PreferenceSummary | null) {
  if (!summary) return '暂无偏好数据'
  return `渠道 ${summary.totalChannels} 个，可达 ${summary.reachableChannelCount} 个，同意 ${summary.optInCount} 个，退订 ${summary.optOutCount} 个，生效抑制 ${summary.activeSuppressionCount} 条`
}
