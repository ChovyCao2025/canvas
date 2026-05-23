import type { CSSProperties, ReactNode } from 'react'
import { Tag, Typography } from 'antd'

const CARD_STYLE: CSSProperties = {
  background: '#fff',
  border: '1px solid #e8eaef',
  borderRadius: 14,
  boxShadow: '0 6px 18px rgba(15, 23, 42, 0.06)',
}

const TAGGER_STYLES = {
  shell: {
    background: '#eff6ff',
    border: '1px solid #dbeafe',
  },
  badge: {
    background: '#dbeafe',
    color: '#1d4ed8',
    border: '1px solid #bfdbfe',
  },
  accent: '#1d4ed8',
}

const ROUTE_STYLES = {
  success: {
    background: '#eff6ff',
    border: '1px solid #bfdbfe',
    title: '#1d4ed8',
    value: '#1e3a8a',
  },
  danger: {
    background: '#fef2f2',
    border: '1px solid #fecaca',
    title: '#dc2626',
    value: '#991b1b',
  },
} as const

export interface NodeHeaderCardProps {
  tone: 'default' | 'tagger'
  typeBadge: string
  title: string
  metaBadges: string[]
  description?: string
  statusLabel: string
}

export function NodeHeaderCard({
  tone,
  typeBadge,
  title,
  metaBadges,
  description,
  statusLabel,
}: NodeHeaderCardProps) {
  const isTagger = tone === 'tagger'
  const shellStyle = isTagger ? TAGGER_STYLES.shell : {}

  return (
    <div
      style={{
        ...CARD_STYLE,
        padding: 14,
        marginBottom: 12,
        ...shellStyle,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ minWidth: 0 }}>
          <Tag
            style={{
              margin: 0,
              borderRadius: 999,
              padding: '0 10px',
              ...(isTagger ? TAGGER_STYLES.badge : { background: '#f8fafc', color: '#475569', border: '1px solid #e2e8f0' }),
            }}
          >
            {typeBadge}
          </Tag>
          <Typography.Title level={5} style={{ margin: '10px 0 6px', color: '#0f172a' }}>
            {title}
          </Typography.Title>
          {description && (
            <div style={{ color: '#64748b', fontSize: 12, lineHeight: 1.6 }}>
              {description}
            </div>
          )}
        </div>

        <div
          style={{
            flexShrink: 0,
            borderRadius: 999,
            padding: '4px 10px',
            fontSize: 12,
            fontWeight: 600,
            color: isTagger ? TAGGER_STYLES.accent : '#475569',
            background: isTagger ? '#eff6ff' : '#f8fafc',
            border: `1px solid ${isTagger ? '#bfdbfe' : '#e2e8f0'}`,
          }}
        >
          {statusLabel}
        </div>
      </div>

      {!!metaBadges.length && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 12 }}>
          {metaBadges.map((badge) => (
            <Tag
              key={badge}
              style={{
                margin: 0,
                borderRadius: 999,
                padding: '0 10px',
                ...(isTagger
                  ? { background: '#eff6ff', color: '#1d4ed8', border: '1px solid #dbeafe' }
                  : { background: '#f8fafc', color: '#334155', border: '1px solid #e2e8f0' }),
              }}
            >
              {badge}
            </Tag>
          ))}
        </div>
      )}
    </div>
  )
}

export interface ConfigSectionCardProps {
  title: string
  children: ReactNode
}

export function ConfigSectionCard({ title, children }: ConfigSectionCardProps) {
  return (
    <section style={{ ...CARD_STYLE, padding: 14, marginBottom: 12 }}>
      <div style={{ marginBottom: 12, fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
        {title}
      </div>
      {children}
    </section>
  )
}

export interface FieldSummaryRowProps {
  label: string
  value: string
}

export function FieldSummaryRow({ label, value }: FieldSummaryRowProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        padding: '10px 12px',
        background: '#f8fafc',
        border: '1px solid #e8eaef',
        borderRadius: 10,
      }}
    >
      <div style={{ color: '#64748b', fontSize: 12, flexShrink: 0 }}>
        {label}
      </div>
      <div style={{ color: '#0f172a', fontSize: 12, fontWeight: 500, textAlign: 'right', wordBreak: 'break-word' }}>
        {value}
      </div>
    </div>
  )
}

export interface BranchRouteCardProps {
  label: string
  value: string
  tone: 'success' | 'danger'
}

export function BranchRouteCard({ label, value, tone }: BranchRouteCardProps) {
  const styles = ROUTE_STYLES[tone]

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        padding: '12px 14px',
        borderRadius: 12,
        background: styles.background,
        border: styles.border,
      }}
    >
      <div style={{ minWidth: 0 }}>
        <div style={{ color: styles.title, fontSize: 12, fontWeight: 600, marginBottom: 4 }}>
          {label}
        </div>
        <div style={{ color: styles.value, fontSize: 13, fontWeight: 500, wordBreak: 'break-word' }}>
          {value}
        </div>
      </div>
    </div>
  )
}
