export type BranchHandle = {
  id:    string  // matches deriveEdges / patchBizConfig sourceHandle values
  label: string
  color: string
}

const GROUP_COLORS = ['#1677ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']

export function getBranchHandles(
  nodeType: string,
  bizConfig: Record<string, unknown>,
): BranchHandle[] {
  switch (nodeType) {
    case 'IF_CONDITION':
      return [
        { id: 'success', label: '条件成立', color: '#52c41a' },
        { id: 'else',    label: '否则',     color: '#8c8c8c' },
      ]

    case 'MANUAL_APPROVAL':
      return [
        { id: 'approve', label: '通过', color: '#52c41a' },
        { id: 'reject',  label: '拒绝', color: '#f5222d' },
      ]

    case 'API_CALL':
      return [
        { id: 'success', label: '成功', color: '#52c41a' },
        { id: 'fail',    label: '失败', color: '#f5222d' },
      ]

    case 'SELECTOR': {
      const branches = (bizConfig.branches as { label?: string }[]) ?? []
      const handles: BranchHandle[] = branches.map((b, i) => ({
        id:    `branch-${i}`,
        label: b.label ?? `分支 ${i + 1}`,
        color: '#1677ff',
      }))
      handles.push({ id: 'else', label: '否则', color: '#8c8c8c' })
      return handles
    }

    case 'AB_SPLIT': {
      const groups = (bizConfig.groups as { groupKey: string }[]) ?? []
      if (groups.length === 0) return []
      const bucketSize = Math.floor(100 / groups.length)
      return groups.map((g, i) => ({
        id:    `group-${g.groupKey}`,
        label: `${g.groupKey} ${i === groups.length - 1 ? 100 - bucketSize * i : bucketSize}%`,
        color: GROUP_COLORS[i % GROUP_COLORS.length],
      }))
    }

    case 'PRIORITY': {
      const priorities = (bizConfig.priorities as { order: number }[]) ?? []
      const handles: BranchHandle[] = priorities.map((p, i) => ({
        id:    `priority-${i}`,
        label: `优先 ${p.order ?? i + 1}`,
        color: '#eb2f96',
      }))
      if (priorities.length > 0) {
        handles.push({ id: 'default', label: '其余', color: '#8c8c8c' })
      }
      return handles
    }

    default:
      return []
  }
}
