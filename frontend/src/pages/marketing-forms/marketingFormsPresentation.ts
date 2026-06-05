export interface MarketingFormFieldOption {
  label?: string
  value: string
}

export interface MarketingFormField {
  key: string
  label?: string
  type?: string
  required?: boolean
  placeholder?: string
  options?: MarketingFormFieldOption[]
}

export const DEFAULT_FORM_SCHEMA = JSON.stringify([
  { key: 'email', label: '邮箱', type: 'email', required: true },
  { key: 'name', label: '姓名', type: 'text', required: false },
  { key: 'marketingConsent', label: '同意接收营销消息', type: 'checkbox', required: false },
], null, 2)

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

export function parseFormFields(json?: string | null): MarketingFormField[] {
  if (!json) return []
  try {
    const parsed = JSON.parse(json)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter(item => item && typeof item === 'object' && typeof item.key === 'string' && item.key.trim())
      .map(item => ({
        key: item.key.trim(),
        label: typeof item.label === 'string' ? item.label : item.key,
        type: typeof item.type === 'string' ? item.type : 'text',
        required: Boolean(item.required),
        placeholder: typeof item.placeholder === 'string' ? item.placeholder : undefined,
        options: Array.isArray(item.options)
          ? item.options
            .filter((option: unknown) => option && typeof option === 'object' && 'value' in option)
            .map((option: any) => ({
              label: typeof option.label === 'string' ? option.label : String(option.value),
              value: String(option.value),
            }))
          : undefined,
      }))
  } catch {
    return []
  }
}

export function formatFormDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function publicFormPath(publicKey?: string | null) {
  return publicKey ? `/public/forms/${encodeURIComponent(publicKey)}` : ''
}

export function summarizeFormSchema(json?: string | null) {
  const fields = parseFormFields(json)
  const required = fields.filter(field => field.required).length
  return `字段 ${fields.length} 个，必填 ${required} 个`
}

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
