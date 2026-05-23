import type { CanvasNodeData } from '../../types/canvas'

export interface ConfigPanelPresentationField {
  key: string
  label: string
  type: string
}

export interface BuildConfigPanelPresentationInput {
  nodeData: CanvasNodeData
  formValues: Record<string, unknown>
  displayValues: Record<string, string | undefined>
  fields: ConfigPanelPresentationField[]
  getNodeName: (id: string | undefined) => string | null
}

export interface ConfigPanelPresentation {
  header: {
    tone: 'default' | 'tagger'
    typeBadge: string
    title: string
    metaBadges: string[]
    description?: string
    statusLabel: string
  }
  summaryRows: Array<{ label: string; value: string }>
  branchRoutes: Array<{ label: string; value: string; tone: 'success' | 'danger' }>
}

const TAGGER_SUMMARY_KEYS = new Set(['mode', 'audienceId'])

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

function resolveTaggerMetaBadges(mode: unknown, displayMode: string | undefined): string[] {
  if (mode === 'audience') return ['人群圈选', 'Audience Segment']
  if (typeof displayMode === 'string' && displayMode.trim()) return [displayMode.trim()]
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
      metaBadges: isTagger ? resolveTaggerMetaBadges(formValues.mode, displayValues.mode) : [],
      description: isTagger ? '标签判断节点，根据圈选人群决定后续分支流向' : nodeData.category,
      statusLabel: '已配置',
    },
    summaryRows,
    branchRoutes,
  }
}
