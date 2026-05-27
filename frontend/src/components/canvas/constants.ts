/**
 * 组件职责：画布节点样式、尺寸、分支 handle ID 和节点类型常量。
 *
 * 维护说明：布局、占位符和节点组件依赖这些值保持视觉与命中区域一致。
 */
/**
 * 节点类别 -> 头部背景色（可渐变）。
 *
 * 用途：
 * - `CanvasNode` 顶部色带；
 * - 同类节点视觉分组，帮助快速扫图。
 */
export const CATEGORY_COLORS: Record<string, string> = {
  '入口节点': '#0f766e',
  '行为策略': 'linear-gradient(135deg, #14b8a6, #06b6d4)',
  '逻辑分支': '#1677ff',
  '人群圈选': '#1677ff',
  '流程控制': '#4f46e5',
  '决策增强': '#7c3aed',
  '合规保护': '#dc2626',
  '权益发放': 'linear-gradient(135deg, #ef4444, #ec4899)',
  '数据操作': '#0891b2',
  '内部动作': '#475569',
  '用户触达': '#f59e0b',
  '消息触达': '#f59e0b',
  '结构复用': '#64748b',
  '其他':     '#6d5efc',
}

/** 节点分类实色，用于分类色条和节点库 hover 态。 */
export const CATEGORY_SOLID: Record<string, string> = {
  '入口节点': '#0f766e',
  '行为策略': '#14b8a6',
  '逻辑分支': '#1677ff',
  '人群圈选': '#1677ff',
  '流程控制': '#4f46e5',
  '决策增强': '#7c3aed',
  '合规保护': '#dc2626',
  '权益发放': '#ef4444',
  '数据操作': '#0891b2',
  '内部动作': '#475569',
  '用户触达': '#f59e0b',
  '消息触达': '#f59e0b',
  '结构复用': '#64748b',
  '其他':     '#6d5efc',
}

/**
 * 不能有上游节点的节点类型（START 是流程唯一入口）。
 *
 * 说明：
 * - BEHAVIOR_TRIGGER / MQ_TRIGGER / SCHEDULED_TRIGGER 虽然是触发器，
 *   但它们放在 START 之后，需要有 target handle 接受 START 连线；
 * - TAGGER 的触发属性是动态的（`mode=realtime`），在 `CanvasNode` 内单独判断。
 */
export const TRIGGER_TYPES = new Set(['START'])

/** 终止节点类型：没有后继出口。 */
export const TERMINAL_TYPES = new Set(['DIRECT_RETURN', 'END'])

/**
 * 节点类型编码 -> 默认显示名。
 *
 * 当节点未自定义名称时，编辑器使用此映射做兜底文案。
 */
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
  WAIT:              '等待',
  GOAL_CHECK:        '目标检测',
  SUPPRESSION_CHECK: '抑制检查',
  QUIET_HOURS:       '静默时段',
  CHANNEL_AVAILABILITY: '渠道可达',
  FREQUENCY_CAP:     '频率限制',
  CANVAS_TRIGGER:    '触发子画布',
  SUB_FLOW_REF:      '子流程引用',
  API_TRIGGER:       'API触发',
  AUDIENCE_TRIGGER:  '受众触发',
  SEND_EMAIL:        '发送邮件',
  SEND_SMS:          '发送短信',
  SEND_PUSH:         '发送Push',
  SEND_IN_APP:       '发送站内信',
  SEND_WECHAT:       '发送微信消息',
  UPDATE_PROFILE:    '更新属性',
  TAG_OPERATION:     '标签操作',
  POINTS_OPERATION:  '积分操作',
  CREATE_TASK:       '创建任务',
  TRACK_EVENT:       '记录事件',
  RANDOM_SPLIT:      '随机分流',
  EXPERIMENT:        '实验',
  SCORING:           '用户评分',
  RECOMMENDATION:    '推荐',
  AI_NEXT_BEST_ACTION: 'AI下一步动作',
  MERGE:             '合并',
  LOOP:              '循环',
  GOTO:              '跳转',
  TRANSFER_JOURNEY:  '跳转旅程',
  SUBFLOW:           '子流程',
  GROUP:             '分组',
  TEMPLATE_NODE:     '模板节点',
}

/** 画布节点 data 类型，供 React Flow Node<T> 使用。 */
export type CanvasNodeData = {
  /** 节点类型编码（与后端 node_type_registry.type_key 对齐）。 */
  nodeType: string

  /** 节点展示名（可由用户重命名）。 */
  name: string

  /** 节点分类（用于面板分组与颜色主题）。 */
  category: string

  /** 节点业务配置（后继分支、参数映射、脚本等）。 */
  bizConfig: Record<string, unknown>
  outletSchema?: string
}
