export type AssetType = 'IMAGE' | 'FILE' | 'VIDEO' | 'AUDIO' | string
export type ContentStatus = 'DRAFT' | 'READY' | 'PUBLISHED' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'ARCHIVED' | string

export interface MarketingAsset {
  assetKey: string
  name: string
  assetType: AssetType
  mimeType: string
  storageUrl?: string
  folderId?: number | null
  status: ContentStatus
  tags: string[]
  durationMs?: number | null
  transcodeStatus?: string | null
  posterUrl?: string | null
}

export interface MarketingAssetFolder {
  id: number
  folderKey: string
  name: string
  parentId?: number | null
}

export interface MarketingAssetFolderDraft {
  folderKey: string
  name: string
  parentId?: number | null
}

export interface MarketingAssetDraft {
  assetKey: string
  name: string
  assetType: string
  mimeType: string
  storageUrl: string
  folderId?: number | null
  sizeBytes?: number | null
  checksumSha256?: string
  thumbnailUrl?: string
  posterUrl?: string
  width?: number | null
  height?: number | null
  durationMs?: number | null
  transcodeStatus?: string
  tags?: string[]
  metadata?: Record<string, unknown>
  status?: string
  reviewNotes?: string
  createdBy?: string
}

export interface MarketingAssetUploadIntentDraft {
  assetKey: string
  assetType: string
  provider: string
  mimeType: string
  fileName?: string
  sizeBytes?: number | null
  createdBy?: string
}

export interface MarketingAssetUploadIntent {
  intentKey: string
  assetKey: string
  assetType: string
  provider: string
  uploadToken: string
  uploadUrl: string
  uploadParams: Record<string, unknown>
  status: string
  providerAssetId?: string | null
  expiresAt?: string | null
}

export interface MarketingAssetUploadCleanupPayload {
  limit?: number
  actor?: string
}

export interface MarketingAssetUploadCleanupResult {
  scanned: number
  expired: number
  cutoff?: string | null
}

export function assetTypeFromMime(mimeType?: string): AssetType {
  const normalized = (mimeType ?? '').toLowerCase()
  if (normalized.startsWith('image/')) return 'IMAGE'
  if (normalized.startsWith('video/')) return 'VIDEO'
  if (normalized.startsWith('audio/')) return 'AUDIO'
  return 'FILE'
}

export function assetKeyFromFileName(fileName: string): string {
  const baseName = fileName
    .replace(/\.[^.]+$/, '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
  return baseName || 'uploaded_asset'
}

export function uploadIntentRequiredHeaders(intent: MarketingAssetUploadIntent): Record<string, string> {
  const rawHeaders = intent.uploadParams?.requiredHeaders
  if (!rawHeaders || typeof rawHeaders !== 'object' || Array.isArray(rawHeaders)) {
    return {}
  }
  return Object.fromEntries(
    Object.entries(rawHeaders)
      .filter((entry): entry is [string, string] => typeof entry[0] === 'string' && typeof entry[1] === 'string'),
  )
}

export function uploadIntentStorageUrl(intent: MarketingAssetUploadIntent): string | undefined {
  const value = intent.uploadParams?.storageUrl
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

export function isPresignedPutIntent(intent: MarketingAssetUploadIntent): boolean {
  return intent.uploadParams?.handoffMode === 'PRESIGNED_PUT'
}

export interface MarketingAssetStatusPayload {
  status: string
  reviewNotes?: string
}

export interface ContentTemplate {
  templateKey: string
  displayName: string
  channel: string
  subject?: string | null
  body: string
  designJson: string
  assetRefsJson: string
  variables: string[]
  status: ContentStatus
}

export interface ContentTemplateDraft {
  templateKey: string
  displayName: string
  channel: string
  subject?: string
  body: string
  designJson: string
  assetRefsJson: string
  status?: string
  reviewNotes?: string
  createdBy?: string
}

export interface TemplatePreviewResult {
  renderedSubject?: string | null
  renderedBody: string
  missingVariables: string[]
}

export interface ContentEntry {
  entryKey: string
  contentType: string
  title: string
  slug?: string | null
  locale?: string | null
  status: ContentStatus
  bodyJson: string
  assetRefsJson: string
}

export interface ContentEntryDraft {
  entryKey: string
  contentType: string
  title: string
  slug?: string
  locale?: string
  summary?: string
  bodyJson: string
  seoJson: string
  assetRefsJson: string
  createdBy?: string
}

export interface ContentEntryStatusPayload {
  actor?: string
}

export type ContentReleaseSourceType = 'TEMPLATE' | 'ENTRY'

export interface ReleaseBlocker {
  scope: string
  key: string
  reason: string
}

export interface ReleaseValidationResult {
  ready: boolean
  blockers: ReleaseBlocker[]
  assetRefs: string[]
}

export interface ContentRelease {
  releaseKey: string
  sourceType: ContentReleaseSourceType | string
  sourceKey: string
  sourceVersion: number
  channel: string
  status: string
  checksumSha256: string
}

export interface ResolvedReleaseAsset {
  assetKey: string
  status: string
  snapshotJson: string
}

export interface ResolvedContentRelease {
  releaseKey: string
  sourceType: ContentReleaseSourceType | string
  sourceKey: string
  sourceVersion: number
  status: string
  renderedSubject?: string | null
  renderedBody?: string | null
  missingVariables: string[]
  snapshotJson: string
  assets: ResolvedReleaseAsset[]
}

export interface ContentAuditEvent {
  eventType: string
  targetType: string
  targetKey: string
  actor: string
  note?: string | null
  createdAt?: string | null
}

export interface ReleaseBlockerGroup {
  scope: string
  blockers: ReleaseBlocker[]
}

export function extractTemplateVariables(subject = '', body = ''): string[] {
  const variables: string[] = []
  for (const source of [subject, body]) {
    for (const match of source.matchAll(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*}}/g)) {
      const variable = match[1]
      if (!variables.includes(variable)) variables.push(variable)
    }
  }
  return variables
}

export function localTemplatePreview(
  subject: string,
  body: string,
  context: Record<string, unknown>,
): TemplatePreviewResult {
  const missing: string[] = []
  const render = (source: string) => source.replace(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*}}/g, (placeholder, variable: string) => {
    const value = context[variable]
    if (value === undefined || value === null || value === '') {
      if (!missing.includes(variable)) missing.push(variable)
      return placeholder
    }
    return String(value)
  })
  return {
    renderedSubject: render(subject),
    renderedBody: render(body),
    missingVariables: missing,
  }
}

export function assetTypeLabel(assetType: string) {
  const labels: Record<string, string> = {
    IMAGE: '图片',
    FILE: '文件',
    VIDEO: '视频',
    AUDIO: '音频',
  }
  return labels[assetType] ?? assetType
}

export function channelLabel(channel: string) {
  const labels: Record<string, string> = {
    EMAIL: '邮件',
    SMS: '短信',
    PUSH: '推送',
    WECHAT: '微信',
    IN_APP: '站内信',
    WEB: '网页',
    VIDEO: '视频',
  }
  return labels[channel] ?? channel
}

export function contentStatusView(status: string): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    DRAFT: { text: '草稿', color: 'default' },
    READY: { text: '可用', color: 'green' },
    PUBLISHED: { text: '已发布', color: 'green' },
    PENDING_APPROVAL: { text: '待审批', color: 'gold' },
    APPROVED: { text: '已通过', color: 'green' },
    REJECTED: { text: '已拒绝', color: 'red' },
    ARCHIVED: { text: '已归档', color: 'default' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}

export function releaseStatusView(status: string): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '生效中', color: 'green' },
    SUPERSEDED: { text: '已替换', color: 'default' },
    ROLLED_BACK: { text: '已回滚', color: 'orange' },
    FAILED: { text: '失败', color: 'red' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}

export function releaseSourceLabel(sourceType: string) {
  const labels: Record<string, string> = {
    TEMPLATE: '模板',
    ENTRY: '内容',
  }
  return labels[sourceType] ?? sourceType
}

export function durationLabel(durationMs?: number | null) {
  if (!durationMs || durationMs < 0) return '-'
  const totalSeconds = Math.floor(durationMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

export function parseAssetRefs(assetRefsJson?: string | null): string[] {
  if (!assetRefsJson) return []
  try {
    const parsed = JSON.parse(assetRefsJson) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed.flatMap(item => {
      if (typeof item === 'string') return [item]
      if (item && typeof item === 'object') {
        const candidate = item as { assetKey?: unknown }
        if (typeof candidate.assetKey === 'string') return [candidate.assetKey]
      }
      return []
    })
  } catch {
    return []
  }
}

export function releaseKeyFor(sourceType: string, sourceKey: string) {
  return `${sourceType.toLowerCase()}-${sourceKey}`.replace(/[^a-z0-9_-]+/g, '-')
}

export function groupReleaseBlockers(blockers: ReleaseBlocker[]): ReleaseBlockerGroup[] {
  const grouped = new Map<string, ReleaseBlocker[]>()
  for (const blocker of blockers) {
    const scope = blocker.scope || 'UNKNOWN'
    grouped.set(scope, [...(grouped.get(scope) ?? []), blocker])
  }
  return Array.from(grouped.entries()).map(([scope, groupedBlockers]) => ({
    scope,
    blockers: groupedBlockers,
  }))
}

export function formatMissingVariables(missing: string[]) {
  return missing.length === 0 ? '变量已全部解析' : `缺少变量：${missing.join(', ')}`
}
