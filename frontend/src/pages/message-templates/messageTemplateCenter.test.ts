import { describe, expect, it } from 'vitest'
import {
  channelLabel,
  formatMissingVariables,
  localTemplatePreview,
  templatePreviewState,
  templateStatusView,
  variablesFromBody,
} from './messageTemplateCenter'

describe('messageTemplateCenter', () => {
  it('extracts variables in display order without duplicates', () => {
    expect(variablesFromBody('Hi {{firstName}}, {{firstName}} uses {{couponCode}}')).toEqual([
      'firstName',
      'couponCode',
    ])
  })

  it('formats missing variables, channel labels, and statuses', () => {
    expect(formatMissingVariables(['couponCode', 'tier'])).toBe('Missing: couponCode, tier')
    expect(formatMissingVariables([])).toBe('All variables resolved')
    expect(channelLabel('IN_APP')).toBe('In-app')
    expect(templateStatusView('DRAFT')).toEqual({ text: '草稿', color: 'default' })
  })

  it('maps preview state for UI rendering', () => {
    expect(templatePreviewState({ renderedBody: 'Hi Alice', missingVariables: [] })).toEqual({
      status: 'READY',
      text: 'Hi Alice',
    })
    expect(templatePreviewState({ renderedBody: 'Hi {{couponCode}}', missingVariables: ['couponCode'] }).status)
      .toBe('MISSING_VARIABLES')
  })

  it('renders local previews before a template is persisted', () => {
    expect(localTemplatePreview('Hi {{firstName}}, use {{couponCode}}', { firstName: 'Alice' })).toEqual({
      renderedBody: 'Hi Alice, use {{couponCode}}',
      missingVariables: ['couponCode'],
    })
  })
})
