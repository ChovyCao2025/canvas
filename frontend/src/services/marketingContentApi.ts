import type { R } from '../types'
import type {
  ContentEntry,
  ContentEntryDraft,
  ContentEntryStatusPayload,
  ContentAuditEvent,
  ContentRelease,
  ContentTemplate,
  ContentTemplateDraft,
  ReleaseValidationResult,
  ResolvedContentRelease,
  MarketingAsset,
  MarketingAssetDraft,
  MarketingAssetFolder,
  MarketingAssetFolderDraft,
  MarketingAssetStatusPayload,
  MarketingAssetUploadCleanupPayload,
  MarketingAssetUploadCleanupResult,
  MarketingAssetUploadIntent,
  MarketingAssetUploadIntentDraft,
  TemplatePreviewResult,
} from '../pages/content-hub/contentHubPresentation'
import {
  isPresignedPutIntent,
  uploadIntentRequiredHeaders,
} from '../pages/content-hub/contentHubPresentation'
import http from './api'

/** 发布来源载荷，指定模板或内容条目进入发布闭环。 */
export interface ReleaseSourcePayload {
  /** 发布来源类型。 */
  sourceType: 'TEMPLATE' | 'ENTRY' | string
  /** 发布来源业务键。 */
  sourceKey: string
}

/** 发布快照请求载荷。 */
export interface ReleasePublishPayload extends ReleaseSourcePayload {
  /** 创建人。 */
  createdBy?: string
  /** 发布备注。 */
  note?: string
}

/** 发布回滚请求载荷。 */
export interface ReleaseRollbackPayload {
  /** 操作人。 */
  actor?: string
  /** 回滚原因。 */
  reason?: string
}

/** 内容中心 API，覆盖资产、模板、CMS 条目、发布快照和审计事件。 */
export const marketingContentApi = {
  /** 查询资产目录。 */
  listAssetFolders: () =>
    http.get<R<MarketingAssetFolder[]>, R<MarketingAssetFolder[]>>('/marketing/content/asset-folders'),

  /** 保存资产目录。 */
  saveAssetFolder: (payload: MarketingAssetFolderDraft) =>
    http.post<R<MarketingAssetFolder>, R<MarketingAssetFolder>>('/marketing/content/asset-folders', payload),

  /** 查询 DAM 资产列表。 */
  listAssets: (params?: { keyword?: string; assetType?: string; status?: string }) =>
    http.get<R<MarketingAsset[]>, R<MarketingAsset[]>>('/marketing/content/assets', { params }),

  /** 保存 DAM 资产草稿。 */
  saveAsset: (payload: MarketingAssetDraft) =>
    http.post<R<MarketingAsset>, R<MarketingAsset>>('/marketing/content/assets', payload),

  /** 创建资产文件直传意图。 */
  createUploadIntent: (payload: MarketingAssetUploadIntentDraft) =>
    http.post<R<MarketingAssetUploadIntent>, R<MarketingAssetUploadIntent>>(
      '/marketing/content/assets/upload-intents',
      payload,
    ),

  /** 按上传意图把本地文件直传到供应商存储。 */
  uploadAssetFile: async (intent: MarketingAssetUploadIntent, file: File) => {
    // 上传意图必须是后端签发的 PRESIGNED_PUT 模式。
    if (!isPresignedPutIntent(intent)) {
      throw new Error('当前上传意图不是直传 PUT 模式')
    }
    // requiredHeaders 来自后端签名参数，必须原样带给供应商。
    const response = await fetch(intent.uploadUrl, {
      method: 'PUT',
      headers: uploadIntentRequiredHeaders(intent),
      body: file,
    })
    if (!response.ok) {
      throw new Error(`资产文件上传失败 (${response.status})`)
    }
    return response
  },

  /** 清理过期上传意图。 */
  expireStaleUploadIntents: (payload: MarketingAssetUploadCleanupPayload = {}) =>
    http.post<R<MarketingAssetUploadCleanupResult>, R<MarketingAssetUploadCleanupResult>>(
      '/marketing/content/assets/upload-intents/expire-stale',
      payload,
    ),

  /** 更新资产审核状态。 */
  setAssetStatus: (assetKey: string, payload: MarketingAssetStatusPayload) =>
    http.post<R<MarketingAsset>, R<MarketingAsset>>(`/marketing/content/assets/${assetKey}/status`, payload),

  /** 查询内容模板。 */
  listTemplates: (params?: { keyword?: string; channel?: string; status?: string }) =>
    http.get<R<ContentTemplate[]>, R<ContentTemplate[]>>('/marketing/content/templates', { params }),

  /** 保存内容模板草稿。 */
  saveTemplate: (payload: ContentTemplateDraft) =>
    http.post<R<ContentTemplate>, R<ContentTemplate>>('/marketing/content/templates', payload),

  /** 使用后端模板引擎预览模板。 */
  previewTemplate: (templateKey: string, context: Record<string, unknown>) =>
    http.post<R<TemplatePreviewResult>, R<TemplatePreviewResult>>(
      `/marketing/content/templates/${templateKey}/preview`,
      context,
    ),

  /** 更新模板审核状态。 */
  setTemplateStatus: (templateKey: string, payload: MarketingAssetStatusPayload) =>
    http.post<R<ContentTemplate>, R<ContentTemplate>>(`/marketing/content/templates/${templateKey}/status`, payload),

  /** 查询 CMS 内容条目。 */
  listEntries: (params?: { keyword?: string; contentType?: string; status?: string }) =>
    http.get<R<ContentEntry[]>, R<ContentEntry[]>>('/marketing/content/entries', { params }),

  /** 保存 CMS 内容条目草稿。 */
  saveEntry: (payload: ContentEntryDraft) =>
    http.post<R<ContentEntry>, R<ContentEntry>>('/marketing/content/entries', payload),

  /** 发布 CMS 内容条目。 */
  publishEntry: (entryKey: string, payload: ContentEntryStatusPayload) =>
    http.post<R<ContentEntry>, R<ContentEntry>>(`/marketing/content/entries/${entryKey}/publish`, payload),

  /** 归档 CMS 内容条目。 */
  archiveEntry: (entryKey: string, payload: ContentEntryStatusPayload) =>
    http.post<R<ContentEntry>, R<ContentEntry>>(`/marketing/content/entries/${entryKey}/archive`, payload),

  /** 校验指定来源是否满足发布门禁。 */
  validateRelease: (payload: ReleaseSourcePayload) =>
    http.post<R<ReleaseValidationResult>, R<ReleaseValidationResult>>('/marketing/content/releases/validate', payload),

  /** 发布指定来源的生产快照。 */
  publishRelease: (payload: ReleasePublishPayload) =>
    http.post<R<ContentRelease>, R<ContentRelease>>('/marketing/content/releases/publish', payload),

  /** 查询发布快照列表。 */
  listReleases: (params?: { sourceType?: string; sourceKey?: string; status?: string }) =>
    http.get<R<ContentRelease[]>, R<ContentRelease[]>>('/marketing/content/releases', { params }),

  /** 解析发布快照，生成最终主题、正文和资产快照。 */
  resolveRelease: (releaseKey: string, context: Record<string, unknown>) =>
    http.post<R<ResolvedContentRelease>, R<ResolvedContentRelease>>(
      `/marketing/content/releases/${releaseKey}/resolve`,
      context,
    ),

  /** 回滚指定发布快照。 */
  rollbackRelease: (releaseKey: string, payload: ReleaseRollbackPayload) =>
    http.post<R<ContentRelease>, R<ContentRelease>>(`/marketing/content/releases/${releaseKey}/rollback`, payload),

  /** 查询内容中心审计事件。 */
  listAuditEvents: (params?: { targetType?: string; targetKey?: string; limit?: number }) =>
    http.get<R<ContentAuditEvent[]>, R<ContentAuditEvent[]>>('/marketing/content/audit-events', { params }),
}
