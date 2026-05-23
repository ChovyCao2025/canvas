import { describe, expect, it } from 'vitest'
import { buildKpiCards, getAttentionPresentation, HOME_RANGE_OPTIONS, type HomeOverview } from './homeOverview'

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
})

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
