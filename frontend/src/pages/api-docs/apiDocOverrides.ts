import type { ApiDocAuth, ApiDocMethod, ApiDocParam } from './apiDocs'

export interface ApiDocOverride {
  title?: string
  category?: string
  summary?: string
  auth?: ApiDocAuth
  internal?: boolean
  params?: ApiDocParam[]
  requestExample?: unknown
  responseExample?: unknown
}

export const success = (data: unknown) => ({ code: 0, message: 'success', data })
export const page = (list: unknown[]) => success({ total: list.length, list })

export const bodyParam = (name: string, desc: string): ApiDocParam => ({
  name,
  in: 'body',
  required: true,
  desc,
})

export const pathParam = (name: string, desc: string): ApiDocParam => ({
  name,
  in: 'path',
  required: true,
  desc,
})

export const overrideKey = (method: ApiDocMethod, path: string) => `${method} ${path}`

export const API_DOC_OVERRIDES: Record<string, ApiDocOverride> = {
  [overrideKey('POST', '/auth/login')]: {
    title: '账号登录',
    category: 'auth',
    summary: '使用账号密码登录后台并换取 Bearer 访问令牌。',
    auth: 'none',
    internal: false,
    params: [bodyParam('username', '登录账号'), bodyParam('password', '登录密码')],
    requestExample: { username: 'admin@example.com', password: 'password' },
    responseExample: success({
      token: 'eyJhbGciOi...',
      user: { id: 1, name: '管理员', roles: ['admin'] },
    }),
  },
  [overrideKey('POST', '/auth/logout')]: {
    title: '退出登录',
    category: 'auth',
    summary: '注销当前登录会话。',
    internal: false,
    responseExample: success(null),
  },
  [overrideKey('GET', '/auth/me')]: {
    title: '当前用户',
    category: 'auth',
    summary: '读取当前 Bearer 令牌对应的用户身份和权限。',
    internal: false,
    responseExample: success({
      id: 1,
      name: '管理员',
      email: 'admin@example.com',
      roles: ['admin'],
      permissions: ['canvas:read', 'canvas:write'],
    }),
  },
  [overrideKey('POST', '/canvas/events/report')]: {
    title: '上报业务事件',
    category: 'external-trigger',
    summary: '业务系统上报事件编码和用户属性，触发匹配画布执行。',
    internal: false,
    params: [bodyParam('eventCode', '事件编码'), bodyParam('userId', '业务用户 ID')],
    requestExample: {
      eventCode: 'ORDER_PAID',
      userId: 'user_10001',
      attributes: { orderId: 'ord_202605230001', amount: 199 },
    },
    responseExample: success({ accepted: true, executionId: 'exec_202605230001' }),
  },
  [overrideKey('POST', '/canvas/trigger/behavior')]: {
    title: '触发行为策略',
    category: 'external-trigger',
    internal: false,
    params: [bodyParam('strategyType', '行为策略类型'), bodyParam('userId', '业务用户 ID')],
    requestExample: {
      strategyType: 'RETENTION_COUPON',
      userId: 'user_10001',
      payload: { scene: 'checkout' },
    },
    responseExample: success({ accepted: true, matchedCanvasIds: [42] }),
  },
  [overrideKey('POST', '/canvas/execute/direct/{canvasId}')]: {
    title: '直接执行画布',
    category: 'external-trigger',
    internal: false,
    params: [
      pathParam('canvasId', '画布 ID'),
      bodyParam('userId', '业务用户 ID'),
      bodyParam('idempotencyKey', '幂等键'),
      bodyParam('inputParams', '输入参数'),
    ],
    requestExample: {
      userId: 'user_10001',
      idempotencyKey: 'idem_202605230001',
      inputParams: { source: 'crm', couponType: 'WELCOME' },
    },
    responseExample: success({ executionId: 'exec_202605230002', status: 'RUNNING' }),
  },
  [overrideKey('POST', '/canvas/execute/dry-run/{canvasId}')]: {
    title: '试运行画布',
    category: 'external-trigger',
    internal: false,
    params: [
      pathParam('canvasId', '画布 ID'),
      bodyParam('userId', '业务用户 ID'),
      bodyParam('inputParams', '输入参数'),
    ],
    requestExample: { userId: 'user_10001', inputParams: { source: 'debug' } },
    responseExample: success({ traceId: 'trace_dry_run_001', result: 'PASSED' }),
  },
  [overrideKey('POST', '/canvas/execution/{executionId}/approve')]: {
    title: '审批通过',
    category: 'approval',
    internal: false,
    params: [pathParam('executionId', '执行记录 ID'), bodyParam('comment', '审批意见')],
    requestExample: { comment: '同意发放权益' },
    responseExample: success({ executionId: 'exec_202605230001', approved: true }),
  },
  [overrideKey('POST', '/canvas/execution/{executionId}/reject')]: {
    title: '审批拒绝',
    category: 'approval',
    internal: false,
    params: [pathParam('executionId', '执行记录 ID'), bodyParam('reason', '拒绝原因')],
    requestExample: { reason: '用户不满足风控条件' },
    responseExample: success({ executionId: 'exec_202605230001', rejected: true }),
  },
  [overrideKey('GET', '/meta/node-types')]: {
    title: '节点类型列表',
    category: 'metadata',
    summary: '获取前端编排可使用的节点类型。',
    responseExample: success([{ typeKey: 'api-call', title: 'API 调用' }]),
  },
  [overrideKey('GET', '/meta/context-fields')]: {
    title: '上下文字段',
    category: 'metadata',
    summary: '获取外部调用和节点编排可引用的上下文字段。',
    responseExample: success([{ key: 'userId', title: '用户 ID', type: 'string' }]),
  },
  [overrideKey('GET', '/canvas/api-definitions')]: {
    title: 'API 定义列表',
    category: 'configuration',
    summary: '分页查询后台 API 配置。',
    internal: true,
    responseExample: page([{ id: 1, name: '发券接口' }]),
  },
  [overrideKey('GET', '/canvas/dlq')]: {
    title: '死信列表',
    category: 'observability',
    summary: '分页查询进入死信队列的触发消息。',
    internal: true,
    responseExample: page([{ id: 1, reason: 'handler timeout' }]),
  },
  [overrideKey('GET', '/admin/users')]: {
    title: '用户列表',
    category: 'users',
    summary: '分页查询后台用户。',
    internal: true,
    responseExample: page([{ id: 1, name: '管理员', email: 'admin@example.com' }]),
  },
}
