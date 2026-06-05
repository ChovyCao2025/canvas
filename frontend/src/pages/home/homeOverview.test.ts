/**
 * 测试职责：验证首页概览 KPI、统计范围和关注项展示模型。
 *
 * 维护说明：首页后端概览字段变化时，应同步 HomeOverview 类型和这些展示模型测试。
 */
import { describe, expect, it } from 'vitest'
import {
  buildKpiCards,
  buildRiskSummary,
  filterHomeOverview,
  getAttentionAction,
  getAttentionPresentation,
  HOME_RANGE_OPTIONS,
  sortAttentionItems,
  type HomeOverview,
} from './homeOverview'

describe('homeOverview helpers', () => {
  it('defines the supported homepage ranges', () => {
    expect(HOME_RANGE_OPTIONS).toEqual([
      { label: '今日', value: 1 },
      { label: '近 7 天', value: 7 },
      { label: '近 30 天', value: 30 },
    ])
  })

  it('builds KPI cards from overview summary', () => {
    const cards = buildKpiCards(overview())

    expect(cards.map(card => card.label)).toEqual([
      '已发布旅程',
      '触达用户数',
      '执行成功率',
      '触发次数',
      '执行失败',
    ])
    expect(cards.map(card => card.value)).toEqual(['3', '860', '97.8%', '1,200', '26'])
  })

  it('keeps zero values readable', () => {
    const cards = buildKpiCards({
      ...overview(),
      summary: {
        publishedCanvasCount: 0,
        totalExecutions: 0,
        uniqueUsers: 0,
        successRate: '0%',
        failedExecutions: 0,
      },
    })

    expect(cards.map(card => card.value)).toEqual(['0', '0', '0%', '0', '0'])
  })

  it('maps attention severity to display color', () => {
    expect(getAttentionPresentation('warning')).toEqual({ color: 'orange', label: '关注' })
    expect(getAttentionPresentation('info')).toEqual({ color: 'blue', label: '提示' })
  })

  it('builds risk summary from the highest-priority attention item', () => {
    expect(buildRiskSummary(overviewForRiskSummary())).toEqual({
      healthy: false,
      title: '高失败旅程',
      message: '失败率 4.8%',
      severity: 'error',
      actionLabel: '处理',
      targetCanvasId: 2,
      failedExecutions: '26',
      successRate: '97.8%',
      pendingCount: 3,
    })
  })

  it('builds healthy risk summary when no attention items exist', () => {
    expect(buildRiskSummary(overview())).toEqual({
      healthy: true,
      title: '当前暂无高优先级异常',
      message: '近 7 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现',
      severity: 'success',
      actionLabel: '查看趋势',
      targetCanvasId: null,
      failedExecutions: '26',
      successRate: '97.8%',
      pendingCount: 0,
    })
  })

  it('sorts attention items by severity while preserving backend order within the same severity', () => {
    const sorted = sortAttentionItems(overviewWithAttention().attentionItems)

    expect(sorted.map(item => item.name)).toEqual([
      '高失败旅程',
      '失败待处理旅程',
      '近期未运行旅程',
      '轻量提示旅程',
    ])
  })

  it('filters top canvases and attention items locally without mutating the source overview', () => {
    const source = overviewWithAttention()
    const filtered = filterHomeOverview(source, '  失败  ')

    expect(filtered).not.toBe(source)
    expect(filtered.topCanvases.map(canvas => canvas.name)).toEqual(['高失败旅程'])
    expect(filtered.attentionItems.map(item => item.name)).toEqual(['高失败旅程', '失败待处理旅程'])
    expect(source.topCanvases.map(canvas => canvas.name)).toEqual(['高失败旅程', '稳定旅程'])
    expect(source.attentionItems.map(item => item.name)).toEqual([
      '近期未运行旅程',
      '高失败旅程',
      '失败待处理旅程',
      '轻量提示旅程',
    ])
  })

  it('returns original overview when filtering with an empty keyword', () => {
    const source = overviewWithAttention()

    expect(filterHomeOverview(source, '   ')).toBe(source)
  })

  it('maps attention type to the primary action', () => {
    expect(getAttentionAction({ type: 'NO_RECENT_EXECUTIONS' })).toEqual({ label: '编辑', destination: 'edit' })
    expect(getAttentionAction({ type: 'HIGH_FAILURE_RATE' })).toEqual({ label: '处理', destination: 'stats' })
    expect(getAttentionAction({ type: 'HAS_FAILURES' })).toEqual({ label: '查看', destination: 'stats' })
  })
})

/** 首页概览测试样本，默认给出一组有量级的运营数据。 */
function overview(): HomeOverview {
  return {
    range: { days: 7, since: '2026-05-17', until: '2026-05-23' },
    summary: {
      publishedCanvasCount: 3,
      totalExecutions: 1200,
      uniqueUsers: 860,
      successRate: '97.8%',
      failedExecutions: 26,
    },
    trend: [],
    topCanvases: [],
    attentionItems: [],
  }
}

function overviewWithAttention(): HomeOverview {
  return {
    ...overview(),
    topCanvases: [
      {
        canvasId: 2,
        name: '高失败旅程',
        total: 500,
        uniqueUsers: 320,
        successRate: '95.2%',
        failed: 24,
      },
      {
        canvasId: 4,
        name: '稳定旅程',
        total: 200,
        uniqueUsers: 160,
        successRate: '99.6%',
        failed: 1,
      },
    ],
    attentionItems: [
      {
        canvasId: 3,
        name: '近期未运行旅程',
        type: 'NO_RECENT_EXECUTIONS',
        message: '近 7 天暂无执行',
        severity: 'warning',
      },
      {
        canvasId: 2,
        name: '高失败旅程',
        type: 'HIGH_FAILURE_RATE',
        message: '失败率 4.8%',
        severity: 'error',
      },
      {
        canvasId: 5,
        name: '失败待处理旅程',
        type: 'HAS_FAILURES',
        message: '存在 2 次失败',
        severity: 'error',
      },
      {
        canvasId: 6,
        name: '轻量提示旅程',
        type: 'OTHER',
        message: '建议关注',
        severity: 'info',
      },
    ],
  }
}

function overviewForRiskSummary(): HomeOverview {
  return {
    ...overviewWithAttention(),
    attentionItems: [
      {
        canvasId: 6,
        name: '轻量提示旅程',
        type: 'OTHER',
        message: '建议关注',
        severity: 'info',
      },
      {
        canvasId: 2,
        name: '高失败旅程',
        type: 'HIGH_FAILURE_RATE',
        message: '失败率 4.8%',
        severity: 'error',
      },
      {
        canvasId: 3,
        name: '近期未运行旅程',
        type: 'NO_RECENT_EXECUTIONS',
        message: '近 7 天暂无执行',
        severity: 'warning',
      },
    ],
  }
}
