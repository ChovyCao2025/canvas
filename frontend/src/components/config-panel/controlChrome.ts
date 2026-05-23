import type { CSSProperties } from 'react'

export function getControlChrome(): CSSProperties {
  return {
    height: 60,
    borderRadius: 18,
    paddingInline: 22,
    border: '1px solid transparent',
    background: '#f5f5f7',
    boxShadow: 'inset 0 0 0 1px #c7c7cc',
  }
}

export function getInlineControlChrome(): CSSProperties {
  return {
    height: 30,
    borderRadius: 10,
    paddingInline: 9,
    border: '1px solid #c7c7cc',
    background: '#ffffff',
  }
}

export function getControlLabelStyle(): CSSProperties {
  return {
    fontSize: 14,
    fontWeight: 600,
    color: '#6e6e73',
  }
}

export const CONTROL_SELECT_CLASS_NAMES = {
  popup: { root: 'config-panel-ios-dropdown' },
} as const

export const CONTROL_CHROME_SELECTOR_CSS = `
  .config-panel-form .ant-form-item {
    margin-bottom: 16px;
  }

  .config-panel-form .ant-form-item-label {
    padding-bottom: 6px !important;
  }

  .config-panel-form .ant-form-item-label > label {
    height: auto !important;
    color: #1d1d1f !important;
    font-size: 14px !important;
    font-weight: 600 !important;
    line-height: 1.35 !important;
  }

  .config-panel-ios-select.ant-select-single,
  .config-panel-ios-auto-complete.ant-select-single {
    height: 60px !important;
  }

  .config-panel-ios-select .ant-select-selector,
  .config-panel-ios-auto-complete .ant-select-selector,
  .config-panel-ios-input-number,
  .config-panel-ios-textarea,
  .config-panel-ios-input {
    border-radius: 18px !important;
    border: 1px solid transparent !important;
    background: #f5f5f7 !important;
    box-shadow: inset 0 0 0 1px #c7c7cc !important;
    transition: border-color .16s ease, box-shadow .16s ease, background-color .16s ease !important;
  }

  .config-panel-ios-select:hover .ant-select-selector,
  .config-panel-ios-auto-complete:hover .ant-select-selector,
  .config-panel-ios-input-number:hover,
  .config-panel-ios-textarea:hover,
  .config-panel-ios-input:hover {
    background: #f0f0f2 !important;
    box-shadow: inset 0 0 0 1px #aeb0b8 !important;
  }

  .config-panel-ios-select.ant-select-focused .ant-select-selector,
  .config-panel-ios-auto-complete.ant-select-focused .ant-select-selector,
  .config-panel-ios-input-number.ant-input-number-focused,
  .config-panel-ios-textarea:focus-within,
  .config-panel-ios-input:focus {
    background: #ffffff !important;
    box-shadow: 0 0 0 4px rgba(0,113,227,.14), inset 0 0 0 1px #0071e3 !important;
    outline: none !important;
  }

  .config-panel-ios-select .ant-select-selector,
  .config-panel-ios-auto-complete .ant-select-selector {
    height: 60px !important;
    min-height: 60px !important;
    padding: 0 22px !important;
    align-items: center !important;
  }

  .config-panel-ios-select .ant-select-selection-placeholder,
  .config-panel-ios-select .ant-select-selection-item,
  .config-panel-ios-auto-complete .ant-select-selection-placeholder,
  .config-panel-ios-auto-complete .ant-select-selection-item {
    line-height: 58px !important;
    font-size: 14px !important;
    font-weight: 500 !important;
    color: #1d1d1f !important;
  }

  .config-panel-ios-select.ant-select-single .ant-select-selector .ant-select-selection-search,
  .config-panel-ios-auto-complete.ant-select-single .ant-select-selector .ant-select-selection-search {
    inset-inline-start: 22px !important;
    inset-inline-end: 64px !important;
  }

  .config-panel-ios-select.ant-select-single .ant-select-selector .ant-select-selection-search-input,
  .config-panel-ios-auto-complete.ant-select-single .ant-select-selector .ant-select-selection-search-input {
    height: 58px !important;
    font-size: 14px !important;
    font-weight: 500 !important;
  }

  .config-panel-ios-input-number {
    width: 100%;
    padding-inline: 18px !important;
  }

  .config-panel-ios-input-number input,
  .config-panel-ios-textarea,
  .config-panel-ios-textarea textarea,
  .config-panel-ios-input {
    font-size: 14px;
    font-weight: 500;
    color: #1d1d1f;
  }

  .config-panel-ios-textarea,
  .config-panel-ios-textarea textarea,
  .config-panel-ios-input {
    padding-inline: 22px !important;
  }

  .config-panel-ios-select .ant-select-arrow,
  .config-panel-ios-auto-complete .ant-select-arrow {
    width: 34px !important;
    height: 34px !important;
    inset-inline-end: 13px !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    margin-top: -17px !important;
    border-radius: 999px !important;
    background: rgba(255,255,255,.86) !important;
    box-shadow: inset 0 0 0 1px rgba(0,0,0,.06) !important;
    color: #6e6e73 !important;
    font-size: 12px !important;
  }

  .config-panel-ios-select .ant-select-arrow svg,
  .config-panel-ios-auto-complete .ant-select-arrow svg {
    width: 14px !important;
    height: 14px !important;
  }

  .config-panel-ios-select.ant-select-sm.ant-select-single,
  .config-panel-ios-auto-complete.ant-select-sm.ant-select-single {
    height: 30px !important;
  }

  .config-panel-ios-select.ant-select-sm .ant-select-selector,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selector {
    min-height: 30px !important;
    height: 30px !important;
    border-radius: 10px !important;
    padding: 0 9px !important;
  }

  .config-panel-ios-select.ant-select-sm .ant-select-selection-placeholder,
  .config-panel-ios-select.ant-select-sm .ant-select-selection-item,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selection-placeholder,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selection-item {
    line-height: 28px !important;
    font-size: 12px !important;
  }

  .config-panel-ios-select.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search,
  .config-panel-ios-auto-complete.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search {
    inset-inline-start: 9px !important;
    inset-inline-end: 34px !important;
  }

  .config-panel-ios-select.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search-input,
  .config-panel-ios-auto-complete.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search-input {
    height: 28px !important;
    font-size: 12px !important;
  }

  .config-panel-ios-select.ant-select-sm .ant-select-arrow,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-arrow {
    width: 22px !important;
    height: 22px !important;
    inset-inline-end: 5px !important;
    margin-top: -11px !important;
  }

  .config-panel-ios-dropdown {
    padding: 6px !important;
    border-radius: 16px !important;
    border: 1px solid rgba(0,0,0,.08) !important;
    background: rgba(255,255,255,.96) !important;
    box-shadow: 0 18px 48px rgba(0,0,0,.14) !important;
    backdrop-filter: blur(18px);
  }

  .config-panel-ios-dropdown .ant-select-item {
    min-height: 34px !important;
    padding: 7px 10px !important;
    border-radius: 12px !important;
    color: #1d1d1f !important;
    font-size: 13px !important;
    font-weight: 500 !important;
  }

  .config-panel-ios-dropdown .ant-select-item-option-active:not(.ant-select-item-option-disabled) {
    background: #f5f5f7 !important;
  }

  .config-panel-ios-dropdown .ant-select-item-option-selected:not(.ant-select-item-option-disabled) {
    background: rgba(0,113,227,.1) !important;
    color: #0071e3 !important;
    font-weight: 600 !important;
  }
`
