/**
 * 测试职责：验证配置面板控件的必要尺寸契约。
 *
 * 维护说明：只锁定会影响“大框突兀感”和复杂行密度的关键值。
 */
import { describe, expect, it } from 'vitest'
import { findAccessibilityIssues } from '../../test/accessibilityChecks'
import { getControlChrome, getControlLabelStyle, getInlineControlChrome } from './controlChrome'

describe('controlChrome', () => {
  it('uses compact main controls for the right inspector', () => {
    const chrome = getControlChrome()

    expect(chrome.height).toBe(40)
    expect(chrome.minHeight).toBe(40)
    expect(chrome.borderRadius).toBe(8)
    expect(chrome.paddingInline).toBe(10)
    expect(chrome.border).toBe('1px solid #d9e1ec')
    expect(chrome.background).toBe('#ffffff')
    expect(chrome.boxShadow).toBe('none')
  })

  it('uses dense inline controls for rules and mappings', () => {
    const chrome = getInlineControlChrome()

    expect(chrome.height).toBe(32)
    expect(chrome.minHeight).toBe(32)
    expect(chrome.borderRadius).toBe(7)
    expect(chrome.paddingInline).toBe(8)
  })

  it('keeps field labels subdued without becoming tiny', () => {
    const labelStyle = getControlLabelStyle()

    expect(labelStyle.fontSize).toBe(12)
    expect(labelStyle.color).toBe('#64748b')
  })

  it('keeps config-panel control contracts named and focusable', () => {
    expect(findAccessibilityIssues([
      { id: 'config-panel-field-label', role: 'label', name: '配置字段标签', focusable: false },
      { id: 'config-panel-main-control', role: 'textbox', name: '配置字段输入控件', focusable: true },
      { id: 'config-panel-inline-control', role: 'spinbutton', name: '动态列表行内控件', focusable: true },
    ])).toEqual([])
  })
})
