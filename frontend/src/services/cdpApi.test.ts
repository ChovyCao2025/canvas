import { describe, expect, it, vi } from 'vitest'
import { createCdpApi } from './cdpApi'

describe('cdpApi', () => {
  it('calls computed profile attribute endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createCdpApi(client as any)

    await api.computedProfiles.list()
    await api.computedProfiles.create({
      attrCode: 'lifecycle_stage',
      displayName: 'Lifecycle stage',
      valueType: 'STRING',
      computeType: 'RULE',
      expressionJson: '{"field":"paidCount","op":">=","value":2}',
      refreshMode: 'MANUAL',
    })
    await api.computedProfiles.preview(7)
    await api.computedProfiles.activate(7)
    await api.computedProfiles.pause(7)
    await api.computedProfiles.run(7)
    await api.computedProfiles.runs(7)
    await api.computedProfiles.changes(7, 'u1')

    expect(client.get).toHaveBeenCalledWith('/cdp/computed-profile-attributes')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-profile-attributes', {
      attrCode: 'lifecycle_stage',
      displayName: 'Lifecycle stage',
      valueType: 'STRING',
      computeType: 'RULE',
      expressionJson: '{"field":"paidCount","op":">=","value":2}',
      refreshMode: 'MANUAL',
    })
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-profile-attributes/7/preview')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-profile-attributes/7/activate')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-profile-attributes/7/pause')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-profile-attributes/7/run')
    expect(client.get).toHaveBeenCalledWith('/cdp/computed-profile-attributes/7/runs')
    expect(client.get).toHaveBeenCalledWith('/cdp/computed-profile-attributes/7/changes', { params: { userId: 'u1' } })
  })

  it('calls computed tag and lineage endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createCdpApi(client as any)

    await api.computedTags.list()
    await api.computedTags.create({
      tagCode: 'vip_likely',
      displayName: 'VIP likely',
      valueType: 'BOOLEAN',
      computeType: 'RULE',
      expressionJson: '{"field":"paidCount","op":">=","value":2}',
      refreshMode: 'MANUAL',
      dependencies: ['paid_user'],
    })
    await api.computedTags.preview('vip_likely')
    await api.computedTags.activate('vip_likely')
    await api.computedTags.pause('vip_likely')
    await api.computedTags.run('vip_likely')
    await api.computedTags.runs('vip_likely')
    await api.computedTags.lineage('vip_likely')
    await api.computedTags.impactCheck('vip_likely', 'BOOLEAN', 'NUMBER')

    expect(client.get).toHaveBeenCalledWith('/cdp/computed-tags')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-tags', {
      tagCode: 'vip_likely',
      displayName: 'VIP likely',
      valueType: 'BOOLEAN',
      computeType: 'RULE',
      expressionJson: '{"field":"paidCount","op":">=","value":2}',
      refreshMode: 'MANUAL',
      dependencies: ['paid_user'],
    })
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/preview')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/activate')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/pause')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/run')
    expect(client.get).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/runs')
    expect(client.get).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/lineage')
    expect(client.post).toHaveBeenCalledWith('/cdp/computed-tags/vip_likely/impact-check', {
      oldValueType: 'BOOLEAN',
      newValueType: 'NUMBER',
    })
  })

  it('calls realtime audience event, overlap, set operation, and snapshot endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createCdpApi(client as any)

    await api.realtimeAudiences.processEvent(10, {
      sourceEventId: 'evt-1',
      userId: 'u1',
      eventTime: '2026-06-03T00:00:00Z',
      properties: { event: 'Paid' },
      removeOnNoMatch: true,
    })
    await api.realtimeAudiences.createSnapshot(10)
    await api.realtimeAudiences.overlap(10, 11)
    await api.realtimeAudiences.merge(10, 11)
    await api.realtimeAudiences.exclude(10, 11)
    await api.realtimeAudiences.snapshots(10)

    expect(client.post).toHaveBeenCalledWith('/cdp/realtime-audiences/10/events', {
      sourceEventId: 'evt-1',
      userId: 'u1',
      eventTime: '2026-06-03T00:00:00Z',
      properties: { event: 'Paid' },
      removeOnNoMatch: true,
    })
    expect(client.post).toHaveBeenCalledWith('/cdp/realtime-audiences/10/snapshot')
    expect(client.get).toHaveBeenCalledWith('/cdp/audiences/10/overlap/11')
    expect(client.post).toHaveBeenCalledWith('/cdp/audiences/merge', null, { params: { leftId: 10, rightId: 11 } })
    expect(client.post).toHaveBeenCalledWith('/cdp/audiences/exclude', null, { params: { baseId: 10, excludedId: 11 } })
    expect(client.get).toHaveBeenCalledWith('/cdp/realtime-audiences/10/snapshots')
  })
})
