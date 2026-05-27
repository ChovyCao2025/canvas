/**
 * 测试职责：验证内置 API 文档数据的过滤、分类统计、格式化和 ID 唯一性。
 *
 * 维护说明：新增 API 文档条目时，这些测试能防止分类遗漏和 endpoint id 冲突。
 */
import { describe, expect, it } from 'vitest'

import {
  API_DOC_CATEGORIES,
  endpointId,
  filterApiDocEndpoints,
  formatJsonExample,
  getApiDocCategorySummaries,
  type ApiDocEndpoint,
} from './apiDocs'

const endpoints: ApiDocEndpoint[] = [
  {
    id: endpointId('POST', '/canvas/events/report'),
    title: '上报业务事件',
    method: 'POST',
    path: '/canvas/events/report',
    category: 'external-trigger',
    summary: '业务系统上报事件编码和用户属性。',
    auth: 'bearer',
    responseExample: { code: 0, message: 'success', data: { accepted: true } },
  },
  {
    id: endpointId('GET', '/canvas/api-definitions'),
    title: 'API 定义列表',
    method: 'GET',
    path: '/canvas/api-definitions',
    category: 'configuration',
    summary: '分页查询后台 API 配置。',
    auth: 'bearer',
    internal: true,
    responseExample: { code: 0, message: 'success', data: { total: 0, list: [] } },
  },
  {
    id: endpointId('GET', '/admin/users'),
    title: '用户列表',
    method: 'GET',
    path: '/admin/users',
    category: 'users',
    summary: '分页查询后台用户。',
    auth: 'bearer',
    internal: true,
    responseExample: { code: 0, message: 'success', data: [] },
  },
]

describe('api docs data helpers', () => {
  it('hides internal endpoints by default', () => {
    const visible = filterApiDocEndpoints(endpoints, { showInternal: false })

    expect(visible.map(endpoint => endpoint.path)).toEqual(['/canvas/events/report'])
  })

  it('reveals internal endpoints when requested', () => {
    const visible = filterApiDocEndpoints(endpoints, { showInternal: true })

    expect(visible.map(endpoint => endpoint.path)).toEqual([
      '/canvas/events/report',
      '/canvas/api-definitions',
      '/admin/users',
    ])
  })

  it('filters by path title summary and category title', () => {
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, keyword: 'events/report' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/events/report'])
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, keyword: 'API 定义' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/api-definitions'])
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, keyword: '用户属性' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/events/report'])
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, keyword: '用户管理' })
      .map(endpoint => endpoint.path)).toEqual(['/admin/users'])
  })

  it('filters by category', () => {
    expect(filterApiDocEndpoints(endpoints, { showInternal: true, category: 'configuration' })
      .map(endpoint => endpoint.path)).toEqual(['/canvas/api-definitions'])
  })

  it('builds category summaries from visible endpoints', () => {
    const summaries = getApiDocCategorySummaries(filterApiDocEndpoints(endpoints, { showInternal: false }))

    expect(summaries[0]).toMatchObject({
      key: API_DOC_CATEGORIES[1].key,
      title: API_DOC_CATEGORIES[1].title,
      description: API_DOC_CATEGORIES[1].description,
      count: 1,
    })
  })

  it('formats JSON examples with two-space indentation', () => {
    expect(formatJsonExample({ code: 0, message: 'success', data: { ok: true } })).toBe(
      '{\n  "code": 0,\n  "message": "success",\n  "data": {\n    "ok": true\n  }\n}',
    )
  })

  it('formats undefined examples as undefined', () => {
    expect(formatJsonExample(undefined)).toBe('undefined')
  })

  it('builds stable endpoint IDs', () => {
    expect(endpointId('GET', '/canvas/{id}/versions/{versionId}')).toBe('get-canvas-id-versions-versionId')
  })
})
