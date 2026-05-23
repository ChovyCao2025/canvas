import type { DragEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Empty, Input, Spin, Tooltip } from 'antd'
import { SearchOutlined, DownOutlined, RightOutlined, HolderOutlined, SlidersOutlined } from '@ant-design/icons'

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

const DEFAULT_EXPANDED_CATEGORIES = new Set(['其他', '逻辑分支'])
const COLLAPSE_STATE_KEY = 'canvas_node_panel_collapsed_groups_v1'
const CATEGORY_ORDER_KEY = 'canvas_node_panel_category_order_v1'

export default function NodePanel({ onDragStart }: Props) {
  const [nodes, setNodes] = useState<NodeTypeRegistry[]>([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({})
  const [categoryOrder, setCategoryOrder] = useState<string[]>([])
  const [sorting, setSorting] = useState(false)
  const [draggingCategory, setDraggingCategory] = useState<string | null>(null)
  const [dropTargetCategory, setDropTargetCategory] = useState<string | null>(null)

  useEffect(() => {
    metaApi.getNodeTypes()
      .then((res) => {
        setNodes(res.data)
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(COLLAPSE_STATE_KEY)
      if (!saved) return
      const parsed = JSON.parse(saved) as Record<string, boolean>
      setCollapsedGroups(parsed)
    } catch {
      // ignore invalid local cache
    }
  }, [])

  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(CATEGORY_ORDER_KEY)
      if (!saved) return
      const parsed = JSON.parse(saved) as string[]
      if (Array.isArray(parsed)) setCategoryOrder(parsed)
    } catch {
      // ignore invalid local cache
    }
  }, [])

  const categories = useMemo(() => {
    const built = buildCategoryOptions(nodes)
    const base = built.filter(category => category !== '全部')
    const ordered = categoryOrder.filter(category => base.includes(category))
    const missing = base.filter(category => !ordered.includes(category))
    return ['全部', ...ordered, ...missing]
  }, [categoryOrder, nodes])
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
  const visibleCategories = useMemo(
    () => categories.filter(category => category !== '全部' && groupedNodes[category]?.length),
    [categories, groupedNodes],
  )
  const sortableCategories = useMemo(
    () => categories.filter(category => category !== '全部'),
    [categories],
  )

  useEffect(() => {
    if (!visibleCategories.length) return
    setCollapsedGroups((current) => {
      const next = { ...current }
      for (const category of visibleCategories) {
        if (next[category] == null) next[category] = !DEFAULT_EXPANDED_CATEGORIES.has(category)
        if (keyword.trim() || activeCategory !== '全部') next[category] = false
      }
      return next
    })
  }, [activeCategory, keyword, visibleCategories])

  const handleNodeDragStart = (event: DragEvent<HTMLDivElement>, node: NodeTypeRegistry) => {
    event.dataTransfer.setData('application/canvas-node-type', node.typeKey)
    event.dataTransfer.setData('application/canvas-node-category', node.category)
    if (node.configSchema) {
      event.dataTransfer.setData('application/canvas-node-config-schema', node.configSchema)
    }
    if (node.outletSchema) {
      event.dataTransfer.setData('application/canvas-node-outlet-schema', node.outletSchema)
    }
    event.dataTransfer.effectAllowed = 'move'
    onDragStart(node.typeKey, node.category)
  }

  const toggleGroup = (category: string) => {
    setCollapsedGroups((current) => ({
      ...current,
      [category]: !current[category],
    }))
  }

  const getCategoryTint = (category: string) => {
    const color = CATEGORY_SOLID[category] ?? '#6d5efc'
    return `${color}12`
  }

  const moveCategory = (source: string, target: string) => {
    if (source === target) return
    setCategoryOrder((current) => {
      const base = current.length > 0 ? [...current] : [...sortableCategories]
      const withoutSource = base.filter(category => category !== source)
      const targetIndex = withoutSource.indexOf(target)
      if (targetIndex < 0) return base
      withoutSource.splice(targetIndex, 0, source)
      return withoutSource
    })
  }

  useEffect(() => {
    if (!Object.keys(collapsedGroups).length) return
    window.localStorage.setItem(COLLAPSE_STATE_KEY, JSON.stringify(collapsedGroups))
  }, [collapsedGroups])

  useEffect(() => {
    if (!categoryOrder.length) return
    window.localStorage.setItem(CATEGORY_ORDER_KEY, JSON.stringify(categoryOrder))
  }, [categoryOrder])

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
      {sorting ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#334155' }}>排序分类</div>
            <button
              type="button"
              onClick={() => {
                setSorting(false)
                setDraggingCategory(null)
                setDropTargetCategory(null)
              }}
              style={{
                border: 'none',
                background: '#111827',
                color: '#fff',
                borderRadius: 999,
                padding: '6px 12px',
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              完成
            </button>
          </div>
          <div
            style={{
              padding: '10px 12px',
              borderRadius: 10,
              background: '#fff7ed',
              border: '1px solid #fed7aa',
              color: '#9a3412',
              fontSize: 12,
              lineHeight: 1.6,
            }}
          >
            拖动分类标题调整顺序。排序时先隐藏节点列表，避免和展开/收起操作冲突。
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
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
                borderColor: '#d9e0ea',
                boxShadow: '0 1px 2px rgba(15,23,42,.04)',
              }}
            />
            <Tooltip title="排序分类">
              <button
                type="button"
                onClick={() => {
                  setSorting(true)
                  setDraggingCategory(null)
                  setDropTargetCategory(null)
                }}
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 10,
                  border: '1px solid #d9e0ea',
                  background: '#fff',
                  color: '#64748b',
                  cursor: 'pointer',
                  flexShrink: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 1px 2px rgba(15,23,42,.04)',
                }}
              >
                <SlidersOutlined style={{ fontSize: 14 }} />
              </button>
            </Tooltip>
          </div>
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
                    : `${CATEGORY_SOLID[category] ?? '#6d5efc'}33`}`,
                  background: activeCategory === category
                    ? (category === '全部' ? '#111827' : CATEGORY_SOLID[category] ?? '#6d5efc')
                    : (category === '全部' ? '#f3f4f6' : getCategoryTint(category)),
                  color: activeCategory === category
                    ? '#fff'
                    : (category === '全部' ? '#111827' : CATEGORY_SOLID[category] ?? '#6d5efc'),
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
      )}

      <section style={{ display: 'flex', flexDirection: 'column', gap: 8, minHeight: 0 }}>
        {sorting ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, paddingRight: 4 }}>
            {sortableCategories.map((category) => {
              const color = CATEGORY_SOLID[category] ?? '#6d5efc'
              const isDragging = draggingCategory === category
              const isDropTarget = dropTargetCategory === category && draggingCategory !== category
              return (
                <div key={category} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {isDropTarget && (
                    <div
                      style={{
                        background: '#eef5ff',
                        border: '1px dashed #93c5fd',
                        borderRadius: 12,
                        padding: '12px 16px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#2563eb',
                        fontSize: 12,
                        fontWeight: 600,
                      }}
                    >
                      放到这里
                    </div>
                  )}
                  <div
                    draggable
                    onDragStart={() => {
                      setDraggingCategory(category)
                      setDropTargetCategory(null)
                    }}
                    onDragOver={(event) => {
                      event.preventDefault()
                      setDropTargetCategory(category)
                    }}
                    onDrop={(event) => {
                      event.preventDefault()
                      if (draggingCategory) moveCategory(draggingCategory, category)
                      setDraggingCategory(null)
                      setDropTargetCategory(null)
                    }}
                    onDragEnd={() => {
                      setDraggingCategory(null)
                      setDropTargetCategory(null)
                    }}
                    style={{
                      border: `1px solid ${color}26`,
                      borderRadius: 12,
                      background: getCategoryTint(category),
                      padding: '14px 16px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      cursor: 'grab',
                      boxShadow: isDragging ? '0 6px 16px rgba(15,23,42,.08)' : 'none',
                      opacity: isDragging ? 0.92 : 1,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <HolderOutlined style={{ color: '#94a3b8', fontSize: 14 }} />
                      <span
                        aria-hidden="true"
                        style={{ width: 8, height: 8, borderRadius: '50%', background: color }}
                      />
                      <span style={{ fontSize: 13, fontWeight: 700, color }}>
                        {category}
                      </span>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        ) : view.filteredNodes.length === 0 ? (
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
              const color = CATEGORY_SOLID[category] ?? '#6d5efc'
              const collapsed = collapsedGroups[category] ?? false
              return (
                <div
                  key={category}
                  style={{
                    border: `1px solid ${color}26`,
                    borderRadius: 12,
                    background: getCategoryTint(category),
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
                      borderBottom: collapsed ? 'none' : `1px solid ${color}1a`,
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
                    <div style={{ padding: '10px 14px 14px 14px', display: 'grid', gap: 8 }}>
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
