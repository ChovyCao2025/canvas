import { createContext, useContext } from 'react'

export interface CanvasActions {
  deleteNode: (id: string) => void
  copyNode:   (id: string) => void
  deleteEdge: (id: string) => void
  startInsertOnEdge: (id: string) => void
}

export const CanvasActionsContext = createContext<CanvasActions>({
  deleteNode: () => {},
  copyNode:   () => {},
  deleteEdge: () => {},
  startInsertOnEdge: () => {},
})

export const useCanvasActions = () => useContext(CanvasActionsContext)
