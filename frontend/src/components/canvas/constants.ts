/**
 * Canvas node display constants for the governed node catalog.
 */
export const CATEGORY_COLORS: Record<string, string> = {
  '基础控制': '#475569',
  '入口触发': '#0f766e',
  '条件与分流': '#1677ff',
  '等待与汇聚': '#4f46e5',
  '动作执行': '#0891b2',
  '消息触达': '#f59e0b',
  'AI智能': '#7c3aed',
  '数据与权益': '#ef4444',
  '流程复用': '#64748b',
}

/** 节点分类实色，用于分类色条和节点库 hover 态。 */
export const CATEGORY_SOLID: Record<string, string> = {
  '基础控制': '#475569',
  '入口触发': '#0f766e',
  '条件与分流': '#1677ff',
  '等待与汇聚': '#4f46e5',
  '动作执行': '#0891b2',
  '消息触达': '#f59e0b',
  'AI智能': '#7c3aed',
  '数据与权益': '#ef4444',
  '流程复用': '#64748b',
}

export const TRIGGER_TYPES = new Set(['START'])

export const TERMINAL_TYPES = new Set(['DIRECT_RETURN', 'END'])

export const PUBLISH_TRIGGER_NODE_TYPES = new Set([
  'DIRECT_CALL',
  'EVENT_TRIGGER',
  'MQ_TRIGGER',
  'SCHEDULED_TRIGGER',
])

export const DEFAULT_NAMES: Record<string, string> = {
  START: '开始',
  END: '结束',
  DIRECT_RETURN: '直调返回',
  DIRECT_CALL: 'API入口',
  EVENT_TRIGGER: '事件触发',
  MQ_TRIGGER: 'MQ消息触发',
  SCHEDULED_TRIGGER: '定时触发',
  IF_CONDITION: 'IF判断',
  SPLIT: '通用分流',
  WAIT: '等待',
  HUB: '集线器',
  AGGREGATE: '聚合评估',
  THRESHOLD: '阈值触发',
  API_CALL: '接口调用',
  SEND_MQ: '发送MQ',
  GROOVY: 'Groovy脚本',
  SEND_MESSAGE: '发送消息',
  AI_LLM: 'AI 智能节点',
  TAGGER: 'Tagger 标签',
  COMMIT_ACTION: '提交动作',
  SUB_FLOW_REF: '子流程引用',
}

export type CanvasNodeData = {
  nodeType: string
  name: string
  category: string
  bizConfig: Record<string, unknown>
  outletSchema?: string
}
