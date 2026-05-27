import { describe, expect, it } from 'vitest'

import { overrideKey } from './apiDocOverrides'
import {
  classifyOpenApiPath,
  inferOpenApiInternalFlag,
  parseOpenApiEndpoints,
} from './openApiDocs'

const spec = {
  openapi: '3.0.1',
  paths: {
    '/canvas/events/report': {
      post: {
        summary: 'report event',
        parameters: [{ name: 'source', in: 'query', required: false, description: '来源' }],
        requestBody: { required: true, content: { 'application/json': { schema: { type: 'object' } } } },
        responses: { '200': { description: 'OK' } },
      },
    },
    '/canvas/{id}/versions/{versionId}': {
      get: {
        parameters: [
          { name: 'id', in: 'path', required: true, description: '画布 ID' },
          { name: 'versionId', in: 'path', required: true },
        ],
        responses: { '200': { description: 'OK' } },
      },
    },
    '/admin/users': {
      get: { responses: { '200': { description: 'OK' } } },
    },
    '/canvas/mq-trigger-rejected/{id}': {
      get: { responses: { '200': { description: 'OK' } } },
    },
    '/canvas/data-sources': {
      put: {
        parameters: [{ name: 'keyword', in: 'query', required: false, description: '关键字' }],
        requestBody: { required: true, content: { 'application/json': { schema: { type: 'object' } } } },
        responses: { '200': { description: 'OK' } },
      },
      delete: { responses: { '200': { description: 'OK' } } },
      trace: { responses: { '200': { description: 'ignored' } } },
    },
  },
}

describe('OpenAPI docs adapter', () => {
  it('parses supported OpenAPI operations into endpoints', () => {
    const { endpoints } = parseOpenApiEndpoints(spec)

    expect(endpoints.map(endpoint => `${endpoint.method} ${endpoint.path}`)).toEqual([
      'GET /admin/users',
      'GET /canvas/{id}/versions/{versionId}',
      'PUT /canvas/data-sources',
      'DELETE /canvas/data-sources',
      'POST /canvas/events/report',
      'GET /canvas/mq-trigger-rejected/{id}',
    ])
  })

  it('extracts query path and body params', () => {
    const { endpoints } = parseOpenApiEndpoints(spec)
    const dataSources = endpoints.find(endpoint => endpoint.method === 'PUT' && endpoint.path === '/canvas/data-sources')
    const versions = endpoints.find(endpoint => endpoint.method === 'GET' && endpoint.path === '/canvas/{id}/versions/{versionId}')

    expect(dataSources?.params).toEqual([
      { name: 'keyword', in: 'query', required: false, desc: '关键字' },
      { name: 'body', in: 'body', required: true, desc: 'Request body' },
    ])
    expect(versions?.params).toEqual([
      { name: 'id', in: 'path', required: true, desc: '画布 ID' },
      { name: 'versionId', in: 'path', required: true, desc: 'versionId 路径参数' },
    ])
  })

  it('applies curated overrides', () => {
    const { endpoints } = parseOpenApiEndpoints(spec)
    const endpoint = endpoints.find(endpoint => endpoint.method === 'POST' && endpoint.path === '/canvas/events/report')

    expect(endpoint).toMatchObject({
      title: '上报业务事件',
      category: 'external-trigger',
      internal: false,
      requestExample: {
        eventCode: 'ORDER_PAID',
        userId: 'user_10001',
        attributes: { orderId: 'ord_202605230001', amount: 199 },
      },
    })
  })

  it('classifies known path families', () => {
    expect(classifyOpenApiPath('/auth/login')).toBe('auth')
    expect(classifyOpenApiPath('/canvas/execution/{executionId}/approve')).toBe('approval')
    expect(classifyOpenApiPath('/canvas/mq-trigger-rejected')).toBe('observability')
    expect(classifyOpenApiPath('/canvas/templates')).toBe('operations')
    expect(classifyOpenApiPath('/admin/users')).toBe('users')
    expect(classifyOpenApiPath('/meta/options')).toBe('metadata')
    expect(classifyOpenApiPath('/canvas/list')).toBe('canvas')
  })

  it('infers conservative internal flags', () => {
    expect(inferOpenApiInternalFlag('POST', '/auth/login')).toBe(false)
    expect(inferOpenApiInternalFlag('GET', '/auth/me')).toBe(false)
    expect(inferOpenApiInternalFlag('POST', '/canvas/events/report')).toBe(false)
    expect(inferOpenApiInternalFlag('GET', '/meta/options')).toBe(true)
    expect(inferOpenApiInternalFlag('GET', '/canvas/api-definitions')).toBe(true)
    expect(inferOpenApiInternalFlag('GET', '/cdp/users')).toBe(true)
  })

  it('supports explicit override keys', () => {
    expect(overrideKey('GET', '/admin/users')).toBe('GET /admin/users')
  })

  it('returns a warning for missing paths without crashing', () => {
    expect(parseOpenApiEndpoints({ openapi: '3.0.1' })).toEqual({
      endpoints: [],
      warnings: ['OpenAPI document does not contain a paths object.'],
    })
  })
})
