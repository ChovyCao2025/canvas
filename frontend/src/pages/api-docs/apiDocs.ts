export type ApiDocMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
export type ApiDocAuth = 'none' | 'bearer'

export interface ApiDocParam {
  name: string
  in: 'path' | 'query' | 'body'
  required?: boolean
  desc: string
}

export interface ApiDocEndpoint {
  id: string
  title: string
  method: ApiDocMethod
  path: string
  category: string
  summary: string
  auth: ApiDocAuth
  internal?: boolean
  params?: ApiDocParam[]
  requestExample?: unknown
  responseExample?: unknown
}

export interface ApiDocCategory {
  key: string
  title: string
  description: string
}

export interface ApiDocCategorySummary extends ApiDocCategory {
  count: number
}

export interface ApiDocFilter {
  showInternal: boolean
  keyword?: string
  category?: string
}

type EndpointInput = Omit<ApiDocEndpoint, 'id' | 'auth' | 'responseExample'> & {
  auth?: ApiDocAuth
  responseExample?: unknown
}

export const API_DOC_CATEGORIES: ApiDocCategory[] = [
  { key: 'auth', title: '认证', description: '登录、登出和当前用户信息' },
  { key: 'external-trigger', title: '外部触发', description: '业务系统触发画布执行的主要入口' },
  { key: 'approval', title: '审批回调', description: '人工审批节点的通过和拒绝操作' },
  { key: 'canvas', title: '画布管理', description: '画布草稿、发布、版本和灰度管控' },
  { key: 'configuration', title: '配置管理', description: 'API、事件、MQ、标签、人群和实验配置' },
  { key: 'metadata', title: '元数据', description: '节点、上下文字段和下拉选项' },
  { key: 'observability', title: '运行观测', description: '执行记录、轨迹、统计和重放' },
  { key: 'operations', title: '运维与模板', description: '缓存、模板和发布审批工具' },
  { key: 'users', title: '用户管理', description: '后台用户管理接口' },
]

const success = (data: unknown) => ({ code: 0, message: 'success', data })
const page = (list: unknown[]) => success({ total: list.length, list })
const idParam = (name: string, desc: string): ApiDocParam => ({ name, in: 'path', required: true, desc })
const queryParam = (name: string, desc: string): ApiDocParam => ({ name, in: 'query', desc })
const bodyParam = (name: string, desc: string): ApiDocParam => ({ name, in: 'body', required: true, desc })

const pathParams = (path: string): ApiDocParam[] =>
  Array.from(path.matchAll(/\{([^}]+)\}/g), match => idParam(match[1], `${match[1]} 路径参数`))

const endpointId = (method: ApiDocMethod, path: string) =>
  `${method.toLowerCase()}-${path.replace(/[{}]/g, '').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '')}`

const endpoint = ({
  method,
  path,
  category,
  internal,
  params,
  auth,
  responseExample,
  ...rest
}: EndpointInput): ApiDocEndpoint => ({
  id: endpointId(method, path),
  method,
  path,
  category,
  internal,
  auth: auth ?? (path === '/auth/login' ? 'none' : 'bearer'),
  params: [...pathParams(path), ...(params ?? [])],
  responseExample: responseExample ?? success({ id: 'demo_id', status: 'ok' }),
  ...rest,
})

export const API_DOCS: ApiDocEndpoint[] = [
  endpoint({
    title: '账号登录',
    method: 'POST',
    path: '/auth/login',
    category: 'auth',
    summary: '使用账号密码登录后台并换取 Bearer 访问令牌。',
    params: [bodyParam('username', '登录账号'), bodyParam('password', '登录密码')],
    requestExample: { username: 'admin@example.com', password: 'password' },
    responseExample: success({ token: 'eyJhbGciOi...', user: { id: 1, name: '管理员' } }),
  }),
  endpoint({
    title: '退出登录',
    method: 'POST',
    path: '/auth/logout',
    category: 'auth',
    summary: '注销当前登录会话并使令牌失效。',
    responseExample: success({ loggedOut: true }),
  }),
  endpoint({
    title: '当前用户',
    method: 'GET',
    path: '/auth/me',
    category: 'auth',
    summary: '读取当前 Bearer 令牌对应的用户身份和权限。',
    responseExample: success({ id: 1, name: '管理员', roles: ['admin'] }),
  }),
  endpoint({
    title: '上报业务事件',
    method: 'POST',
    path: '/canvas/events/report',
    category: 'external-trigger',
    summary: '业务系统上报事件编码和用户属性，触发匹配画布执行。',
    params: [bodyParam('eventCode', '事件编码'), bodyParam('userId', '业务用户 ID')],
    requestExample: {
      eventCode: 'ORDER_PAID',
      userId: 'user_10001',
      attributes: { orderId: 'ord_202605230001', amount: 199 },
    },
    responseExample: success({ accepted: true, executionId: 'exec_202605230001' }),
  }),
  endpoint({
    title: '触发行为策略',
    method: 'POST',
    path: '/canvas/trigger/behavior',
    category: 'external-trigger',
    summary: '按行为策略类型提交业务行为，触发已发布画布。',
    params: [bodyParam('strategyType', '行为策略类型'), bodyParam('userId', '业务用户 ID')],
    requestExample: { strategyType: 'RETENTION_COUPON', userId: 'user_10001', payload: { scene: 'checkout' } },
    responseExample: success({ accepted: true, matchedCanvasIds: [42] }),
  }),
  endpoint({
    title: '直接执行画布',
    method: 'POST',
    path: '/canvas/execute/direct/{canvasId}',
    category: 'external-trigger',
    summary: '按画布 ID 直接发起一次线上执行，适合业务系统主动调用。',
    params: [bodyParam('userId', '业务用户 ID'), bodyParam('idempotencyKey', '幂等键'), bodyParam('inputParams', '输入参数')],
    requestExample: {
      userId: 'user_10001',
      idempotencyKey: 'idem_202605230001',
      inputParams: { source: 'crm', couponType: 'WELCOME' },
    },
    responseExample: success({ executionId: 'exec_202605230002', status: 'RUNNING' }),
  }),
  endpoint({
    title: '试运行画布',
    method: 'POST',
    path: '/canvas/execute/dry-run/{canvasId}',
    category: 'external-trigger',
    summary: '使用指定输入参数模拟执行画布，不产生真实业务副作用。',
    requestExample: { userId: 'user_10001', inputParams: { source: 'debug' } },
    responseExample: success({ traceId: 'trace_dry_run_001', result: 'PASSED' }),
  }),
  endpoint({
    title: '审批通过',
    method: 'POST',
    path: '/canvas/execution/{executionId}/approve',
    category: 'approval',
    summary: '人工审批节点回调通过结果，推动执行继续流转。',
    params: [bodyParam('comment', '审批意见')],
    requestExample: { comment: '同意发放权益' },
    responseExample: success({ executionId: 'exec_202605230001', approved: true }),
  }),
  endpoint({
    title: '审批拒绝',
    method: 'POST',
    path: '/canvas/execution/{executionId}/reject',
    category: 'approval',
    summary: '人工审批节点回调拒绝结果，终止或转入拒绝分支。',
    params: [bodyParam('reason', '拒绝原因')],
    requestExample: { reason: '用户不满足风控条件' },
    responseExample: success({ executionId: 'exec_202605230001', rejected: true }),
  }),
  endpoint({
    title: '节点类型列表',
    method: 'GET',
    path: '/meta/node-types',
    category: 'metadata',
    summary: '获取前端编排可使用的节点类型。',
    responseExample: success([{ typeKey: 'api-call', title: 'API 调用' }]),
  }),
  endpoint({
    title: '节点类型 Schema',
    method: 'GET',
    path: '/meta/node-types/{typeKey}/schema',
    category: 'metadata',
    summary: '读取指定节点类型的配置表单 Schema。',
    responseExample: success({ typeKey: 'api-call', fields: [{ key: 'apiId', type: 'select' }] }),
  }),
  endpoint({
    title: '上下文字段',
    method: 'GET',
    path: '/meta/context-fields',
    category: 'metadata',
    summary: '获取外部调用和节点编排可引用的上下文字段。',
    responseExample: success([{ key: 'userId', title: '用户 ID', type: 'string' }]),
  }),
  endpoint({
    title: '业务线列表',
    method: 'GET',
    path: '/meta/biz-lines',
    category: 'metadata',
    summary: '获取画布所属业务线下拉选项。',
    responseExample: success([{ key: 'growth', title: '增长业务' }]),
  }),
  endpoint({
    title: '元数据 API 定义',
    method: 'GET',
    path: '/meta/api-definitions',
    category: 'metadata',
    summary: '获取节点配置中可选择的 API 定义。',
    responseExample: success([{ id: 1, name: '发券接口', method: 'POST' }]),
  }),
  endpoint({
    title: '元数据事件定义',
    method: 'GET',
    path: '/meta/event-definitions',
    category: 'metadata',
    summary: '获取可触发画布的事件定义。',
    responseExample: success([{ eventCode: 'ORDER_PAID', title: '订单支付' }]),
  }),
  endpoint({ title: '创建画布', method: 'POST', path: '/canvas', category: 'canvas', internal: true, summary: '创建新的画布草稿。', requestExample: { name: '新用户转化画布', bizLine: 'growth' } }),
  endpoint({ title: '画布详情', method: 'GET', path: '/canvas/{id}', category: 'canvas', internal: true, summary: '读取画布基础信息、节点和连线。' }),
  endpoint({ title: '更新画布', method: 'PUT', path: '/canvas/{id}', category: 'canvas', internal: true, summary: '保存画布草稿内容。', requestExample: { name: '新用户转化画布', nodes: [], edges: [] } }),
  endpoint({ title: '安全更新画布', method: 'PUT', path: '/canvas/{id}/safe', category: 'canvas', internal: true, summary: '带版本校验保存画布，避免覆盖他人编辑。', requestExample: { version: 7, nodes: [], edges: [] } }),
  endpoint({ title: '画布列表', method: 'GET', path: '/canvas/list', category: 'canvas', internal: true, summary: '分页查询后台画布列表。', params: [queryParam('keyword', '画布名称关键字')], responseExample: page([{ id: 42, name: '新用户转化画布', status: 'DRAFT' }]) }),
  endpoint({ title: '发布画布', method: 'POST', path: '/canvas/{id}/publish', category: 'canvas', internal: true, summary: '发布指定画布版本。' }),
  endpoint({ title: '下线画布', method: 'POST', path: '/canvas/{id}/offline', category: 'canvas', internal: true, summary: '将已发布画布下线。' }),
  endpoint({ title: '归档画布', method: 'POST', path: '/canvas/{id}/archive', category: 'canvas', internal: true, summary: '归档不再维护的画布。' }),
  endpoint({ title: '画布版本列表', method: 'GET', path: '/canvas/{id}/versions', category: 'canvas', internal: true, summary: '查看画布历史版本。', responseExample: page([{ versionId: 7, publishedAt: '2026-05-23T09:00:00Z' }]) }),
  endpoint({ title: '画布版本详情', method: 'GET', path: '/canvas/{id}/versions/{versionId}', category: 'canvas', internal: true, summary: '读取指定画布版本内容。' }),
  endpoint({ title: '画布版本 Diff', method: 'GET', path: '/canvas/{id}/versions/{v1}/diff/{v2}', category: 'canvas', internal: true, summary: '比较两个画布版本的节点和配置差异。' }),
  endpoint({ title: '终止画布执行', method: 'POST', path: '/canvas/{id}/kill', category: 'canvas', internal: true, summary: '紧急终止画布的运行中任务。' }),
  endpoint({ title: '回滚到版本', method: 'POST', path: '/canvas/{id}/revert/{versionId}', category: 'canvas', internal: true, summary: '将画布草稿恢复到指定历史版本。' }),
  endpoint({ title: '开启灰度', method: 'POST', path: '/canvas/{id}/canary', category: 'canvas', internal: true, summary: '为画布开启灰度发布。', requestExample: { percent: 10 } }),
  endpoint({ title: '提升灰度', method: 'POST', path: '/canvas/{id}/promote-canary', category: 'canvas', internal: true, summary: '扩大或转正画布灰度流量。', requestExample: { percent: 50 } }),
  endpoint({ title: '回滚灰度', method: 'POST', path: '/canvas/{id}/rollback-canary', category: 'canvas', internal: true, summary: '撤销当前灰度版本。' }),
  endpoint({ title: '发布回滚', method: 'POST', path: '/canvas/{id}/rollback', category: 'canvas', internal: true, summary: '回滚画布线上发布状态。' }),
  endpoint({ title: '克隆画布', method: 'POST', path: '/canvas/{id}/clone', category: 'canvas', internal: true, summary: '复制已有画布为新的草稿。', requestExample: { name: '新用户转化画布副本' } }),
  endpoint({ title: 'API 定义列表', method: 'GET', path: '/canvas/api-definitions', category: 'configuration', internal: true, summary: '分页查询后台 API 配置。', responseExample: page([{ id: 1, name: '发券接口' }]) }),
  endpoint({ title: '创建 API 定义', method: 'POST', path: '/canvas/api-definitions', category: 'configuration', internal: true, summary: '新增节点可调用的 API 配置。', requestExample: { name: '发券接口', method: 'POST', url: 'https://api.example.com/coupons' } }),
  endpoint({
    title: '更新 API 定义',
    method: 'PUT',
    path: '/canvas/api-definitions/{id}',
    category: 'configuration',
    internal: true,
    summary: '更新 API 配置。',
    params: [bodyParam('name', 'API 名称'), bodyParam('method', 'HTTP 方法'), bodyParam('url', '请求地址'), bodyParam('timeoutMs', '超时时间，单位毫秒')],
    requestExample: { name: '发券接口', method: 'POST', url: 'https://api.example.com/coupons', timeoutMs: 3000 },
  }),
  endpoint({ title: '删除 API 定义', method: 'DELETE', path: '/canvas/api-definitions/{id}', category: 'configuration', internal: true, summary: '删除 API 配置。' }),
  endpoint({ title: '事件定义列表', method: 'GET', path: '/canvas/event-definitions', category: 'configuration', internal: true, summary: '分页查询事件配置。', responseExample: page([{ id: 1, eventCode: 'ORDER_PAID' }]) }),
  endpoint({ title: '创建事件定义', method: 'POST', path: '/canvas/event-definitions', category: 'configuration', internal: true, summary: '新增外部事件配置。', requestExample: { eventCode: 'ORDER_PAID', title: '订单支付' } }),
  endpoint({
    title: '更新事件定义',
    method: 'PUT',
    path: '/canvas/event-definitions/{id}',
    category: 'configuration',
    internal: true,
    summary: '更新事件配置。',
    params: [bodyParam('eventCode', '事件编码'), bodyParam('title', '事件名称'), bodyParam('attributeSchema', '事件属性 Schema')],
    requestExample: { eventCode: 'ORDER_PAID', title: '订单支付', attributeSchema: { orderId: 'string', amount: 'number' } },
  }),
  endpoint({ title: '删除事件定义', method: 'DELETE', path: '/canvas/event-definitions/{id}', category: 'configuration', internal: true, summary: '删除事件配置。' }),
  endpoint({ title: 'MQ 定义列表', method: 'GET', path: '/canvas/mq-definitions', category: 'configuration', internal: true, summary: '分页查询 MQ 主题配置。', responseExample: page([{ id: 1, topic: 'order-paid-topic' }]) }),
  endpoint({ title: '创建 MQ 定义', method: 'POST', path: '/canvas/mq-definitions', category: 'configuration', internal: true, summary: '新增 MQ 触发配置。', requestExample: { topic: 'order-paid-topic', consumerGroup: 'canvas-growth' } }),
  endpoint({
    title: '更新 MQ 定义',
    method: 'PUT',
    path: '/canvas/mq-definitions/{id}',
    category: 'configuration',
    internal: true,
    summary: '更新 MQ 触发配置。',
    params: [bodyParam('topic', 'MQ 主题'), bodyParam('consumerGroup', '消费组'), bodyParam('enabled', '是否启用')],
    requestExample: { topic: 'order-paid-topic', consumerGroup: 'canvas-growth', enabled: true },
  }),
  endpoint({ title: '删除 MQ 定义', method: 'DELETE', path: '/canvas/mq-definitions/{id}', category: 'configuration', internal: true, summary: '删除 MQ 触发配置。' }),
  endpoint({ title: '标签定义列表', method: 'GET', path: '/canvas/tag-definitions', category: 'configuration', internal: true, summary: '分页查询用户标签配置。', responseExample: page([{ id: 1, tagKey: 'vip_level' }]) }),
  endpoint({ title: '创建标签定义', method: 'POST', path: '/canvas/tag-definitions', category: 'configuration', internal: true, summary: '新增用户标签配置。', requestExample: { tagKey: 'vip_level', title: '会员等级' } }),
  endpoint({
    title: '更新标签定义',
    method: 'PUT',
    path: '/canvas/tag-definitions/{id}',
    category: 'configuration',
    internal: true,
    summary: '更新用户标签配置。',
    params: [bodyParam('tagKey', '标签键'), bodyParam('title', '标签名称'), bodyParam('valueType', '标签值类型')],
    requestExample: { tagKey: 'vip_level', title: '会员等级', valueType: 'number' },
  }),
  endpoint({ title: '删除标签定义', method: 'DELETE', path: '/canvas/tag-definitions/{id}', category: 'configuration', internal: true, summary: '删除用户标签配置。' }),
  endpoint({ title: '实验列表', method: 'GET', path: '/canvas/ab-experiments', category: 'configuration', internal: true, summary: '分页查询 AB 实验配置。', responseExample: page([{ id: 1, experimentKey: 'checkout_coupon' }]) }),
  endpoint({ title: '创建实验', method: 'POST', path: '/canvas/ab-experiments', category: 'configuration', internal: true, summary: '新增 AB 实验配置。', requestExample: { experimentKey: 'checkout_coupon', groups: ['A', 'B'] } }),
  endpoint({
    title: '更新实验',
    method: 'PUT',
    path: '/canvas/ab-experiments/{id}',
    category: 'configuration',
    internal: true,
    summary: '更新 AB 实验配置。',
    params: [bodyParam('experimentKey', '实验键'), bodyParam('groups', '实验分组'), bodyParam('trafficPercent', '流量比例')],
    requestExample: { experimentKey: 'checkout_coupon', groups: ['A', 'B'], trafficPercent: 50 },
  }),
  endpoint({ title: '删除实验', method: 'DELETE', path: '/canvas/ab-experiments/{id}', category: 'configuration', internal: true, summary: '删除 AB 实验配置。' }),
  endpoint({ title: '人群列表', method: 'GET', path: '/canvas/audiences', category: 'configuration', internal: true, summary: '分页查询人群配置。', responseExample: page([{ id: 1, name: '高价值用户' }]) }),
  endpoint({ title: '人群详情', method: 'GET', path: '/canvas/audiences/{id}', category: 'configuration', internal: true, summary: '读取人群配置详情。' }),
  endpoint({ title: '就绪人群', method: 'GET', path: '/canvas/audiences/ready', category: 'configuration', internal: true, summary: '查询可用于画布执行的人群。', responseExample: success([{ id: 1, name: '高价值用户' }]) }),
  endpoint({ title: '创建人群', method: 'POST', path: '/canvas/audiences', category: 'configuration', internal: true, summary: '新增人群配置。', requestExample: { name: '高价值用户', condition: 'vip_level >= 3' } }),
  endpoint({
    title: '更新人群',
    method: 'PUT',
    path: '/canvas/audiences/{id}',
    category: 'configuration',
    internal: true,
    summary: '更新人群配置。',
    params: [bodyParam('name', '人群名称'), bodyParam('condition', '圈选条件'), bodyParam('refreshCron', '刷新计划')],
    requestExample: { name: '高价值用户', condition: 'vip_level >= 3', refreshCron: '0 3 * * *' },
  }),
  endpoint({ title: '删除人群', method: 'DELETE', path: '/canvas/audiences/{id}', category: 'configuration', internal: true, summary: '删除人群配置。' }),
  endpoint({ title: '计算人群', method: 'POST', path: '/canvas/audiences/{id}/compute', category: 'configuration', internal: true, summary: '触发人群离线计算任务。' }),
  endpoint({ title: '人群统计', method: 'GET', path: '/canvas/audiences/{id}/stat', category: 'configuration', internal: true, summary: '查询人群规模和计算状态。', responseExample: success({ totalUsers: 12800, status: 'READY' }) }),
  endpoint({ title: 'MQ 主题选项', method: 'GET', path: '/meta/mq-topics', category: 'metadata', internal: true, summary: '获取 MQ 主题下拉选项。', responseExample: success([{ topic: 'order-paid-topic' }]) }),
  endpoint({ title: 'MQ 定义选项', method: 'GET', path: '/meta/mq-definitions', category: 'metadata', internal: true, summary: '获取 MQ 定义下拉选项。' }),
  endpoint({ title: '券类型选项', method: 'GET', path: '/meta/coupon-types', category: 'metadata', internal: true, summary: '获取优惠券类型下拉选项。' }),
  endpoint({ title: '触达场景选项', method: 'GET', path: '/meta/reach-scenes', category: 'metadata', internal: true, summary: '获取消息触达场景下拉选项。' }),
  endpoint({ title: '实验选项', method: 'GET', path: '/meta/ab-experiments', category: 'metadata', internal: true, summary: '获取 AB 实验下拉选项。' }),
  endpoint({ title: '实验分组选项', method: 'GET', path: '/meta/ab-experiments/{key}/groups', category: 'metadata', internal: true, summary: '获取指定 AB 实验的分组选项。' }),
  endpoint({ title: '标签选项', method: 'GET', path: '/meta/tagger-tags', category: 'metadata', internal: true, summary: '获取标签平台标签下拉选项。' }),
  endpoint({ title: '业务线 API 选项', method: 'GET', path: '/meta/biz-lines/{key}/apis', category: 'metadata', internal: true, summary: '获取指定业务线可用 API。' }),
  endpoint({ title: '行为策略类型', method: 'GET', path: '/meta/behavior-strategy-types', category: 'metadata', internal: true, summary: '获取行为触发策略类型。' }),
  endpoint({ title: '消息编码选项', method: 'GET', path: '/meta/message-codes', category: 'metadata', internal: true, summary: '获取消息模板编码下拉选项。' }),
  endpoint({ title: '画布上下文字段', method: 'GET', path: '/meta/canvas-context-fields', category: 'metadata', internal: true, summary: '获取画布运行时上下文字段。' }),
  endpoint({ title: '执行轨迹', method: 'GET', path: '/canvas/{id}/execution/{executionId}/trace', category: 'observability', internal: true, summary: '查询单次执行的节点轨迹。', responseExample: success({ executionId: 'exec_202605230001', nodes: [] }) }),
  endpoint({ title: '执行记录', method: 'GET', path: '/canvas/{id}/executions', category: 'observability', internal: true, summary: '分页查询画布执行记录。', responseExample: page([{ executionId: 'exec_202605230001', status: 'SUCCESS' }]) }),
  endpoint({ title: '画布统计', method: 'GET', path: '/canvas/{id}/stats', category: 'observability', internal: true, summary: '查看画布执行量和成功率。', responseExample: success({ executions: 1200, successRate: 0.98 }) }),
  endpoint({ title: '转化漏斗', method: 'GET', path: '/canvas/{id}/funnel', category: 'observability', internal: true, summary: '查看画布关键节点漏斗数据。' }),
  endpoint({ title: '趋势数据', method: 'GET', path: '/canvas/{id}/trend', category: 'observability', internal: true, summary: '查看画布按时间聚合的运行趋势。' }),
  endpoint({ title: '死信列表', method: 'GET', path: '/canvas/dlq', category: 'observability', internal: true, summary: '分页查询进入死信队列的触发消息。', responseExample: page([{ id: 1, reason: 'handler timeout' }]) }),
  endpoint({ title: '重放死信', method: 'POST', path: '/canvas/dlq/{id}/replay', category: 'observability', internal: true, summary: '重新投递指定死信消息。' }),
  endpoint({ title: '删除死信', method: 'DELETE', path: '/canvas/dlq/{id}', category: 'observability', internal: true, summary: '删除指定死信消息。' }),
  endpoint({ title: '执行请求列表', method: 'GET', path: '/canvas/execution-requests', category: 'observability', internal: true, summary: '分页查询外部触发执行请求。', responseExample: page([{ id: 1, requestId: 'req_202605230001' }]) }),
  endpoint({ title: '重放执行请求', method: 'POST', path: '/canvas/execution-requests/{id}/replay', category: 'observability', internal: true, summary: '重新执行指定外部触发请求。' }),
  endpoint({ title: '清理缓存', method: 'POST', path: '/ops/cache/invalidate/{id}', category: 'operations', internal: true, summary: '按资源 ID 失效后台缓存。' }),
  endpoint({ title: '模板列表', method: 'GET', path: '/canvas/templates', category: 'operations', internal: true, summary: '查询可复用画布模板。', responseExample: page([{ templateId: 1, name: '新用户激活模板' }]) }),
  endpoint({ title: '保存为模板', method: 'POST', path: '/canvas/{id}/save-as-template', category: 'operations', internal: true, summary: '将当前画布保存为模板。', requestExample: { name: '新用户激活模板' } }),
  endpoint({ title: '从模板创建', method: 'POST', path: '/canvas/from-template/{templateId}', category: 'operations', internal: true, summary: '基于模板创建新画布草稿。', requestExample: { name: '模板创建画布' } }),
  endpoint({ title: '待审核发布', method: 'GET', path: '/canvas/pending-reviews', category: 'operations', internal: true, summary: '查询等待审批的发布申请。', responseExample: page([{ id: 1, canvasName: '新用户转化画布' }]) }),
  endpoint({ title: '用户列表', method: 'GET', path: '/admin/users', category: 'users', internal: true, summary: '分页查询后台用户。', params: [queryParam('keyword', '用户姓名或邮箱')], responseExample: page([{ id: 1, name: '管理员', email: 'admin@example.com' }]) }),
  endpoint({ title: '创建用户', method: 'POST', path: '/admin/users', category: 'users', internal: true, summary: '新增后台用户并分配角色。', requestExample: { name: '运营同学', email: 'ops@example.com', roles: ['operator'] } }),
  endpoint({
    title: '更新用户',
    method: 'PUT',
    path: '/admin/users/{id}',
    category: 'users',
    internal: true,
    summary: '更新后台用户资料和角色。',
    params: [bodyParam('name', '用户姓名'), bodyParam('email', '邮箱地址'), bodyParam('roles', '角色列表')],
    requestExample: { name: '运营同学', email: 'ops@example.com', roles: ['operator'] },
  }),
  endpoint({
    title: '禁用用户',
    method: 'PUT',
    path: '/admin/users/{id}/disable',
    category: 'users',
    internal: true,
    summary: '禁用指定后台用户。',
    params: [bodyParam('reason', '禁用原因')],
    requestExample: { reason: '员工离职' },
  }),
]

const categoriesByKey = new Map(API_DOC_CATEGORIES.map(category => [category.key, category]))

export function filterApiDocEndpoints(filter: ApiDocFilter): ApiDocEndpoint[] {
  const keyword = filter.keyword?.trim().toLowerCase()

  return API_DOCS.filter(endpoint => {
    if (!filter.showInternal && endpoint.internal) {
      return false
    }

    if (filter.category && endpoint.category !== filter.category) {
      return false
    }

    if (!keyword) {
      return true
    }

    const category = categoriesByKey.get(endpoint.category)
    return [
      endpoint.title,
      endpoint.path,
      endpoint.method,
      endpoint.summary,
      category?.title,
      category?.description,
    ].some(value => value?.toLowerCase().includes(keyword))
  })
}

export function getApiDocCategorySummaries(endpoints: ApiDocEndpoint[]): ApiDocCategorySummary[] {
  const counts = endpoints.reduce<Map<string, number>>((nextCounts, endpoint) => {
    nextCounts.set(endpoint.category, (nextCounts.get(endpoint.category) ?? 0) + 1)
    return nextCounts
  }, new Map())

  return API_DOC_CATEGORIES.map(category => ({
    ...category,
    count: counts.get(category.key) ?? 0,
  })).filter(summary => summary.count > 0)
}

export function formatJsonExample(value: unknown): string {
  return JSON.stringify(value, null, 2) ?? String(value)
}
