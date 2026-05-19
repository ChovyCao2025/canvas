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

/** 不能有上游节点的节点类型（START 是流程唯一入口）
 *  BEHAVIOR_TRIGGER / MQ_TRIGGER / SCHEDULED_TRIGGER 虽然是触发器，
 *  但它们放在 START 之后，需要有 target handle 接受 START 的连线。
 *  TAGGER 的 isTrigger 是动态的（mode=realtime 时），在 CanvasNode 里单独处理。
 */
export const TRIGGER_TYPES = new Set(['START'])

/** 是否为终止节点 */
export const TERMINAL_TYPES = new Set(['DIRECT_RETURN', 'END'])

/** 各节点类型显示名称 */
export const DEFAULT_NAMES: Record<string, string> = {
  TAGGER:            'Tagger 标签',
  EVENT_TRIGGER:     '事件触发',
  MQ_TRIGGER:        'MQ消息触发',
  DIRECT_CALL:       '业务直调',
  SCHEDULED_TRIGGER: '定时触发',
  TAGGER_REALTIME:   'Tagger实时标签',
  IF_CONDITION:      'IF判断',
  SELECTOR:          '条件选择器',
  LOGIC_RELATION:    '逻辑关系',
  HUB:               '集线器',
  AGGREGATE:         '聚合评估',
  THRESHOLD:         '阈值触发',
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
