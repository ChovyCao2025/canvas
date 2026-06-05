import { describe, expect, it } from 'vitest'

import { availableVariables, deriveUpstreamNodeIds } from './variableAvailability'

describe('variableAvailability', () => {
  it('returns trigger profile and upstream variables but hides downstream output', () => {
    const result = availableVariables({
      selectedNodeId: 'send',
      nodes: [
        { id: 'start', outputs: ['trigger.eventCode'] },
        { id: 'api', outputs: ['api_user.name'] },
        { id: 'send', outputs: [] },
        { id: 'later', outputs: ['later.value'] },
      ],
      upstreamNodeIds: ['start', 'api'],
      profileFields: ['profile.email'],
      computedFields: ['computed.churnRisk'],
    })

    expect(result.map(item => item.token)).toContain('{{api_user.name}}')
    expect(result.map(item => item.token)).toContain('{{profile.email}}')
    expect(result.map(item => item.token)).not.toContain('{{later.value}}')
  })

  it('derives upstream nodes from node targets when explicit ids are absent', () => {
    expect(deriveUpstreamNodeIds([
      { id: 'start', nextNodeIds: ['api'] },
      { id: 'api', nextNodeIds: ['send'], outputs: ['api_user.name'] },
      { id: 'send', nextNodeIds: ['later'] },
      { id: 'later', outputs: ['later.value'] },
    ], 'send').sort()).toEqual(['api', 'start'])
  })

  it('deduplicates fields by token and keeps first source metadata', () => {
    const result = availableVariables({
      selectedNodeId: 'send',
      nodes: [{ id: 'api', outputs: ['profile.email'] }],
      upstreamNodeIds: ['api'],
      profileFields: [{ fieldKey: 'profile.email', fieldName: 'Email' }],
    })

    expect(result.filter(item => item.token === '{{profile.email}}')).toHaveLength(1)
    expect(result.find(item => item.token === '{{profile.email}}')).toMatchObject({
      label: 'Email',
      source: 'profile',
    })
  })
})
