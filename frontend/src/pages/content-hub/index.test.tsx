/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ContentHubPage from './index'
import { marketingContentApi } from '../../services/marketingContentApi'

vi.mock('../../services/marketingContentApi', () => ({
  marketingContentApi: {
    listAssetFolders: vi.fn(),
    saveAssetFolder: vi.fn(),
    listAssets: vi.fn(),
    saveAsset: vi.fn(),
    createUploadIntent: vi.fn(),
    uploadAssetFile: vi.fn(),
    setAssetStatus: vi.fn(),
    listTemplates: vi.fn(),
    saveTemplate: vi.fn(),
    previewTemplate: vi.fn(),
    setTemplateStatus: vi.fn(),
    listEntries: vi.fn(),
    saveEntry: vi.fn(),
    publishEntry: vi.fn(),
    archiveEntry: vi.fn(),
    validateRelease: vi.fn(),
    publishRelease: vi.fn(),
    listReleases: vi.fn(),
    resolveRelease: vi.fn(),
    rollbackRelease: vi.fn(),
    listAuditEvents: vi.fn(),
  },
}))

describe('ContentHubPage', () => {
  beforeEach(() => {
    vi.mocked(marketingContentApi.listAssetFolders).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(marketingContentApi.listAssets).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(marketingContentApi.listTemplates).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(marketingContentApi.listEntries).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(marketingContentApi.listReleases).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(marketingContentApi.listAuditEvents).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(marketingContentApi.createUploadIntent).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        intentKey: 's3-launch-1',
        assetKey: 'launch_hero',
        assetType: 'VIDEO',
        provider: 'S3',
        uploadToken: 'token',
        uploadUrl: 'https://s3.example.com/canvas-assets/launch.mp4?X-Amz-Signature=abc',
        uploadParams: {
          handoffMode: 'PRESIGNED_PUT',
          storageUrl: 'https://cdn.example.com/launch.mp4',
          requiredHeaders: {
            'content-type': 'video/mp4',
            'x-amz-server-side-encryption': 'aws:kms',
          },
        },
        status: 'PENDING',
        expiresAt: '2026-06-06T15:00:00',
      },
    })
    vi.mocked(marketingContentApi.uploadAssetFile).mockResolvedValue({ ok: true } as Response)
    vi.mocked(marketingContentApi.validateRelease).mockResolvedValue({
      code: 0,
      message: 'success',
      data: { ready: true, blockers: [], assetRefs: [] },
    })
    vi.mocked(marketingContentApi.publishRelease).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        releaseKey: 'template-welcome_email',
        sourceType: 'TEMPLATE',
        sourceKey: 'welcome_email',
        sourceVersion: 1,
        channel: 'EMAIL',
        status: 'ACTIVE',
        checksumSha256: 'sha256',
      },
    })
    vi.mocked(marketingContentApi.resolveRelease).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        releaseKey: 'template-welcome_email',
        sourceType: 'TEMPLATE',
        sourceKey: 'welcome_email',
        sourceVersion: 1,
        status: 'ACTIVE',
        renderedSubject: 'Hi Alice',
        renderedBody: '<h1>Welcome</h1>',
        missingVariables: [],
        snapshotJson: '{}',
        assets: [],
      },
    })
    vi.mocked(marketingContentApi.rollbackRelease).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        releaseKey: 'template-welcome_email',
        sourceType: 'TEMPLATE',
        sourceKey: 'welcome_email',
        sourceVersion: 1,
        channel: 'EMAIL',
        status: 'ROLLED_BACK',
        checksumSha256: 'sha256',
      },
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('loads CMS entries, DAM assets, and template design data on first render', async () => {
    vi.mocked(marketingContentApi.listAssetFolders).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [{
        id: 9,
        folderKey: 'campaign_assets',
        name: 'Campaign Assets',
        parentId: null,
      }],
    })
    vi.mocked(marketingContentApi.listAssets).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [{
        assetKey: 'launch_video',
        name: 'Launch video',
        assetType: 'VIDEO',
        mimeType: 'video/mp4',
        storageUrl: 'https://cdn.example.com/launch.mp4',
        folderId: 9,
        status: 'READY',
        tags: ['campaign'],
        durationMs: 95000,
        transcodeStatus: 'READY',
        posterUrl: 'https://cdn.example.com/poster.jpg',
      }],
    })
    vi.mocked(marketingContentApi.listTemplates).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [{
        templateKey: 'welcome_email',
        displayName: 'Welcome email',
        channel: 'EMAIL',
        subject: 'Hi {{firstName}}',
        body: '<h1>{{headline}}</h1>',
        designJson: '{"blocks":[]}',
        assetRefsJson: '["launch_video"]',
        variables: ['firstName', 'headline'],
        status: 'DRAFT',
      }],
    })
    vi.mocked(marketingContentApi.listEntries).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [{
        entryKey: 'spring_guide',
        contentType: 'ARTICLE',
        title: 'Spring guide',
        slug: 'spring-guide',
        locale: 'zh-CN',
        status: 'PUBLISHED',
        bodyJson: '{"blocks":[]}',
        assetRefsJson: '["launch_video"]',
      }],
    })

    render(<ContentHubPage />)

    await waitFor(() => expect(marketingContentApi.listAssetFolders).toHaveBeenCalled())
    await waitFor(() => expect(marketingContentApi.listAssets).toHaveBeenCalled())
    await waitFor(() => expect(marketingContentApi.listTemplates).toHaveBeenCalled())
    await waitFor(() => expect(marketingContentApi.listEntries).toHaveBeenCalled())
    await waitFor(() => expect(marketingContentApi.listReleases).toHaveBeenCalled())
    await waitFor(() => expect(marketingContentApi.listAuditEvents).toHaveBeenCalled())

    expect(screen.getByRole('heading', { name: '内容中心' })).toBeInTheDocument()
    expect(screen.getByText('生产发布闭环')).toBeInTheDocument()
    expect(screen.getByText('Spring guide')).toBeInTheDocument()
    expect(screen.getByText('资产库')).toBeInTheDocument()
    expect(screen.getByText('资产目录')).toBeInTheDocument()
    expect(screen.getAllByText('Campaign Assets').length).toBeGreaterThan(0)
    expect(screen.getByText('设计模板')).toBeInTheDocument()
    expect(screen.getByText('视频资产')).toBeInTheDocument()
    expect(screen.getByText('邮件模板')).toBeInTheDocument()
  })

  it('renders empty states for entries, assets, and templates', async () => {
    vi.mocked(marketingContentApi.listAssetFolders).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })
    vi.mocked(marketingContentApi.listAssets).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })
    vi.mocked(marketingContentApi.listTemplates).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })
    vi.mocked(marketingContentApi.listEntries).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })

    render(<ContentHubPage />)

    await waitFor(() => expect(marketingContentApi.listEntries).toHaveBeenCalled())
    expect(screen.getByText('暂无内容条目')).toBeInTheDocument()

    fireEvent.click(screen.getByText('资产库'))
    expect(screen.getByText('暂无资产')).toBeInTheDocument()

    fireEvent.click(screen.getByText('设计模板'))
    expect(screen.getByText('暂无内容模板')).toBeInTheDocument()
  })

  it('creates an S3 upload intent, uploads the selected file, and backfills the storage URL', async () => {
    render(<ContentHubPage />)

    fireEvent.click(await screen.findByText('资产库'))
    const fileInput = screen.getByLabelText('文件上传') as HTMLInputElement
    const file = new File(['video'], 'Launch Hero.mp4', { type: 'video/mp4' })
    fireEvent.change(fileInput, { target: { files: [file] } })
    await waitFor(() => expect(screen.getByDisplayValue('launch_hero')).toBeInTheDocument())

    const uploadButton = screen.getByRole('button', { name: /上传文件/ })
    await waitFor(() => expect(uploadButton).toBeEnabled())
    fireEvent.click(uploadButton)

    await waitFor(() => expect(marketingContentApi.createUploadIntent).toHaveBeenCalledWith({
      assetKey: 'launch_hero',
      assetType: 'VIDEO',
      provider: 'S3',
      mimeType: 'video/mp4',
      fileName: 'Launch Hero.mp4',
      sizeBytes: file.size,
    }))
    await waitFor(() => expect(marketingContentApi.uploadAssetFile).toHaveBeenCalled())
    expect(screen.getByDisplayValue('https://cdn.example.com/launch.mp4')).toBeInTheDocument()
  })

  it('validates and publishes the selected template from the readiness panel', async () => {
    vi.mocked(marketingContentApi.listTemplates).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [{
        templateKey: 'welcome_email',
        displayName: 'Welcome email',
        channel: 'EMAIL',
        subject: 'Hi {{firstName}}',
        body: '<h1>{{headline}}</h1>',
        designJson: '{"blocks":[]}',
        assetRefsJson: '["launch_video"]',
        variables: ['firstName', 'headline'],
        status: 'APPROVED',
      }],
    })
    vi.mocked(marketingContentApi.validateRelease).mockResolvedValue({
      code: 0,
      message: 'success',
      data: { ready: true, blockers: [], assetRefs: ['launch_video'] },
    })

    render(<ContentHubPage />)

    await waitFor(() => expect(screen.getByText('生产发布闭环')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /校验门禁/ }))

    await waitFor(() => expect(marketingContentApi.validateRelease).toHaveBeenCalledWith({
      sourceType: 'TEMPLATE',
      sourceKey: 'welcome_email',
    }))
    expect(await screen.findByText('可发布')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /发布快照/ }))
    await waitFor(() => expect(marketingContentApi.publishRelease).toHaveBeenCalledWith({
      sourceType: 'TEMPLATE',
      sourceKey: 'welcome_email',
      note: 'content hub readiness panel',
    }))
  })
})
