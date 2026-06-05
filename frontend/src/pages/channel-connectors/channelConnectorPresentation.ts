export function connectorModeBadge(mode: string) {
  if (mode === 'REAL') return { text: 'Real', color: 'green' }
  if (mode === 'SANDBOX') return { text: 'Sandbox', color: 'gold' }
  return { text: 'Disabled', color: 'red' }
}

export function connectorHealthBadge(status?: string | null) {
  if (status === 'UP') return { text: 'UP', color: 'green' }
  if (status === 'DISABLED') return { text: 'Disabled', color: 'red' }
  if (status === 'DOWN') return { text: 'Down', color: 'red' }
  return { text: status || 'Unknown', color: 'default' }
}

export function connectorWarning(row: { channel: string; provider: string; mode: string }) {
  return row.mode === 'REAL' ? null : `${row.channel}/${row.provider} is ${row.mode.toLowerCase()}`
}

export function formatDecisionRow(row: {
  originalChannel: string
  finalChannel?: string | null
  decisionReason: string
  createdAt: string
}) {
  return `${row.originalChannel} -> ${row.finalChannel ?? 'SKIP'}: ${row.decisionReason} at ${row.createdAt}`
}

export function formatLimitRow(row: {
  channel: string
  provider: string
  operation: string
  perSecondLimit: number
  dailyLimit?: number | null
}) {
  const daily = row.dailyLimit == null ? 'daily unlimited' : `daily ${row.dailyLimit}`
  return `${row.channel}/${row.provider} ${row.operation}: ${row.perSecondLimit}/s, ${daily}`
}
