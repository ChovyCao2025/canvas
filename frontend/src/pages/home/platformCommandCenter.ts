export interface PlatformWorkstreamStatus {
  workstreamKey: string
  displayName: string
  priority: string
  status: 'BLOCKED_CHILD_SPEC_REQUIRED' | 'READY_FOR_CHILD_EXECUTION'
  childSpecPath: string | null
  summary: string
}

export function blockedWorkstreamCount(rows: PlatformWorkstreamStatus[]) {
  return rows.filter(row => row.status === 'BLOCKED_CHILD_SPEC_REQUIRED').length
}

export function groupWorkstreamsByStatus(rows: PlatformWorkstreamStatus[]) {
  return rows.reduce<Record<PlatformWorkstreamStatus['status'], PlatformWorkstreamStatus[]>>((groups, row) => {
    groups[row.status] = [...(groups[row.status] ?? []), row]
    return groups
  }, {
    BLOCKED_CHILD_SPEC_REQUIRED: [],
    READY_FOR_CHILD_EXECUTION: [],
  })
}

export function workstreamStatusText(status: PlatformWorkstreamStatus['status']) {
  return status === 'BLOCKED_CHILD_SPEC_REQUIRED'
    ? 'Child spec required'
    : 'Ready for child execution'
}

export function workstreamStatusColor(status: PlatformWorkstreamStatus['status']) {
  return status === 'BLOCKED_CHILD_SPEC_REQUIRED' ? 'orange' : 'green'
}
