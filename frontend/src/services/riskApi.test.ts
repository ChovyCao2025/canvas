import { beforeEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { riskApi, type RiskDecisionEvaluateRequest, type RiskListCommand, type RiskSimulationRequest, type RiskStrategyCommand } from './riskApi'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockedHttp = vi.mocked(http)

describe('riskApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedHttp.get.mockResolvedValue({ code: 0, message: 'success', data: [] })
    mockedHttp.post.mockResolvedValue({ code: 0, message: 'success', data: {} })
    mockedHttp.delete.mockResolvedValue({ code: 0, message: 'success', data: undefined })
  })

  it('loads risk scenes', async () => {
    await riskApi.listScenes()

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/scenes')
  })

  it('loads strategies by scene, detail, versions, and pause endpoint', async () => {
    await riskApi.listStrategies('payment-abuse')
    await riskApi.getStrategy('payment-abuse-main')
    await riskApi.listStrategyVersions('payment-abuse-main')
    await riskApi.pauseStrategy('payment-abuse-main')

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/strategies?sceneKey=payment-abuse')
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/strategies/payment-abuse-main')
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/strategies/payment-abuse-main/versions')
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/strategies/payment-abuse-main/pause', {})
  })

  it('creates strategy drafts and advances governed versions', async () => {
    const command: RiskStrategyCommand = {
      sceneKey: 'payment-abuse',
      strategyKey: 'payment-abuse-main',
      name: 'Payment Abuse Main',
      riskLevel: 'HIGH',
      definitionJson: '{"mode":"ENFORCE","groups":[]}',
    }

    await riskApi.createStrategyDraft(command)
    await riskApi.validateStrategyVersion('payment-abuse-main', 1)
    await riskApi.submitStrategyVersion('payment-abuse-main', 1, { reason: 'ready' })
    await riskApi.approveStrategyVersion('payment-abuse-main', 1, { reason: 'approved' })
    await riskApi.activateStrategyVersion('payment-abuse-main', 1, { reason: 'activate' })
    await riskApi.rollbackStrategy('payment-abuse-main', { targetVersion: 1, reason: 'rollback' })
    await riskApi.diffStrategyVersions('payment-abuse-main', 1, 2)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/strategies', command)
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/strategies/payment-abuse-main/versions/1/validate', {})
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/risk/strategies/payment-abuse-main/versions/1/submit',
      { reason: 'ready' },
    )
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/risk/strategies/payment-abuse-main/versions/1/approve',
      { reason: 'approved' },
    )
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/risk/strategies/payment-abuse-main/versions/1/activate',
      { reason: 'activate' },
    )
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/strategies/payment-abuse-main/rollback', {
      targetVersion: 1,
      reason: 'rollback',
    })
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/strategies/payment-abuse-main/versions/1/diff/2')
  })

  it('evaluates risk decisions', async () => {
    const request: RiskDecisionEvaluateRequest = {
      requestId: 'req-1',
      sceneKey: 'payment-abuse',
      eventTime: '2026-06-08T00:00:00Z',
      subject: { userId: 'user-1' },
      event: { eventType: 'PAYMENT_SUBMITTED' },
      context: { channel: 'APP' },
      features: { paymentAmount: 299 },
      options: { deadlineMs: 50, includeTrace: true, modeOverride: 'SHADOW' },
    }

    await riskApi.evaluateDecision(request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/decisions/evaluate', request)
  })

  it('loads decision traces by scene and limit', async () => {
    await riskApi.listDecisionTraces('payment-abuse', 25)

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/decisions/traces?sceneKey=payment-abuse&limit=25')
  })

  it('creates, loads, and deletes list entries', async () => {
    const list: RiskListCommand = {
      listKey: 'blocked-devices',
      listType: 'BLACK',
      subjectType: 'DEVICE',
      requiresApproval: true,
    }

    await riskApi.listLists()
    await riskApi.createList(list)
    await riskApi.listListEntries('blocked-devices')
    await riskApi.addListEntry('blocked-devices', {
      subjectType: 'DEVICE',
      rawSubject: 'device-1',
      reason: 'manual review',
      source: 'ops',
      effectiveFrom: '2026-06-08T00:00:00Z',
      expiresAt: '2026-06-09T00:00:00Z',
    })
    await riskApi.importListEntries('blocked-devices', {
      entries: [{ subjectType: 'DEVICE', rawSubject: 'device-2', reason: 'batch import', source: 'ops' }],
      replaceExisting: false,
    })
    await riskApi.deleteListEntry('blocked-devices', 42)

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/lists')
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/lists', list)
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/lists/blocked-devices/entries')
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/lists/blocked-devices/entries', {
      subjectType: 'DEVICE',
      rawSubject: 'device-1',
      reason: 'manual review',
      source: 'ops',
      effectiveFrom: '2026-06-08T00:00:00Z',
      expiresAt: '2026-06-09T00:00:00Z',
    })
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/lists/blocked-devices/entries/import', {
      entries: [{ subjectType: 'DEVICE', rawSubject: 'device-2', reason: 'batch import', source: 'ops' }],
      replaceExisting: false,
    })
    expect(mockedHttp.delete).toHaveBeenCalledWith('/canvas/risk/lists/blocked-devices/entries/42')
  })

  it('starts risk lab simulations', async () => {
    const request: RiskSimulationRequest = {
      sceneKey: 'payment-abuse',
      strategyKey: 'payment-abuse-main',
      version: 1,
      sampleWindow: {
        startTime: '2026-06-01T00:00:00Z',
        endTime: '2026-06-08T00:00:00Z',
      },
      sampleLimit: 1000,
    }

    await riskApi.startSimulation(request)
    await riskApi.listSimulations('payment-abuse', 20)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/risk/lab/simulations', request)
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/risk/lab/simulations?sceneKey=payment-abuse&limit=20')
  })
})
