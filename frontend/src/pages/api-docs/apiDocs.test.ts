/**
 * 测试职责：验证内置 API 文档数据的过滤、分类统计、格式化和 ID 唯一性。
 *
 * 维护说明：新增 API 文档条目时，这些测试能防止分类遗漏和 endpoint id 冲突。
 */
import { describe, expect, it } from 'vitest'

import {
  API_DOCS,
  API_DOC_CATEGORIES,
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
} from './apiDocs'

describe('api docs data helpers', () => {
  it('contains external and internal endpoints', () => {
    expect(API_DOCS.some(endpoint => !endpoint.internal)).toBe(true)
    expect(API_DOCS.some(endpoint => endpoint.internal)).toBe(true)
  })

  it('hides internal endpoints by default', () => {
    const visible = filterApiDocEndpoints({ showInternal: false })

    expect(visible.every(endpoint => endpoint.internal !== true)).toBe(true)
    expect(visible.map(endpoint => endpoint.path)).toContain('/canvas/events/report')
    expect(visible.map(endpoint => endpoint.path)).not.toContain('/canvas/api-definitions')
  })

  it('reveals internal endpoints when requested', () => {
    const visible = filterApiDocEndpoints({ showInternal: true })

    expect(visible.map(endpoint => endpoint.path)).toContain('/canvas/api-definitions')
    expect(visible.map(endpoint => endpoint.path)).toContain('/admin/users')
  })

  it('filters by path title summary and category title', () => {
    expect(filterApiDocEndpoints({ showInternal: true, keyword: 'events/report' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/events/report'])
    expect(filterApiDocEndpoints({ showInternal: true, keyword: '用户管理' })
      .map(endpoint => endpoint.category)).toContain('users')
  })

  it('builds category summaries from visible endpoints', () => {
    const summaries = getApiDocCategorySummaries(filterApiDocEndpoints({ showInternal: false }))

    expect(summaries[0]).toMatchObject({
      key: API_DOC_CATEGORIES[0].key,
      title: API_DOC_CATEGORIES[0].title,
    })
    expect(summaries.every(summary => summary.count > 0)).toBe(true)
  })

  it('formats JSON examples with two-space indentation', () => {
    expect(formatJsonExample({ code: 0, message: 'success', data: { ok: true } })).toBe(
      '{\n  "code": 0,\n  "message": "success",\n  "data": {\n    "ok": true\n  }\n}',
    )
  })

  it('formats undefined examples with a safe string fallback', () => {
    expect(formatJsonExample(undefined)).toBe('undefined')
  })

  it('does not duplicate endpoint ids', () => {
    const ids = API_DOCS.map(endpoint => endpoint.id)

    expect(new Set(ids).size).toBe(ids.length)
  })
})
