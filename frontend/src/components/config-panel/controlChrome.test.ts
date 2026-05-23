import { describe, expect, it } from 'vitest'
import { getControlChrome, getControlLabelStyle } from './controlChrome'

describe('controlChrome', () => {
  it('returns the heavier iOS-like select/input shell', () => {
    const chrome = getControlChrome()

    expect(chrome.height).toBe(52)
    expect(chrome.borderRadius).toBe(18)
    expect(chrome.border).toContain('#d8e3f2')
    expect(String(chrome.background)).toContain('linear-gradient')
  })

  it('returns a subdued field label style', () => {
    const labelStyle = getControlLabelStyle()

    expect(labelStyle.fontSize).toBe(11)
    expect(labelStyle.color).toBe('#7b8798')
  })
})
