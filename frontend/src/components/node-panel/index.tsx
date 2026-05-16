import { useEffect, useState } from 'react'
import { Collapse, Tooltip, Typography, Spin, Tag } from 'antd'
import { metaApi } from '../../services/api'
import type { NodeTypeRegistry } from '../../types'
import { CATEGORY_SOLID, TRIGGER_TYPES } from '../canvas/constants'

// 旧触发器节点（已被 START 统一入口取代，保留用于兼容已有画布）
const LEGACY_TRIGGERS = new Set(['BEHAVIOR_IN_APP', 'SCHEDULED_TRIGGER', 'MQ_TRIGGER', 'DIRECT_CALL', 'TAGGER_REALTIME'])

const { Text } = Typography
  onDragStart: (nodeType: string, category: string) => void
}

// 模块级缓存（会话内只请求一次）
let cachedGroups: Record<string, NodeTypeRegistry[]> | null = null

export default function NodePanel({ onDragStart }: Props) {
  const [groups, setGroups] = useState<Record<string, NodeTypeRegistry[]>>(cachedGroups ?? {})
  const [loading, setLoading] = useState(!cachedGroups)

  useEffect(() => {
    if (cachedGroups) return // 已有缓存，不重复请求
    metaApi.getNodeTypes().then((res) => {
      const map: Record<string, NodeTypeRegistry[]> = {}
      for (const nt of res.data) {
        if (!map[nt.category]) map[nt.category] = []
        map[nt.category].push(nt)
      }
      cachedGroups = map
      setGroups(map)
    }).finally(() => setLoading(false))
  }, [])

  if (loading) return <div style={{ padding: 16 }}><Spin /></div>

  const items = Object.entries(groups).map(([category, nodes]) => ({
    key: category,
    label: (
      <Text strong style={{ color: CATEGORY_SOLID[category] ?? '#722ed1' }}>
        {category}
      </Text>
    ),
    children: (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {nodes.map((nt) => {
          const isLegacy = LEGACY_TRIGGERS.has(nt.typeKey)
          return (
          <Tooltip
            key={nt.typeKey}
            title={
              nt.typeKey === 'START'
                ? '统一入口节点 — 拖入后在配置面板选择触发方式（直调/事件/定时/MQ）'
                : isLegacy
                  ? `【兼容模式】${nt.description || nt.typeName} — 推荐改用「开始」节点统一配置触发方式`
                  : (nt.description || nt.typeName)
            }
            placement="right"
            mouseEnterDelay={0.5}
          >
            <div
              draggable
              onDragStart={(e) => {
                e.dataTransfer.setData('application/canvas-node-type', nt.typeKey)
                e.dataTransfer.setData('application/canvas-node-category', nt.category)
                e.dataTransfer.effectAllowed = 'move'
                onDragStart(nt.typeKey, nt.category)
              }}
              style={{
                padding: '5px 8px', borderRadius: 4, cursor: 'grab',
                background: isLegacy ? '#fffbe6' : '#fafafa',
                border: `1px solid ${isLegacy ? '#ffe58f' : '#f0f0f0'}`,
                fontSize: 12, userSelect: 'none',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                opacity: isLegacy ? 0.75 : 1,
              }}
              onMouseEnter={e => (e.currentTarget.style.borderColor = CATEGORY_SOLID[category] ?? '#722ed1')}
              onMouseLeave={e => (e.currentTarget.style.borderColor = isLegacy ? '#ffe58f' : '#f0f0f0')}
            >
              {nt.typeName}
              {nt.typeKey === 'START'
                ? <Tag color="green" style={{ fontSize: 9, padding: '0 3px', lineHeight: '14px', marginLeft: 4, flexShrink: 0 }}>入口</Tag>
                : TRIGGER_TYPES.has(nt.typeKey)
                  ? <Tag color="orange" style={{ fontSize: 9, padding: '0 3px', lineHeight: '14px', marginLeft: 4, flexShrink: 0 }}>兼容</Tag>
                  : null
              }
            </div>
          </Tooltip>
          )
        })}
      </div>
    ),
  }))

  return (
    <div style={{ height: '100%', overflowY: 'auto' }}>
      <Collapse items={items} defaultActiveKey={Object.keys(groups)} ghost size="small" />
    </div>
  )
}
