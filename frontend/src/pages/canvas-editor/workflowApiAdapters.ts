import type { CanvasVersion } from '../../types'
import type { CanvasDryRunResult, CanvasVersionsPayload } from '../../services/api'
import { classifyApiError, userFacingApiErrorMessage } from '../../services/apiError'

/** Convert version-list API variants into the editor drawer model. */
export function extractCanvasVersions(payload: CanvasVersionsPayload | null | undefined): CanvasVersion[] {
  if (Array.isArray(payload)) return payload
  return payload?.list ?? []
}

/** Pull the execution id out of dry-run responses without leaking response casts into the page shell. */
export function extractDryRunExecutionId(payload: CanvasDryRunResult | null | undefined): string | undefined {
  const executionId = payload?.executionId
  return typeof executionId === 'string' && executionId.trim().length > 0 ? executionId : undefined
}

export interface ParsedJsonPayload {
  ok: true
  payload: Record<string, unknown>
}

export interface InvalidJsonPayload {
  ok: false
  message: string
}

/** Parse the test-run payload as a JSON object accepted by the backend inputParams map. */
export function parseTestRunPayload(input: string): ParsedJsonPayload | InvalidJsonPayload {
  try {
    const parsed: unknown = JSON.parse(input)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return { ok: false, message: 'Payload 必须是 JSON 对象' }
    }
    return { ok: true, payload: parsed as Record<string, unknown> }
  } catch {
    return { ok: false, message: 'Payload 不是合法 JSON' }
  }
}

/** Keep editor-specific fallback text while still honoring standardized API error messages. */
export function editorApiErrorMessage(error: unknown, fallback: string): string {
  const classified = classifyApiError(error)
  const friendly = userFacingApiErrorMessage(error)
  return classified.kind === 'unknown' && (!friendly.trim() || friendly === '请求失败，请稍后重试')
    ? fallback
    : friendly
}

export function isApiConflict(error: unknown): boolean {
  return classifyApiError(error).kind === 'conflict'
}
