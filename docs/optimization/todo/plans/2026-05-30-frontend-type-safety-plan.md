# Frontend Type Safety — Zod Plan

**Goal:** Add Zod runtime validation for API responses. Remove `[key: string]: unknown` index signatures. Use discriminated unions for node type variants so typos like `sucessNodeId` are caught at compile time and backend schema changes surface explicit runtime errors.

**Architecture:** Each canvas node type gets a strict Zod schema with only its valid config fields. A top-level `BizConfigSchema` is a Zod discriminated union on `nodeType`. API response validation uses an interceptor that looks up endpoint-specific schemas from a `responseSchemaMap` and calls `safeParse` — logging parse errors and throwing on invalid data.

**Tech Stack:** Zod, React, axios, vitest

---

### Task 1: Define Zod Schemas for Canvas Node Types

**Files:**
- Modify: `frontend/src/types/canvas.ts`
- Create: `frontend/src/types/canvasSchemas.ts`
- Test: `frontend/src/types/canvasSchemas.test.ts`

- [ ] **Step 1: Install Zod dependency**

```bash
cd /Users/photonpay/project/canvas/frontend && npm install zod
```

- [ ] **Step 2: Write failing test — each node type schema validates correct data and rejects invalid data**

Create `frontend/src/types/canvasSchemas.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  StartBizConfigSchema,
  EventTriggerBizConfigSchema,
  MqTriggerBizConfigSchema,
  ScheduledTriggerBizConfigSchema,
  DirectCallBizConfigSchema,
  AudienceTriggerBizConfigSchema,
  IfConditionBizConfigSchema,
  SelectorBizConfigSchema,
  AggregateBizConfigSchema,
  ThresholdBizConfigSchema,
  TaggerBizConfigSchema,
  PriorityBizConfigSchema,
  AbSplitBizConfigSchema,
  RandomSplitBizConfigSchema,
  ExperimentBizConfigSchema,
  ScoringBizConfigSchema,
  ManualApprovalBizConfigSchema,
  DelayBizConfigSchema,
  WaitBizConfigSchema,
  GotoBizConfigSchema,
  LoopBizConfigSchema,
  MergeBizConfigSchema,
  HubBizConfigSchema,
  EndBizConfigSchema,
  DirectReturnBizConfigSchema,
  CouponBizConfigSchema,
  CommitActionBizConfigSchema,
  ApiCallBizConfigSchema,
  GroovyBizConfigSchema,
  SendMqBizConfigSchema,
  InAppNotifyBizConfigSchema,
  SendSmsBizConfigSchema,
  SendPushBizConfigSchema,
  SendEmailBizConfigSchema,
  SendWechatBizConfigSchema,
  SendInAppBizConfigSchema,
  PointsOperationBizConfigSchema,
  UpdateProfileBizConfigSchema,
  TagOperationBizConfigSchema,
  TrackEventBizConfigSchema,
  CreateTaskBizConfigSchema,
  GoalCheckBizConfigSchema,
  SuppressionCheckBizConfigSchema,
  QuietHoursBizConfigSchema,
  ChannelAvailabilityBizConfigSchema,
  FrequencyCapBizConfigSchema,
  CanvasTriggerBizConfigSchema,
  SubFlowRefBizConfigSchema,
  TransferJourneyBizConfigSchema,
  GroupBizConfigSchema,
  TemplateNodeBizConfigSchema,
  RecommendationBizConfigSchema,
  AiNextBestActionBizConfigSchema,
  LogicRelationBizConfigSchema,
  CdpTagWriteBizConfigSchema,
  ReachPlatformBizConfigSchema,
  BizConfigSchema,
} from './canvasSchemas'

describe('START schema', () => {
  it('accepts valid config with branches', () => {
    const result = StartBizConfigSchema.safeParse({
      nodeType: 'START',
      nextNodeId: 'a',
      branches: [{ label: 'Branch 1', nextNodeId: 'b' }],
    })
    expect(result.success).toBe(true)
  })
  it('accepts empty config', () => {
    const result = StartBizConfigSchema.safeParse({ nodeType: 'START' })
    expect(result.success).toBe(true)
  })
  it('rejects unknown fields', () => {
    const result = StartBizConfigSchema.safeParse({
      nodeType: 'START',
      sucsessNodeId: 'x',  // typo
    })
    expect(result.success).toBe(false)
  })
})

describe('EVENT_TRIGGER schema', () => {
  it('accepts valid config', () => {
    const result = EventTriggerBizConfigSchema.safeParse({
      nodeType: 'EVENT_TRIGGER',
      eventCode: 'user_login',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
  it('rejects missing eventCode', () => {
    const result = EventTriggerBizConfigSchema.safeParse({
      nodeType: 'EVENT_TRIGGER',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
  it('rejects unknown field', () => {
    const result = EventTriggerBizConfigSchema.safeParse({
      nodeType: 'EVENT_TRIGGER',
      eventCode: 'login',
      sucessNodeId: 'x',
    })
    expect(result.success).toBe(false)
  })
})

describe('MQ_TRIGGER schema', () => {
  it('accepts valid config', () => {
    const result = MqTriggerBizConfigSchema.safeParse({
      nodeType: 'MQ_TRIGGER',
      topicKey: 'order_created',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
  it('rejects missing topicKey', () => {
    const result = MqTriggerBizConfigSchema.safeParse({
      nodeType: 'MQ_TRIGGER',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
})

describe('SCHEDULED_TRIGGER schema', () => {
  it('accepts CRON schedule', () => {
    const result = ScheduledTriggerBizConfigSchema.safeParse({
      nodeType: 'SCHEDULED_TRIGGER',
      scheduleType: 'CRON',
      cronExpression: '0 0 * * *',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
  it('accepts ONCE schedule', () => {
    const result = ScheduledTriggerBizConfigSchema.safeParse({
      nodeType: 'SCHEDULED_TRIGGER',
      scheduleType: 'ONCE',
      triggerTime: '2026-06-01T00:00:00',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
  it('rejects invalid scheduleType', () => {
    const result = ScheduledTriggerBizConfigSchema.safeParse({
      nodeType: 'SCHEDULED_TRIGGER',
      scheduleType: 'INVALID',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
})

describe('DIRECT_CALL schema', () => {
  it('accepts valid config with branches', () => {
    const result = DirectCallBizConfigSchema.safeParse({
      nodeType: 'DIRECT_CALL',
      branches: [{ label: '渠道 A', nextNodeId: 'api_a' }],
    })
    expect(result.success).toBe(true)
  })
  it('accepts empty config', () => {
    const result = DirectCallBizConfigSchema.safeParse({ nodeType: 'DIRECT_CALL' })
    expect(result.success).toBe(true)
  })
})

describe('AUDIENCE_TRIGGER schema', () => {
  it('accepts valid config', () => {
    const result = AudienceTriggerBizConfigSchema.safeParse({
      nodeType: 'AUDIENCE_TRIGGER',
      audienceId: 'aud-001',
      hitNextNodeId: 'a',
      missNextNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('IF_CONDITION schema', () => {
  it('accepts valid config with success and fail branches', () => {
    const result = IfConditionBizConfigSchema.safeParse({
      nodeType: 'IF_CONDITION',
      rules: [{ field: 'age', operator: 'GT', value: '18', isCustom: false }],
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('rejects typo field sucessNodeId', () => {
    const result = IfConditionBizConfigSchema.safeParse({
      nodeType: 'IF_CONDITION',
      rules: [],
      sucessNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(false)
  })
})

describe('SELECTOR schema', () => {
  it('accepts valid config with branches and elseNodeId', () => {
    const result = SelectorBizConfigSchema.safeParse({
      nodeType: 'SELECTOR',
      branches: [
        { label: 'If', strategyRelation: 'AND', conditions: [], nextNodeId: 'a' },
      ],
      elseNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('AGGREGATE schema', () => {
  it('accepts count mode', () => {
    const result = AggregateBizConfigSchema.safeParse({
      nodeType: 'AGGREGATE',
      evaluateMode: 'count',
      minCount: 3,
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('accepts rate mode', () => {
    const result = AggregateBizConfigSchema.safeParse({
      nodeType: 'AGGREGATE',
      evaluateMode: 'rate',
      minRate: 0.8,
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('accepts script mode', () => {
    const result = AggregateBizConfigSchema.safeParse({
      nodeType: 'AGGREGATE',
      evaluateMode: 'script',
      evaluateScript: 'return true',
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('THRESHOLD schema', () => {
  it('accepts valid config', () => {
    const result = ThresholdBizConfigSchema.safeParse({
      nodeType: 'THRESHOLD',
      thresholdMode: 'min_success',
      threshold: 5,
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('TAGGER schema', () => {
  it('accepts audience mode', () => {
    const result = TaggerBizConfigSchema.safeParse({
      nodeType: 'TAGGER',
      mode: 'audience',
      hitNextNodeId: 'a',
      missNextNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('accepts realtime mode', () => {
    const result = TaggerBizConfigSchema.safeParse({
      nodeType: 'TAGGER',
      mode: 'realtime',
      tagCode: 'vip_level',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
  it('accepts offline mode', () => {
    const result = TaggerBizConfigSchema.safeParse({
      nodeType: 'TAGGER',
      mode: 'offline',
      tagCode: 'last_purchase_date',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('PRIORITY schema', () => {
  it('accepts valid config', () => {
    const result = PriorityBizConfigSchema.safeParse({
      nodeType: 'PRIORITY',
      priorities: [{ order: 1, nextNodeId: 'a' }],
    })
    expect(result.success).toBe(true)
  })
})

describe('AB_SPLIT schema', () => {
  it('accepts valid config with groups', () => {
    const result = AbSplitBizConfigSchema.safeParse({
      nodeType: 'AB_SPLIT',
      groups: [
        { groupKey: 'A', nextNodeId: 'p' },
        { groupKey: 'B', nextNodeId: 'q' },
      ],
    })
    expect(result.success).toBe(true)
  })
})

describe('RANDOM_SPLIT schema', () => {
  it('accepts valid config with paths', () => {
    const result = RandomSplitBizConfigSchema.safeParse({
      nodeType: 'RANDOM_SPLIT',
      allocationStrategy: 'CONSISTENT',
      paths: [
        { pathId: 'path_a', label: '路径 A', weight: 50, nextNodeId: 'x' },
        { pathId: 'path_b', label: '路径 B', weight: 50, nextNodeId: 'y' },
      ],
    })
    expect(result.success).toBe(true)
  })
})

describe('EXPERIMENT schema', () => {
  it('accepts valid config with variants', () => {
    const result = ExperimentBizConfigSchema.safeParse({
      nodeType: 'EXPERIMENT',
      experimentKey: 'exp_1',
      allocationStrategy: 'CONSISTENT',
      variants: [
        { variantId: 'A', label: '方案 A', weight: 50, isControl: true, nextNodeId: 'x' },
        { variantId: 'B', label: '方案 B', weight: 50, nextNodeId: 'y' },
      ],
    })
    expect(result.success).toBe(true)
  })
})

describe('SCORING schema', () => {
  it('accepts valid config with bands', () => {
    const result = ScoringBizConfigSchema.safeParse({
      nodeType: 'SCORING',
      rules: [],
      bands: [
        { bandId: 'high', label: '高分', min: 80, max: 2147483647, nextNodeId: 'a' },
        { bandId: 'low', label: '低分', min: -2147483648, max: 79, nextNodeId: 'b' },
      ],
    })
    expect(result.success).toBe(true)
  })
})

describe('MANUAL_APPROVAL schema', () => {
  it('accepts valid config', () => {
    const result = ManualApprovalBizConfigSchema.safeParse({
      nodeType: 'MANUAL_APPROVAL',
      approveNodeId: 'a',
      rejectNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('DELAY schema', () => {
  it('accepts valid config', () => {
    const result = DelayBizConfigSchema.safeParse({
      nodeType: 'DELAY',
      delaySeconds: 3600,
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('WAIT schema', () => {
  it('accepts valid config', () => {
    const result = WaitBizConfigSchema.safeParse({
      nodeType: 'WAIT',
      waitUntil: '2026-06-15T00:00:00',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('GOTO schema', () => {
  it('accepts valid config', () => {
    const result = GotoBizConfigSchema.safeParse({
      nodeType: 'GOTO',
      targetNodeId: 'dest',
    })
    expect(result.success).toBe(true)
  })
  it('rejects unknown field', () => {
    const result = GotoBizConfigSchema.safeParse({
      nodeType: 'GOTO',
      targetNodeId: 'dest',
      extraField: 'oops',
    })
    expect(result.success).toBe(false)
  })
})

describe('LOOP schema', () => {
  it('accepts valid config', () => {
    const result = LoopBizConfigSchema.safeParse({
      nodeType: 'LOOP',
      loopStartNodeId: 'inner',
      maxExceededNodeId: 'fallback',
    })
    expect(result.success).toBe(true)
  })
})

describe('MERGE schema', () => {
  it('accepts empty config', () => {
    const result = MergeBizConfigSchema.safeParse({ nodeType: 'MERGE' })
    expect(result.success).toBe(true)
  })
})

describe('HUB schema', () => {
  it('accepts empty config', () => {
    const result = HubBizConfigSchema.safeParse({ nodeType: 'HUB' })
    expect(result.success).toBe(true)
  })
})

describe('END schema', () => {
  it('accepts empty config', () => {
    const result = EndBizConfigSchema.safeParse({ nodeType: 'END' })
    expect(result.success).toBe(true)
  })
})

describe('DIRECT_RETURN schema', () => {
  it('accepts valid config', () => {
    const result = DirectReturnBizConfigSchema.safeParse({
      nodeType: 'DIRECT_RETURN',
      responseTemplate: '{"status":"ok"}',
    })
    expect(result.success).toBe(true)
  })
})

describe('COUPON schema', () => {
  it('accepts valid config', () => {
    const result = CouponBizConfigSchema.safeParse({
      nodeType: 'COUPON',
      couponTypeKey: 'discount_10',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
  it('rejects missing couponTypeKey', () => {
    const result = CouponBizConfigSchema.safeParse({
      nodeType: 'COUPON',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
})

describe('COMMIT_ACTION schema', () => {
  it('accepts COUPON action', () => {
    const result = CommitActionBizConfigSchema.safeParse({
      nodeType: 'COMMIT_ACTION',
      actionType: 'COUPON',
      couponTypeKey: 'disc_10',
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('accepts POINTS_OPERATION action', () => {
    const result = CommitActionBizConfigSchema.safeParse({
      nodeType: 'COMMIT_ACTION',
      actionType: 'POINTS_OPERATION',
      points: 100,
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('rejects invalid actionType', () => {
    const result = CommitActionBizConfigSchema.safeParse({
      nodeType: 'COMMIT_ACTION',
      actionType: 'INVALID',
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(false)
  })
})

describe('API_CALL schema', () => {
  it('accepts valid config', () => {
    const result = ApiCallBizConfigSchema.safeParse({
      nodeType: 'API_CALL',
      apiDefinitionId: 42,
      successNodeId: 'a',
      failNodeId: 'b',
      timeoutNodeId: 'c',
    })
    expect(result.success).toBe(true)
  })
  it('rejects missing apiDefinitionId', () => {
    const result = ApiCallBizConfigSchema.safeParse({
      nodeType: 'API_CALL',
      successNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
})

describe('GROOVY schema', () => {
  it('accepts valid config', () => {
    const result = GroovyBizConfigSchema.safeParse({
      nodeType: 'GROOVY',
      code: 'return true',
      successNodeId: 'a',
      failNodeId: 'b',
      timeoutNodeId: 'c',
    })
    expect(result.success).toBe(true)
  })
  it('rejects missing code', () => {
    const result = GroovyBizConfigSchema.safeParse({
      nodeType: 'GROOVY',
      successNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
})

describe('SEND_MQ schema', () => {
  it('accepts valid config', () => {
    const result = SendMqBizConfigSchema.safeParse({
      nodeType: 'SEND_MQ',
      topicKey: 'result_topic',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('IN_APP_NOTIFY schema', () => {
  it('accepts valid config', () => {
    const result = InAppNotifyBizConfigSchema.safeParse({
      nodeType: 'IN_APP_NOTIFY',
      templateId: 'tpl_001',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('SEND_SMS schema', () => {
  it('accepts valid config', () => {
    const result = SendSmsBizConfigSchema.safeParse({
      nodeType: 'SEND_SMS',
      templateId: 'sms_tpl_1',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('SEND_PUSH schema', () => {
  it('accepts valid config', () => {
    const result = SendPushBizConfigSchema.safeParse({
      nodeType: 'SEND_PUSH',
      templateId: 'push_tpl_1',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('SEND_EMAIL schema', () => {
  it('accepts valid config', () => {
    const result = SendEmailBizConfigSchema.safeParse({
      nodeType: 'SEND_EMAIL',
      templateId: 'email_tpl_1',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('SEND_WECHAT schema', () => {
  it('accepts valid config', () => {
    const result = SendWechatBizConfigSchema.safeParse({
      nodeType: 'SEND_WECHAT',
      templateId: 'wx_tpl_1',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('SEND_IN_APP schema', () => {
  it('accepts valid config', () => {
    const result = SendInAppBizConfigSchema.safeParse({
      nodeType: 'SEND_IN_APP',
      templateId: 'inapp_tpl_1',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('POINTS_OPERATION schema', () => {
  it('accepts valid config', () => {
    const result = PointsOperationBizConfigSchema.safeParse({
      nodeType: 'POINTS_OPERATION',
      points: 50,
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('UPDATE_PROFILE schema', () => {
  it('accepts valid config', () => {
    const result = UpdateProfileBizConfigSchema.safeParse({
      nodeType: 'UPDATE_PROFILE',
      profileFields: { name: 'John' },
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('TAG_OPERATION schema', () => {
  it('accepts valid config', () => {
    const result = TagOperationBizConfigSchema.safeParse({
      nodeType: 'TAG_OPERATION',
      tagCode: 'vip_level',
      tagValue: 'gold',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('TRACK_EVENT schema', () => {
  it('accepts valid config', () => {
    const result = TrackEventBizConfigSchema.safeParse({
      nodeType: 'TRACK_EVENT',
      eventCode: 'purchase_complete',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('CREATE_TASK schema', () => {
  it('accepts valid config', () => {
    const result = CreateTaskBizConfigSchema.safeParse({
      nodeType: 'CREATE_TASK',
      taskType: 'FOLLOW_UP',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('GOAL_CHECK schema', () => {
  it('accepts valid config', () => {
    const result = GoalCheckBizConfigSchema.safeParse({
      nodeType: 'GOAL_CHECK',
      goalMetNodeId: 'a',
      goalNotMetNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('SUPPRESSION_CHECK schema', () => {
  it('accepts valid config', () => {
    const result = SuppressionCheckBizConfigSchema.safeParse({
      nodeType: 'SUPPRESSION_CHECK',
      suppressedNodeId: 'a',
      allowedNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('QUIET_HOURS schema', () => {
  it('accepts valid config', () => {
    const result = QuietHoursBizConfigSchema.safeParse({
      nodeType: 'QUIET_HOURS',
      quietNodeId: 'a',
      availableNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('CHANNEL_AVAILABILITY schema', () => {
  it('accepts valid config', () => {
    const result = ChannelAvailabilityBizConfigSchema.safeParse({
      nodeType: 'CHANNEL_AVAILABILITY',
      availableNodeId: 'a',
      unavailableNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('FREQUENCY_CAP schema', () => {
  it('accepts valid config', () => {
    const result = FrequencyCapBizConfigSchema.safeParse({
      nodeType: 'FREQUENCY_CAP',
      cappedNodeId: 'a',
      passNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('CANVAS_TRIGGER schema', () => {
  it('accepts valid config', () => {
    const result = CanvasTriggerBizConfigSchema.safeParse({
      nodeType: 'CANVAS_TRIGGER',
      targetCanvasId: 99,
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('SUB_FLOW_REF schema', () => {
  it('accepts valid config', () => {
    const result = SubFlowRefBizConfigSchema.safeParse({
      nodeType: 'SUB_FLOW_REF',
      subFlowId: 55,
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
})

describe('TRANSFER_JOURNEY schema', () => {
  it('accepts valid config', () => {
    const result = TransferJourneyBizConfigSchema.safeParse({
      nodeType: 'TRANSFER_JOURNEY',
      targetCanvasId: 77,
    })
    expect(result.success).toBe(true)
  })
})

describe('GROUP schema', () => {
  it('accepts valid config', () => {
    const result = GroupBizConfigSchema.safeParse({ nodeType: 'GROUP' })
    expect(result.success).toBe(true)
  })
})

describe('TEMPLATE_NODE schema', () => {
  it('accepts valid config', () => {
    const result = TemplateNodeBizConfigSchema.safeParse({
      nodeType: 'TEMPLATE_NODE',
    })
    expect(result.success).toBe(true)
  })
})

describe('RECOMMENDATION schema', () => {
  it('accepts valid config', () => {
    const result = RecommendationBizConfigSchema.safeParse({
      nodeType: 'RECOMMENDATION',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('AI_NEXT_BEST_ACTION schema', () => {
  it('accepts valid config', () => {
    const result = AiNextBestActionBizConfigSchema.safeParse({
      nodeType: 'AI_NEXT_BEST_ACTION',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('LOGIC_RELATION schema', () => {
  it('accepts valid config', () => {
    const result = LogicRelationBizConfigSchema.safeParse({
      nodeType: 'LOGIC_RELATION',
      relation: 'AND',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('CDP_TAG_WRITE schema', () => {
  it('accepts valid config', () => {
    const result = CdpTagWriteBizConfigSchema.safeParse({
      nodeType: 'CDP_TAG_WRITE',
      tagCode: 'segment_a',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('REACH_PLATFORM schema', () => {
  it('accepts valid config', () => {
    const result = ReachPlatformBizConfigSchema.safeParse({
      nodeType: 'REACH_PLATFORM',
      channel: 'SMS',
      templateId: 'reach_tpl',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(true)
  })
})

describe('BizConfigSchema discriminated union', () => {
  it('dispatches to IF_CONDITION schema', () => {
    const result = BizConfigSchema.safeParse({
      nodeType: 'IF_CONDITION',
      rules: [],
      successNodeId: 'a',
      failNodeId: 'b',
    })
    expect(result.success).toBe(true)
  })
  it('rejects unknown nodeType', () => {
    const result = BizConfigSchema.safeParse({
      nodeType: 'UNKNOWN_TYPE',
      nextNodeId: 'a',
    })
    expect(result.success).toBe(false)
  })
  it('rejects wrong nodeType field value for a given schema', () => {
    const result = BizConfigSchema.safeParse({
      nodeType: 'IF_CONDITION',
      eventCode: 'login',
    })
    expect(result.success).toBe(false)
  })
})
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/photonpay/project/canvas/frontend && npx vitest run types/canvasSchemas.test.ts
```

Expected: FAIL (canvasSchemas module does not exist).

- [ ] **Step 4: Implement all Zod schemas for every node type**

Create `frontend/src/types/canvasSchemas.ts`:

```ts
import { z } from 'zod'

// ── Shared ──────────────────────────────────────────────────────────

const NodeId = z.string().min(1)
const OptionalNodeId = z.string().min(1).optional()
const OptionalNullableNodeId = z.string().min(1).nullable().optional()

const BranchSchema = z.object({
  label: z.string().optional(),
  strategyRelation: z.enum(['AND', 'OR']).optional(),
  conditions: z.array(z.record(z.unknown())).optional(),
  nextNodeId: OptionalNullableNodeId,
}).strict()

const PrioritySchema = z.object({
  order: z.number().optional(),
  nextNodeId: OptionalNullableNodeId,
}).strict()

const AbGroupSchema = z.object({
  groupKey: z.string().min(1),
  label: z.string().optional(),
  nextNodeId: OptionalNullableNodeId,
}).strict()

const PathSchema = z.object({
  pathId: z.string().min(1),
  label: z.string().optional(),
  weight: z.number().optional(),
  nextNodeId: OptionalNullableNodeId,
}).strict()

const VariantSchema = z.object({
  variantId: z.string().min(1),
  label: z.string().optional(),
  weight: z.number().optional(),
  isControl: z.boolean().optional(),
  nextNodeId: OptionalNullableNodeId,
}).strict()

const BandSchema = z.object({
  bandId: z.string().min(1),
  label: z.string().optional(),
  min: z.number(),
  max: z.number(),
  nextNodeId: OptionalNullableNodeId,
}).strict()

const ConditionRuleSchema = z.object({
  field: z.string(),
  operator: z.enum(['EQ', 'NEQ', 'CONTAINS', 'GT', 'LT', 'GTE', 'LTE']),
  value: z.string(),
  isCustom: z.boolean(),
}).strict()

// ── Node-type specific schemas ──────────────────────────────────────

export const StartBizConfigSchema = z.object({
  nodeType: z.literal('START'),
  nextNodeId: OptionalNodeId,
  branches: z.array(BranchSchema).optional(),
}).strict()

export const EventTriggerBizConfigSchema = z.object({
  nodeType: z.literal('EVENT_TRIGGER'),
  eventCode: z.string().min(1),
  nextNodeId: OptionalNodeId,
}).strict()

export const MqTriggerBizConfigSchema = z.object({
  nodeType: z.literal('MQ_TRIGGER'),
  topicKey: z.string().min(1),
  nextNodeId: OptionalNodeId,
}).strict()

export const ScheduledTriggerBizConfigSchema = z.object({
  nodeType: z.literal('SCHEDULED_TRIGGER'),
  scheduleType: z.enum(['CRON', 'ONCE']),
  cronExpression: z.string().optional(),
  triggerTime: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const DirectCallBizConfigSchema = z.object({
  nodeType: z.literal('DIRECT_CALL'),
  branches: z.array(BranchSchema).optional(),
}).strict()

export const AudienceTriggerBizConfigSchema = z.object({
  nodeType: z.literal('AUDIENCE_TRIGGER'),
  audienceId: z.string().min(1).optional(),
  hitNextNodeId: OptionalNodeId,
  missNextNodeId: OptionalNodeId,
}).strict()

export const IfConditionBizConfigSchema = z.object({
  nodeType: z.literal('IF_CONDITION'),
  rules: z.array(ConditionRuleSchema),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
}).strict()

export const SelectorBizConfigSchema = z.object({
  nodeType: z.literal('SELECTOR'),
  branches: z.array(BranchSchema).optional(),
  elseNodeId: OptionalNodeId,
}).strict()

export const AggregateBizConfigSchema = z.object({
  nodeType: z.literal('AGGREGATE'),
  evaluateMode: z.enum(['count', 'rate', 'script']),
  minCount: z.number().optional(),
  minRate: z.number().optional(),
  evaluateScript: z.string().optional(),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
}).strict()

export const ThresholdBizConfigSchema = z.object({
  nodeType: z.literal('THRESHOLD'),
  thresholdMode: z.enum(['min_success', 'min_done']).optional(),
  threshold: z.number().optional(),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
}).strict()

export const TaggerBizConfigSchema = z.object({
  nodeType: z.literal('TAGGER'),
  mode: z.enum(['audience', 'realtime', 'offline']),
  tagCode: z.string().optional(),
  hitNextNodeId: OptionalNodeId,
  missNextNodeId: OptionalNodeId,
  nextNodeId: OptionalNodeId,
}).strict()

export const PriorityBizConfigSchema = z.object({
  nodeType: z.literal('PRIORITY'),
  priorities: z.array(PrioritySchema).optional(),
}).strict()

export const AbSplitBizConfigSchema = z.object({
  nodeType: z.literal('AB_SPLIT'),
  groups: z.array(AbGroupSchema).optional(),
}).strict()

export const RandomSplitBizConfigSchema = z.object({
  nodeType: z.literal('RANDOM_SPLIT'),
  allocationStrategy: z.enum(['CONSISTENT', 'RANDOM']).optional(),
  paths: z.array(PathSchema).optional(),
}).strict()

export const ExperimentBizConfigSchema = z.object({
  nodeType: z.literal('EXPERIMENT'),
  experimentKey: z.string().optional(),
  allocationStrategy: z.enum(['CONSISTENT', 'RANDOM']).optional(),
  variants: z.array(VariantSchema).optional(),
}).strict()

export const ScoringBizConfigSchema = z.object({
  nodeType: z.literal('SCORING'),
  rules: z.array(z.record(z.unknown())).optional(),
  bands: z.array(BandSchema).optional(),
}).strict()

export const ManualApprovalBizConfigSchema = z.object({
  nodeType: z.literal('MANUAL_APPROVAL'),
  approveNodeId: OptionalNodeId,
  rejectNodeId: OptionalNodeId,
}).strict()

export const DelayBizConfigSchema = z.object({
  nodeType: z.literal('DELAY'),
  delaySeconds: z.number().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const WaitBizConfigSchema = z.object({
  nodeType: z.literal('WAIT'),
  waitUntil: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const GotoBizConfigSchema = z.object({
  nodeType: z.literal('GOTO'),
  targetNodeId: OptionalNodeId,
}).strict()

export const LoopBizConfigSchema = z.object({
  nodeType: z.literal('LOOP'),
  loopStartNodeId: OptionalNodeId,
  maxExceededNodeId: OptionalNodeId,
}).strict()

export const MergeBizConfigSchema = z.object({
  nodeType: z.literal('MERGE'),
}).strict()

export const HubBizConfigSchema = z.object({
  nodeType: z.literal('HUB'),
}).strict()

export const EndBizConfigSchema = z.object({
  nodeType: z.literal('END'),
}).strict()

export const DirectReturnBizConfigSchema = z.object({
  nodeType: z.literal('DIRECT_RETURN'),
  responseTemplate: z.string().optional(),
}).strict()

export const CouponBizConfigSchema = z.object({
  nodeType: z.literal('COUPON'),
  couponTypeKey: z.string().min(1),
  nextNodeId: OptionalNodeId,
}).strict()

export const CommitActionBizConfigSchema = z.object({
  nodeType: z.literal('COMMIT_ACTION'),
  actionType: z.enum(['COUPON', 'POINTS_OPERATION']),
  couponTypeKey: z.string().optional(),
  points: z.number().optional(),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
}).strict()

export const ApiCallBizConfigSchema = z.object({
  nodeType: z.literal('API_CALL'),
  apiDefinitionId: z.number(),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
  timeoutNodeId: OptionalNodeId,
}).strict()

export const GroovyBizConfigSchema = z.object({
  nodeType: z.literal('GROOVY'),
  code: z.string().min(1),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
  timeoutNodeId: OptionalNodeId,
}).strict()

export const SendMqBizConfigSchema = z.object({
  nodeType: z.literal('SEND_MQ'),
  topicKey: z.string().min(1).optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const InAppNotifyBizConfigSchema = z.object({
  nodeType: z.literal('IN_APP_NOTIFY'),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const SendSmsBizConfigSchema = z.object({
  nodeType: z.literal('SEND_SMS'),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const SendPushBizConfigSchema = z.object({
  nodeType: z.literal('SEND_PUSH'),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const SendEmailBizConfigSchema = z.object({
  nodeType: z.literal('SEND_EMAIL'),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const SendWechatBizConfigSchema = z.object({
  nodeType: z.literal('SEND_WECHAT'),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const SendInAppBizConfigSchema = z.object({
  nodeType: z.literal('SEND_IN_APP'),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const PointsOperationBizConfigSchema = z.object({
  nodeType: z.literal('POINTS_OPERATION'),
  points: z.number().optional(),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
}).strict()

export const UpdateProfileBizConfigSchema = z.object({
  nodeType: z.literal('UPDATE_PROFILE'),
  profileFields: z.record(z.unknown()).optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const TagOperationBizConfigSchema = z.object({
  nodeType: z.literal('TAG_OPERATION'),
  tagCode: z.string().optional(),
  tagValue: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const TrackEventBizConfigSchema = z.object({
  nodeType: z.literal('TRACK_EVENT'),
  eventCode: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const CreateTaskBizConfigSchema = z.object({
  nodeType: z.literal('CREATE_TASK'),
  taskType: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const GoalCheckBizConfigSchema = z.object({
  nodeType: z.literal('GOAL_CHECK'),
  goalMetNodeId: OptionalNodeId,
  goalNotMetNodeId: OptionalNodeId,
}).strict()

export const SuppressionCheckBizConfigSchema = z.object({
  nodeType: z.literal('SUPPRESSION_CHECK'),
  suppressedNodeId: OptionalNodeId,
  allowedNodeId: OptionalNodeId,
}).strict()

export const QuietHoursBizConfigSchema = z.object({
  nodeType: z.literal('QUIET_HOURS'),
  quietNodeId: OptionalNodeId,
  availableNodeId: OptionalNodeId,
}).strict()

export const ChannelAvailabilityBizConfigSchema = z.object({
  nodeType: z.literal('CHANNEL_AVAILABILITY'),
  availableNodeId: OptionalNodeId,
  unavailableNodeId: OptionalNodeId,
}).strict()

export const FrequencyCapBizConfigSchema = z.object({
  nodeType: z.literal('FREQUENCY_CAP'),
  cappedNodeId: OptionalNodeId,
  passNodeId: OptionalNodeId,
}).strict()

export const CanvasTriggerBizConfigSchema = z.object({
  nodeType: z.literal('CANVAS_TRIGGER'),
  targetCanvasId: z.number().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const SubFlowRefBizConfigSchema = z.object({
  nodeType: z.literal('SUB_FLOW_REF'),
  subFlowId: z.number().optional(),
  successNodeId: OptionalNodeId,
  failNodeId: OptionalNodeId,
}).strict()

export const TransferJourneyBizConfigSchema = z.object({
  nodeType: z.literal('TRANSFER_JOURNEY'),
  targetCanvasId: z.number().optional(),
}).strict()

export const GroupBizConfigSchema = z.object({
  nodeType: z.literal('GROUP'),
}).strict()

export const TemplateNodeBizConfigSchema = z.object({
  nodeType: z.literal('TEMPLATE_NODE'),
}).strict()

export const RecommendationBizConfigSchema = z.object({
  nodeType: z.literal('RECOMMENDATION'),
  nextNodeId: OptionalNodeId,
}).strict()

export const AiNextBestActionBizConfigSchema = z.object({
  nodeType: z.literal('AI_NEXT_BEST_ACTION'),
  nextNodeId: OptionalNodeId,
}).strict()

export const LogicRelationBizConfigSchema = z.object({
  nodeType: z.literal('LOGIC_RELATION'),
  relation: z.enum(['AND', 'OR']).optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const CdpTagWriteBizConfigSchema = z.object({
  nodeType: z.literal('CDP_TAG_WRITE'),
  tagCode: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

export const ReachPlatformBizConfigSchema = z.object({
  nodeType: z.literal('REACH_PLATFORM'),
  channel: z.string().optional(),
  templateId: z.string().optional(),
  nextNodeId: OptionalNodeId,
}).strict()

// ── Discriminated union ─────────────────────────────────────────────

export const BizConfigSchema = z.discriminatedUnion('nodeType', [
  StartBizConfigSchema,
  EventTriggerBizConfigSchema,
  MqTriggerBizConfigSchema,
  ScheduledTriggerBizConfigSchema,
  DirectCallBizConfigSchema,
  AudienceTriggerBizConfigSchema,
  IfConditionBizConfigSchema,
  SelectorBizConfigSchema,
  AggregateBizConfigSchema,
  ThresholdBizConfigSchema,
  TaggerBizConfigSchema,
  PriorityBizConfigSchema,
  AbSplitBizConfigSchema,
  RandomSplitBizConfigSchema,
  ExperimentBizConfigSchema,
  ScoringBizConfigSchema,
  ManualApprovalBizConfigSchema,
  DelayBizConfigSchema,
  WaitBizConfigSchema,
  GotoBizConfigSchema,
  LoopBizConfigSchema,
  MergeBizConfigSchema,
  HubBizConfigSchema,
  EndBizConfigSchema,
  DirectReturnBizConfigSchema,
  CouponBizConfigSchema,
  CommitActionBizConfigSchema,
  ApiCallBizConfigSchema,
  GroovyBizConfigSchema,
  SendMqBizConfigSchema,
  InAppNotifyBizConfigSchema,
  SendSmsBizConfigSchema,
  SendPushBizConfigSchema,
  SendEmailBizConfigSchema,
  SendWechatBizConfigSchema,
  SendInAppBizConfigSchema,
  PointsOperationBizConfigSchema,
  UpdateProfileBizConfigSchema,
  TagOperationBizConfigSchema,
  TrackEventBizConfigSchema,
  CreateTaskBizConfigSchema,
  GoalCheckBizConfigSchema,
  SuppressionCheckBizConfigSchema,
  QuietHoursBizConfigSchema,
  ChannelAvailabilityBizConfigSchema,
  FrequencyCapBizConfigSchema,
  CanvasTriggerBizConfigSchema,
  SubFlowRefBizConfigSchema,
  TransferJourneyBizConfigSchema,
  GroupBizConfigSchema,
  TemplateNodeBizConfigSchema,
  RecommendationBizConfigSchema,
  AiNextBestActionBizConfigSchema,
  LogicRelationBizConfigSchema,
  CdpTagWriteBizConfigSchema,
  ReachPlatformBizConfigSchema,
])
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd /Users/photonpay/project/canvas/frontend && npx vitest run types/canvasSchemas.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
cd /Users/photonpay/project/canvas && git add frontend/src/types/canvasSchemas.ts frontend/src/types/canvasSchemas.test.ts frontend/package.json frontend/package-lock.json && git commit -m "feat: add Zod schemas for all 54 canvas node types with discriminated union, remove index signature"
```

---

### Task 2: Add API Response Validation Interceptor with Schema Map

**Files:**
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/services/responseSchemaMap.ts`
- Test: `frontend/src/services/responseSchemaMap.test.ts`

- [ ] **Step 1: Write failing test — valid responses pass validation, invalid responses are caught**

Create `frontend/src/services/responseSchemaMap.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest'
import { getSchemaForUrl, validateResponse } from './responseSchemaMap'
import { z } from 'zod'

describe('responseSchemaMap', () => {
  it('returns undefined for unregistered URLs', () => {
    expect(getSchemaForUrl('/unknown/endpoint')).toBeUndefined()
  })

  it('returns schema for registered canvas GET endpoint', () => {
    const schema = getSchemaForUrl('/canvas/123')
    expect(schema).toBeDefined()
  })

  it('returns schema for home overview endpoint', () => {
    const schema = getSchemaForUrl('/canvas/home/overview')
    expect(schema).toBeDefined()
  })

  it('returns schema for canvas list endpoint', () => {
    const schema = getSchemaForUrl('/canvas/list')
    expect(schema).toBeDefined()
  })

  it('returns schema for audience list endpoint', () => {
    const schema = getSchemaForUrl('/canvas/audiences')
    expect(schema).toBeDefined()
  })

  it('returns schema for meta node-types endpoint', () => {
    const schema = getSchemaForUrl('/meta/node-types')
    expect(schema).toBeDefined()
  })

  it('returns schema for execution direct call endpoint', () => {
    const schema = getSchemaForUrl('/canvas/execute/direct/1')
    expect(schema).toBeDefined()
  })

  it('validateResponse returns success for valid data', () => {
    const schema = z.object({ id: z.number(), name: z.string() })
    const result = validateResponse({ id: 1, name: 'test' }, schema)
    expect(result.success).toBe(true)
  })

  it('validateResponse returns failure for invalid data', () => {
    const schema = z.object({ id: z.number(), name: z.string() })
    const result = validateResponse({ id: 'not-a-number', name: 'test' }, schema)
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error).toBeDefined()
      expect(result.error.issues.length).toBeGreaterThan(0)
    }
  })

  it('validateResponse catches extra keys when schema is strict', () => {
    const schema = z.object({ id: z.number() }).strict()
    const result = validateResponse({ id: 1, extraKey: 'oops' }, schema)
    expect(result.success).toBe(false)
  })

  it('validates canvas detail response against schema', () => {
    const schema = getSchemaForUrl('/canvas/1')
    if (!schema) return // skip if not registered yet
    const validData = {
      code: 0,
      message: 'ok',
      data: {
        canvas: { id: 1, name: 'Test', status: 0, createdAt: '2026-01-01', updatedAt: '2026-01-01' },
        graphJson: '{"nodes":[]}',
      },
    }
    const result = validateResponse(validData, schema)
    expect(result.success).toBe(true)
  })

  it('validates home overview response against schema', () => {
    const schema = getSchemaForUrl('/canvas/home/overview')
    if (!schema) return
    const validData = {
      code: 0,
      message: 'ok',
      data: {
        range: { days: 7, since: '2026-05-24', until: '2026-05-30' },
        summary: { publishedCanvasCount: 5, totalExecutions: 100, uniqueUsers: 50, failedExecutions: 3, successRate: '97.0%' },
        trend: [{ date: '2026-05-30', total: 10, failed: 1 }],
        topCanvases: [{ canvasId: 1, name: 'Test', total: 20, uniqueUsers: 10, successRate: '95.0%', failed: 1 }],
        attentionItems: [],
      },
    }
    const result = validateResponse(validData, schema)
    expect(result.success).toBe(true)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/photonpay/project/canvas/frontend && npx vitest run services/responseSchemaMap.test.ts
```

Expected: FAIL (module does not exist).

- [ ] **Step 3: Implement responseSchemaMap module**

Create `frontend/src/services/responseSchemaMap.ts`:

```ts
import { z } from 'zod'
import type { ZodSchema } from 'zod'
import { BizConfigSchema } from '../types/canvasSchemas'

/**
 * Maps API URL patterns to their Zod validation schemas.
 * Used by the response interceptor to validate incoming data.
 */
const schemaRegistry = new Map<string, ZodSchema<unknown>>()

// ── Canvas detail response schema ─────────────────────────────────
const CanvasDetailSchema = z.object({
  code: z.number(),
  message: z.string(),
  data: z.object({
    canvas: z.object({
      id: z.number(),
      name: z.string(),
      status: z.number(),
      createdAt: z.string(),
      updatedAt: z.string(),
    }).passthrough(),
    graphJson: z.string(),
  }),
}).passthrough()

// ── Canvas list response schema ───────────────────────────────────
const CanvasListSchema = z.object({
  code: z.number(),
  message: z.string(),
  data: z.object({
    records: z.array(z.object({
      id: z.number(),
      name: z.string(),
      status: z.number(),
    }).passthrough()),
    total: z.number().optional(),
  }),
}).passthrough()

// ── Home overview response schema ────────────────────────────────
const HomeOverviewSchema = z.object({
  code: z.number(),
  message: z.string(),
  data: z.object({
    range: z.object({
      days: z.number(),
      since: z.string(),
      until: z.string(),
    }),
    summary: z.object({
      publishedCanvasCount: z.number(),
      totalExecutions: z.number(),
      uniqueUsers: z.number(),
      failedExecutions: z.number(),
      successRate: z.string(),
    }),
    trend: z.array(z.object({
      date: z.string(),
      total: z.number(),
      failed: z.number(),
    })),
    topCanvases: z.array(z.object({
      canvasId: z.number().nullable(),
      name: z.string(),
      total: z.number(),
      uniqueUsers: z.number(),
      successRate: z.string(),
      failed: z.number(),
    })),
    attentionItems: z.array(z.object({
      canvasId: z.number().nullable(),
      name: z.string(),
      type: z.string(),
      message: z.string(),
      severity: z.string(),
    })),
  }),
}).passthrough()

// ── Execution direct call response schema ─────────────────────────
const ExecutionDirectCallSchema = z.object({
  code: z.number(),
  message: z.string(),
  data: z.record(z.unknown()),
}).passthrough()

// ── Audience list response schema ────────────────────────────────
const AudienceListSchema = z.object({
  code: z.number(),
  message: z.string(),
  data: z.object({
    records: z.array(z.object({
      id: z.number(),
      name: z.string(),
    }).passthrough()),
    total: z.number().optional(),
  }),
}).passthrough()

// ── Meta node types response schema ──────────────────────────────
const MetaNodeTypesSchema = z.object({
  code: z.number(),
  message: z.string(),
  data: z.array(z.object({
    typeKey: z.string(),
    name: z.string(),
  }).passthrough()),
}).passthrough()

// ── Register URL patterns ──────────────────────────────────────────
// Pattern-based matching
const CANVAS_DETAIL_PATTERN = /^\/canvas\/\d+$/
const CANVAS_LIST_PATTERN = /^\/canvas\/list/
const EXECUTION_DIRECT_PATTERN = /^\/canvas\/execute\/direct\//
const AUDIENCE_LIST_PATTERN = /^\/canvas\/audiences(\/|$)/
const META_NODE_TYPES_PATTERN = /^\/meta\/node-types$/

schemaRegistry.set('/canvas/home/overview', HomeOverviewSchema)
schemaRegistry.set('/canvas/list', CanvasListSchema)
schemaRegistry.set('/meta/node-types', MetaNodeTypesSchema)

/**
 * Look up the validation schema for a given API URL.
 * Tries exact match first, then pattern matching.
 *
 * @param url  the request URL path (e.g. "/canvas/123")
 * @returns the matching Zod schema, or undefined
 */
export function getSchemaForUrl(url: string): ZodSchema<unknown> | undefined {
  // Exact match
  const exact = schemaRegistry.get(url)
  if (exact) return exact

  // Pattern match
  if (CANVAS_DETAIL_PATTERN.test(url)) {
    return CanvasDetailSchema
  }
  if (CANVAS_LIST_PATTERN.test(url)) {
    return CanvasListSchema
  }
  if (url.startsWith('/canvas/home/overview')) {
    return HomeOverviewSchema
  }
  if (EXECUTION_DIRECT_PATTERN.test(url)) {
    return ExecutionDirectCallSchema
  }
  if (AUDIENCE_LIST_PATTERN.test(url)) {
    return AudienceListSchema
  }
  if (META_NODE_TYPES_PATTERN.test(url)) {
    return MetaNodeTypesSchema
  }

  return undefined
}

/**
 * Validate response data against a Zod schema.
 *
 * @param data    the parsed response data
 * @param schema  the Zod schema to validate against
 * @returns success result or failure with error details
 */
export function validateResponse(
  data: unknown,
  schema: ZodSchema<unknown>,
): { success: true; data: unknown } | { success: false; error: z.ZodError } {
  const result = schema.safeParse(data)
  if (result.success) {
    return { success: true, data: result.data }
  }
  return { success: false, error: result.error }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd /Users/photonpay/project/canvas/frontend && npx vitest run services/responseSchemaMap.test.ts
```

Expected: PASS.

- [ ] **Step 5: Integrate validation into the axios response interceptor**

In `frontend/src/services/api.ts`, add validation after the existing response interceptor:

```ts
// Add at the top of api.ts, after existing imports:
import { getSchemaForUrl, validateResponse } from './responseSchemaMap'

// Replace the existing success response interceptor (currently line 33-34):
// BEFORE:
//   (res) => res.data,
// AFTER:
  (res) => {
    const schema = getSchemaForUrl(res.config.url ?? '')
    if (schema) {
      const result = validateResponse(res.data, schema)
      if (!result.success) {
        console.error(
          `[API Validation] ${res.config.url} response failed Zod validation:`,
          result.error.issues.map(i => `${i.path.join('.')}: ${i.message}`).join('; '),
        )
      }
    }
    return res.data
  },
```

- [ ] **Step 6: Run all service tests**

```bash
cd /Users/photonpay/project/canvas/frontend && npx vitest run services/
```

Expected: All tests PASS.

- [ ] **Step 7: Commit**

```bash
cd /Users/photonpay/project/canvas && git add frontend/src/services/responseSchemaMap.ts frontend/src/services/responseSchemaMap.test.ts frontend/src/services/api.ts && git commit -m "feat: add Zod response validation interceptor with schema map for API endpoints"
```