import type { ContactabilityCheck, ContactabilityReport } from '../../services/contactabilityApi'

const checkLabels: Record<string, string> = {
  CONSENT: '授权',
  SUPPRESSION: '抑制名单',
  CHANNEL: '渠道',
  QUIET_HOURS: '静默时段',
  FREQUENCY: '频控',
}

export function contactabilityStatusView(report: ContactabilityReport) {
  return report.allowed
    ? { label: '可触达', color: 'green' }
    : { label: '已拦截', color: 'red' }
}

export function contactabilityCheckView(check: ContactabilityCheck) {
  return {
    label: checkLabels[check.checkKey] ?? check.checkKey,
    color: check.allowed ? 'green' : 'red',
    text: check.allowed ? '通过' : check.reasonMessage || check.reasonCode || '未通过',
  }
}
