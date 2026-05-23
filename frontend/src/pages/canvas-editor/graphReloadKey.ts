import type { CanvasDetail } from '../../types'

export function getCanvasGraphReloadKey(detail: CanvasDetail): string {
  return `${detail.draftVersionId ?? 'none'}:${detail.graphJson}`
}
