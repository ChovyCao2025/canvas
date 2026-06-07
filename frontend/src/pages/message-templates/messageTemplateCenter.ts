export interface MessageTemplateDraft {
  templateCode: string
  displayName: string
  channel: string
  body: string
}

export interface MessageTemplate extends MessageTemplateDraft {
  tenantId: number
  variables: string[]
  status: string
  createdBy: string
}

export interface TemplatePreviewResult {
  renderedBody: string
  missingVariables: string[]
}

export function variablesFromBody(body: string): string[] {
  const variables: string[] = []
  for (const match of body.matchAll(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*}}/g)) {
    const variable = match[1]
    if (!variables.includes(variable)) variables.push(variable)
  }
  return variables
}

export function formatMissingVariables(missing: string[]) {
  return missing.length === 0 ? 'All variables resolved' : `Missing: ${missing.join(', ')}`
}

export function channelLabel(channel: string) {
  if (channel === 'IN_APP') return 'In-app'
  return channel.charAt(0) + channel.slice(1).toLowerCase()
}

export function templateStatusView(status: string) {
  const views: Record<string, { text: string; color: string }> = {
    DRAFT: { text: '草稿', color: 'default' },
    PENDING_APPROVAL: { text: '待审批', color: 'gold' },
    APPROVED: { text: '已通过', color: 'green' },
    REJECTED: { text: '已拒绝', color: 'red' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}

export function templatePreviewState(result: TemplatePreviewResult) {
  return {
    status: result.missingVariables.length === 0 ? 'READY' : 'MISSING_VARIABLES',
    text: result.renderedBody,
  }
}

export function localTemplatePreview(body: string, context: Record<string, unknown>): TemplatePreviewResult {
  const missing: string[] = []
  const renderedBody = body.replace(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*}}/g, (placeholder, variable: string) => {
    const value = context[variable]
    if (value === undefined || value === null || value === '') {
      if (!missing.includes(variable)) missing.push(variable)
      return placeholder
    }
    return String(value)
  })
  return { renderedBody, missingVariables: missing }
}
