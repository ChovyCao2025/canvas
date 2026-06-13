import type { Canvas } from '../../types'
import type { CanvasTemplate } from '../../services/api'

export type OfficialTemplateRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface OfficialTemplateTraceStep {
  nodeId: string
  nodeType: string
  outcome: 'MATCHED' | 'SKIPPED' | 'SENT' | 'BLOCKED' | 'APPROVED' | 'ASSIGNED' | 'GENERATED'
  summary: string
}

export interface OfficialTemplateCatalogEntry extends CanvasTemplate {
  key: string
  title: string
  riskLevel: OfficialTemplateRiskLevel
  requiredPlugins: string[]
  canvas: {
    apiVersion: 'canvas/v1'
    kind: 'Journey'
    metadata: {
      name: string
      title: string
    }
    spec: {
      trigger: Record<string, unknown>
      nodes: Array<{
        id: string
        type: string
        label: string
        config?: Record<string, unknown>
      }>
      edges: Array<{
        from: string
        to: string
        when?: string | boolean
      }>
    }
  }
  samplePayload: Record<string, unknown>
  expectedTrace: OfficialTemplateTraceStep[]
  docs: string
}

export type PlaygroundGoldenPathStepId =
  | 'demo-compose-config'
  | 'template-import-draft'
  | 'dry-run-trace'
  | 'dsl-export-cli-validate'
  | 'mock-ai-risk-audit'

export interface PlaygroundGoldenPathStep {
  id: PlaygroundGoldenPathStepId
  title: string
  command: string
  expected: string
  safety: 'frontend-only' | 'draft-only' | 'trace-only' | 'cli-validation-only' | 'mock-provider-preview-only'
}

export interface PlaygroundGoldenPath {
  template: Pick<
    OfficialTemplateCatalogEntry,
    'key' | 'title' | 'requiredPlugins' | 'samplePayload' | 'expectedTrace' | 'docs'
  >
  steps: PlaygroundGoldenPathStep[]
  publishBoundary: 'draft-preview-only'
}

export const officialTemplateCatalog: OfficialTemplateCatalogEntry[] = [
  {
    id: 100001,
    key: 'new-user-welcome',
    name: '新用户欢迎旅程',
    title: '新用户欢迎旅程',
    description: '注册后识别新客，发放首单券并发送欢迎消息。',
    category: 'Lifecycle',
    useCount: 0,
    riskLevel: 'LOW',
    requiredPlugins: ['canvas-plugin-webhook', 'canvas-plugin-coupon', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/new-user-welcome.md',
    canvas: journey('new-user-welcome', '新用户欢迎旅程', { type: 'webhook', event: 'user.registered' }, [
      node('segment', 'condition', '新客判断', { expression: 'user.lifecycleStage == "new"' }),
      node('coupon', 'coupon.grant', '发放首单券', { couponKey: 'WELCOME_10' }),
      node('message', 'message.send', '欢迎短信', { channel: 'sms', template: 'welcome_coupon' }),
    ], [
      edge('segment', 'coupon', true),
      edge('coupon', 'message'),
    ]),
    samplePayload: {
      event: 'user.registered',
      user: { id: 'u_1001', lifecycleStage: 'new', phone: '+8613800000001' },
    },
    expectedTrace: [
      trace('segment', 'condition', 'MATCHED', '用户处于 new 生命周期阶段'),
      trace('coupon', 'coupon.grant', 'SENT', '发放 WELCOME_10 首单券'),
      trace('message', 'message.send', 'SENT', '发送欢迎短信'),
    ],
  },
  {
    id: 100002,
    key: 'dormant-user-winback',
    name: '沉睡用户召回',
    title: '沉睡用户召回',
    description: '识别 30 天未活跃用户，发放召回权益并控制触达频次。',
    category: 'Retention',
    useCount: 0,
    riskLevel: 'MEDIUM',
    requiredPlugins: ['canvas-plugin-schedule', 'canvas-plugin-coupon', 'canvas-plugin-message', 'canvas-plugin-risk'],
    docs: 'docs/open-source/templates/dormant-user-winback.md',
    canvas: journey('dormant-user-winback', '沉睡用户召回', { type: 'schedule', cron: '0 9 * * *' }, [
      node('segment', 'condition', '沉睡分群', { expression: 'user.daysSinceLastActive >= 30' }),
      node('risk', 'risk.check', '频控检查', { policy: 'WINBACK_DAILY_CAP' }),
      node('coupon', 'coupon.grant', '召回券', { couponKey: 'WINBACK_20' }),
      node('message', 'message.send', '召回消息', { channel: 'push', template: 'winback_coupon' }),
    ], [
      edge('segment', 'risk', true),
      edge('risk', 'coupon', 'allowed'),
      edge('coupon', 'message'),
    ]),
    samplePayload: {
      user: { id: 'u_2002', daysSinceLastActive: 45, pushEnabled: true },
      policy: { dailyTouches: 0 },
    },
    expectedTrace: [
      trace('segment', 'condition', 'MATCHED', '用户 45 天未活跃'),
      trace('risk', 'risk.check', 'MATCHED', '通过召回频控'),
      trace('coupon', 'coupon.grant', 'SENT', '发放 WINBACK_20 召回券'),
      trace('message', 'message.send', 'SENT', '发送 Push 召回消息'),
    ],
  },
  {
    id: 100003,
    key: 'coupon-approval-release',
    name: '优惠券审批发布',
    title: '优惠券审批发布',
    description: '对高面额券先发起审批，通过后再发布触达。',
    category: 'Promotion Governance',
    useCount: 0,
    riskLevel: 'HIGH',
    requiredPlugins: ['canvas-plugin-approval', 'canvas-plugin-coupon', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/coupon-approval-release.md',
    canvas: journey('coupon-approval-release', '优惠券审批发布', { type: 'manual', event: 'campaign.release.requested' }, [
      node('approval', 'approval.request', '运营审批', { approvalCode: 'HIGH_VALUE_COUPON' }),
      node('coupon', 'coupon.grant', '发布优惠券', { couponKey: 'VIP_50' }),
      node('message', 'message.send', '发布通知', { channel: 'email', template: 'coupon_release' }),
    ], [
      edge('approval', 'coupon', 'approved'),
      edge('coupon', 'message'),
    ]),
    samplePayload: {
      campaign: { id: 'cmp_3003', couponKey: 'VIP_50', budget: 50000 },
      operator: { id: 'op_1', team: 'growth' },
    },
    expectedTrace: [
      trace('approval', 'approval.request', 'APPROVED', '审批 HIGH_VALUE_COUPON 通过'),
      trace('coupon', 'coupon.grant', 'SENT', '发放 VIP_50 优惠券'),
      trace('message', 'message.send', 'SENT', '发送发布通知邮件'),
    ],
  },
  {
    id: 100004,
    key: 'ai-copy-review-publish',
    name: 'AI 文案生成与人工审核',
    title: 'AI 文案生成与人工审核',
    description: '用 AI 生成营销文案，人工审核通过后再发送。',
    category: 'AI Operations',
    useCount: 0,
    riskLevel: 'MEDIUM',
    requiredPlugins: ['canvas-plugin-ai', 'canvas-plugin-approval', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/ai-copy-review-publish.md',
    canvas: journey('ai-copy-review-publish', 'AI 文案生成与人工审核', { type: 'manual', event: 'copy.requested' }, [
      node('generate', 'ai.generate-copy', '生成文案', { promptKey: 'seasonal_offer' }),
      node('review', 'approval.request', '人工审核', { approvalCode: 'AI_COPY_REVIEW' }),
      node('message', 'message.send', '发送文案', { channel: 'sms', template: 'ai_reviewed_copy' }),
    ], [
      edge('generate', 'review'),
      edge('review', 'message', 'approved'),
    ]),
    samplePayload: {
      campaign: { id: 'cmp_4004', offer: 'summer bundle' },
      audience: { segment: 'recent_buyers' },
    },
    expectedTrace: [
      trace('generate', 'ai.generate-copy', 'GENERATED', '生成活动短信候选文案'),
      trace('review', 'approval.request', 'APPROVED', '人工审核通过'),
      trace('message', 'message.send', 'SENT', '发送审核后的短信'),
    ],
  },
  {
    id: 100005,
    key: 'lead-capture-assignment',
    name: '表单线索分配',
    title: '表单线索分配',
    description: '表单提交后按地区和价值分配给销售跟进。',
    category: 'Lead Management',
    useCount: 0,
    riskLevel: 'LOW',
    requiredPlugins: ['canvas-plugin-webhook', 'canvas-plugin-assignment', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/lead-capture-assignment.md',
    canvas: journey('lead-capture-assignment', '表单线索分配', { type: 'webhook', event: 'lead.submitted' }, [
      node('qualify', 'condition', '线索评分', { expression: 'lead.score >= 60' }),
      node('assign', 'lead.assign', '分配销售', { strategy: 'region_round_robin' }),
      node('notify', 'message.send', '通知销售', { channel: 'workchat', template: 'new_lead_assigned' }),
    ], [
      edge('qualify', 'assign', true),
      edge('assign', 'notify'),
    ]),
    samplePayload: {
      lead: { id: 'lead_5005', score: 82, region: 'east' },
      form: { id: 'trial_request' },
    },
    expectedTrace: [
      trace('qualify', 'condition', 'MATCHED', '线索分数满足分配门槛'),
      trace('assign', 'lead.assign', 'ASSIGNED', '按 east 区域分配销售'),
      trace('notify', 'message.send', 'SENT', '发送销售通知'),
    ],
  },
  {
    id: 100006,
    key: 'birthday-benefit',
    name: '生日权益触达',
    title: '生日权益触达',
    description: '生日当天发放权益并发送祝福消息。',
    category: 'Lifecycle',
    useCount: 0,
    riskLevel: 'LOW',
    requiredPlugins: ['canvas-plugin-schedule', 'canvas-plugin-coupon', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/birthday-benefit.md',
    canvas: journey('birthday-benefit', '生日权益触达', { type: 'schedule', cron: '0 10 * * *' }, [
      node('birthday', 'condition', '生日匹配', { expression: 'user.birthday == today' }),
      node('benefit', 'coupon.grant', '生日权益', { couponKey: 'BIRTHDAY_GIFT' }),
      node('message', 'message.send', '生日祝福', { channel: 'sms', template: 'birthday_benefit' }),
    ], [
      edge('birthday', 'benefit', true),
      edge('benefit', 'message'),
    ]),
    samplePayload: {
      user: { id: 'u_6006', birthday: 'today', phone: '+8613800000006' },
    },
    expectedTrace: [
      trace('birthday', 'condition', 'MATCHED', '用户生日命中当天'),
      trace('benefit', 'coupon.grant', 'SENT', '发放生日权益'),
      trace('message', 'message.send', 'SENT', '发送生日祝福短信'),
    ],
  },
  {
    id: 100007,
    key: 'vip-retention',
    name: '高价值用户维护',
    title: '高价值用户维护',
    description: '识别高价值用户，分配专属权益并通知客户经理。',
    category: 'VIP',
    useCount: 0,
    riskLevel: 'MEDIUM',
    requiredPlugins: ['canvas-plugin-coupon', 'canvas-plugin-assignment', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/vip-retention.md',
    canvas: journey('vip-retention', '高价值用户维护', { type: 'schedule', cron: '0 11 * * MON' }, [
      node('vip', 'condition', 'VIP 判断', { expression: 'user.ltv >= 10000' }),
      node('benefit', 'coupon.grant', '专属权益', { couponKey: 'VIP_SERVICE' }),
      node('owner', 'lead.assign', '客户经理分配', { strategy: 'vip_owner' }),
      node('notify', 'message.send', '经理通知', { channel: 'workchat', template: 'vip_follow_up' }),
    ], [
      edge('vip', 'benefit', true),
      edge('benefit', 'owner'),
      edge('owner', 'notify'),
    ]),
    samplePayload: {
      user: { id: 'u_7007', ltv: 18800, tier: 'diamond' },
    },
    expectedTrace: [
      trace('vip', 'condition', 'MATCHED', '用户 LTV 达到 VIP 门槛'),
      trace('benefit', 'coupon.grant', 'SENT', '发放 VIP_SERVICE 权益'),
      trace('owner', 'lead.assign', 'ASSIGNED', '分配客户经理'),
      trace('notify', 'message.send', 'SENT', '通知客户经理跟进'),
    ],
  },
  {
    id: 100008,
    key: 'ab-message-experiment',
    name: 'A/B 实验触达',
    title: 'A/B 实验触达',
    description: '按实验分流发送不同消息版本，沉淀实验 trace。',
    category: 'Experiment',
    useCount: 0,
    riskLevel: 'MEDIUM',
    requiredPlugins: ['canvas-plugin-experiment', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/ab-message-experiment.md',
    canvas: journey('ab-message-experiment', 'A/B 实验触达', { type: 'manual', event: 'campaign.experiment.started' }, [
      node('split', 'experiment.split', '实验分流', { experimentKey: 'WELCOME_COPY_AB' }),
      node('messageA', 'message.send', 'A 版消息', { channel: 'sms', template: 'welcome_a' }),
      node('messageB', 'message.send', 'B 版消息', { channel: 'sms', template: 'welcome_b' }),
    ], [
      edge('split', 'messageA', 'variant=A'),
      edge('split', 'messageB', 'variant=B'),
    ]),
    samplePayload: {
      user: { id: 'u_8008' },
      experiment: { key: 'WELCOME_COPY_AB', assignedVariant: 'A' },
    },
    expectedTrace: [
      trace('split', 'experiment.split', 'MATCHED', '用户进入 A 版实验组'),
      trace('messageA', 'message.send', 'SENT', '发送 A 版欢迎消息'),
      trace('messageB', 'message.send', 'SKIPPED', 'B 版分支未命中'),
    ],
  },
  {
    id: 100009,
    key: 'risk-blocked-outreach',
    name: '风险触达拦截',
    title: '风险触达拦截',
    description: '触达前先执行风险和合规检查，命中风险时阻断发送。',
    category: 'Risk',
    useCount: 0,
    riskLevel: 'HIGH',
    requiredPlugins: ['canvas-plugin-risk', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/risk-blocked-outreach.md',
    canvas: journey('risk-blocked-outreach', '风险触达拦截', { type: 'webhook', event: 'campaign.outreach.requested' }, [
      node('risk', 'risk.check', '风险检查', { policy: 'OUTREACH_COMPLIANCE' }),
      node('message', 'message.send', '合规触达', { channel: 'sms', template: 'compliant_offer' }),
    ], [
      edge('risk', 'message', 'allowed'),
    ]),
    samplePayload: {
      user: { id: 'u_9009', consent: false },
      campaign: { id: 'cmp_9009', channel: 'sms' },
    },
    expectedTrace: [
      trace('risk', 'risk.check', 'BLOCKED', '用户未授权短信触达'),
      trace('message', 'message.send', 'SKIPPED', '风险阻断后不发送消息'),
    ],
  },
  {
    id: 100010,
    key: 'private-domain-follow-up',
    name: '私域跟进旅程',
    title: '私域跟进旅程',
    description: '用户进入私域后创建跟进任务并同步企微消息。',
    category: 'Private Domain',
    useCount: 0,
    riskLevel: 'MEDIUM',
    requiredPlugins: ['canvas-plugin-webhook', 'canvas-plugin-private-domain', 'canvas-plugin-message'],
    docs: 'docs/open-source/templates/private-domain-follow-up.md',
    canvas: journey('private-domain-follow-up', '私域跟进旅程', { type: 'webhook', event: 'private_domain.member_joined' }, [
      node('tag', 'private-domain.tag', '打私域标签', { tag: 'new_community_member' }),
      node('task', 'private-domain.task', '创建跟进任务', { slaHours: 24 }),
      node('message', 'message.send', '欢迎群消息', { channel: 'workchat', template: 'community_welcome' }),
    ], [
      edge('tag', 'task'),
      edge('task', 'message'),
    ]),
    samplePayload: {
      member: { id: 'pd_1010', source: 'qr_campaign' },
      community: { id: 'group_1' },
    },
    expectedTrace: [
      trace('tag', 'private-domain.tag', 'MATCHED', '写入私域成员标签'),
      trace('task', 'private-domain.task', 'ASSIGNED', '创建 24 小时跟进任务'),
      trace('message', 'message.send', 'SENT', '发送企微欢迎消息'),
    ],
  },
]

export const officialCanvasTemplates: CanvasTemplate[] = officialTemplateCatalog.map(template => ({
  id: template.id,
  name: template.name,
  description: template.description,
  category: template.category,
  useCount: template.useCount,
}))

export function buildTemplateCategoryOptions(templates: CanvasTemplate[]) {
  return Array.from(new Set(templates.map(template => template.category).filter(Boolean) as string[]))
    .sort()
    .map(value => ({ label: value, value }))
}

export function buildTemplateCloneSuccessMessage(canvas: Pick<Canvas, 'id' | 'name'>) {
  return `已从模板创建「${canvas.name}」(ID: ${canvas.id})`
}

export function getPlaygroundGoldenPath(): PlaygroundGoldenPath {
  const template = officialTemplateCatalog.find(item => item.key === 'new-user-welcome')

  if (!template) {
    throw new Error('Missing new-user-welcome official template')
  }

  return {
    template: {
      key: template.key,
      title: template.title,
      requiredPlugins: [...template.requiredPlugins],
      samplePayload: template.samplePayload,
      expectedTrace: [...template.expectedTrace],
      docs: template.docs,
    },
    steps: [
      {
        id: 'demo-compose-config',
        title: 'Validate demo compose wiring',
        command: 'docker compose -f docker-compose.demo.yml config',
        expected: 'Demo dependencies and WireMock mock providers render without starting production providers.',
        safety: 'frontend-only',
      },
      {
        id: 'template-import-draft',
        title: 'Import welcome template as draft',
        command: 'Use the template catalog to clone new-user-welcome into a draft canvas.',
        expected: 'Required plugin checks pass and the cloned canvas remains a draft.',
        safety: 'draft-only',
      },
      {
        id: 'dry-run-trace',
        title: 'Dry-run sample payload and inspect trace',
        command: 'Run the draft with the sample payload and compare the trace to expectedTrace.',
        expected: 'segment, coupon, and message nodes appear in order with no external provider calls.',
        safety: 'trace-only',
      },
      {
        id: 'dsl-export-cli-validate',
        title: 'Export DSL and validate with CLI',
        command: 'cd tools/canvas-cli && node src/index.mjs validate test/fixtures/valid-journey.json',
        expected: 'Current checked-in CLI fixture validates as a Journey with metadata.name new-user-welcome until a dedicated playground example is reserved.',
        safety: 'cli-validation-only',
      },
      {
        id: 'mock-ai-risk-audit',
        title: 'Run mock AI risk audit',
        command: 'curl -X POST http://localhost:8099/mock/ai/audit',
        expected: 'Mock AI returns structured risk findings without publishing or overwriting a canvas.',
        safety: 'mock-provider-preview-only',
      },
    ],
    publishBoundary: 'draft-preview-only',
  }
}

function journey(
  key: string,
  title: string,
  trigger: Record<string, unknown>,
  nodes: OfficialTemplateCatalogEntry['canvas']['spec']['nodes'],
  edges: OfficialTemplateCatalogEntry['canvas']['spec']['edges'],
): OfficialTemplateCatalogEntry['canvas'] {
  return {
    apiVersion: 'canvas/v1',
    kind: 'Journey',
    metadata: { name: key, title },
    spec: { trigger, nodes, edges },
  }
}

function node(
  id: string,
  type: string,
  label: string,
  config?: Record<string, unknown>,
): OfficialTemplateCatalogEntry['canvas']['spec']['nodes'][number] {
  return { id, type, label, config }
}

function edge(
  from: string,
  to: string,
  when?: string | boolean,
): OfficialTemplateCatalogEntry['canvas']['spec']['edges'][number] {
  return { from, to, when }
}

function trace(
  nodeId: string,
  nodeType: string,
  outcome: OfficialTemplateTraceStep['outcome'],
  summary: string,
): OfficialTemplateTraceStep {
  return { nodeId, nodeType, outcome, summary }
}
