/** 表单字段选项，供 select/radio/checkbox 类控件使用。 */
export interface MarketingFormFieldOption {
  /** 选项展示文案，缺省时回退为 value。 */
  label?: string

  /** 选项提交值。 */
  value: string
}

/** 营销表单字段展示模型。 */
export interface MarketingFormField {
  /** 字段 key，对应提交 response 的属性名。 */
  key: string

  /** 字段展示名。 */
  label?: string

  /** 字段类型，例如 text / email / checkbox / select。 */
  type?: string

  /** 是否必填。 */
  required?: boolean

  /** 输入占位提示。 */
  placeholder?: string

  /** 枚举选项。 */
  options?: MarketingFormFieldOption[]
}

export const DEFAULT_FORM_SCHEMA = JSON.stringify([
  { key: 'email', label: '邮箱', type: 'email', required: true },
  { key: 'name', label: '姓名', type: 'text', required: false },
  { key: 'marketingConsent', label: '同意接收营销消息', type: 'checkbox', required: false },
], null, 2)

/** 将表单状态转换为运营列表标签。 */
export function formStatusView(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'ACTIVE':
      return { text: '已启用', color: 'green' }
    case 'INACTIVE':
      return { text: '已停用', color: 'default' }
    default:
      return { text: status || '未知', color: 'default' }
  }
}

/** 解析字段 schema JSON，非法或非数组结构按空字段兜底。 */
export function parseFormFields(json?: string | null): MarketingFormField[] {
  if (!json) return []
  try {
    const parsed = JSON.parse(json)
    if (!Array.isArray(parsed)) return []
    return parsed
      // 仅保留具备有效 key 的对象字段，避免错误 schema 影响页面渲染。
      .filter(item => item && typeof item === 'object' && typeof item.key === 'string' && item.key.trim())
      .map(item => ({
        key: item.key.trim(),
        label: typeof item.label === 'string' ? item.label : item.key,
        type: typeof item.type === 'string' ? item.type : 'text',
        required: Boolean(item.required),
        placeholder: typeof item.placeholder === 'string' ? item.placeholder : undefined,
        options: Array.isArray(item.options)
          ? item.options
            // 选项只要求存在 value，label 缺失时用 value 兜底展示。
            .filter((option: unknown) => option && typeof option === 'object' && 'value' in option)
            .map((option: any) => ({
              label: typeof option.label === 'string' ? option.label : String(option.value),
              value: String(option.value),
            }))
          : undefined,
      }))
  } catch {
    // 运营编辑中的 JSON 可能暂时不完整，展示层用空数组兜底，避免页面崩溃。
    return []
  }
}

/** 格式化表单时间字段，空值用占位符展示。 */
export function formatFormDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

/** 生成公开表单访问路径，publicKey 为空时返回空串避免错误链接。 */
export function publicFormPath(publicKey?: string | null) {
  return publicKey ? `/public/forms/${encodeURIComponent(publicKey)}` : ''
}

/** 汇总字段数量和必填数量，用于表单列表快速扫描配置复杂度。 */
export function summarizeFormSchema(json?: string | null) {
  const fields = parseFormFields(json)
  const required = fields.filter(field => field.required).length
  return `字段 ${fields.length} 个，必填 ${required} 个`
}

/** 生成提交响应预览，最多展示前四个字段，非法 JSON 直接展示原文。 */
export function responsePreview(responseJson?: string | null) {
  if (!responseJson) return '-'
  try {
    const parsed = JSON.parse(responseJson)
    return Object.entries(parsed)
      .slice(0, 4)
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join(' / ') || '-'
  } catch {
    return responseJson
  }
}
