/** 内容生产闭环中用于区分素材处理链路的资产类型。 */
export type AssetType = 'IMAGE' | 'FILE' | 'VIDEO' | 'AUDIO' | string
/** 内容生产闭环里草稿、审核、发布和归档阶段共用的状态值。 */
export type ContentStatus = 'DRAFT' | 'READY' | 'PUBLISHED' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'ARCHIVED' | string

/** DAM 资产在内容中心列表、编辑器和发布校验中的展示模型。 */
export interface MarketingAsset {
  /** 资产在发布快照和内容引用中的稳定业务键。 */
  assetKey: string
  /** 运营侧可读名称，用于资产库列表和编辑面板。 */
  name: string
  /** 决定图片、视频、文件、音频等不同处理方式的资产类型。 */
  assetType: AssetType
  /** 文件 MIME，用于推断资产类型和直传请求头。 */
  mimeType: string
  /** 资产可被内容模板或条目引用的最终存储地址。 */
  storageUrl?: string
  /** 所属资产目录，用于 DAM 侧归档和筛选。 */
  folderId?: number | null
  /** 资产在审核、可用、拒绝、归档闭环中的状态。 */
  status: ContentStatus
  /** 运营标签，用于活动素材检索和聚合展示。 */
  tags: string[]
  /** 视频/音频时长，支撑内容资产预览和视频指标展示。 */
  durationMs?: number | null
  /** 视频转码状态，用于判断视频资产是否可进入发布链路。 */
  transcodeStatus?: string | null
  /** 视频封面或素材预览图地址。 */
  posterUrl?: string | null
}

/** 内容中心资产目录，用于组织 DAM 素材。 */
export interface MarketingAssetFolder {
  /** 后端目录主键。 */
  id: number
  /** 目录业务键，用于幂等创建和外部引用。 */
  folderKey: string
  /** 目录展示名称。 */
  name: string
  /** 父目录 ID，支持未来扩展层级资产库。 */
  parentId?: number | null
}

/** 新建或更新资产目录时提交的草稿。 */
export interface MarketingAssetFolderDraft {
  /** 目录业务键。 */
  folderKey: string
  /** 目录展示名称。 */
  name: string
  /** 父目录 ID；为空表示根目录。 */
  parentId?: number | null
}

/** 资产编辑器保存时发送给内容生产 API 的草稿。 */
export interface MarketingAssetDraft {
  /** 资产业务键，保存后用于模板和内容条目引用。 */
  assetKey: string
  /** 资产展示名称。 */
  name: string
  /** 资产类型，决定发布校验和预览方式。 */
  assetType: string
  /** 文件 MIME 类型。 */
  mimeType: string
  /** 文件存储 URL，发布快照会固化该地址。 */
  storageUrl: string
  /** 资产目录 ID。 */
  folderId?: number | null
  /** 文件大小，用于上传后展示和审计。 */
  sizeBytes?: number | null
  /** 文件校验和，用于内容发布快照一致性追踪。 */
  checksumSha256?: string
  /** 缩略图地址。 */
  thumbnailUrl?: string
  /** 视频封面地址。 */
  posterUrl?: string
  /** 图片或视频宽度。 */
  width?: number | null
  /** 图片或视频高度。 */
  height?: number | null
  /** 视频或音频时长。 */
  durationMs?: number | null
  /** 转码状态。 */
  transcodeStatus?: string
  /** 资产标签。 */
  tags?: string[]
  /** 额外素材元数据。 */
  metadata?: Record<string, unknown>
  /** 资产审核/可用状态。 */
  status?: string
  /** 审核备注。 */
  reviewNotes?: string
  /** 创建人标识。 */
  createdBy?: string
}

/** 创建直传上传意图时的请求草稿。 */
export interface MarketingAssetUploadIntentDraft {
  /** 上传完成后要绑定的资产键。 */
  assetKey: string
  /** 上传素材类型。 */
  assetType: string
  /** 上传供应商，例如 S3。 */
  provider: string
  /** 文件 MIME 类型。 */
  mimeType: string
  /** 原始文件名，用于生成对象路径和审计。 */
  fileName?: string
  /** 原始文件大小。 */
  sizeBytes?: number | null
  /** 创建人标识。 */
  createdBy?: string
}

/** 后端生成的上传意图，驱动浏览器直传和资产 URL 回填。 */
export interface MarketingAssetUploadIntent {
  /** 上传意图业务键。 */
  intentKey: string
  /** 目标资产键。 */
  assetKey: string
  /** 目标资产类型。 */
  assetType: string
  /** 上传供应商。 */
  provider: string
  /** 上传凭证或一次性 token。 */
  uploadToken: string
  /** 浏览器直传地址。 */
  uploadUrl: string
  /** 直传模式、请求头、最终 storageUrl 等供应商参数。 */
  uploadParams: Record<string, unknown>
  /** 上传意图状态。 */
  status: string
  /** 供应商侧资产 ID。 */
  providerAssetId?: string | null
  /** 上传意图过期时间。 */
  expiresAt?: string | null
}

/** 清理过期上传意图的请求参数。 */
export interface MarketingAssetUploadCleanupPayload {
  /** 最多扫描的上传意图数量。 */
  limit?: number
  /** 触发清理的操作人。 */
  actor?: string
}

/** 清理过期上传意图后的统计结果。 */
export interface MarketingAssetUploadCleanupResult {
  /** 本次扫描数量。 */
  scanned: number
  /** 本次过期数量。 */
  expired: number
  /** 后端使用的过期截止时间。 */
  cutoff?: string | null
}

/** 根据 MIME 类型推断内容中心资产类型，支撑文件选择后的表单自动填充。 */
export function assetTypeFromMime(mimeType?: string): AssetType {
  const normalized = (mimeType ?? '').toLowerCase()
  // 按 MIME 前缀映射到内容生产闭环认可的资产大类。
  if (normalized.startsWith('image/')) return 'IMAGE'
  if (normalized.startsWith('video/')) return 'VIDEO'
  if (normalized.startsWith('audio/')) return 'AUDIO'
  return 'FILE'
}

/** 将上传文件名归一化为可用于内容引用的 assetKey。 */
export function assetKeyFromFileName(fileName: string): string {
  // 去掉扩展名并压缩为小写下划线，避免发布引用中出现不稳定字符。
  const baseName = fileName
    .replace(/\.[^.]+$/, '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
  return baseName || 'uploaded_asset'
}

/** 从上传意图中提取浏览器直传必须携带的请求头。 */
export function uploadIntentRequiredHeaders(intent: MarketingAssetUploadIntent): Record<string, string> {
  const rawHeaders = intent.uploadParams?.requiredHeaders
  if (!rawHeaders || typeof rawHeaders !== 'object' || Array.isArray(rawHeaders)) {
    return {}
  }
  // 只保留字符串键值，避免把非 header 元数据传给 fetch。
  return Object.fromEntries(
    Object.entries(rawHeaders)
      .filter((entry): entry is [string, string] => typeof entry[0] === 'string' && typeof entry[1] === 'string'),
  )
}

/** 从上传意图中读取后续保存资产所需的最终存储 URL。 */
export function uploadIntentStorageUrl(intent: MarketingAssetUploadIntent): string | undefined {
  const value = intent.uploadParams?.storageUrl
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

/** 判断上传意图是否是浏览器可直接 PUT 的供应商交接模式。 */
export function isPresignedPutIntent(intent: MarketingAssetUploadIntent): boolean {
  return intent.uploadParams?.handoffMode === 'PRESIGNED_PUT'
}

/** 更新资产状态时提交的审核状态和备注。 */
export interface MarketingAssetStatusPayload {
  /** 目标资产状态。 */
  status: string
  /** 状态变更备注。 */
  reviewNotes?: string
}

/** 营销内容模板，承载渠道文案、变量和素材引用。 */
export interface ContentTemplate {
  /** 模板业务键。 */
  templateKey: string
  /** 模板展示名称。 */
  displayName: string
  /** 投放渠道，例如 EMAIL、PUSH。 */
  channel: string
  /** 可选主题，主要用于邮件模板。 */
  subject?: string | null
  /** 模板正文或 HTML。 */
  body: string
  /** 设计器结构快照。 */
  designJson: string
  /** 模板引用的资产 JSON。 */
  assetRefsJson: string
  /** 从主题和正文中提取的变量列表。 */
  variables: string[]
  /** 模板审核状态。 */
  status: ContentStatus
}

/** 保存模板时提交的编辑器草稿。 */
export interface ContentTemplateDraft {
  /** 模板业务键。 */
  templateKey: string
  /** 模板展示名称。 */
  displayName: string
  /** 模板渠道。 */
  channel: string
  /** 邮件主题或渠道标题。 */
  subject?: string
  /** 模板正文。 */
  body: string
  /** 设计器 JSON 文本。 */
  designJson: string
  /** 资产引用 JSON 文本。 */
  assetRefsJson: string
  /** 模板状态。 */
  status?: string
  /** 审核备注。 */
  reviewNotes?: string
  /** 创建人标识。 */
  createdBy?: string
}

/** 模板预览渲染结果，用于发现缺失变量。 */
export interface TemplatePreviewResult {
  /** 渲染后的主题。 */
  renderedSubject?: string | null
  /** 渲染后的正文。 */
  renderedBody: string
  /** 上下文未提供的变量名。 */
  missingVariables: string[]
}

/** CMS 内容条目，作为发布快照的 ENTRY 来源。 */
export interface ContentEntry {
  /** 内容条目业务键。 */
  entryKey: string
  /** 内容类型，例如 ARTICLE、LANDING_PAGE。 */
  contentType: string
  /** 内容标题。 */
  title: string
  /** URL slug。 */
  slug?: string | null
  /** 内容语言区域。 */
  locale?: string | null
  /** 内容发布状态。 */
  status: ContentStatus
  /** 正文结构 JSON。 */
  bodyJson: string
  /** 条目引用的资产 JSON。 */
  assetRefsJson: string
}

/** 保存 CMS 内容条目时提交的草稿。 */
export interface ContentEntryDraft {
  /** 内容条目业务键。 */
  entryKey: string
  /** 内容类型。 */
  contentType: string
  /** 内容标题。 */
  title: string
  /** URL slug。 */
  slug?: string
  /** 内容语言区域。 */
  locale?: string
  /** 内容摘要。 */
  summary?: string
  /** 正文结构 JSON 文本。 */
  bodyJson: string
  /** SEO 配置 JSON 文本。 */
  seoJson: string
  /** 资产引用 JSON 文本。 */
  assetRefsJson: string
  /** 创建人标识。 */
  createdBy?: string
}

/** 内容条目发布或归档时的操作者载荷。 */
export interface ContentEntryStatusPayload {
  /** 操作人标识。 */
  actor?: string
}

/** 发布快照来源类型，区分模板发布和内容条目发布。 */
export type ContentReleaseSourceType = 'TEMPLATE' | 'ENTRY'

/** 发布门禁发现的阻断项。 */
export interface ReleaseBlocker {
  /** 阻断所属范围，例如 TEMPLATE、ENTRY、ASSET。 */
  scope: string
  /** 阻断对象业务键。 */
  key: string
  /** 阻断原因。 */
  reason: string
}

/** 发布门禁校验结果，决定内容是否可进入生产发布。 */
export interface ReleaseValidationResult {
  /** 是否允许发布。 */
  ready: boolean
  /** 阻断项明细。 */
  blockers: ReleaseBlocker[]
  /** 发布快照解析到的资产引用。 */
  assetRefs: string[]
}

/** 已发布的内容快照元数据。 */
export interface ContentRelease {
  /** 发布快照键。 */
  releaseKey: string
  /** 发布来源类型。 */
  sourceType: ContentReleaseSourceType | string
  /** 发布来源业务键。 */
  sourceKey: string
  /** 来源版本号。 */
  sourceVersion: number
  /** 渠道。 */
  channel: string
  /** 发布状态。 */
  status: string
  /** 快照校验和，用于回滚和审计追踪。 */
  checksumSha256: string
}

/** 发布快照解析出的资产副本。 */
export interface ResolvedReleaseAsset {
  /** 资产业务键。 */
  assetKey: string
  /** 资产当时状态。 */
  status: string
  /** 资产快照 JSON。 */
  snapshotJson: string
}

/** 发布快照解析结果，用于上线前预览最终内容和资产。 */
export interface ResolvedContentRelease {
  /** 发布快照键。 */
  releaseKey: string
  /** 发布来源类型。 */
  sourceType: ContentReleaseSourceType | string
  /** 发布来源业务键。 */
  sourceKey: string
  /** 来源版本号。 */
  sourceVersion: number
  /** 发布状态。 */
  status: string
  /** 渲染后的主题。 */
  renderedSubject?: string | null
  /** 渲染后的正文。 */
  renderedBody?: string | null
  /** 渲染上下文缺失的变量。 */
  missingVariables: string[]
  /** 发布快照 JSON。 */
  snapshotJson: string
  /** 快照中的资产副本。 */
  assets: ResolvedReleaseAsset[]
}

/** 内容发布、回滚和状态变更的审计事件。 */
export interface ContentAuditEvent {
  /** 审计事件类型。 */
  eventType: string
  /** 被操作对象类型。 */
  targetType: string
  /** 被操作对象业务键。 */
  targetKey: string
  /** 操作人。 */
  actor: string
  /** 审计备注。 */
  note?: string | null
  /** 事件发生时间。 */
  createdAt?: string | null
}

/** 按 scope 聚合后的发布阻断项，用于门禁面板展示。 */
export interface ReleaseBlockerGroup {
  /** 阻断所属范围。 */
  scope: string
  /** 该范围下的阻断项。 */
  blockers: ReleaseBlocker[]
}

/** 从模板主题和正文中提取 {{variable}} 变量，驱动预览上下文提示。 */
export function extractTemplateVariables(subject = '', body = ''): string[] {
  const variables: string[] = []
  // 逐个扫描主题和正文，按首次出现顺序去重。
  for (const source of [subject, body]) {
    for (const match of source.matchAll(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*}}/g)) {
      const variable = match[1]
      if (!variables.includes(variable)) variables.push(variable)
    }
  }
  return variables
}

/** 在未保存模板时执行本地预览，标记上下文缺失的变量。 */
export function localTemplatePreview(
  subject: string,
  body: string,
  context: Record<string, unknown>,
): TemplatePreviewResult {
  const missing: string[] = []
  // 替换上下文中存在的变量，缺失变量保留占位符方便运营定位。
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

/** 将资产类型映射为内容中心展示文案。 */
export function assetTypeLabel(assetType: string) {
  const labels: Record<string, string> = {
    IMAGE: '图片',
    FILE: '文件',
    VIDEO: '视频',
    AUDIO: '音频',
  }
  return labels[assetType] ?? assetType
}

/** 将渠道枚举映射为运营可读文案。 */
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

/** 将内容状态映射为标签文案和颜色。 */
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

/** 将发布快照状态映射为标签文案和颜色。 */
export function releaseStatusView(status: string): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '生效中', color: 'green' },
    SUPERSEDED: { text: '已替换', color: 'default' },
    ROLLED_BACK: { text: '已回滚', color: 'orange' },
    FAILED: { text: '失败', color: 'red' },
  }
  return views[status] ?? { text: status || '-', color: 'default' }
}

/** 将发布来源类型映射为内容生产面板文案。 */
export function releaseSourceLabel(sourceType: string) {
  const labels: Record<string, string> = {
    TEMPLATE: '模板',
    ENTRY: '内容',
  }
  return labels[sourceType] ?? sourceType
}

/** 将毫秒时长格式化为视频/音频资产的 mm:ss 展示。 */
export function durationLabel(durationMs?: number | null) {
  if (!durationMs || durationMs < 0) return '-'
  const totalSeconds = Math.floor(durationMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

/** 从资产引用 JSON 中抽取 assetKey 列表，供发布门禁和列表计数使用。 */
export function parseAssetRefs(assetRefsJson?: string | null): string[] {
  if (!assetRefsJson) return []
  try {
    const parsed = JSON.parse(assetRefsJson) as unknown
    if (!Array.isArray(parsed)) return []
    // 兼容字符串数组和对象数组两种资产引用写法。
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

/** 根据来源类型和来源键生成默认发布快照键。 */
export function releaseKeyFor(sourceType: string, sourceKey: string) {
  return `${sourceType.toLowerCase()}-${sourceKey}`.replace(/[^a-z0-9_-]+/g, '-')
}

/** 按阻断范围聚合发布门禁问题，便于生产发布面板分组展示。 */
export function groupReleaseBlockers(blockers: ReleaseBlocker[]): ReleaseBlockerGroup[] {
  const grouped = new Map<string, ReleaseBlocker[]>()
  // 保留后端原始顺序，只把同一 scope 的阻断项归到同一组。
  for (const blocker of blockers) {
    const scope = blocker.scope || 'UNKNOWN'
    grouped.set(scope, [...(grouped.get(scope) ?? []), blocker])
  }
  return Array.from(grouped.entries()).map(([scope, groupedBlockers]) => ({
    scope,
    blockers: groupedBlockers,
  }))
}

/** 将模板预览缺失变量格式化为面板提示文案。 */
export function formatMissingVariables(missing: string[]) {
  return missing.length === 0 ? '变量已全部解析' : `缺少变量：${missing.join(', ')}`
}
