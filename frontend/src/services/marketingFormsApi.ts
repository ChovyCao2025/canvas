import http from './api'
import type { R } from '../types'

/** 营销表单定义，供运营后台创建、编辑和上下线。 */
export interface MarketingFormDefinition {
  /** 表单 ID。 */
  id: number

  /** 公开访问 key，用于生成外部提交地址。 */
  publicKey: string

  /** 表单名称。 */
  name: string

  /** 表单描述。 */
  description?: string | null

  /** 表单状态，例如 ACTIVE / INACTIVE。 */
  status: string

  /** 字段 schema JSON 字符串，前端按数组字段定义解析。 */
  fieldSchemaJson: string

  /** 提交后动作 JSON，例如触发 CDP 事件或写入标签。 */
  submitActionJson?: string | null

  /** 提交成功提示文案。 */
  successMessage?: string | null

  /** 创建时间。 */
  createdAt?: string | null

  /** 更新时间。 */
  updatedAt?: string | null
}

/** 公开端表单定义，不包含运营侧提交动作细节。 */
export interface PublicMarketingForm {
  /** 公开访问 key。 */
  publicKey: string

  /** 表单名称。 */
  name: string

  /** 表单描述。 */
  description?: string | null

  /** 表单状态。 */
  status?: string

  /** 字段 schema JSON 字符串。 */
  fieldSchemaJson?: string

  /** 首次运行公开目录返回的字段 schema，可为字段 key 数组或字段对象数组。 */
  fieldSchema?: unknown

  /** 提交成功提示文案。 */
  successMessage?: string | null
}

/** 表单提交记录，用于运营查看线索和事件触发结果。 */
export interface MarketingFormSubmission {
  /** 提交记录 ID。 */
  id: number

  /** 表单 ID。 */
  formId: number

  /** 表单公开 key。 */
  publicKey: string

  /** 识别出的用户 ID。 */
  userId?: string | null

  /** 匿名访客 ID。 */
  anonymousId?: string | null

  /** 表单响应 JSON。 */
  responseJson: string

  /** UTM 归因参数 JSON。 */
  utmJson?: string | null

  /** 授权渠道，例如 EMAIL / SMS。 */
  consentChannel?: string | null

  /** 授权状态。 */
  consentStatus?: string | null

  /** 提交幂等键，避免重复落库和重复触发事件。 */
  idempotencyKey: string

  /** 提交后触发的 CDP 事件编码。 */
  triggerEventCode?: string | null

  /** 提交时间。 */
  createdAt?: string | null
}

/** 运营端创建或更新表单的请求体。 */
export interface MarketingFormPayload {
  /** 指定公开 key，缺省由后端生成。 */
  publicKey?: string

  /** 表单名称。 */
  name: string

  /** 表单描述。 */
  description?: string

  /** 字段 schema JSON 字符串。 */
  fieldSchemaJson?: string

  /** 提交动作 JSON 字符串。 */
  submitActionJson?: string

  /** 成功提示文案。 */
  successMessage?: string

  /** 是否启用表单。 */
  active?: boolean

  /** 创建人。 */
  createdBy?: string
}

/** 公开端提交表单的请求体。 */
export interface PublicSubmitPayload {
  /** 字段响应，以字段 key 为键。 */
  response: Record<string, unknown>

  /** UTM 归因参数。 */
  utm?: Record<string, unknown>

  /** 匿名访客 ID。 */
  anonymousId?: string

  /** 客户端幂等键。 */
  idempotencyKey?: string

  /** 授权渠道。 */
  consentChannel?: string

  /** 授权状态。 */
  consentStatus?: string
}

/** 公开端提交结果。 */
export interface PublicSubmitResult {
  /** 提交记录 ID。 */
  submissionId?: number | null

  /** 识别或创建的用户 ID。 */
  userId?: string | null

  /** 成功提示文案。 */
  successMessage?: string | null

  /** 已触发的 CDP 事件编码。 */
  triggerEventCode?: string | null

  /** 是否成功触发表单提交后的自动化动作。 */
  triggered: boolean
}

type MarketingFormsHttpClient = Pick<typeof http, 'get' | 'post' | 'put'>

function encodePath(value: string | number) {
  return encodeURIComponent(String(value))
}

/** 移除空值字段，避免创建/更新时用空字符串覆盖后端默认配置。 */
function compactBody<T extends object>(body: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(body).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  ) as Partial<T>
}

/** 创建营销表单 API，支持在测试中注入 HTTP client。 */
export function createMarketingFormsApi(client: MarketingFormsHttpClient = http) {
  return {
    /** 查询运营端表单定义列表。 */
    list: () => client.get<R<MarketingFormDefinition[]>, R<MarketingFormDefinition[]>>('/canvas/marketing-forms'),
    /** 查询单个表单定义。 */
    get: (id: number) =>
      client.get<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(`/canvas/marketing-forms/${encodePath(id)}`),
    /** 创建表单定义，提交前清理空字段以保留后端默认值。 */
    create: (payload: MarketingFormPayload) =>
      client.post<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(
        '/canvas/marketing-forms',
        compactBody(payload),
      ),
    /** 更新表单定义，路径 ID 做统一编码。 */
    update: (id: number, payload: MarketingFormPayload) =>
      client.put<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(
        `/canvas/marketing-forms/${encodePath(id)}`,
        compactBody(payload),
      ),
    /** 启用或停用表单，控制公开端是否允许继续提交。 */
    setStatus: (id: number, active: boolean) =>
      client.put<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(
        `/canvas/marketing-forms/${encodePath(id)}/status`,
        { active },
      ),
    /** 查询提交记录；formId 为空时查最近全量提交，limit 始终显式拼接。 */
    submissions: (formId?: number, limit = 50) => {
      // 这里手工构造查询串是为了兼容仅暴露 get/post/put 的轻量测试 client。
      const query = formId == null
        ? `?limit=${encodeURIComponent(String(limit))}`
        : `?formId=${encodePath(formId)}&limit=${encodeURIComponent(String(limit))}`
      return client.get<R<MarketingFormSubmission[]>, R<MarketingFormSubmission[]>>(
        `/canvas/marketing-forms/submissions${query}`,
      )
    },
    /** 查询公开端表单定义。 */
    publicForm: (publicKey: string) =>
      client.get<R<PublicMarketingForm>, R<PublicMarketingForm>>(
        `/public/marketing-forms/${encodePath(publicKey)}`,
      ),
    /** 提交公开表单，后端负责幂等、用户识别、授权记录和 CDP 事件触发。 */
    publicSubmit: (publicKey: string, payload: PublicSubmitPayload) =>
      client.post<R<PublicSubmitResult>, R<PublicSubmitResult>>(
        `/public/marketing-forms/${encodePath(publicKey)}/submit`,
        compactBody(payload),
      ),
  }
}

export const marketingFormsApi = createMarketingFormsApi()
