/**
 * 组件职责：节点切换时的表单同步计划工具。
 *
 * 维护说明：通过显式返回 action 区分重置、合并和跳过，减少 ConfigPanel 内部副作用分支。
 */
import type { CanvasNodeData } from '../../types/canvas'

/** 表单同步计划，描述节点切换后应写入表单的值和需要清理的旧字段。 */
export interface NodeConfigFormSyncPlan {
  /** 下一轮应写入 Form 的完整值集合。 */
  values: Record<string, unknown>

  /** 旧节点残留、但新节点不再使用的字段 key。 */
  staleKeys: string[]

  /** 是否需要先 resetFields，再 setFieldsValue，防止旧字段残留提交。 */
  shouldResetBeforeApply: boolean
}

/** 根据上一个表单快照和当前节点 data 计算最小副作用的表单同步策略。 */
export function buildNodeConfigFormSyncPlan(
  previousValues: Record<string, unknown>,
  nodeData: CanvasNodeData | null,
): NodeConfigFormSyncPlan {
  // name 作为通用字段并入 bizConfig，保持配置面板表单只有一个数据源。
  const values = nodeData ? { name: nodeData.name, ...nodeData.bizConfig } : {}
  const nextKeys = new Set(Object.keys(values))
  // 找出新节点 schema 不再拥有的字段，避免从 A 节点切到 B 节点时把隐藏字段误保存。
  const staleKeys = Object.keys(previousValues)
    .filter((key) => !nextKeys.has(key))
    .sort()

  return {
    values,
    staleKeys,
    shouldResetBeforeApply: staleKeys.length > 0,
  }
}
