/**
 * Service contract for canvas batch operations.
 */
import type { R } from '../types'
import http from './api'

export type CanvasBatchOperation = 'pause' | 'resume' | 'archive' | 'clone'

export interface CanvasBatchFilters {
  status?: number
  name?: string
  triggerType?: string
  limit?: number
}

export interface CanvasBatchRequest {
  canvasIds?: number[]
  filters?: CanvasBatchFilters
  replacements?: Record<string, string>
  reason?: string
}

export interface CanvasBatchItem {
  canvasId: number
  targetCanvasId?: number | null
  status: 'SUCCESS' | 'SKIPPED' | 'FAILED' | string
  message?: string | null
}

export interface CanvasBatchResult {
  operation: string
  totalCount: number
  successCount: number
  skippedCount: number
  failedCount: number
  items: CanvasBatchItem[]
  countsByStatus: Record<string, number>
}

export function createCanvasBatchApi(client = http) {
  return {
    run: (operation: CanvasBatchOperation, payload: CanvasBatchRequest) =>
      client.post<R<CanvasBatchResult>, R<CanvasBatchResult>>(`/canvas/batch/${operation}`, payload),
  }
}

export const canvasBatchApi = createCanvasBatchApi()
