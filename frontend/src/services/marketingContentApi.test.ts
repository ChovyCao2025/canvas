import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { marketingContentApi } from './marketingContentApi'

describe('marketingContentApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('calls marketing content asset, template, preview, and entry endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })

    await marketingContentApi.listAssetFolders()
    await marketingContentApi.saveAssetFolder({
      folderKey: 'campaign_assets',
      name: 'Campaign Assets',
      parentId: null,
    })
    await marketingContentApi.listAssets({ keyword: 'hero', assetType: 'VIDEO', status: 'READY' })
    await marketingContentApi.saveAsset({
      assetKey: 'launch_hero',
      name: 'Launch hero',
      assetType: 'VIDEO',
      mimeType: 'video/mp4',
      storageUrl: 'https://cdn.example.com/launch.mp4',
      folderId: 9,
      durationMs: 95000,
      transcodeStatus: 'READY',
      tags: ['launch'],
      metadata: { campaign: 'spring' },
      status: 'READY',
    })
    await marketingContentApi.createUploadIntent({
      assetKey: 'launch_hero',
      assetType: 'VIDEO',
      provider: 'MUX',
      mimeType: 'video/mp4',
      fileName: 'launch.mp4',
      sizeBytes: 12345,
    })
    await marketingContentApi.expireStaleUploadIntents({ limit: 50, actor: 'Alice' })
    await marketingContentApi.setAssetStatus('launch_hero', { status: 'ARCHIVED', reviewNotes: 'superseded' })
    await marketingContentApi.listTemplates({ keyword: 'welcome', channel: 'EMAIL', status: 'DRAFT' })
    await marketingContentApi.saveTemplate({
      templateKey: 'welcome_email',
      displayName: 'Welcome email',
      channel: 'EMAIL',
      subject: 'Hi {{firstName}}',
      body: '<h1>{{headline}}</h1>',
      designJson: '{"blocks":[]}',
      assetRefsJson: '[]',
      status: 'DRAFT',
    })
    await marketingContentApi.previewTemplate('welcome_email', { firstName: 'Alice', headline: 'Welcome' })
    await marketingContentApi.listEntries({ keyword: 'guide', contentType: 'ARTICLE', status: 'PUBLISHED' })
    await marketingContentApi.saveEntry({
      entryKey: 'spring_guide',
      contentType: 'ARTICLE',
      title: 'Spring guide',
      slug: 'spring-guide',
      locale: 'zh-CN',
      summary: 'Guide',
      bodyJson: '{"blocks":[]}',
      seoJson: '{}',
      assetRefsJson: '["launch_hero"]',
    })
    await marketingContentApi.publishEntry('spring_guide', { actor: 'Alice' })
    await marketingContentApi.archiveEntry('spring_guide', { actor: 'Alice' })
    await marketingContentApi.validateRelease({ sourceType: 'TEMPLATE', sourceKey: 'welcome_email' })
    await marketingContentApi.publishRelease({
      sourceType: 'TEMPLATE',
      sourceKey: 'welcome_email',
      createdBy: 'Alice',
      note: 'approved',
    })
    await marketingContentApi.listReleases({ sourceType: 'TEMPLATE', sourceKey: 'welcome_email', status: 'ACTIVE' })
    await marketingContentApi.resolveRelease('template-welcome_email', { firstName: 'Alice' })
    await marketingContentApi.rollbackRelease('template-welcome_email', { actor: 'Alice', reason: 'bad copy' })
    await marketingContentApi.listAuditEvents({ targetType: 'RELEASE', targetKey: 'template-welcome_email', limit: 20 })

    expect(get).toHaveBeenCalledWith('/marketing/content/asset-folders')
    expect(post).toHaveBeenCalledWith('/marketing/content/asset-folders', {
      folderKey: 'campaign_assets',
      name: 'Campaign Assets',
      parentId: null,
    })
    expect(get).toHaveBeenCalledWith('/marketing/content/assets', {
      params: { keyword: 'hero', assetType: 'VIDEO', status: 'READY' },
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/assets', expect.objectContaining({
      assetKey: 'launch_hero',
      assetType: 'VIDEO',
      folderId: 9,
      durationMs: 95000,
    }))
    expect(post).toHaveBeenCalledWith('/marketing/content/assets/upload-intents', {
      assetKey: 'launch_hero',
      assetType: 'VIDEO',
      provider: 'MUX',
      mimeType: 'video/mp4',
      fileName: 'launch.mp4',
      sizeBytes: 12345,
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/assets/upload-intents/expire-stale', {
      limit: 50,
      actor: 'Alice',
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/assets/launch_hero/status', {
      status: 'ARCHIVED',
      reviewNotes: 'superseded',
    })
    expect(get).toHaveBeenCalledWith('/marketing/content/templates', {
      params: { keyword: 'welcome', channel: 'EMAIL', status: 'DRAFT' },
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/templates/welcome_email/preview', {
      firstName: 'Alice',
      headline: 'Welcome',
    })
    expect(get).toHaveBeenCalledWith('/marketing/content/entries', {
      params: { keyword: 'guide', contentType: 'ARTICLE', status: 'PUBLISHED' },
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/entries/spring_guide/publish', { actor: 'Alice' })
    expect(post).toHaveBeenCalledWith('/marketing/content/entries/spring_guide/archive', { actor: 'Alice' })
    expect(post).toHaveBeenCalledWith('/marketing/content/releases/validate', {
      sourceType: 'TEMPLATE',
      sourceKey: 'welcome_email',
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/releases/publish', {
      sourceType: 'TEMPLATE',
      sourceKey: 'welcome_email',
      createdBy: 'Alice',
      note: 'approved',
    })
    expect(get).toHaveBeenCalledWith('/marketing/content/releases', {
      params: { sourceType: 'TEMPLATE', sourceKey: 'welcome_email', status: 'ACTIVE' },
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/releases/template-welcome_email/resolve', {
      firstName: 'Alice',
    })
    expect(post).toHaveBeenCalledWith('/marketing/content/releases/template-welcome_email/rollback', {
      actor: 'Alice',
      reason: 'bad copy',
    })
    expect(get).toHaveBeenCalledWith('/marketing/content/audit-events', {
      params: { targetType: 'RELEASE', targetKey: 'template-welcome_email', limit: 20 },
    })
  })

  it('uploads direct asset files with provider-signed required headers', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200 })
    vi.stubGlobal('fetch', fetchMock)

    await marketingContentApi.uploadAssetFile({
      intentKey: 's3-launch-1',
      assetKey: 'launch_hero',
      assetType: 'VIDEO',
      provider: 'S3',
      uploadToken: 'token',
      uploadUrl: 'https://s3.example.com/canvas-assets/launch.mp4?X-Amz-Signature=abc',
      status: 'PENDING',
      uploadParams: {
        handoffMode: 'PRESIGNED_PUT',
        requiredHeaders: {
          'content-type': 'video/mp4',
          'x-amz-server-side-encryption': 'aws:kms',
        },
      },
    }, { name: 'launch.mp4' } as unknown as File)

    expect(fetchMock).toHaveBeenCalledWith(
      'https://s3.example.com/canvas-assets/launch.mp4?X-Amz-Signature=abc',
      {
        method: 'PUT',
        headers: {
          'content-type': 'video/mp4',
          'x-amz-server-side-encryption': 'aws:kms',
        },
        body: { name: 'launch.mp4' },
      },
    )
  })
})
