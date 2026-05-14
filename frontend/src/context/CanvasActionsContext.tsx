import { createContext, useContext } from 'react'

interface CanvasActions {
  deleteNode: (id: string) => void
  copyNode:   (id: string) => void
  deleteEdge: (id: string) => void
}

export const CanvasActionsContext = createContext<CanvasActions>({
  deleteNode: () => {},
  copyNode:   () => {},
  deleteEdge: () => {},
})

export const useCanvasActions = () => useContext(CanvasActionsContext)
