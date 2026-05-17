/** 节点类别 → CSS 颜色 */
export const CATEGORY_COLORS: Record<string, string> = {
  '行为策略': 'linear-gradient(135deg, #13c2c2, #1677ff)',
  '逻辑分支': '#1677ff',
  '权益发放': 'linear-gradient(135deg, #f5222d, #eb2f96)',
  '用户触达': '#faad14',
  '其他':     '#722ed1',
}

export const CATEGORY_SOLID: Record<string, string> = {
  '行为策略': '#13c2c2',
  '逻辑分支': '#1677ff',
  '权益发放': '#f5222d',
  '用户触达': '#faad14',
  '其他':     '#722ed1',
}

/** 触发器节点（无 target handle，只能作为第一个节点）
 *  BEHAVIOR_TRIGGER 是新的统一行为触发节点。
 *  TAGGER 的 isTrigger 是动态的（mode=realtime 时为触发器），在 CanvasNode 里单独处理。
 */
export const TRIGGER_TYPES = new Set(['START', 'BEHAVIOR_TRIGGER'])

/** 是否为终止节点 */
export const TERMINAL_TYPES = new Set(['DIRECT_RETURN', 'END'])

/** 各节点类型默认名称 */
export const DEFAULT_NAMES: Record<string, string> = {
  // 新合并类型
  TAGGER:            'Tagger 标签',
  BEHAVIOR_TRIGGER:  '行为触发',
  // 旧类型（保留兼容已有画布）
  MQ_TRIGGER:        'MQ消息触发',
  BEHAVIOR_IN_APP:   '端内行为触发',
  DIRECT_CALL:       '业务直调',
  TAGGER_REALTIME:   'Tagger实时标签',
  SCHEDULED_TRIGGER: '定时触发',
  IF_CONDITION:      'IF判断',
  SELECTOR:          '条件选择器',
  LOGIC_RELATION:    '逻辑关系',
  HUB:               '集线器',
  PRIORITY:          '优先级',
  AB_SPLIT:          'AB分流',
  TAGGER_OFFLINE:    'Tagger离线标签',
  COUPON:            '代金券',
  IN_APP_NOTIFY:     '端内通知',
  REACH_PLATFORM:    '触达平台',
  DIRECT_RETURN:     '直调返回',
  API_CALL:          '接口调用',
  DELAY:             '延迟器',
  SEND_MQ:           '发送MQ',
  GROOVY:            'Groovy脚本',
  MANUAL_APPROVAL:   '人工审批',
  CANVAS_TRIGGER:    '触发子画布',
  SUB_FLOW_REF:      '子流程引用',
}

export type CanvasNodeData = {
  nodeType: string
  name: string
  category: string
  bizConfig: Record<string, unknown>
}
