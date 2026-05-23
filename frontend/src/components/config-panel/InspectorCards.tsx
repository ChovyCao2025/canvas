import type { CSSProperties, ReactNode } from 'react'
import { Tag, Typography } from 'antd'

/**
 * 配置面板摘要卡片的基础视觉样式。
 */
const CARD_STYLE: CSSProperties = {
  background: '#fff',
  border: '1px solid #e8eaef',
  borderRadius: 14,
  boxShadow: '0 6px 18px rgba(15, 23, 42, 0.06)',
}

/**
 * TAGGER 节点的专属配色。
 */
const TAGGER_STYLES = {
  shell: {
    background: '#e9f0f7',
    border: '1px solid #d9e4ef',
  },
  badge: {
    background: '#f6f9fc',
    color: '#345472',
    border: '1px solid #d4e1ee',
  },
  status: {
    background: '#f6f9fc',
    color: '#345472',
    border: '1px solid #d4e1ee',
  },
  title: '#17324d',
  description: '#6e8093',
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
      background: `${color}14`,
      color,
      border: `1px solid ${color}33`,
    },
    status: {
      background: `${color}0d`,
      color: `${color}cc`,
      border: `1px solid ${color}26`,
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
  // TAGGER 节点使用更高识别度主题，普通节点保持中性展示
  const shellStyle = isTagger ? TAGGER_STYLES.shell : {}
  const resolvedCategoryColor = categoryColor ?? '#475569'
  const pillStyles = getHeaderPillStyles(resolvedCategoryColor)

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
              ...pillStyles.badge,
            }}
          >
            {typeBadge}
          </Tag>
          <Typography.Title level={5} style={{ margin: '10px 0 6px', color: isTagger ? TAGGER_STYLES.title : '#0f172a' }}>
            {title}
          </Typography.Title>
          {description && (
            <div style={{ color: isTagger ? TAGGER_STYLES.description : '#64748b', fontSize: 12, lineHeight: 1.6 }}>
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
            color: pillStyles.status.color,
            background: pillStyles.status.background,
            border: pillStyles.status.border,
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
                  ? { background: '#f6f9fc', color: '#345472', border: '1px solid #d4e1ee' }
                  : {
                      background: '#f8fafc',
                      color: resolvedCategoryColor,
                      border: `1px solid ${resolvedCategoryColor}33`,
                    }),
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
    <section style={{ ...CARD_STYLE, padding: 14, marginBottom: 12 }}>
      <div style={{ marginBottom: 12, fontSize: 13, fontWeight: 600, color: '#0f172a' }}>
        {title}
      </div>
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
