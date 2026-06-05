/**
 * 类型职责：前端共享业务类型集合，覆盖后端响应、画布、元数据、标签和导入模型。
 *
 * 维护说明：这些类型是页面和服务层之间的契约，字段名应尽量与后端 DTO 保持一致。
 */
// ============================================================
// 后端 API 响应类型
// ============================================================

export interface R<T> {
  /** 业务状态码，0 表示成功。 */
  code: number

  /** 稳定错误码，成功响应通常为空。 */
  errorCode?: string

  /** 响应消息。 */
  message: string

  /** 具体数据载荷。 */
  data: T

  /** 链路追踪 ID，用于错误排查时关联日志。 */
  traceId?: string
}

/** 后端统一分页结果。 */
export interface PageResult<T> {
  /** 总记录数。 */
  total: number

  /** 当前分页结果。 */
  list: T[]
}

// ============================================================
// 画布领域
// ============================================================

/** 画布状态：0草稿 1已发布 2已下线 */
export type CanvasStatus = 0 | 1 | 2

/** 画布列表和详情中的基础元信息。 */
export interface Canvas {
  /** 画布主键。 */
  id: number

  /** 画布名称。 */
  name: string

  /** 描述信息。 */
  description?: string

  /** 当前状态。 */
  status: CanvasStatus

  /** 正式发布版本 ID。 */
  publishedVersionId?: number

  /** 当前灰度版本 ID。 */
  canaryVersionId?: number

  /** 灰度比例（0-100）。 */
  canaryPercent?: number

  /** 创建人。 */
  createdBy?: string

  /** 是否官方示例画布。 */
  isExample?: 0 | 1

  /** 来源官方模板 key。 */
  sourceTemplateKey?: string

  /** 平铺项目分组 key。 */
  projectKey?: string

  /** 平铺项目展示名。 */
  projectName?: string

  /** 平铺文件夹分组 key。 */
  folderKey?: string

  /** 平铺文件夹展示名。 */
  folderName?: string

  /** 创建时间。 */
  createdAt: string

  /** 更新时间。 */
  updatedAt: string

  /** 画布级触发方式（REALTIME/SCHEDULED）。 */
  triggerType?: string

  /** 画布级 cron 表达式。 */
  cronExpression?: string

  /** 草稿编辑版本（乐观锁）。 */
  editVersion?: number

  /** 生效开始时间。 */
  validStart?: string

  /** 生效结束时间。 */
  validEnd?: string

  /** 总执行次数上限。 */
  maxTotalExecutions?: number

  /** 单用户每日上限。 */
  perUserDailyLimit?: number

  /** 单用户总上限。 */
  perUserTotalLimit?: number

  /** 冷却时长（秒）。 */
  cooldownSeconds?: number

  /** 控制组比例，0-50。 */
  controlGroupPercent?: number

  /** 控制组分桶盐值。 */
  controlGroupSalt?: string

  /** 转化事件编码。 */
  conversionEventCode?: string

  /** 归因窗口天数。 */
  attributionWindowDays?: number
}

/** 画布详情，包含画布元信息和草稿图结构。 */
export interface CanvasDetail {
  /** 画布元信息。 */
  canvas: Canvas

  /** 图结构 JSON。 */
  graphJson: string

  /** 草稿版本 ID。 */
  draftVersionId?: number
}

/** 画布版本记录。 */
export interface CanvasVersion {
  /** 版本记录 ID。 */
  id: number

  /** 所属画布 ID。 */
  canvasId: number

  /** 版本号。 */
  version: number

  /** 当时图结构。 */
  graphJson: string

  /** 0草稿 1已发布 2已下线（兼容历史版本列表展示） */
  status: 0 | 1 | 2

  /** 操作人。 */
  createdBy?: string

  /** 创建时间。 */
  createdAt: string
}

// ============================================================
// 画布图数据（节点中心式存储）
// ============================================================

export interface ValueRef {
  /** 固定值或上下文引用。 */
  valueType: 'CUSTOM' | 'CONTEXT'

  /** 对应值。 */
  value: string
}

/** 条件判断规则。 */
export interface ConditionRule {
  /** 字段 key。 */
  field: string

  /** 比较运算符。 */
  operator: 'EQ' | 'NEQ' | 'CONTAINS' | 'GT' | 'LT' | 'GTE' | 'LTE'

  /** 比较值。 */
  value: string

  /** 是否由人工输入。 */
  isCustom: boolean
}

/** graph_json 中的单个后端节点。 */
export interface CanvasNode {
  /** 节点 ID。 */
  id: string

  /** 节点类型。 */
  type: string

  /** 节点名称。 */
  name: string

  /** 节点配置。 */
  config: Record<string, unknown>

  /** 动态出口 schema。 */
  outletSchema?: string

  /** 画布坐标（仅前端展示用） */
  x?: number

  y?: number
}

/** graph_json 根结构。 */
export interface CanvasGraph {
  /** 节点数组（边关系放在节点 config 中）。 */
  nodes: CanvasNode[]
}

// ============================================================
// 元数据
// ============================================================

export interface NodeTypeRegistry {
  /** 节点类型编码。 */
  typeKey: string

  /** 节点类型名称。 */
  typeName: string

  /** 分类。 */
  category: string

  /** 配置 schema（JSON 字符串，解析后为 SchemaField[]）。 */
  configSchema: string

  /** 输出 schema。 */
  outputSchema: string

  /** 动态出口 schema。 */
  outletSchema?: string

  /** 摘要模板。 */
  summaryTemplate?: string

  /** 运行策略 schema。 */
  runtimePolicySchema?: string

  /** 风险等级。 */
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | string

  /** 是否触发器节点。 */
  isTrigger: 0 | 1

  /** 是否终止节点。 */
  isTerminal: 0 | 1

  /** 描述。 */
  description?: string

  /** 启用状态。 */
  enabled: 0 | 1
}

/** 可在节点配置中引用的上下文字段。 */
export interface ContextField {
  /** 字段主键。 */
  id: number

  /** 字段 key。 */
  fieldKey: string

  /** 字段展示名。 */
  fieldName: string

  /** 字段数据类型。 */
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'LIST'

  /** 来源节点类型。 */
  sourceNodeType?: string

  /** 字段描述。 */
  description?: string
}

/** 后端元数据接口返回的轻量下拉选项。 */
export interface StubOption {
  /** 实际 value。 */
  key: string

  /** 展示 label。 */
  label: string
}

/** 系统字典项完整模型。 */
export interface SystemOption {
  id: number
  tenantId?: number | null
  category: string
  optionKey: string
  label: string
  description?: string
  sortOrder: number
  enabled: 0 | 1
  systemBuiltin: 0 | 1
  createdAt?: string
  updatedAt?: string
}

/** AB 实验分组元数据。 */
export interface AbExperimentGroup {
  id: number
  experimentId: number
  groupKey: string
  label: string
  sortOrder: number
  enabled: 0 | 1
  createdAt?: string
  updatedAt?: string
}

/** antd Select 通用选项类型。 */
export type SelectOption = { label: string; value: string }

/** 表单 Schema 字段（config_schema 解析结果） */
export interface SchemaField {
  /** 字段 key。 */
  key: string

  /** 字段标题。 */
  label: string

  /** 字段控件类型。 */
  type: string

  /** 是否必填。 */
  required?: boolean

  /** 固定选项。 */
  options?: {
    label: string
    value: string
  }[]

  /** 远程数据源 URL。 */
  dataSource?: string

  /** 系统选项分类。 */
  optionCategory?: string

  /** 显隐表达式。 */
  visible?: string
}

/** 用户身份类型定义。 */
export interface IdentityType {
  id: number
  code: string
  name: string
  description?: string
  enabled: 0 | 1
  allowImport: 0 | 1
  multiValue: 0 | 1
  priority: number
  participateMapping: 0 | 1
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

/** 标签定义。 */
export interface TagDefinition {
  id: number
  name: string
  tagCode: string
  tagType: 'offline' | 'realtime'
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'
  description?: string
  enabled: 0 | 1
  manualEnabled?: 0 | 1
  defaultTtlDays?: number
  category?: string
  owner?: string
  writePolicy?: string
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

/** 标签枚举值定义。 */
export interface TagValueDefinition {
  id: number
  tagCode: string
  value: string
  label: string
  sortOrder: number
  enabled: 0 | 1
  source: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

/** 标签导入单行数据。 */
export interface TagImportRow {
  rowNo?: number
  idType: string
  idValue: string
  tagCode: string
  tagValue: string
  tagTime?: string
}

/** 标签导入执行结果。 */
export interface TagImportResult {
  batchId: number
  status: string
  totalRows: number
  successRows: number
  failedRows: number
}

/** 标签导入批次。 */
export interface TagImportBatch {
  id: number
  sourceType: string
  status: string
  fileName?: string
  externalUrl?: string
  totalRows: number
  successRows: number
  failedRows: number
  createdBy?: string
  startedAt?: string
  finishedAt?: string
  errorMessage?: string
  createdAt?: string
  updatedAt?: string
}

/** 标签导入失败明细。 */
export interface TagImportError {
  id: number
  batchId: number
  rowNo: number
  rawPayload?: string
  errorCode: string
  errorMsg: string
  createdAt?: string
}

/** 标签导入来源配置。 */
export interface TagImportSource {
  id: number
  name: string
  url: string
  method: string
  headersJson?: string
  bodyTemplate?: string
  pageParam?: string
  pageSizeParam?: string
  pageSize?: number
  recordsPath?: string
  fieldMapping?: string
  enabled: 0 | 1
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}
