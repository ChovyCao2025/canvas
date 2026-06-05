import { describe, expect, it } from 'vitest'
import {
  formStatusView,
  parseFormFields,
  publicFormPath,
  responsePreview,
  summarizeFormSchema,
} from './marketingFormsPresentation'

describe('marketingFormsPresentation', () => {
  it('maps form states and public paths', () => {
    expect(formStatusView('ACTIVE')).toEqual({ text: '已启用', color: 'green' })
    expect(formStatusView('INACTIVE')).toEqual({ text: '已停用', color: 'default' })
    expect(publicFormPath('signup form')).toBe('/public/forms/signup%20form')
  })

  it('parses supported fields and summarizes required counts', () => {
    const schema = JSON.stringify([
      { key: 'email', label: '邮箱', type: 'email', required: true },
      { key: 'segment', type: 'select', options: [{ label: 'VIP', value: 'vip' }] },
      { missing: 'key' },
    ])

    const fields = parseFormFields(schema)

    expect(fields).toHaveLength(2)
    expect(fields[0]).toMatchObject({ key: 'email', label: '邮箱', type: 'email', required: true })
    expect(fields[1].options).toEqual([{ label: 'VIP', value: 'vip' }])
    expect(summarizeFormSchema(schema)).toBe('字段 2 个，必填 1 个')
  })

  it('formats submission response previews safely', () => {
    expect(responsePreview('{"email":"lead@example.com","name":"Alice"}')).toBe('email: lead@example.com / name: Alice')
    expect(responsePreview('not-json')).toBe('not-json')
    expect(parseFormFields('not-json')).toEqual([])
  })
})
