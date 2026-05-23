import type { CSSProperties, ReactNode } from 'react'
import { Tag, Typography } from 'antd'

const PANEL_SURFACE = '#f5f5f7'
const PANEL_CARD = '#ffffff'
const PANEL_BORDER = '#c7c7cc'

/**
 * 配置面板普通卡片的基础视觉样式。
 */
const CARD_STYLE: CSSProperties = {
  background: PANEL_CARD,
  border: `1px solid ${PANEL_BORDER}`,
  borderRadius: 22,
  boxShadow: '0 10px 28px rgba(0, 0, 0, 0.045)',
}

/**
 * TAGGER 节点的专属配色。
 */
const TAGGER_STYLES = {
  title: '#1d1d1f',
  description: '#6f7c91',
}

/**
 * 分支路由卡片配色（成功分支 / 失败分支）。
 */
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

/**
 * 节点头卡片视图模型。
 */
export interface NodeHeaderCardProps {
  /** 头卡主题。 */
  tone: 'default' | 'tagger'

  /** 节点类型标签。 */
  typeBadge: string

  /** 节点标题。 */
  title: string

  /** 头卡辅助标签。 */
  metaBadges: string[]

  /** 节点说明。 */
  description?: string

  /** 右上角状态文案。 */
  statusLabel: string
  categoryColor?: string
}

function getHeaderPillStyles(color: string) {
  return {
    badge: {
      background: `${color}12`,
      color,
      border: `1px solid ${color}33`,
    },
    status: {
      background: `${color}10`,
      color,
      border: `1px solid ${color}30`,
    },
    meta: {
      background: `${color}0d`,
      color,
      border: `1px solid ${color}24`,
    },
  }
}

export function NodeHeaderCard({
  tone,
  typeBadge,
  title,
  metaBadges,
  description,
  statusLabel,
  categoryColor,
}: NodeHeaderCardProps) {
  const isTagger = tone === 'tagger'
  const resolvedCategoryColor = categoryColor ?? '#475569'
  const pillStyles = getHeaderPillStyles(resolvedCategoryColor)
  const headerBackground = isTagger
    ? `linear-gradient(135deg, ${resolvedCategoryColor}0f 0%, #f8fbff 56%, ${resolvedCategoryColor}08 100%)`
    : PANEL_CARD

  return (
    <div
      style={{
        marginBottom: 16,
        padding: '24px 24px 26px',
        background: headerBackground,
        border: `1px solid ${resolvedCategoryColor}35`,
        borderRadius: 26,
        boxShadow: `0 12px 32px ${resolvedCategoryColor}14`,
        overflow: 'hidden',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
        <Tag
          style={{
            margin: 0,
            borderRadius: 999,
            height: 36,
            lineHeight: '34px',
            padding: '0 17px',
            fontSize: 14,
            fontWeight: 700,
            letterSpacing: '.01em',
            ...pillStyles.badge,
          }}
        >
          {typeBadge}
        </Tag>
        <div
          style={{
            flexShrink: 0,
            borderRadius: 999,
            height: 36,
            lineHeight: '34px',
            padding: '0 17px',
            fontSize: 14,
            fontWeight: 700,
            color: pillStyles.status.color,
            background: pillStyles.status.background,
            border: pillStyles.status.border,
            whiteSpace: 'nowrap',
          }}
        >
          {statusLabel}
        </div>
      </div>

      <Typography.Title level={5} style={{
        margin: '22px 0 0',
        color: isTagger ? TAGGER_STYLES.title : '#1d1d1f',
        fontSize: 20,
        lineHeight: 1.35,
        fontWeight: 700,
        letterSpacing: '-0.025em',
      }}>
        {title}
      </Typography.Title>

      {description && (
        <div style={{ color: isTagger ? TAGGER_STYLES.description : '#6e6e73', fontSize: 14, fontWeight: 600, lineHeight: 1.7, marginTop: 14 }}>
          {description}
        </div>
      )}

      {!!metaBadges.length && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 24 }}>
          {metaBadges.map((badge) => (
            <Tag
              key={badge}
              style={{
                margin: 0,
                height: 34,
                lineHeight: '32px',
                borderRadius: 999,
                padding: '0 17px',
                fontSize: 14,
                fontWeight: 650,
                ...pillStyles.meta,
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

/**
 * 通用分节容器卡片。
 */
export interface ConfigSectionCardProps {
  /** 分节标题。 */
  title: string

  /** 分节内容。 */
  children: ReactNode
}

export function ConfigSectionCard({ title, children }: ConfigSectionCardProps) {
  return (
    <section style={{ ...CARD_STYLE, padding: '20px 20px 22px', marginBottom: 16 }}>
      <div style={{ marginBottom: 16, fontSize: 14, fontWeight: 650, color: '#1d1d1f' }}>
        {title}
      </div>
      {children}
    </section>
  )
}

/**
 * 无标题表单容器，避免“配置详情”标题增加冗余层级。
 */
export function ConfigFormCard({ children }: { children: ReactNode }) {
  return (
    <section style={{ ...CARD_STYLE, padding: '20px 20px 6px', marginBottom: 16 }}>
      {children}
    </section>
  )
}

/**
 * 字段摘要行（左侧字段名 + 右侧值）。
 */
export interface FieldSummaryRowProps {
  /** 字段标签。 */
  label: string

  /** 字段值。 */
  value: string
}

export interface ConfigSummaryListProps {
  /** 摘要行列表。 */
  rows: FieldSummaryRowProps[]
}

export function ConfigSummaryList({ rows }: ConfigSummaryListProps) {
  return (
    <section style={{ ...CARD_STYLE, padding: 18, marginBottom: 16 }}>
      <div style={{ display: 'grid', gap: 10 }}>
        {rows.map((row) => (
          <FieldSummaryRow key={row.label} label={row.label} value={row.value} />
        ))}
      </div>
    </section>
  )
}

export function FieldSummaryRow({ label, value }: FieldSummaryRowProps) {
  return (
    <div
      style={{
        padding: '16px 18px',
        background: PANEL_SURFACE,
        border: '1px solid #dedee5',
        borderRadius: 17,
      }}
    >
      <div style={{ color: '#6e6e73', fontSize: 14, fontWeight: 600, marginBottom: 6, lineHeight: 1.35 }}>
        {label}
      </div>
      <div style={{ color: '#1d1d1f', fontSize: 14, fontWeight: 500, lineHeight: 1.35, wordBreak: 'break-word' }}>
        {value}
      </div>
    </div>
  )
}

/**
 * 分支去向摘要卡片。
 */
export interface BranchRouteCardProps {
  /** 路由标签。 */
  label: string

  /** 路由目标名称。 */
  value: string

  /** 卡片风格（成功/失败语义）。 */
  tone: 'success' | 'danger'
}

export function BranchRouteCard({ label, value, tone }: BranchRouteCardProps) {
  const styles = ROUTE_STYLES[tone]

  return (
    <div
      style={{
        padding: '16px 18px',
        borderRadius: 17,
        background: PANEL_SURFACE,
        border: '1px solid #dedee5',
      }}
    >
      <div style={{ minWidth: 0 }}>
        <div style={{ color: styles.title, fontSize: 14, fontWeight: 600, marginBottom: 6, lineHeight: 1.35 }}>
          {label}
        </div>
        <div style={{ color: styles.value, fontSize: 14, fontWeight: 500, lineHeight: 1.35, wordBreak: 'break-word' }}>
          {value}
        </div>
      </div>
    </div>
  )
}
