import type { DragEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Empty, Input, Spin } from 'antd'
import { SearchOutlined, DownOutlined, RightOutlined } from '@ant-design/icons'

import { metaApi } from '../../services/api'
import type { NodeTypeRegistry } from '../../types'
import { CATEGORY_SOLID } from '../canvas/constants'
import NodeLibraryItem from './NodeLibraryItem'
import {
  buildCategoryOptions,
  buildNodeLibraryView,
  getNodeSummary,
} from './nodeLibrary'

interface Props {
  onDragStart: (nodeType: string, category: string) => void
}

export default function NodePanel({ onDragStart }: Props) {
  const [nodes, setNodes] = useState<NodeTypeRegistry[]>([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({})

  useEffect(() => {
    metaApi.getNodeTypes()
      .then((res) => {
        setNodes(res.data)
      })
      .finally(() => setLoading(false))
  }, [])

  const categories = useMemo(() => buildCategoryOptions(nodes), [nodes])
  const view = useMemo(
    () =>
      buildNodeLibraryView(nodes, {
        activeCategory,
        keyword,
        commonTypeKeys: [],
      }),
    [activeCategory, keyword, nodes],
  )
  const groupedNodes = useMemo(() => {
    const groups: Record<string, NodeTypeRegistry[]> = {}
    for (const node of view.filteredNodes) {
      if (!groups[node.category]) groups[node.category] = []
      groups[node.category].push(node)
    }
    return groups
  }, [view.filteredNodes])
  const visibleCategories = useMemo(() => Object.keys(groupedNodes), [groupedNodes])

  useEffect(() => {
    if (!visibleCategories.length) return
    setCollapsedGroups((current) => {
      const next = { ...current }
      for (const category of visibleCategories) {
        if (next[category] == null) next[category] = false
        if (keyword.trim() || activeCategory !== '全部') next[category] = false
      }
      return next
    })
  }, [activeCategory, keyword, visibleCategories])

  const handleNodeDragStart = (event: DragEvent<HTMLDivElement>, node: NodeTypeRegistry) => {
    event.dataTransfer.setData('application/canvas-node-type', node.typeKey)
    event.dataTransfer.setData('application/canvas-node-category', node.category)
    event.dataTransfer.effectAllowed = 'move'
    onDragStart(node.typeKey, node.category)
  }

  const toggleGroup = (category: string) => {
    setCollapsedGroups((current) => ({
      ...current,
      [category]: !current[category],
    }))
  }

  if (loading) {
    return (
      <div style={{ padding: 16 }}>
        <Spin />
      </div>
    )
  }

  return (
    <div
      style={{
        height: '100%',
        overflowY: 'auto',
        padding: '18px 20px',
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
        background: '#fafafa',
      }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Input
          allowClear
          size="middle"
          prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
          placeholder="搜索节点"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          style={{
            height: 40,
            borderRadius: 10,
            background: '#fff',
          }}
        />
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              onClick={() => setActiveCategory(category)}
              style={{
                borderRadius: 999,
                border: `1px solid ${category === '全部'
                  ? (activeCategory === '全部' ? '#111827' : '#d6dae1')
                  : `${CATEGORY_SOLID[category] ?? '#722ed1'}33`}`,
                background: activeCategory === category
                  ? (category === '全部' ? '#111827' : CATEGORY_SOLID[category] ?? '#722ed1')
                  : (category === '全部' ? '#f3f4f6' : `${CATEGORY_SOLID[category] ?? '#722ed1'}12`),
                color: activeCategory === category
                  ? '#fff'
                  : (category === '全部' ? '#111827' : CATEGORY_SOLID[category] ?? '#722ed1'),
                padding: '6px 12px',
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      <section style={{ display: 'flex', flexDirection: 'column', gap: 8, minHeight: 0 }}>
        {view.filteredNodes.length === 0 ? (
          <div
            style={{
              padding: '24px 16px',
              border: '1px dashed #d9d9d9',
              borderRadius: 10,
              background: '#fff',
            }}
          >
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配的节点" />
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, paddingRight: 4 }}>
            {visibleCategories.map((category) => {
              const color = CATEGORY_SOLID[category] ?? '#722ed1'
              const collapsed = collapsedGroups[category] ?? false
              return (
                <div
                  key={category}
                  style={{
                    border: '1px solid #e7ebf0',
                    borderRadius: 12,
                    background: '#fbfcfd',
                    overflow: 'hidden',
                  }}
                >
                  <button
                    type="button"
                    onClick={() => toggleGroup(category)}
                    style={{
                      width: '100%',
                      padding: '12px 16px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      border: 'none',
                      background: 'transparent',
                      cursor: 'pointer',
                    }}
                  >
                    <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <span
                        aria-hidden="true"
                        style={{ width: 8, height: 8, borderRadius: '50%', background: color }}
                      />
                      <span style={{ fontSize: 13, fontWeight: 700, color }}>
                        {category}
                      </span>
                    </span>
                    {collapsed
                      ? <RightOutlined style={{ color: '#9ca3af', fontSize: 12 }} />
                      : <DownOutlined style={{ color: '#9ca3af', fontSize: 12 }} />}
                  </button>
                  {!collapsed && (
                    <div style={{ padding: '0 14px 14px 14px', display: 'grid', gap: 8 }}>
                      {groupedNodes[category].map((node) => (
                        <NodeLibraryItem
                          key={node.typeKey}
                          node={node}
                          detail={getNodeSummary(node)}
                          categoryColor={color}
                          onDragStart={handleNodeDragStart}
                        />
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
