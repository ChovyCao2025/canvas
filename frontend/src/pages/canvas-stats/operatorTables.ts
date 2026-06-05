export const MAX_SYNC_EXPORT_ROWS = 5000

export const OPERATION_COLUMN = { key: 'operation', fixed: 'right' as const, width: 160 }

export function canExportSynchronously(totalRows: number) {
  return totalRows <= MAX_SYNC_EXPORT_ROWS
}

export function buildOperatorTableQuery(filters: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}
