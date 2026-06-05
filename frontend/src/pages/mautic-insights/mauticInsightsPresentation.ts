export function audienceMembershipStatusView(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'MATCHED':
      return { text: '命中人群', color: 'green' }
    case 'NOT_MATCHED':
      return { text: '未命中', color: 'red' }
    case 'NOT_READY':
      return { text: '未就绪', color: 'orange' }
    case 'UNKNOWN':
      return { text: '未知', color: 'default' }
    default:
      return { text: status || '未知', color: 'default' }
  }
}

export function journeyStepStatusView(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'SUCCESS':
      return { text: 'SUCCESS', color: 'green' }
    case 'FAILED':
      return { text: 'FAILED', color: 'red' }
    case 'SKIPPED':
      return { text: 'SKIPPED', color: 'orange' }
    case 'RUNNING':
      return { text: 'RUNNING', color: 'blue' }
    default:
      return { text: status || 'UNKNOWN', color: 'default' }
  }
}

export function channelCandidateView(state?: string | null) {
  switch ((state ?? '').toUpperCase()) {
    case 'ELIGIBLE':
      return { text: '可用', color: 'green' }
    case 'SUPPRESSED':
      return { text: '已抑制', color: 'red' }
    case 'UNAVAILABLE':
      return { text: '不可用', color: 'orange' }
    default:
      return { text: state || '未知', color: 'default' }
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

export function healthCheckView(passed: boolean) {
  return passed
    ? { text: '通过', color: 'green' }
    : { text: '未通过', color: 'red' }
}

export function healthScoreView(score: number) {
  if (score >= 80) return { text: '健康', color: 'green' }
  if (score >= 60) return { text: '需关注', color: 'orange' }
  return { text: '阻塞', color: 'red' }
}

export function formatWindowSeconds(seconds?: number | null) {
  if (!seconds || seconds <= 0) return '-'
  if (seconds % 86_400 === 0) return `${seconds / 86_400} 天`
  if (seconds % 3_600 === 0) return `${seconds / 3_600} 小时`
  if (seconds % 60 === 0) return `${seconds / 60} 分钟`
  return `${seconds} 秒`
}

export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}
