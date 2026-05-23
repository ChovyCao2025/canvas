import { createContext, useContext } from 'react'

/**
 * 画布级行为注入点。
 * 节点/边组件通过 Context 获取删除、复制等动作，避免深层 props 透传。
 *
 * 架构边界：
 * - 这里仅定义“动作接口”，不处理具体状态；
 * - 真正的节点/连线增删改在编辑器页实现后注入。
 */
interface CanvasActions {
  /** 删除节点（通常会同步删除关联边并回写图数据）。 */
  deleteNode: (id: string) => void

  /** 复制节点（通常会生成新 ID 并做位置偏移）。 */
  copyNode:   (id: string) => void

  /** 删除连线（仅断开节点关系，不删除节点本体）。 */
  deleteEdge: (id: string) => void
}

export const CanvasActionsContext = createContext<CanvasActions>({
  // 默认 no-op 主要用于防御性兜底，正常应由编辑器页面提供真实实现
  // 这样即使调用方遗漏 Provider，页面也不会直接崩溃。
  // 但功能会失效，因此开发期仍建议尽早暴露 Provider 漏配问题。
  deleteNode: () => {},
  copyNode:   () => {},
  deleteEdge: () => {},
})

/**
 * 供节点和边渲染组件调用的轻量 Hook。
 * 典型调用方：`CanvasNode`、`HoverEdge`。
 */
export const useCanvasActions = () => useContext(CanvasActionsContext)
