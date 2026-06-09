import type { Node } from '@xyflow/react'
import type { BackendNode, CanvasNodeData } from '../../types/canvas'
import type { CanvasSettingsLike } from './settingsPresentation'

/** 可用于判断是否需要保存的画布快照。 */
export interface ComparableSaveSnapshot {
  /** 持久化图 JSON，包含真实节点和节点内出口配置。 */
  graphJson: string

  /** 画布名称。 */
  canvasName: string

  /** 画布级设置。 */
  canvasSettings: CanvasSettingsLike

  /** 画布描述。 */
  description?: string
}

/** React Flow 占位节点的最小 data 形态。 */
interface PlaceholderLikeData {
  /** 存在即表示这是编辑器辅助占位节点，不应持久化。 */
  _placeholder?: unknown
}

/** 判断节点是否是编辑器专用占位节点；占位节点绝不能保存到 graphJson。 */
export function isPlaceholderFlowNode(node: Node): boolean {
  return Boolean((node.data as PlaceholderLikeData | undefined)?._placeholder)
}

/** 过滤掉占位节点，并把节点 data 收窄为画布节点数据模型。 */
export function realCanvasNodes(nodes: Node[]): Node<CanvasNodeData>[] {
  return nodes.filter(node => !isPlaceholderFlowNode(node)) as Node<CanvasNodeData>[]
}

/** 将 React Flow 节点转换为后端 graph_json 节点结构。 */
export function buildBackendNodesFromFlowNodes(nodes: Node[]): BackendNode[] {
  return realCanvasNodes(nodes).map(node => {
    const data = node.data
    return {
      id: node.id,
      type: data.nodeType,
      name: data.name,
      category: data.category,
      // 坐标取整，避免拖拽产生的小数造成无意义保存差异。
      x: Math.round(node.position.x),
      y: Math.round(node.position.y),
      config: data.bizConfig,
      outletSchema: data.outletSchema,
    }
  })
}

/** 构造持久化 graphJson；边关系由节点 bizConfig 中的 nextNodeId 等字段表达。 */
export function buildSaveGraphJson(nodes: Node[]): string {
  return JSON.stringify({ nodes: buildBackendNodesFromFlowNodes(nodes) })
}

/** 比较保存快照，决定自动保存后是否还存在更新内容需要继续保存。 */
export function sameSaveSnapshot(a: ComparableSaveSnapshot, b: ComparableSaveSnapshot): boolean {
  return a.graphJson === b.graphJson
    && a.canvasName === b.canvasName
    && JSON.stringify(a.canvasSettings) === JSON.stringify(b.canvasSettings)
    && a.description === b.description
}
