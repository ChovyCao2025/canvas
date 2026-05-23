/**
 * 画布分支出口定义：
 * 负责把节点类型 + bizConfig 映射为可渲染的 source handles。
 */
export type BranchHandle = {
  // 必须与 deriveEdges / patchBizConfig 的 sourceHandle 语义一致
  // 否则“连线渲染”和“配置回写”会出现映射错位
  /** handle 唯一标识（会写入 edge.sourceHandle）。 */
  id: string

  /** UI 展示标签。 */
  label: string

  /** handle 主题色。 */
  color: string
}

const GROUP_COLORS = ['#1677ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']

/**
 * 计算节点出口 handles。
 * 返回结果与后端 bizConfig 字段一一对应，用于连线渲染与回写。
 */
export function getBranchHandles(
  nodeType: string,
  bizConfig: Record<string, unknown>,
): BranchHandle[] {
  // 这里返回的是“可渲染出口集合”，并非所有潜在分支。
  // 部分节点（如 TAGGER / AB_SPLIT / PRIORITY）需要结合 bizConfig 动态生成。
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
      // TAGGER 仅在 audience 模式下暴露命中/未命中两路出口
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
      // 仅用于 UI 标签展示，最后一组吃掉余数，保证总和=100%
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
        // default 表示所有优先规则未命中时的兜底路由
        handles.push({ id: 'default', label: '其余', color: '#8c8c8c' })
      }
      return handles
    }

    default:
      return []
  }
}
