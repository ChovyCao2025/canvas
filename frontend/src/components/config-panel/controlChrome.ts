/**
 * 组件职责：配置面板控件的统一视觉样式定义。
 *
 * 维护说明：集中维护输入框、下拉框和标签样式，避免各字段重复写内联样式。
 */
import type { CSSProperties } from 'react'

/** 标准大尺寸控件外观，用于右侧配置面板的主要输入项。 */
export function getControlChrome(): CSSProperties {
  return {
    height: 40,
    borderRadius: 8,
    paddingInline: 10,
    border: '1px solid #d9e1ec',
    background: '#ffffff',
    boxShadow: 'none',
  }
}

/** 行内小控件外观，用于动态列表、表格内字段等紧凑场景。 */
export function getInlineControlChrome(): CSSProperties {
  return {
    height: 32,
    borderRadius: 7,
    paddingInline: 8,
    border: '1px solid #d9e1ec',
    background: '#ffffff',
  }
}

/** 表单标签统一样式，保持复杂 schema 字段的标题层级一致。 */
export function getControlLabelStyle(): CSSProperties {
  return {
    fontSize: 12,
    fontWeight: 700,
    color: '#64748b',
  }
}

/** Ant Design Select v5 的 popup className 配置，集中引用避免调用处写错结构。 */
export const CONTROL_SELECT_CLASS_NAMES = {
  popup: { root: 'config-panel-ios-dropdown' },
} as const

/**
 * 配置面板控件覆盖样式。
 *
 * 这里使用 class 选择器覆盖 antd 内部结构，适合全局注入一次；
 * 若调整控件高度或圆角，要同步检查 ConfigPanel 内的布局间距。
 */
export const CONTROL_CHROME_SELECTOR_CSS = `
  .config-panel-form .ant-form-item {
    margin-bottom: 10px;
  }

  .config-panel-form .ant-form-item-label {
    padding-bottom: 5px !important;
  }

  .config-panel-form .ant-form-item-label > label {
    height: auto !important;
    color: #64748b !important;
    font-size: 12px !important;
    font-weight: 700 !important;
    line-height: 1.35 !important;
  }

  .config-panel-ios-select.ant-select-single,
  .config-panel-ios-auto-complete.ant-select-single {
    height: 40px !important;
  }

  .config-panel-ios-select .ant-select-selector,
  .config-panel-ios-auto-complete .ant-select-selector,
  .config-panel-ios-input-number,
  .config-panel-ios-textarea,
  .config-panel-ios-input {
    border-radius: 8px !important;
    border: 1px solid #d9e1ec !important;
    background: #ffffff !important;
    box-shadow: none !important;
    transition: border-color .16s ease, box-shadow .16s ease, background-color .16s ease !important;
  }

  .config-panel-ios-select:hover .ant-select-selector,
  .config-panel-ios-auto-complete:hover .ant-select-selector,
  .config-panel-ios-input-number:hover,
  .config-panel-ios-textarea:hover,
  .config-panel-ios-input:hover {
    background: #f8fafc !important;
    border-color: #c5d1e0 !important;
    box-shadow: none !important;
  }

  .config-panel-ios-select.ant-select-focused .ant-select-selector,
  .config-panel-ios-auto-complete.ant-select-focused .ant-select-selector,
  .config-panel-ios-input-number.ant-input-number-focused,
  .config-panel-ios-textarea:focus-within,
  .config-panel-ios-input:focus {
    background: #ffffff !important;
    border-color: #1677ff !important;
    box-shadow: 0 0 0 3px rgba(22,119,255,.12) !important;
    outline: none !important;
  }

  .config-panel-ios-select .ant-select-selector,
  .config-panel-ios-auto-complete .ant-select-selector {
    height: 40px !important;
    min-height: 40px !important;
    padding: 0 10px !important;
    align-items: center !important;
  }

  .config-panel-ios-select .ant-select-selection-placeholder,
  .config-panel-ios-select .ant-select-selection-item,
  .config-panel-ios-auto-complete .ant-select-selection-placeholder,
  .config-panel-ios-auto-complete .ant-select-selection-item {
    line-height: 38px !important;
    font-size: 13px !important;
    font-weight: 650 !important;
    color: #172033 !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    white-space: nowrap !important;
  }

  /* placeholder 单独设浅色，覆盖上面的深色规则 */
  .config-panel-ios-select .ant-select-selection-placeholder,
  .config-panel-ios-auto-complete .ant-select-selection-placeholder {
    color: #b8c4ce !important;
    font-weight: 400 !important;
  }

  /* sm AutoComplete 搜索框垂直居中 */
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selector .ant-select-selection-search {
    top: 0 !important;
    bottom: 0 !important;
    display: flex !important;
    align-items: center !important;
  }

  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selector .ant-select-selection-search-input {
    height: 100% !important;
  }

  .config-panel-ios-select.ant-select-single .ant-select-selector .ant-select-selection-search,
  .config-panel-ios-auto-complete.ant-select-single .ant-select-selector .ant-select-selection-search {
    inset-inline-start: 10px !important;
    inset-inline-end: 36px !important;
  }

  .config-panel-ios-select.ant-select-single .ant-select-selector .ant-select-selection-search-input,
  .config-panel-ios-auto-complete.ant-select-single .ant-select-selector .ant-select-selection-search-input {
    height: 38px !important;
    font-size: 13px !important;
    font-weight: 650 !important;
  }

  .config-panel-ios-input-number {
    width: 100%;
    padding-inline: 10px !important;
  }

  .config-panel-ios-input-number .ant-input-number-input {
    padding-inline: 0 !important;
    width: 100% !important;
  }

  .config-panel-ios-input-number .ant-input-number-input-wrap {
    height: 100% !important;
    display: flex !important;
    align-items: center !important;
  }

  .config-panel-ios-input-number input,
  .config-panel-ios-textarea,
  .config-panel-ios-textarea textarea,
  .config-panel-ios-input {
    font-size: 14px;
    font-weight: 650;
    color: #172033;
  }

  .config-panel-ios-input-number input {
    text-align: center !important;
  }

  .config-panel-ios-textarea,
  .config-panel-ios-textarea textarea,
  .config-panel-ios-input {
    padding-inline: 10px !important;
  }

  .config-panel-ios-select .ant-select-arrow,
  .config-panel-ios-auto-complete .ant-select-arrow {
    width: 24px !important;
    height: 24px !important;
    inset-inline-end: 7px !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    margin-top: -12px !important;
    border-radius: 7px !important;
    background: #f8fafc !important;
    box-shadow: none !important;
    color: #64748b !important;
    font-size: 12px !important;
  }

  .config-panel-ios-select .ant-select-arrow svg,
  .config-panel-ios-auto-complete .ant-select-arrow svg {
    width: 12px !important;
    height: 12px !important;
  }

  .config-panel-ios-select.ant-select-sm.ant-select-single,
  .config-panel-ios-auto-complete.ant-select-sm.ant-select-single {
    height: 32px !important;
  }

  .config-panel-ios-select.ant-select-sm .ant-select-selector,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selector {
    min-height: 32px !important;
    height: 32px !important;
    border-radius: 7px !important;
    padding: 0 8px !important;
  }

  .config-panel-ios-select.ant-select-sm .ant-select-selection-placeholder,
  .config-panel-ios-select.ant-select-sm .ant-select-selection-item,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selection-placeholder,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selection-item {
    line-height: 30px !important;
    font-size: 12px !important;
  }

  .config-panel-ios-select.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search,
  .config-panel-ios-auto-complete.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search {
    inset-inline-start: 8px !important;
    inset-inline-end: 30px !important;
  }

  .config-panel-ios-select.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search-input,
  .config-panel-ios-auto-complete.ant-select-sm.ant-select-single .ant-select-selector .ant-select-selection-search-input {
    height: 30px !important;
    line-height: 30px !important;
    font-size: 12px !important;
  }

  .config-panel-ios-auto-complete .ant-select-selector .ant-select-selection-search-input::placeholder,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-selector .ant-select-selection-search-input::placeholder {
    color: #b8c4ce !important;
    opacity: 1 !important;
  }

  .config-panel-ios-select.ant-select-sm .ant-select-arrow,
  .config-panel-ios-auto-complete.ant-select-sm .ant-select-arrow {
    width: 20px !important;
    height: 20px !important;
    inset-inline-end: 5px !important;
    margin-top: -10px !important;
  }

  .config-panel-ios-dropdown {
    padding: 5px !important;
    border-radius: 10px !important;
    border: 1px solid rgba(0,0,0,.08) !important;
    background: rgba(255,255,255,.96) !important;
    box-shadow: 0 10px 26px rgba(15,23,42,.12) !important;
    backdrop-filter: blur(10px);
  }

  .config-panel-ios-dropdown .ant-select-item {
    min-height: 32px !important;
    padding: 6px 8px !important;
    border-radius: 7px !important;
    color: #172033 !important;
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
