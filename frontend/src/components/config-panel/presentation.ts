/**
 * 组件职责：配置面板头部展示信息计算工具。
 *
 * 维护说明：把节点类型、风险等级、标签模式等原始字段转换成组件可直接展示的文案。
 */
import type { CanvasNodeData } from '../../types/canvas'
import { getOutletHandles, getOutletTargetField } from '../canvas/outletSchema'

/** 可参与摘要展示的 schema 字段子集。 */
export interface ConfigPanelPresentationField {
  /** 字段 key，对应节点 bizConfig 的属性名。 */
  key: string

  /** 字段标题。 */
  label: string

  /** 字段控件类型。 */
  type: string
}

/** 配置面板字段分组 key。 */
export type ConfigPanelFieldGroupKey = 'basic' | 'rules' | 'mapping' | 'preview' | 'advanced'

/** 配置面板字段分组。 */
export interface ConfigPanelFieldGroup {
  /** 分组 key，渲染时用作稳定标识。 */
  key: ConfigPanelFieldGroupKey

  /** 分组标题。 */
  title: string

  /** 分组右侧摘要，例如 2 条。 */
  summary?: string

  /** 归入当前分组的 schema 字段。 */
  fields: ConfigPanelPresentationField[]
}

/** 计算配置面板展示模型所需的上下文。 */
export interface BuildConfigPanelPresentationInput {
  /** 当前节点 data。 */
  nodeData: CanvasNodeData

  /** 当前表单值，用于识别模式等正在编辑但尚未保存的配置。 */
  formValues: Record<string, unknown>

  /** 已格式化的展示值，例如 select 值对应的 label。 */
  displayValues: Record<string, string | undefined>

  /** schema 字段列表。 */
  fields: ConfigPanelPresentationField[]

  /** 通过节点 ID 解析节点名，分支摘要会用它展示目标节点。 */
  getNodeName: (id: string | undefined) => string | null
}

/** 配置面板头部、摘要和分支流向的前端展示模型。 */
export interface ConfigPanelPresentation {
  /** 顶部卡片信息。 */
  header: {
    tone: 'default' | 'tagger'
    typeBadge: string
    title: string
    metaBadges: string[]
    description?: string
    statusLabel: string
    categoryLabel?: string
  }

  /** 通用字段摘要行。 */
  summaryRows: Array<{ label: string; value: string }>

  /** 按编辑意图归类后的字段分组。 */
  fieldGroups: ConfigPanelFieldGroup[]

  /** 分支节点的后继路由摘要。 */
  branchRoutes: Array<{ label: string; value: string; tone: 'success' | 'danger' }>
}

const GROUP_TITLES: Record<ConfigPanelFieldGroupKey, string> = {
  basic: '基础配置',
  rules: '条件规则',
  mapping: '参数映射',
  preview: '预览信息',
  advanced: '高级配置',
}

const GROUP_ORDER: ConfigPanelFieldGroupKey[] = ['basic', 'rules', 'mapping', 'preview', 'advanced']

/** 根据控件类型判断字段在紧凑 inspector 中的分组。 */
function resolveGroupKey(type: string): ConfigPanelFieldGroupKey {
  if (['condition-rule-list', 'branch-list', 'priority-list', 'ab-group-list', 'cron', 'delay-input'].includes(type)) {
    return 'rules'
  }
  if (['context-value-list', 'param-define-list', 'key-value', 'api-input-params'].includes(type)) {
    return 'mapping'
  }
  if (['event-attr-preview', 'edge-hint'].includes(type)) {
    return 'preview'
  }
  return 'basic'
}

/** 把可见字段归并为右侧配置面板分组。 */
function buildFieldGroups(fields: ConfigPanelPresentationField[]): ConfigPanelFieldGroup[] {
  const grouped = new Map<ConfigPanelFieldGroupKey, ConfigPanelPresentationField[]>()
  for (const field of fields) {
    const key = resolveGroupKey(field.type)
    grouped.set(key, [...(grouped.get(key) ?? []), field])
  }

  return GROUP_ORDER.flatMap((key) => {
    const groupFields = grouped.get(key) ?? []
    if (groupFields.length === 0) return []
    return [{
      key,
      title: GROUP_TITLES[key],
      ...(key === 'basic' ? {} : { summary: `${groupFields.length} 项` }),
      fields: groupFields,
    }]
  })
}

/** 从集合型出口 handle 中解析目标节点 ID。 */
function resolveIndexedRouteTarget(handleId: string, bizConfig: CanvasNodeData['bizConfig']): string | undefined {
  if (handleId.startsWith('branch-')) {
    const index = Number(handleId.replace('branch-', ''))
    return bizConfig.branches?.[index]?.nextNodeId
  }
  if (handleId.startsWith('priority-')) {
    const index = Number(handleId.replace('priority-', ''))
    return bizConfig.priorities?.[index]?.nextNodeId
  }
  if (handleId.startsWith('group-')) {
    const key = handleId.replace('group-', '')
    return bizConfig.groups?.find(group => group.groupKey === key)?.nextNodeId
  }
  if (handleId.startsWith('path-')) {
    const key = handleId.replace('path-', '')
    return bizConfig.paths?.find(path => String(path.pathId ?? path.id ?? '') === key)?.nextNodeId
  }
  if (handleId.startsWith('variant-')) {
    const key = handleId.replace('variant-', '')
    return bizConfig.variants?.find(variant => String(variant.variantId ?? variant.id ?? '') === key)?.nextNodeId
  }
  if (handleId.startsWith('band-')) {
    const key = handleId.replace('band-', '')
    return bizConfig.bands?.find(band => String(band.bandId ?? band.id ?? '') === key)?.nextNodeId
  }
  return undefined
}

/** 根据出口语义选择路由卡片色调。 */
function resolveRouteTone(handleId: string, label: string): 'success' | 'danger' {
  const dangerIds = new Set([
    'fail',
    'else',
    'miss',
    'reject',
    'timeout',
    'suppressed',
    'skipped',
    'unavailable',
    'capped',
    'fallback',
    'max_exceeded',
    'goal_not_met',
  ])
  if (dangerIds.has(handleId)) return 'danger'
  if (/未|不|失败|拒绝|否则/.test(label)) return 'danger'
  return 'success'
}

/** 从节点出口 handle 和 bizConfig 生成路由摘要。 */
function buildBranchRoutes(
  nodeData: CanvasNodeData,
  getNodeName: (id: string | undefined) => string | null,
): Array<{ label: string; value: string; tone: 'success' | 'danger' }> {
  const handles = getOutletHandles({
    nodeType: nodeData.nodeType,
    bizConfig: nodeData.bizConfig,
    outletSchema: nodeData.outletSchema,
  })

  return handles.map((handle) => {
    const field = getOutletTargetField(handle.id, nodeData.outletSchema)
    const targetId = field
      ? nodeData.bizConfig[field] as string | undefined
      : resolveIndexedRouteTarget(handle.id, nodeData.bizConfig)
    return {
      label: handle.label,
      value: getNodeName(targetId) ?? '未连接',
      tone: resolveRouteTone(handle.id, handle.label),
    }
  })
}

/** TAGGER 节点根据圈选模式追加更可读的徽标。 */
function resolveTaggerMetaBadges(mode: unknown, displayMode: string | undefined): string[] {
  if (mode === 'audience') return ['人群圈选', 'Audience Segment']
  if (typeof displayMode === 'string' && displayMode.trim()) return [displayMode.trim()]
  if (typeof mode === 'string' && mode.trim()) return [mode.trim()]
  if (typeof mode === 'number' || typeof mode === 'boolean') return [String(mode)]
  return []
}

/** 从节点 data 和表单上下文生成右侧配置面板的展示模型。 */
export function buildConfigPanelPresentation(input: BuildConfigPanelPresentationInput): ConfigPanelPresentation {
  const { nodeData, formValues, displayValues, fields, getNodeName } = input
  const isTagger = nodeData.nodeType === 'TAGGER'
  const fieldGroups = buildFieldGroups(fields)
  const branchRoutes = buildBranchRoutes(nodeData, getNodeName)
  const outletBadge = branchRoutes.length > 0 ? `${branchRoutes.length} 出口` : undefined

  const summaryRows = isTagger
    ? fields
        .filter(field => ['mode', 'audienceId', 'tagCodeKey'].includes(field.key))
        .map(field => ({
          label: field.label,
          value: displayValues[field.key] ?? String(formValues[field.key] ?? nodeData.bizConfig[field.key] ?? ''),
        }))
        .filter(row => row.value.trim() !== '')
    : []

  return {
    header: {
      tone: isTagger ? 'tagger' : 'default',
      typeBadge: nodeData.nodeType,
      title: nodeData.name,
      metaBadges: [
        ...(isTagger ? resolveTaggerMetaBadges(formValues.mode, displayValues.mode) : []),
        ...(outletBadge ? [outletBadge] : []),
      ],
      description: isTagger ? '标签判断节点，根据圈选人群决定后续分支流向' : undefined,
      statusLabel: '已配置',
      categoryLabel: nodeData.category,
    },
    summaryRows,
    fieldGroups,
    branchRoutes,
  }
}
