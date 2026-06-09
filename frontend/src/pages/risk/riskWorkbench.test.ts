import { describe, expect, it } from 'vitest'

import {
  applyRiskRemoteSnapshot,
  activateStrategyVersion,
  addRiskListEntry,
  buildRiskStrategyDraftCommand,
  editDraftRule,
  openRuleEditor,
  rollbackStrategyVersion,
  saveRuleGroup,
  selectRiskScene,
  selectRiskTrace,
  selectRiskWorkbenchTab,
  startLocalSimulation,
  submitStrategyApproval,
  validateDraftRule,
  type RiskStudioState,
} from './riskWorkbench'

describe('riskWorkbench', () => {
  it('selects a scene and resets draft state for that scene', () => {
    expect(selectRiskScene(state(), 'payment-abuse')).toMatchObject({
      selectedSceneKey: 'payment-abuse',
      selectedStrategyKey: 'payment-abuse-main',
      draftRule: {
        ruleKey: 'high-amount',
        sceneKey: 'payment-abuse',
      },
      validation: {
        status: 'IDLE',
        errors: [],
      },
    })
  })

  it('hydrates workbench state from backend risk records', () => {
    const next = applyRiskRemoteSnapshot(state(), {
      scenes: [
        { sceneKey: 'MARKETING_BENEFIT_ISSUE', displayName: '营销权益风控', latencyBudgetMs: 50, status: 'ACTIVE' },
      ],
      strategies: [
        {
          tenantId: 7,
          sceneKey: 'MARKETING_BENEFIT_ISSUE',
          strategyKey: 'benefit_default',
          name: '权益默认策略',
          status: 'ACTIVE',
          activeVersion: 3,
          draftVersion: 4,
          riskLevel: 'HIGH',
          owner: 'alice',
        },
      ],
      versions: [
        {
          tenantId: 7,
          strategyKey: 'benefit_default',
          version: 3,
          status: 'ACTIVE',
          submittedBy: 'alice',
          approvedBy: 'bob',
          definitionJson: JSON.stringify({
            groups: [
              {
                groupKey: 'amount-rules',
                executionOrder: 10,
                enabled: true,
                rules: [
                  {
                    ruleKey: 'high-amount',
                    displayName: 'High amount',
                    action: 'BLOCK',
                    dslJson: '{"logic":"AND","conditions":[],"groups":[]}',
                  },
                ],
              },
            ],
          }),
        },
      ],
      lists: [
        {
          tenantId: 7,
          listKey: 'blocked-device',
          listType: 'BLACK',
          subjectType: 'DEVICE_ID',
          requiresApproval: true,
          status: 'ACTIVE',
        },
      ],
      listEntries: [
        {
          id: 42,
          tenantId: 7,
          listKey: 'blocked-device',
          subjectHash: 'sha256:abc',
          subjectMasked: 'd***1',
          reason: 'fraud',
          source: 'ops',
          effectiveFrom: '2026-06-08T00:00:00Z',
          createdBy: 'alice',
        },
      ],
      simulations: [
        {
          simulationId: 'sim-1',
          sceneKey: 'MARKETING_BENEFIT_ISSUE',
          strategyKey: 'benefit_default',
          baselineVersion: 2,
          candidateVersion: 3,
          status: 'COMPLETED',
          sampleSize: 100,
          actionDistribution: { REVIEW: 80, BLOCK: 20 },
          changedActionCount: 20,
          actionChanges: { 'REVIEW->BLOCK': 20 },
          createdAt: '2026-06-08T10:00:00Z',
        },
      ],
      traces: [
        {
          traceId: '99',
          requestId: 'risk-req-1',
          sceneKey: 'MARKETING_BENEFIT_ISSUE',
          strategyKey: 'benefit_default',
          strategyVersion: 3,
          mode: 'ENFORCE',
          decision: 'BLOCK',
          score: 90,
          riskBand: 'HIGH',
          latencyMs: 9,
          createdAt: '2026-06-08T10:00:00Z',
          matchedRules: ['velocity:block'],
        },
      ],
    })

    expect(next.selectedSceneKey).toBe('MARKETING_BENEFIT_ISSUE')
    expect(next.selectedStrategyKey).toBe('benefit_default')
    expect(next.activeVersion).toBe(3)
    expect(next.draftVersion).toBe(4)
    expect(next.strategies).toEqual([
      {
        strategyKey: 'benefit_default',
        sceneKey: 'MARKETING_BENEFIT_ISSUE',
        displayName: '权益默认策略',
        riskLevel: 'HIGH',
      },
    ])
    expect(next.versions).toEqual([{ version: 3, status: 'ACTIVE', submittedBy: 'alice', approvedBy: 'bob' }])
    expect(next.ruleGroups).toEqual([
      { groupKey: 'amount-rules', displayName: 'amount-rules', priority: 10, ruleCount: 1, status: 'ENABLED' },
    ])
    expect(next.draftRule).toMatchObject({
      sceneKey: 'MARKETING_BENEFIT_ISSUE',
      ruleKey: 'high-amount',
      displayName: 'High amount',
      action: 'BLOCK',
      expression: '{"logic":"AND","conditions":[],"groups":[]}',
    })
    expect(next.listEntries).toEqual([
      {
        id: 42,
        listKey: 'blocked-device',
        subjectType: 'DEVICE_ID',
        maskedSubject: 'd***1',
        action: 'BLOCK',
        reason: 'fraud',
      },
    ])
    expect(next.traces).toEqual([
      { traceId: '99', requestId: 'risk-req-1', action: 'BLOCK', score: 90, latencyMs: 9 },
    ])
    expect(next.simulations).toEqual([
      {
        simulationId: 'sim-1',
        baselineVersion: 2,
        candidateVersion: 3,
        sampleSize: 100,
        actionDistribution: { REVIEW: 80, BLOCK: 20 },
        status: 'COMPLETED',
      },
    ])
    expect(JSON.stringify(next)).not.toContain('sha256:abc')
  })

  it('builds backend strategy draft commands from the current rule draft', () => {
    const command = buildRiskStrategyDraftCommand(state({
      scenes: [
        { sceneKey: 'MARKETING_BENEFIT_ISSUE', displayName: '营销权益风控', latencyBudgetMs: 50 },
      ],
      selectedSceneKey: 'MARKETING_BENEFIT_ISSUE',
      selectedStrategyKey: 'benefit_default',
      strategies: [
        {
          strategyKey: 'benefit_default',
          sceneKey: 'MARKETING_BENEFIT_ISSUE',
          displayName: '权益默认策略',
          riskLevel: 'HIGH',
        },
      ],
      ruleGroups: [
        { groupKey: 'amount-rules', displayName: 'Amount rules', priority: 10, ruleCount: 1, status: 'ENABLED' },
      ],
      draftRule: {
        sceneKey: 'MARKETING_BENEFIT_ISSUE',
        ruleKey: 'high-amount',
        displayName: 'High amount',
        action: 'BLOCK',
        expression: '{"logic":"AND","conditions":[],"groups":[]}',
      },
    }))

    expect(command).toMatchObject({
      sceneKey: 'MARKETING_BENEFIT_ISSUE',
      strategyKey: 'benefit_default',
      name: '权益默认策略',
      riskLevel: 'HIGH',
    })
    expect(command?.definitionJson).toContain('"groupKey":"amount-rules"')
    expect(command?.definitionJson).toContain('"ruleKey":"high-amount"')
    expect(command?.definitionJson).toContain('"action":"BLOCK"')
    expect(command?.definitionJson).toContain('"dslJson":"{\\"logic\\":\\"AND\\",\\"conditions\\":[],\\"groups\\":[]}"')
  })

  it('edits the draft rule and marks validation stale', () => {
    const next = editDraftRule(state({ selectedSceneKey: 'payment-abuse' }), {
      ruleKey: 'new-device',
      displayName: 'New device',
      action: 'REVIEW',
      expression: '{"operator":"EQ"}',
    })

    expect(next.draftRule).toMatchObject({
      ruleKey: 'new-device',
      displayName: 'New device',
      action: 'REVIEW',
      expression: '{"operator":"EQ"}',
    })
    expect(next.validation.status).toBe('STALE')
  })

  it('validates a draft rule locally before submission', () => {
    expect(validateDraftRule(state({
      draftRule: {
        sceneKey: 'payment-abuse',
        ruleKey: '',
        displayName: '',
        action: 'BLOCK',
        expression: '',
      },
    }))).toMatchObject({
      validation: {
        status: 'INVALID',
        errors: ['规则 Key 必填', '规则名称必填', '规则条件必填'],
      },
    })

    expect(validateDraftRule(state())).toMatchObject({
      validation: {
        status: 'VALID',
        errors: [],
      },
    })
  })

  it('submits approval only after validation passes', () => {
    expect(submitStrategyApproval(state({
      validation: { status: 'STALE', errors: [] },
    }), 'ready')).toMatchObject({
      approval: {
        status: 'BLOCKED',
        reason: '请先通过规则校验',
      },
    })

    expect(submitStrategyApproval(state({
      validation: { status: 'VALID', errors: [] },
    }), 'ready')).toMatchObject({
      approval: {
        status: 'SUBMITTED',
        reason: 'ready',
      },
    })
  })

  it('activates a validated strategy version and records rollback target', () => {
    const active = activateStrategyVersion(state({
      selectedStrategyKey: 'payment-abuse-main',
      versions: [
        { version: 1, status: 'ACTIVE' },
        { version: 2, status: 'APPROVED' },
      ],
    }), 2)

    expect(active.activeVersion).toBe(2)
    expect(active.versions).toEqual([
      { version: 1, status: 'SUPERSEDED' },
      { version: 2, status: 'ACTIVE' },
    ])

    expect(rollbackStrategyVersion(active, 1, 'incident rollback')).toMatchObject({
      activeVersion: 1,
      rollback: {
        targetVersion: 1,
        reason: 'incident rollback',
      },
    })
  })

  it('switches between studio, lists, simulation, and trace tabs', () => {
    expect(selectRiskWorkbenchTab(state(), 'LISTS').activeTab).toBe('LISTS')
    expect(selectRiskWorkbenchTab(state(), 'SIMULATION').activeTab).toBe('SIMULATION')
    expect(selectRiskWorkbenchTab(state(), 'TRACE').activeTab).toBe('TRACE')
  })

  it('opens rule editor and saves rule groups into the table state', () => {
    const opened = openRuleEditor(state(), 'velocity-rules')
    expect(opened.ruleEditor).toEqual({ open: true, groupKey: 'velocity-rules' })

    const saved = saveRuleGroup(opened, {
      groupKey: 'velocity-rules',
      displayName: 'Velocity rules',
      priority: 10,
      ruleCount: 3,
      status: 'ENABLED',
    })

    expect(saved.ruleEditor.open).toBe(false)
    expect(saved.ruleGroups).toContainEqual({
      groupKey: 'velocity-rules',
      displayName: 'Velocity rules',
      priority: 10,
      ruleCount: 3,
      status: 'ENABLED',
    })
  })

  it('adds list entries without exposing raw subject values in display state', () => {
    const next = addRiskListEntry(state(), {
      listKey: 'blocked-devices',
      subjectType: 'DEVICE',
      subjectKey: 'device-raw-123456',
      action: 'BLOCK',
      reason: 'manual review',
    })

    expect(next.listEntries[0]).toMatchObject({
      listKey: 'blocked-devices',
      subjectType: 'DEVICE',
      maskedSubject: 'de***56',
      action: 'BLOCK',
      reason: 'manual review',
    })
    expect(JSON.stringify(next.listEntries)).not.toContain('device-raw-123456')
  })

  it('records local simulation runs with action distribution inputs', () => {
    const next = startLocalSimulation(state(), {
      simulationId: 'sim-1',
      baselineVersion: 1,
      candidateVersion: 2,
      sampleSize: 1000,
      actionDistribution: { ALLOW: 900, REVIEW: 80, BLOCK: 20 },
    })

    expect(next.activeTab).toBe('SIMULATION')
    expect(next.simulations[0]).toMatchObject({
      simulationId: 'sim-1',
      status: 'RUNNING',
      actionDistribution: { ALLOW: 900, REVIEW: 80, BLOCK: 20 },
    })
  })

  it('selects decision traces for the trace tab', () => {
    const next = selectRiskTrace(state(), 'trace-1')

    expect(next.activeTab).toBe('TRACE')
    expect(next.selectedTraceId).toBe('trace-1')
  })

  it('rolls back to an existing inactive version and records the reason', () => {
    const next = rollbackStrategyVersion(state({
      activeVersion: 2,
      versions: [
        { version: 1, status: 'READY' },
        { version: 2, status: 'ACTIVE' },
      ],
    }), 1, 'bad rule impact')

    expect(next.activeVersion).toBe(1)
    expect(next.versions).toEqual([
      { version: 1, status: 'ACTIVE' },
      { version: 2, status: 'SUPERSEDED' },
    ])
    expect(next.rollback).toMatchObject({
      targetVersion: 1,
      reason: 'bad rule impact',
    })
  })

  it('ignores rollback requests for unknown or active versions', () => {
    const base = state({
      activeVersion: 2,
      versions: [
        { version: 1, status: 'READY' },
        { version: 2, status: 'ACTIVE' },
      ],
    })

    expect(rollbackStrategyVersion(base, 99, 'missing target')).toBe(base)
    expect(rollbackStrategyVersion(base, 2, 'same target')).toBe(base)
  })
})

function state(overrides: Partial<RiskStudioState> = {}): RiskStudioState {
  return {
    scenes: [
      { sceneKey: 'payment-abuse', displayName: '支付风控' },
      { sceneKey: 'signup-abuse', displayName: '注册风控' },
    ],
    strategies: [
      { strategyKey: 'payment-abuse-main', sceneKey: 'payment-abuse', displayName: '支付主策略' },
    ],
    selectedSceneKey: null,
    selectedStrategyKey: null,
    activeVersion: 1,
    draftVersion: null,
    versions: [
      { version: 1, status: 'ACTIVE' },
    ],
    activeTab: 'STUDIO',
    ruleEditor: {
      open: false,
      groupKey: null,
    },
    ruleGroups: [
      { groupKey: 'amount-rules', displayName: 'Amount rules', priority: 1, ruleCount: 2, status: 'ENABLED' },
    ],
    listEntries: [],
    simulations: [],
    traces: [
      { traceId: 'trace-1', requestId: 'req-1', action: 'REVIEW', score: 72, latencyMs: 34 },
    ],
    selectedTraceId: null,
    draftRule: {
      sceneKey: 'payment-abuse',
      ruleKey: 'high-amount',
      displayName: 'High amount',
      action: 'REVIEW',
      expression: '{"operator":"GT","left":{"type":"FEATURE","key":"amount"},"right":{"type":"LITERAL","value":1000}}',
    },
    validation: {
      status: 'VALID',
      errors: [],
    },
    approval: {
      status: 'DRAFT',
    },
    rollback: null,
    ...overrides,
  }
}
