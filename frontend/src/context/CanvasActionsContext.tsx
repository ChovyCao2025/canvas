/**
 * 上下文职责：画布编辑器动作上下文，用于让节点组件触发复制、删除等编辑器级动作。
 *
 * 维护说明：默认实现是空函数，真实处理器由画布编辑器在运行时注入。
 */
import { createContext, useContext } from 'react'

/** 画布节点组件可触发的编辑器动作。 */
export interface CanvasActions {
  deleteNode: (id: string) => void
  copyNode:   (id: string) => void
  deleteEdge: (id: string) => void
  startInsertOnEdge: (id: string) => void
  canInsertOnEdge: boolean
}

/** 画布动作上下文，默认空实现避免组件脱离编辑器时报错。 */
export const CanvasActionsContext = createContext<CanvasActions>({
  deleteNode: () => {},
  copyNode:   () => {},
  deleteEdge: () => {},
  startInsertOnEdge: () => {},
  canInsertOnEdge: false,
})

/** 读取画布动作上下文的便捷 Hook。 */
export const useCanvasActions = () => useContext(CanvasActionsContext)
