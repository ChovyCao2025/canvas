import type { CSSProperties } from 'react'

export function getControlChrome(): CSSProperties {
  return {
    height: 52,
    borderRadius: 18,
    border: '1px solid #d8e3f2',
    background: 'linear-gradient(180deg,#ffffff 0%,#f6f8fb 100%)',
    boxShadow: 'inset 0 1px 0 rgba(255,255,255,.95), 0 5px 14px rgba(15,23,42,.04)',
  }
}

export function getControlLabelStyle(): CSSProperties {
  return {
    fontSize: 11,
    color: '#7b8798',
  }
}
