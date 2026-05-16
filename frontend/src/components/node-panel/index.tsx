import { useEffect, useState } from 'react'
import { Collapse, Tooltip, Typography, Spin, Tag } from 'antd'
import { metaApi } from '../../services/api'
import type { NodeTypeRegistry } from '../../types'
import { CATEGORY_SOLID, TRIGGER_TYPES } from '../canvas/constants'

const { Text } = Typography

interface Props {
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
        {nodes.map((nt) => (
          <Tooltip
            key={nt.typeKey}
            title={
              TRIGGER_TYPES.has(nt.typeKey)
                ? `【触发器】${nt.description || nt.typeName} — 只能作为流程第一个节点，可与其他触发器同时存在于画布中（各自独立入口）`
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
                background: '#fafafa', border: '1px solid #f0f0f0',
                fontSize: 12, userSelect: 'none',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              }}
              onMouseEnter={e => (e.currentTarget.style.borderColor = CATEGORY_SOLID[category] ?? '#722ed1')}
              onMouseLeave={e => (e.currentTarget.style.borderColor = '#f0f0f0')}
            >
              {nt.typeName}
              {TRIGGER_TYPES.has(nt.typeKey) && (
                <Tag color="cyan" style={{ fontSize: 9, padding: '0 3px', lineHeight: '14px', marginLeft: 4, flexShrink: 0 }}>触发器</Tag>
              )}
            </div>
          </Tooltip>
        ))}
      </div>
    ),
  }))

  return (
    <div style={{ height: '100%', overflowY: 'auto' }}>
      <Collapse items={items} defaultActiveKey={Object.keys(groups)} ghost size="small" />
    </div>
  )
}
