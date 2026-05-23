export type BranchHandle = {
  id:    string  // matches deriveEdges / patchBizConfig sourceHandle values
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
        { id: 'else',    label: '否则',     color: '#8c8c8c' },
      ]

    case 'AGGREGATE':
      return [
        { id: 'success', label: '条件满足', color: '#52c41a' },
        { id: 'fail',    label: '条件不满足', color: '#f5222d' },
      ]

    case 'THRESHOLD':
      return [
        { id: 'success', label: '达到阈值', color: '#52c41a' },
        { id: 'fail',    label: '未达阈值', color: '#f5222d' },
      ]

    case 'MANUAL_APPROVAL':
      return [
        { id: 'approve', label: '通过', color: '#52c41a' },
        { id: 'reject',  label: '拒绝', color: '#f5222d' },
      ]

    case 'TAGGER': {
      if (bizConfig.mode === 'audience') {
        return [
          { id: 'hit', label: '命中', color: '#52c41a' },
          { id: 'miss', label: '未命中', color: '#f5222d' },
        ]
      }
      return []
    }

    // API_CALL 暂不实现分支 Handle，保留单路 default 输出
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

    case 'RANDOM_SPLIT': {
      const paths = (bizConfig.paths as Array<Record<string, unknown>>) ?? []
      return paths.map((path, i) => {
        const pathId = stringValue(path.pathId ?? path.id, `path_${i + 1}`)
        const weight = path.weight == null ? '' : ` ${path.weight}%`
        return {
          id: `path-${pathId}`,
          label: `${stringValue(path.label, `路径 ${i + 1}`)}${weight}`,
          color: GROUP_COLORS[i % GROUP_COLORS.length],
        }
      })
    }

    case 'EXPERIMENT': {
      const variants = (bizConfig.variants as Array<Record<string, unknown>>) ?? []
      return variants.map((variant, i) => {
        const variantId = stringValue(variant.variantId ?? variant.id, `variant_${i + 1}`)
        const control = variant.isControl === true ? ' 对照' : ''
        const weight = variant.weight == null ? '' : ` ${variant.weight}%`
        return {
          id: `variant-${variantId}`,
          label: `${stringValue(variant.label, `方案 ${variantId}`)}${control}${weight}`,
          color: GROUP_COLORS[i % GROUP_COLORS.length],
        }
      })
    }

    case 'SCORING': {
      const bands = (bizConfig.bands as Array<Record<string, unknown>>) ?? []
      if (bands.length === 0) return []
      const handles: BranchHandle[] = bands.map((band, i) => {
        const bandId = stringValue(band.bandId ?? band.id, `band_${i + 1}`)
        return {
          id: `band-${bandId}`,
          label: stringValue(band.label, `分数段 ${i + 1}`),
          color: GROUP_COLORS[i % GROUP_COLORS.length],
        }
      })
      handles.push({ id: 'default', label: '未命中', color: '#8c8c8c' })
      return handles
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
