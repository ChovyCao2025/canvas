export const PERF_EVENT_CODE = 'PERF_ORDER_PAID'
export const PERF_CANVAS_NAMES = [
  'PERF_DIRECT_LIGHT',
  'PERF_EVENT_LIGHT',
  'PERF_ENGINE_ACCURACY',
]

function node(id, type, name, x, y, config = {}) {
  return {
    id,
    type,
    name,
    category: '压测',
    x,
    y,
    config,
    bizConfig: config,
  }
}

function canvasPayload({ name, description, nodes }) {
  return {
    name,
    description,
    triggerType: 'REALTIME',
    createdBy: 'perf',
    graphJson: JSON.stringify({ nodes }),
  }
}

export function buildDirectLightCanvasPayload() {
  return canvasPayload({
    name: 'PERF_DIRECT_LIGHT',
    description: '本机容量压测：直调轻链路',
    nodes: [
      node('direct', 'DIRECT_CALL', '直调触发', 420, 80, { nextNodeId: 'end' }),
      node('end', 'END', '结束', 420, 240),
    ],
  })
}

export function buildEventLightCanvasPayload(eventCode = PERF_EVENT_CODE) {
  return canvasPayload({
    name: 'PERF_EVENT_LIGHT',
    description: '本机容量压测：事件上报轻链路',
    nodes: [
      node('event', 'EVENT_TRIGGER', '压测事件', 420, 80, {
        eventCode,
        nextNodeId: 'end',
      }),
      node('end', 'END', '结束', 420, 240),
    ],
  })
}

export function buildEngineAccuracyCanvasPayload() {
  return canvasPayload({
    name: 'PERF_ENGINE_ACCURACY',
    description: '本机高并发准确性压测：直调、Groovy、IF 分支、触达副作用、HUB 汇聚',
    nodes: [
      node('direct', 'DIRECT_CALL', '直调触发', 420, 40, {
        inputParams: [
          { name: 'perfRunId', required: true, desc: '压测批次' },
          { name: 'perfInputId', required: true, desc: '输入唯一ID' },
          { name: 'seq', required: true, desc: '序号' },
        ],
        nextNodeId: 'normalize',
      }),
      node('normalize', 'GROOVY', '归一化输入并计算分支', 420, 170, {
        inputParams: [
          { name: 'perfRunId' },
          { name: 'perfInputId' },
          { name: 'seq' },
        ],
        code: [
          'def rawSeq = input.seq',
          "def seq = rawSeq instanceof Number ? rawSeq.longValue() : Long.valueOf(String.valueOf(rawSeq))",
          'def even = seq % 2L == 0L',
          'return [',
          '  seq: seq,',
          '  isEven: even,',
          '  branch: even ? "even" : "odd",',
          '  perfRunId: input.perfRunId,',
          '  perfInputId: input.perfInputId',
          ']',
        ].join('\n'),
        nextNodeId: 'route_even',
      }),
      node('route_even', 'IF_CONDITION', '按 seq 偶奇路由', 420, 300, {
        rules: [{ field: 'isEven', operator: 'EQ', value: 'true', isCustom: true }],
        successNodeId: 'send_even',
        failNodeId: 'send_odd',
      }),
      node('send_even', 'SEND_MESSAGE', '偶数分支触达', 210, 440, {
        channel: 'EMAIL',
        templateId: 'perf-engine-even',
        title: 'perf-engine-even',
        body: 'perf-engine-even',
        variables: {
          branch: 'even',
          perfRunId: '$perfRunId',
          perfInputId: '$perfInputId',
          seq: '$seq',
        },
        nextNodeId: 'join',
      }),
      node('send_odd', 'SEND_MESSAGE', '奇数分支触达', 630, 440, {
        channel: 'PUSH',
        templateId: 'perf-engine-odd',
        title: 'perf-engine-odd',
        body: 'perf-engine-odd',
        variables: {
          branch: 'odd',
          perfRunId: '$perfRunId',
          perfInputId: '$perfInputId',
          seq: '$seq',
        },
        nextNodeId: 'join',
      }),
      node('join', 'HUB', '分支汇聚', 420, 580, {
        timeout: 60,
        nextNodeId: 'end',
      }),
      node('end', 'END', '结束', 420, 720),
    ],
  })
}

export function buildFixtureCanvases() {
  return [
    buildDirectLightCanvasPayload(),
    buildEventLightCanvasPayload(),
    buildEngineAccuracyCanvasPayload(),
  ]
}

export function engineAccuracyTraceVerifierArgs() {
  return [
    '--expect-trace', 'direct:success=all',
    '--expect-trace', 'normalize:success=all',
    '--expect-trace', 'route_even:success=all',
    '--expect-trace', 'send_even:success=even',
    '--expect-trace', 'send_odd:success=odd',
    '--expect-trace', 'send_even:skipped=odd',
    '--expect-trace', 'send_odd:skipped=even',
    '--expect-trace', 'join:success=all',
    '--expect-trace', 'end:success=all',
  ]
}

function eventDefinitionPayload(eventCode = PERF_EVENT_CODE) {
  return {
    name: '压测订单支付事件',
    eventCode,
    attributes: JSON.stringify([
      { name: 'amount', displayName: '金额', type: 'NUMBER', required: false },
      { name: 'perfRunId', displayName: '压测批次', type: 'STRING', required: false },
      { name: 'perfInputId', displayName: '输入ID', type: 'STRING', required: false },
      { name: 'seq', displayName: '序号', type: 'NUMBER', required: false },
    ]),
    description: '本机容量压测事件定义',
    enabled: 1,
    createdBy: 'perf',
  }
}

function apiPath(path) {
  return path.startsWith('/') ? path : `/${path}`
}

export function createHttpClient({ baseUrl }) {
  return {
    async request(method, path, { body, token } = {}) {
      const headers = {
        accept: 'application/json',
      }
      if (body !== undefined) {
        headers['content-type'] = 'application/json'
      }
      if (token) {
        headers.authorization = `Bearer ${token}`
      }
      const response = await fetch(`${baseUrl}${apiPath(path)}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      })
      const text = await response.text()
      const parsed = text ? JSON.parse(text) : {}
      if (!response.ok) {
        throw new Error(`${method} ${path} failed with HTTP ${response.status}: ${text}`)
      }
      return parsed
    },
  }
}

function unwrap(response, operation) {
  if (!response || response.code !== 0) {
    throw new Error(`${operation} failed: ${response?.message || JSON.stringify(response)}`)
  }
  return response.data
}

function pageItems(data) {
  return Array.isArray(data?.list) ? data.list : []
}

async function login(client, env) {
  const username = env.PERF_ADMIN_USERNAME || 'admin'
  const password = env.PERF_ADMIN_PASSWORD || 'Admin@123'
  const data = unwrap(await client.request('POST', '/auth/login', {
    body: { username, password },
  }), 'login')
  if (!data?.token) {
    throw new Error('login failed: response did not include data.token')
  }
  return data.token
}

async function upsertEventDefinition(client, token, eventCode) {
  const page = unwrap(await client.request('GET', '/canvas/event-definitions?page=1&size=200', { token }),
    'list event definitions')
  const existing = pageItems(page).find((item) => item.eventCode === eventCode)
  const payload = eventDefinitionPayload(eventCode)
  if (existing?.id) {
    unwrap(await client.request('PUT', `/canvas/event-definitions/${existing.id}`, { token, body: payload }),
      'update event definition')
    return existing.id
  }
  const created = unwrap(await client.request('POST', '/canvas/event-definitions', { token, body: payload }),
    'create event definition')
  return created.id
}

async function archiveExistingPerfCanvases(client, token) {
  const page = unwrap(await client.request('GET', '/canvas/list?page=1&size=200&name=PERF_', { token }),
    'list perf canvases')
  const canvases = pageItems(page).filter((canvas) => PERF_CANVAS_NAMES.includes(canvas.name))
  for (const canvas of canvases) {
    unwrap(await client.request('POST', `/canvas/${canvas.id}/archive?operator=perf`, { token }),
      `archive ${canvas.name}`)
  }
  return canvases.map((canvas) => canvas.id)
}

async function createAndPublishCanvas(client, token, payload) {
  const canvas = unwrap(await client.request('POST', '/canvas', { token, body: payload }), `create ${payload.name}`)
  if (!canvas?.id) {
    throw new Error(`create ${payload.name} failed: response did not include data.id`)
  }
  const version = unwrap(await client.request('POST', `/canvas/${canvas.id}/publish?operator=perf`, { token }),
    `publish ${payload.name}`)
  return {
    canvasId: canvas.id,
    publishedVersionId: version?.id || null,
  }
}

export async function createPerfFixtures(config, deps = {}) {
  if (!config.rebuild) {
    return {
      status: 'DRY_RUN',
      message: 'fixture command requires --rebuild true to recreate PERF_ resources',
    }
  }

  const env = deps.env || process.env
  const client = deps.client || createHttpClient({ baseUrl: config.baseUrl })
  const token = await login(client, env)
  await upsertEventDefinition(client, token, PERF_EVENT_CODE)
  const archivedCanvasIds = await archiveExistingPerfCanvases(client, token)
  const created = []
  for (const payload of buildFixtureCanvases()) {
    created.push({
      name: payload.name,
      ...(await createAndPublishCanvas(client, token, payload)),
    })
  }

  const byName = new Map(created.map((item) => [item.name, item]))
  return {
    status: 'READY',
    eventCode: PERF_EVENT_CODE,
    matchedCanvasCount: 1,
    directCanvasId: byName.get('PERF_DIRECT_LIGHT')?.canvasId,
    eventCanvasId: byName.get('PERF_EVENT_LIGHT')?.canvasId,
    engineAccuracyCanvasId: byName.get('PERF_ENGINE_ACCURACY')?.canvasId,
    archivedCanvasIds,
    created,
    engineAccuracyVerifierArgs: engineAccuracyTraceVerifierArgs(),
  }
}
