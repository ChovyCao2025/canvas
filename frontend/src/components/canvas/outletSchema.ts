/**
 * 组件职责：解析节点动态出口 schema，并转换为画布可识别的出口定义。
 *
 * 维护说明：用于支持后端配置驱动的动态分支，解析失败时要安全降级。
 */
import { getBranchHandles, type BranchHandle } from './branchHandles'

/** 后端 bizConfig 中允许作为“后继节点引用”的字段白名单。 */
export const OUTLET_TARGET_FIELDS = [
  'nextNodeId',
  'successNodeId',
  'failNodeId',
  'hitNextNodeId',
  'missNextNodeId',
  'timeoutNodeId',
] as const

/** 允许作为出口目标字段的联合类型。 */
export type OutletTargetField = typeof OUTLET_TARGET_FIELDS[number]

/** 后端 outletSchema 中的单个出口定义。 */
export interface OutletSchemaItem {
  /** 出口 handle ID，必须和 React Flow sourceHandle 保持一致。 */
  id: string

  /** 出口展示名。 */
  label: string

  /** 出口颜色，可省略。 */
  color?: string

  /** 出口命中后要写入 bizConfig 的目标字段。 */
  targetField?: OutletTargetField
}

/** 旧版固定 handle 的默认目标字段，动态 schema 不写 targetField 时也可兜底。 */
const DEFAULT_TARGET_FIELDS: Record<string, OutletTargetField> = {
  default: 'nextNodeId',
  success: 'successNodeId',
  fail: 'failNodeId',
  hit: 'hitNextNodeId',
  miss: 'missNextNodeId',
  timeout: 'timeoutNodeId',
}

/** 类型守卫：确保动态 schema 只能引用白名单字段。 */
function isOutletTargetField(value: unknown): value is OutletTargetField {
  return typeof value === 'string' && (OUTLET_TARGET_FIELDS as readonly string[]).includes(value)
}

/** 安全解析 outletSchema 原始 JSON，失败时返回空数组。 */
export function parseOutletSchemaItems(raw: string | undefined): OutletSchemaItem[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((item): item is OutletSchemaItem => {
        if (item == null || typeof item !== 'object') return false
        const candidate = item as Record<string, unknown>
        return typeof candidate.id === 'string' && typeof candidate.label === 'string'
      })
      .map(item => ({
        id: item.id,
        label: item.label,
        color: typeof item.color === 'string' ? item.color : undefined,
        targetField: isOutletTargetField(item.targetField) ? item.targetField : undefined,
      }))
  } catch {
    return []
  }
}

/** 将动态出口 schema 转换为节点组件可渲染的 BranchHandle 列表。 */
export function parseOutletSchema(raw: string | undefined): BranchHandle[] {
  return parseOutletSchemaItems(raw)
    .filter(item => item.targetField || DEFAULT_TARGET_FIELDS[item.id])
    .map(item => ({
      id: item.id,
      label: item.label,
      color: item.color ?? '#1677ff',
    }))
}

/** 判断原始 schema 是否声明过出口，即便所有出口都因字段非法被过滤。 */
export function hasOutletSchema(raw: string | undefined): boolean {
  return parseOutletSchemaItems(raw).length > 0
}

/** 获取节点最终可渲染出口：优先动态 schema，其次回退到节点类型内置出口。 */
export function getOutletHandles(input: {
  nodeType: string
  bizConfig: Record<string, unknown>
  outletSchema?: string
}): BranchHandle[] {
  const dynamic = parseOutletSchema(input.outletSchema)
  if (dynamic.length > 0) return dynamic
  if (hasOutletSchema(input.outletSchema)) return []
  return getBranchHandles(input.nodeType, input.bizConfig)
}

/** 根据 handleId 解析后端保存后继节点时应写入的 bizConfig 字段。 */
export function getOutletTargetField(
  handleId: string,
  outletSchema?: string,
): OutletTargetField | undefined {
  const dynamic = parseOutletSchemaItems(outletSchema)
  const item = dynamic.find(candidate => candidate.id === handleId)
  if (item?.targetField) return item.targetField
  return DEFAULT_TARGET_FIELDS[handleId]
}
