/**
 * 组件职责：节点配置面板的紧凑 Inspector 展示组件。
 *
 * 维护说明：该组件只负责 presentation，不发起网络请求。
 */
import type { CSSProperties, ReactNode } from 'react'

const PANEL_SURFACE = '#f8fafc'
const PANEL_CARD = '#ffffff'
const PANEL_BORDER = '#d9e1ec'

const CARD_STYLE: CSSProperties = {
  background: PANEL_CARD,
  border: `1px solid ${PANEL_BORDER}`,
  borderRadius: 8,
  boxShadow: 'none',
}

const ROUTE_STYLES = {
  success: {
    title: '#1677ff',
    value: '#334155',
  },
  danger: {
    title: '#dc2626',
    value: '#334155',
  },
} as const

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
  categoryLabel?: string
  categoryColor?: string
}

function getPillStyle(color: string, weight: 'strong' | 'soft' = 'strong'): CSSProperties {
  return {
    height: 22,
    display: 'inline-flex',
    alignItems: 'center',
    maxWidth: '100%',
    borderRadius: 999,
    padding: '0 8px',
    color: weight === 'strong' ? color : '#475569',
    background: weight === 'strong' ? `${color}10` : PANEL_SURFACE,
    border: `1px solid ${weight === 'strong' ? `${color}30` : '#dbe4ef'}`,
    fontSize: 11,
    fontWeight: 760,
    lineHeight: '20px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  }
}

/** 节点配置面板顶部标题卡片。 */
export function NodeHeaderCard({
  typeBadge,
  title,
  description,
  statusLabel,
  categoryLabel,
  categoryColor,
  metaBadges,
}: NodeHeaderCardProps) {
  const resolvedCategoryColor = categoryColor ?? '#475569'
  const badges = [typeBadge, ...(categoryLabel ? [categoryLabel] : []), ...metaBadges]

  return (
    <div
      style={{
        ...CARD_STYLE,
        marginBottom: 10,
        padding: 12,
        overflow: 'hidden',
      }}
    >
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 6, marginBottom: 9 }}>
        {badges.map((badge, index) => (
          <div key={`${badge}-${index}`} style={getPillStyle(resolvedCategoryColor, index === 0 ? 'strong' : 'soft')}>
            {badge}
          </div>
        ))}
        <div style={{ ...getPillStyle(resolvedCategoryColor, 'soft'), marginLeft: 'auto' }}>
          {statusLabel}
        </div>
      </div>

      <div
        style={{
          margin: 0,
          color: '#0f172a',
          fontSize: 15,
          lineHeight: 1.35,
          fontWeight: 780,
          letterSpacing: 0,
          wordBreak: 'break-word',
        }}
      >
        {title}
      </div>

      {description && (
        <div style={{ color: '#64748b', fontSize: 12, lineHeight: 1.55, marginTop: 7 }}>
          {description}
        </div>
      )}
    </div>
  )
}

export interface ConfigSectionCardProps {
  /** 分节标题。 */
  title: string

  /** 标题右侧摘要。 */
  summary?: string

  /** 分节内容。 */
  children: ReactNode
}

/** 配置面板内部分区卡片。 */
export function ConfigSectionCard({ title, summary, children }: ConfigSectionCardProps) {
  return (
    <section style={{ ...CARD_STYLE, padding: '10px 10px 4px', marginBottom: 10 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 9, fontSize: 12, fontWeight: 780, color: '#334155' }}>
        <span>{title}</span>
        {summary && <span style={{ color: '#94a3b8', fontWeight: 700 }}>{summary}</span>}
      </div>
      {children}
    </section>
  )
}

/** 无标题表单容器，保留给少量兼容路径使用。 */
export function ConfigFormCard({ children }: { children: ReactNode }) {
  return (
    <section style={{ ...CARD_STYLE, padding: '10px 10px 4px', marginBottom: 10 }}>
      {children}
    </section>
  )
}

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

/** 配置摘要列表组件。 */
export function ConfigSummaryList({ rows }: ConfigSummaryListProps) {
  return (
    <section style={{ ...CARD_STYLE, padding: 10, marginBottom: 10 }}>
      <div style={{ display: 'grid', gap: 6 }}>
        {rows.map((row) => (
          <FieldSummaryRow key={row.label} label={row.label} value={row.value} />
        ))}
      </div>
    </section>
  )
}

/** 配置摘要单行组件。 */
export function FieldSummaryRow({ label, value }: FieldSummaryRowProps) {
  return (
    <div
      style={{
        padding: '8px 9px',
        background: PANEL_SURFACE,
        border: '1px solid #e7edf5',
        borderRadius: 8,
      }}
    >
      <div style={{ color: '#64748b', fontSize: 12, fontWeight: 700, marginBottom: 4, lineHeight: 1.35 }}>
        {label}
      </div>
      <div style={{ color: '#172033', fontSize: 12, fontWeight: 650, lineHeight: 1.35, wordBreak: 'break-word' }}>
        {value}
      </div>
    </div>
  )
}

export interface BranchRouteCardProps {
  /** 路由标签。 */
  label: string

  /** 路由目标名称。 */
  value: string

  /** 卡片风格（成功/失败语义）。 */
  tone: 'success' | 'danger'
}

/** 分支流向摘要卡片。 */
export function BranchRouteCard({ label, value, tone }: BranchRouteCardProps) {
  const styles = ROUTE_STYLES[tone]
  const unconnected = value === '未连接'

  return (
    <div
      style={{
        minHeight: 38,
        display: 'grid',
        gridTemplateColumns: '62px 1fr',
        alignItems: 'center',
        gap: 8,
        padding: '8px 9px',
        borderRadius: 8,
        background: unconnected ? '#fffbeb' : PANEL_SURFACE,
        border: `1px solid ${unconnected ? '#fde68a' : '#e7edf5'}`,
      }}
    >
      <div style={{ color: unconnected ? '#b45309' : styles.title, fontSize: 12, fontWeight: 780, lineHeight: 1.35 }}>
        {label}
      </div>
      <div style={{ color: unconnected ? '#92400e' : styles.value, fontSize: 12, fontWeight: 680, lineHeight: 1.35, wordBreak: 'break-word' }}>
        {value}
      </div>
    </div>
  )
}
