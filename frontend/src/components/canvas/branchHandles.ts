/** Branch handle derivation for governed canvas nodes. */
export type BranchHandle = {
  id: string
  label: string
  color: string
}

const GROUP_COLORS = ['#1677ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']

function stringValue(value: unknown, fallback: string): string {
  return value == null || value === '' ? fallback : String(value)
}

export function getBranchHandles(
  nodeType: string,
  bizConfig: Record<string, unknown>,
): BranchHandle[] {
  switch (nodeType) {
    case 'IF_CONDITION':
      return [
        { id: 'success', label: '条件成立', color: '#52c41a' },
        { id: 'fail', label: '否则', color: '#8c8c8c' },
      ]

    case 'AGGREGATE':
      return [
        { id: 'success', label: '条件满足', color: '#52c41a' },
        { id: 'fail', label: '条件不满足', color: '#f5222d' },
      ]

    case 'THRESHOLD':
      return [
        { id: 'success', label: '达到阈值', color: '#52c41a' },
        { id: 'fail', label: '未达阈值', color: '#f5222d' },
      ]

    case 'TAGGER':
      if (bizConfig.mode === 'audience') {
        return [
          { id: 'hit', label: '命中', color: '#52c41a' },
          { id: 'miss', label: '未命中', color: '#f5222d' },
        ]
      }
      return []

    case 'SPLIT': {
      const branches = (bizConfig.branches as Array<Record<string, unknown>>) ?? []
      return branches.map((branch, i) => {
        const branchId = stringValue(branch.branchId ?? branch.id, `branch_${i + 1}`)
        const weight = branch.weight == null ? '' : ` ${branch.weight}%`
        return {
          id: `branch-${branchId}`,
          label: `${stringValue(branch.label, `分支 ${i + 1}`)}${weight}`,
          color: GROUP_COLORS[i % GROUP_COLORS.length],
        }
      })
    }

    default:
      return []
  }
}
