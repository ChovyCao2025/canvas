import type { CanvasDetail } from '../../types'
import type { CanvasSettingsLike } from './settingsPresentation'

const LOCAL_DRAFT_PREFIX = 'canvas_editor_local_draft_v1:'

export interface CanvasLocalDraft {
  canvasId: number
  name: string
  graphJson: string
  settings: CanvasSettingsLike
  editVersion: number
  draftVersionId?: number
  savedAt: number
}

function draftKey(canvasId: number): string {
  return `${LOCAL_DRAFT_PREFIX}${canvasId}`
}

function storage(): Storage | null {
  try {
    return globalThis.localStorage ?? null
  } catch {
    return null
  }
}

function normalizeTriggerType(triggerType?: string): CanvasSettingsLike['triggerType'] {
  return triggerType === 'SCHEDULED' ? 'SCHEDULED' : 'REALTIME'
}

function settingsFromDetail(detail: CanvasDetail): CanvasSettingsLike {
  return {
    triggerType: normalizeTriggerType(detail.canvas.triggerType),
    cronExpression: detail.canvas.cronExpression,
    validStart: detail.canvas.validStart,
    validEnd: detail.canvas.validEnd,
    maxTotalExecutions: detail.canvas.maxTotalExecutions,
    perUserDailyLimit: detail.canvas.perUserDailyLimit,
    perUserTotalLimit: detail.canvas.perUserTotalLimit,
    cooldownSeconds: detail.canvas.cooldownSeconds,
  }
}

function sameSettings(a: CanvasSettingsLike, b: CanvasSettingsLike): boolean {
  return JSON.stringify(a) === JSON.stringify(b)
}

function isCanvasLocalDraft(value: unknown, canvasId: number): value is CanvasLocalDraft {
  if (value == null || typeof value !== 'object') return false
  const draft = value as Record<string, unknown>
  return draft.canvasId === canvasId
    && typeof draft.name === 'string'
    && typeof draft.graphJson === 'string'
    && draft.settings != null
    && typeof draft.settings === 'object'
    && typeof draft.editVersion === 'number'
    && typeof draft.savedAt === 'number'
}

export function writeCanvasLocalDraft(draft: CanvasLocalDraft): void {
  storage()?.setItem(draftKey(draft.canvasId), JSON.stringify(draft))
}

export function readCanvasLocalDraft(canvasId: number): CanvasLocalDraft | null {
  const raw = storage()?.getItem(draftKey(canvasId))
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as unknown
    return isCanvasLocalDraft(parsed, canvasId) ? parsed : null
  } catch {
    return null
  }
}

export function clearCanvasLocalDraft(canvasId: number): void {
  storage()?.removeItem(draftKey(canvasId))
}

export function isLocalDraftDifferentFromServer(
  draft: CanvasLocalDraft,
  detail: CanvasDetail,
): boolean {
  return draft.name !== detail.canvas.name
    || draft.graphJson !== detail.graphJson
    || !sameSettings(draft.settings, settingsFromDetail(detail))
}
