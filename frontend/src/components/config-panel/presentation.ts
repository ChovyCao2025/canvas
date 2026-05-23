import type { CanvasNodeData } from '../../types/canvas'

/**
 * 配置面板“展示态”构建器：
 * 把节点原始配置 + 表单值转换成 UI 层直接可渲染的数据结构，
 * 避免渲染组件内散落条件分支。
 */
export interface ConfigPanelPresentationField {
  /** 字段 key。 */
  key: string

  /** 字段标签。 */
  label: string

  /** 字段控件类型。 */
  type: string
}

export interface BuildConfigPanelPresentationInput {
  /** 当前节点数据。 */
  nodeData: CanvasNodeData

  /** 表单当前值。 */
  formValues: Record<string, unknown>

  /** 预格式化显示值（可覆盖 formValues 原始值）。 */
  displayValues: Record<string, string | undefined>

  /** 当前可见字段。 */
  fields: ConfigPanelPresentationField[]

  /** 节点 ID -> 节点名解析函数。 */
  getNodeName: (id: string | undefined) => string | null
}

export interface ConfigPanelPresentation {
  header: {
    /** 头卡主题。 */
    tone: 'default' | 'tagger'

    /** 节点类型标签。 */
    typeBadge: string

    /** 主标题。 */
    title: string

    /** 辅助标签。 */
    metaBadges: string[]

    /** 简要说明。 */
    description?: string

    /** 状态文案。 */
    statusLabel: string
  }

  /** 摘要字段行。 */
  summaryRows: Array<{ label: string; value: string }>

  /** 分支去向摘要。 */
  branchRoutes: Array<{ label: string; value: string; tone: 'success' | 'danger' }>
}

const TAGGER_SUMMARY_KEYS = new Set(['mode', 'audienceId'])

/** 统一“值缺失”展示策略，避免多个 UI 区块出现不同文案。 */
function toDisplayValue(value: unknown): string {
  if (typeof value === 'string' && value.trim()) return value.trim()
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return '未设置'
}

function resolveDisplayValue(input: {
  key: string
  formValues: Record<string, unknown>
  displayValues: Record<string, string | undefined>
}): string {
  const displayValue = input.displayValues[input.key]
  if (typeof displayValue === 'string' && displayValue.trim()) return displayValue.trim()
  return toDisplayValue(input.formValues[input.key])
}

/**
 * TAGGER 节点头部辅助标签。
 * 优先展示可读语义（audience），其余模式回退到原始值。
 */
function resolveTaggerMetaBadges(mode: unknown): string[] {
  if (mode === 'audience') return ['人群圈选', 'Audience Segment']
  if (typeof mode === 'string' && mode.trim()) return [mode.trim()]
  if (typeof mode === 'number' || typeof mode === 'boolean') return [String(mode)]
  return []
}

export function buildConfigPanelPresentation(input: BuildConfigPanelPresentationInput): ConfigPanelPresentation {
  const { nodeData, formValues, displayValues, fields, getNodeName } = input
  const isTagger = nodeData.nodeType === 'TAGGER'

  const summaryRows = isTagger
    ? fields
        .filter((field) => TAGGER_SUMMARY_KEYS.has(field.key))
        .map((field) => ({
          label: field.label,
          value: resolveDisplayValue({ key: field.key, formValues, displayValues }),
        }))
    : []

  const branchRoutes = isTagger
    ? [
        {
          label: '命中分支',
          value: getNodeName(nodeData.bizConfig.hitNextNodeId as string | undefined) ?? '未连接',
          tone: 'success' as const,
        },
        {
          label: '未命中分支',
          value: getNodeName(nodeData.bizConfig.missNextNodeId as string | undefined) ?? '未连接',
          tone: 'danger' as const,
        },
      ]
    : []

  return {
    header: {
      tone: isTagger ? 'tagger' : 'default',
      typeBadge: isTagger ? 'Tagger' : nodeData.nodeType,
      title: nodeData.name,
      metaBadges: isTagger ? resolveTaggerMetaBadges(formValues.mode) : [],
      description: isTagger ? '标签判断节点，根据圈选人群决定后续分支流向' : nodeData.category,
      statusLabel: '已配置',
    },
    summaryRows,
    branchRoutes,
  }
}
