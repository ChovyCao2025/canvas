import type { CanvasNodeData } from '../../types/canvas'

type SchemaField = { key: string; label: string; type: string }

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

const TAGGER_SUMMARY_LABELS = new Set(['标签模式', '人群'])

function toDisplayValue(value: unknown): string {
  if (typeof value === 'string' && value.trim()) return value.trim()
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return '未设置'
}

function resolveTaggerMetaBadges(formValues: Record<string, unknown>): string[] {
  const mode = String(formValues.mode ?? '')
  if (mode === '人群圈选') return ['人群圈选', 'Audience Segment']
  return mode ? [mode] : []
}

export function buildConfigPanelPresentation(input: {
  nodeData: CanvasNodeData
  formValues: Record<string, unknown>
  fields: SchemaField[]
  getNodeName: (id: string | undefined) => string | null
}): ConfigPanelPresentation {
  const { nodeData, formValues, fields, getNodeName } = input
  const isTagger = nodeData.nodeType === 'TAGGER'

  const summaryRows = isTagger
    ? fields
        .filter((field) => TAGGER_SUMMARY_LABELS.has(field.label))
        .map((field) => ({
          label: field.label,
          value: toDisplayValue(formValues[field.key]),
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
      metaBadges: isTagger ? resolveTaggerMetaBadges(formValues) : [],
      description: isTagger ? '标签判断节点，根据圈选人群决定后续分支流向' : nodeData.category,
      statusLabel: '已配置',
    },
    summaryRows,
    branchRoutes,
  }
}
