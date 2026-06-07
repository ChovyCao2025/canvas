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

export interface ReleaseSourcePayload {
  sourceType: 'TEMPLATE' | 'ENTRY' | string
  sourceKey: string
}

export interface ReleasePublishPayload extends ReleaseSourcePayload {
  createdBy?: string
  note?: string
}

export interface ReleaseRollbackPayload {
  actor?: string
  reason?: string
}

export const marketingContentApi = {
  listAssetFolders: () =>
    http.get<R<MarketingAssetFolder[]>, R<MarketingAssetFolder[]>>('/marketing/content/asset-folders'),

  saveAssetFolder: (payload: MarketingAssetFolderDraft) =>
    http.post<R<MarketingAssetFolder>, R<MarketingAssetFolder>>('/marketing/content/asset-folders', payload),

  listAssets: (params?: { keyword?: string; assetType?: string; status?: string }) =>
    http.get<R<MarketingAsset[]>, R<MarketingAsset[]>>('/marketing/content/assets', { params }),

  saveAsset: (payload: MarketingAssetDraft) =>
    http.post<R<MarketingAsset>, R<MarketingAsset>>('/marketing/content/assets', payload),

  createUploadIntent: (payload: MarketingAssetUploadIntentDraft) =>
    http.post<R<MarketingAssetUploadIntent>, R<MarketingAssetUploadIntent>>(
      '/marketing/content/assets/upload-intents',
      payload,
    ),

  uploadAssetFile: async (intent: MarketingAssetUploadIntent, file: File) => {
    if (!isPresignedPutIntent(intent)) {
      throw new Error('当前上传意图不是直传 PUT 模式')
    }
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

  expireStaleUploadIntents: (payload: MarketingAssetUploadCleanupPayload = {}) =>
    http.post<R<MarketingAssetUploadCleanupResult>, R<MarketingAssetUploadCleanupResult>>(
      '/marketing/content/assets/upload-intents/expire-stale',
      payload,
    ),

  setAssetStatus: (assetKey: string, payload: MarketingAssetStatusPayload) =>
    http.post<R<MarketingAsset>, R<MarketingAsset>>(`/marketing/content/assets/${assetKey}/status`, payload),

  listTemplates: (params?: { keyword?: string; channel?: string; status?: string }) =>
    http.get<R<ContentTemplate[]>, R<ContentTemplate[]>>('/marketing/content/templates', { params }),

  saveTemplate: (payload: ContentTemplateDraft) =>
    http.post<R<ContentTemplate>, R<ContentTemplate>>('/marketing/content/templates', payload),

  previewTemplate: (templateKey: string, context: Record<string, unknown>) =>
    http.post<R<TemplatePreviewResult>, R<TemplatePreviewResult>>(
      `/marketing/content/templates/${templateKey}/preview`,
      context,
    ),

  setTemplateStatus: (templateKey: string, payload: MarketingAssetStatusPayload) =>
    http.post<R<ContentTemplate>, R<ContentTemplate>>(`/marketing/content/templates/${templateKey}/status`, payload),

  listEntries: (params?: { keyword?: string; contentType?: string; status?: string }) =>
    http.get<R<ContentEntry[]>, R<ContentEntry[]>>('/marketing/content/entries', { params }),

  saveEntry: (payload: ContentEntryDraft) =>
    http.post<R<ContentEntry>, R<ContentEntry>>('/marketing/content/entries', payload),

  publishEntry: (entryKey: string, payload: ContentEntryStatusPayload) =>
    http.post<R<ContentEntry>, R<ContentEntry>>(`/marketing/content/entries/${entryKey}/publish`, payload),

  archiveEntry: (entryKey: string, payload: ContentEntryStatusPayload) =>
    http.post<R<ContentEntry>, R<ContentEntry>>(`/marketing/content/entries/${entryKey}/archive`, payload),

  validateRelease: (payload: ReleaseSourcePayload) =>
    http.post<R<ReleaseValidationResult>, R<ReleaseValidationResult>>('/marketing/content/releases/validate', payload),

  publishRelease: (payload: ReleasePublishPayload) =>
    http.post<R<ContentRelease>, R<ContentRelease>>('/marketing/content/releases/publish', payload),

  listReleases: (params?: { sourceType?: string; sourceKey?: string; status?: string }) =>
    http.get<R<ContentRelease[]>, R<ContentRelease[]>>('/marketing/content/releases', { params }),

  resolveRelease: (releaseKey: string, context: Record<string, unknown>) =>
    http.post<R<ResolvedContentRelease>, R<ResolvedContentRelease>>(
      `/marketing/content/releases/${releaseKey}/resolve`,
      context,
    ),

  rollbackRelease: (releaseKey: string, payload: ReleaseRollbackPayload) =>
    http.post<R<ContentRelease>, R<ContentRelease>>(`/marketing/content/releases/${releaseKey}/rollback`, payload),

  listAuditEvents: (params?: { targetType?: string; targetKey?: string; limit?: number }) =>
    http.get<R<ContentAuditEvent[]>, R<ContentAuditEvent[]>>('/marketing/content/audit-events', { params }),
}
