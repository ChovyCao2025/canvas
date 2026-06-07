import { describe, expect, it } from 'vitest'

import {
  assetTypeLabel,
  assetKeyFromFileName,
  assetTypeFromMime,
  contentStatusView,
  durationLabel,
  extractTemplateVariables,
  groupReleaseBlockers,
  isPresignedPutIntent,
  localTemplatePreview,
  parseAssetRefs,
  releaseStatusView,
  uploadIntentRequiredHeaders,
  uploadIntentStorageUrl,
} from './contentHubPresentation'

describe('contentHubPresentation', () => {
  it('extracts template variables from subject and body in stable order', () => {
    expect(extractTemplateVariables('Hi {{firstName}}', '<h1>{{headline}}</h1>{{firstName}}')).toEqual([
      'firstName',
      'headline',
    ])
  })

  it('renders local template previews and reports unresolved variables', () => {
    expect(localTemplatePreview('Hi {{firstName}}', '<h1>{{headline}}</h1>', { firstName: 'Alice' })).toEqual({
      renderedSubject: 'Hi Alice',
      renderedBody: '<h1>{{headline}}</h1>',
      missingVariables: ['headline'],
    })
  })

  it('formats video-oriented asset metadata for DAM rows', () => {
    expect(assetTypeLabel('VIDEO')).toBe('视频')
    expect(assetTypeFromMime('video/mp4')).toBe('VIDEO')
    expect(assetTypeFromMime('application/pdf')).toBe('FILE')
    expect(assetKeyFromFileName('Launch Hero 2026.mp4')).toBe('launch_hero_2026')
    expect(durationLabel(95000)).toBe('1:35')
    expect(contentStatusView('PUBLISHED')).toEqual({ text: '已发布', color: 'green' })
  })

  it('extracts direct upload headers and storage URL from S3 upload intents', () => {
    const intent = {
      intentKey: 's3-launch-1',
      assetKey: 'launch_hero',
      assetType: 'VIDEO',
      provider: 'S3',
      uploadToken: 'token',
      uploadUrl: 'https://s3.example.com/bucket/launch.mp4?X-Amz-Signature=abc',
      status: 'PENDING',
      uploadParams: {
        handoffMode: 'PRESIGNED_PUT',
        storageUrl: 'https://cdn.example.com/launch.mp4',
        requiredHeaders: {
          'content-type': 'video/mp4',
          'x-amz-server-side-encryption': 'aws:kms',
        },
      },
    }

    expect(isPresignedPutIntent(intent)).toBe(true)
    expect(uploadIntentRequiredHeaders(intent)).toEqual({
      'content-type': 'video/mp4',
      'x-amz-server-side-encryption': 'aws:kms',
    })
    expect(uploadIntentStorageUrl(intent)).toBe('https://cdn.example.com/launch.mp4')
  })

  it('counts asset references from JSON arrays and ignores invalid payloads', () => {
    expect(parseAssetRefs('["launch_hero","poster"]')).toEqual(['launch_hero', 'poster'])
    expect(parseAssetRefs('[{"assetKey":"hero_video"},{"assetKey":"poster"}]')).toEqual(['hero_video', 'poster'])
    expect(parseAssetRefs('{bad json')).toEqual([])
  })

  it('groups release blockers and formats release status for readiness panels', () => {
    expect(groupReleaseBlockers([
      { scope: 'ASSET', key: 'hero_video', reason: 'video transcode is not ready' },
      { scope: 'TEMPLATE', key: 'welcome_email', reason: 'template is not approved' },
      { scope: 'ASSET', key: 'poster', reason: 'asset not found' },
    ])).toEqual([
      {
        scope: 'ASSET',
        blockers: [
          { scope: 'ASSET', key: 'hero_video', reason: 'video transcode is not ready' },
          { scope: 'ASSET', key: 'poster', reason: 'asset not found' },
        ],
      },
      {
        scope: 'TEMPLATE',
        blockers: [
          { scope: 'TEMPLATE', key: 'welcome_email', reason: 'template is not approved' },
        ],
      },
    ])
    expect(releaseStatusView('ACTIVE')).toEqual({ text: '生效中', color: 'green' })
    expect(releaseStatusView('SUPERSEDED')).toEqual({ text: '已替换', color: 'default' })
  })
})
