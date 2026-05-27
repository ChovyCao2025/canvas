/**
 * 组件职责：配置面板头部展示信息计算工具。
 *
 * 维护说明：把节点类型、风险等级、标签模式等原始字段转换成组件可直接展示的文案。
 */
import type { CanvasNodeData } from '../../types/canvas'

/** 可参与摘要展示的 schema 字段子集。 */
export interface ConfigPanelPresentationField {
  /** 字段 key，对应节点 bizConfig 的属性名。 */
  key: string

  /** 字段标题。 */
  label: string

  /** 字段控件类型。 */
  type: string
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

  /** 分支节点的后继路由摘要。 */
  branchRoutes: Array<{ label: string; value: string; tone: 'success' | 'danger' }>
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
  const { nodeData, formValues, displayValues, getNodeName } = input
  const isTagger = nodeData.nodeType === 'TAGGER'

  const summaryRows: Array<{ label: string; value: string }> = []

  // TAGGER 节点有固定“命中/未命中”双分支，放在头部摘要里能降低连线排查成本。
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
      description: isTagger ? '标签判断节点，根据圈选人群决定后续分支流向' : undefined,
      statusLabel: '已配置',
      categoryLabel: nodeData.category,
    },
    summaryRows,
    branchRoutes,
  }
}
