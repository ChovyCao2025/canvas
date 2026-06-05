/**
 * 页面职责：画布本地草稿工具，负责保存、读取、清理和比较 localStorage 中的未提交编辑。
 *
 * 维护说明：本地草稿只在当前浏览器兜底，最终可信状态仍以后端草稿版本为准。
 */
import type { CanvasDetail } from '../../types'
import type { CanvasSettingsLike } from './settingsPresentation'

/** 本地草稿 key 前缀，后面追加 canvasId 以区分不同画布。 */
const LOCAL_DRAFT_PREFIX = 'canvas_editor_local_draft_v1:'

/** 浏览器本地保存的画布草稿快照。 */
export interface CanvasLocalDraft {
  /** 画布 ID。 */
  canvasId: number

  /** 本地编辑中的画布名称。 */
  name: string

  /** 本地编辑中的 graphJson。 */
  graphJson: string

  /** 本地编辑中的画布设置。 */
  settings: CanvasSettingsLike

  /** 保存本地草稿时对应的服务端编辑版本。 */
  editVersion: number

  /** 保存本地草稿时对应的草稿版本 ID。 */
  draftVersionId?: number

  /** 本地保存时间戳。 */
  savedAt: number
}

/** 生成单个画布的 localStorage key。 */
function draftKey(canvasId: number): string {
  return `${LOCAL_DRAFT_PREFIX}${canvasId}`
}

/** 安全访问 localStorage，兼容隐私模式或测试环境不可用的情况。 */
function storage(): Storage | null {
  try {
    return globalThis.localStorage ?? null
  } catch {
    return null
  }
}

/** 触发类型兜底，未知值按实时触发处理。 */
function normalizeTriggerType(triggerType?: string): CanvasSettingsLike['triggerType'] {
  return triggerType === 'SCHEDULED' ? 'SCHEDULED' : 'REALTIME'
}

/** 从服务端详情中提取可比较的画布设置子集。 */
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
    projectKey: detail.canvas.projectKey,
    projectName: detail.canvas.projectName,
    folderKey: detail.canvas.folderKey,
    folderName: detail.canvas.folderName,
  }
}

/** 设置比较使用 JSON 字符串，字段顺序由 settingsFromDetail/写入方固定。 */
function sameSettings(a: CanvasSettingsLike, b: CanvasSettingsLike): boolean {
  return JSON.stringify(a) === JSON.stringify(b)
}

/** localStorage 读取后的结构校验，避免坏数据进入编辑器恢复流程。 */
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

/** 写入当前浏览器的本地草稿。 */
export function writeCanvasLocalDraft(draft: CanvasLocalDraft): void {
  storage()?.setItem(draftKey(draft.canvasId), JSON.stringify(draft))
}

/** 读取当前浏览器的本地草稿，解析失败或结构不匹配时返回 null。 */
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

/** 清理指定画布的本地草稿。 */
export function clearCanvasLocalDraft(canvasId: number): void {
  storage()?.removeItem(draftKey(canvasId))
}

/** 判断本地草稿相对服务端详情是否仍有差异。 */
export function isLocalDraftDifferentFromServer(
  draft: CanvasLocalDraft,
  detail: CanvasDetail,
): boolean {
  return draft.name !== detail.canvas.name
    || draft.graphJson !== detail.graphJson
    || !sameSettings(draft.settings, settingsFromDetail(detail))
}
