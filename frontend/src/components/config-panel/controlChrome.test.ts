import { describe, expect, it } from 'vitest'
import { getControlChrome, getControlLabelStyle } from './controlChrome'

describe('controlChrome', () => {
  it('returns the heavier iOS-like select/input shell', () => {
    const chrome = getControlChrome()

    expect(chrome.height).toBe(52)
    expect(chrome.borderRadius).toBe(18)
    expect(chrome.border).toBe('1px solid #d8e3f2')
    expect(chrome.background).toBe('linear-gradient(180deg,#ffffff 0%,#f6f8fb 100%)')
    expect(chrome.boxShadow).toBe(
      'inset 0 1px 0 rgba(255,255,255,.95), 0 5px 14px rgba(15,23,42,.04)',
    )
  })

  it('keeps enough horizontal padding for select affordances', () => {
    const chrome = getControlChrome()

    expect(chrome.height).toBe(52)
    expect(chrome.borderRadius).toBe(18)
    expect(chrome.paddingInline).toBe(16)
  })

  it('returns a subdued field label style', () => {
    const labelStyle = getControlLabelStyle()

    expect(labelStyle.fontSize).toBe(11)
    expect(labelStyle.color).toBe('#7b8798')
  })
})
