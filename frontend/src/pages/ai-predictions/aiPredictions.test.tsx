/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { aiPredictionApi } from '../../services/aiPredictionApi'
import AiPredictionsPage from './index'
import {
  distributionPercent,
  distributionTotal,
  formatDateTime,
  formatProbability,
  orderedDistribution,
  riskBandColor,
  riskBandLabel,
  runStatusColor,
} from './aiPredictions'

vi.mock('../../services/aiPredictionApi', async importOriginal => {
  const actual = await importOriginal<typeof import('../../services/aiPredictionApi')>()
  return {
    ...actual,
    aiPredictionApi: {
      latestRun: vi.fn(),
      readiness: vi.fn(),
      churnDistribution: vi.fn(),
      topRiskUsers: vi.fn(),
      recompute: vi.fn(),
    },
  }
})

describe('aiPredictions presentation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    stubBrowserLayout()
    vi.mocked(aiPredictionApi.latestRun).mockResolvedValue({ code: 0, message: 'success', data: null })
    vi.mocked(aiPredictionApi.readiness).mockResolvedValue({
      code: 0,
      message: 'success',
      data: readinessView(true),
    })
    vi.mocked(aiPredictionApi.churnDistribution).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(aiPredictionApi.topRiskUsers).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(aiPredictionApi.recompute).mockResolvedValue({
      code: 0,
      message: 'success',
      data: runView('SUCCESS'),
    })
  })

  it('formats risk bands status and probability', () => {
    expect(riskBandLabel('HIGH')).toBe('高风险')
    expect(riskBandColor('MEDIUM')).toBe('orange')
    expect(runStatusColor('SUCCESS')).toBe('green')
    expect(formatProbability('0.81234')).toBe('81.2%')
    expect(formatDateTime('2026-06-04T10:30:00')).toBe('2026-06-04 10:30:00')
  })

  it('orders distribution and computes percentages', () => {
    const ordered = orderedDistribution([
      { band: 'LOW', count: 2 },
      { band: 'HIGH', count: 3 },
    ])

    expect(ordered.map(item => item.band)).toEqual(['HIGH', 'MEDIUM', 'LOW'])
    expect(distributionTotal(ordered)).toBe(5)
    expect(distributionPercent(3, 5)).toBe(60)
  })

  it('loads latest run distribution and top-risk users', async () => {
    vi.mocked(aiPredictionApi.latestRun).mockResolvedValue({ code: 0, message: 'success', data: runView('SUCCESS') })
    vi.mocked(aiPredictionApi.churnDistribution).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [
        { band: 'HIGH', count: 2 },
        { band: 'MEDIUM', count: 1 },
        { band: 'LOW', count: 3 },
      ],
    })
    vi.mocked(aiPredictionApi.topRiskUsers).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [{
        userId: 'u-risk-1',
        churnProbability: '0.81234',
        churnRiskBand: 'HIGH',
        bestSendHour: 20,
        confidence: '0.80000',
      }],
    })

    render(<AiPredictionsPage />)

    expect(await screen.findByText('baseline_v1')).toBeInTheDocument()
    expect(screen.getByText('u-risk-1')).toBeInTheDocument()
    expect(screen.getByText('81.2%')).toBeInTheDocument()
    expect(screen.getByText('20')).toBeInTheDocument()
    expect(screen.getAllByText('高风险').length).toBeGreaterThan(0)
  })

  it('renders an empty state when no run has produced snapshots', async () => {
    render(<AiPredictionsPage />)

    expect(await screen.findByText('暂无预测结果')).toBeInTheDocument()
  })

  it('disables recompute while the latest run is running', async () => {
    vi.mocked(aiPredictionApi.latestRun).mockResolvedValue({ code: 0, message: 'success', data: runView('RUNNING') })

    render(<AiPredictionsPage />)

    expect(await screen.findByText('RUNNING')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /重新计算/ })).toBeDisabled()
  })

  it('disables recompute when prediction readiness is manually held out', async () => {
    vi.mocked(aiPredictionApi.readiness).mockResolvedValue({
      code: 0,
      message: 'success',
      data: readinessView(false),
    })

    render(<AiPredictionsPage />)

    expect(await screen.findByText('预测重算暂未启用')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /重新计算/ })).toBeDisabled()
    expect(aiPredictionApi.recompute).not.toHaveBeenCalled()
  })

  it('recomputes predictions and reloads page data', async () => {
    vi.mocked(aiPredictionApi.latestRun)
      .mockResolvedValueOnce({ code: 0, message: 'success', data: null })
      .mockResolvedValueOnce({ code: 0, message: 'success', data: runView('SUCCESS') })

    render(<AiPredictionsPage />)
    expect(await screen.findByText('暂无预测结果')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /重新计算/ }))

    await waitFor(() => expect(aiPredictionApi.recompute).toHaveBeenCalledWith({ force: false, limit: 100 }))
    await waitFor(() => expect(aiPredictionApi.latestRun).toHaveBeenCalledTimes(2))
  })

  it('shows a retryable error when prediction data loading fails', async () => {
    vi.mocked(aiPredictionApi.latestRun).mockRejectedValueOnce(new Error('network down'))

    render(<AiPredictionsPage />)

    expect(await screen.findByText('预测数据加载失败')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /刷新/ }))

    await waitFor(() => expect(aiPredictionApi.latestRun).toHaveBeenCalledTimes(2))
  })
})

function runView(status: string) {
  return {
    id: 1,
    tenantId: 7,
    modelKey: 'churn_prediction',
    modelVersion: 'baseline_v1',
    runDate: '2026-06-04',
    status,
    processedCount: 6,
    skippedCount: 0,
    failedCount: 0,
    startedAt: '2026-06-04T10:30:00',
    finishedAt: status === 'RUNNING' ? null : '2026-06-04T10:31:00',
    errorMessage: null,
  }
}

function readinessView(recomputeEnabled: boolean) {
  return {
    recomputeEnabled,
    disabledReason: recomputeEnabled ? null : 'canvas.ai.prediction.enabled must be true to recompute predictions',
    modelVersion: 'baseline_v1',
    batchSize: 500,
  }
}

function stubBrowserLayout() {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver
}
