import type { DragEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Empty, Input, Spin, Tag, Typography } from 'antd'

import { metaApi } from '../../services/api'
import type { NodeTypeRegistry } from '../../types'
import { CATEGORY_SOLID } from '../canvas/constants'
import NodeLibraryItem from './NodeLibraryItem'
import {
  DEFAULT_COMMON_NODE_TYPES,
  buildCategoryOptions,
  buildNodeLibraryView,
  getNodeSummary,
} from './nodeLibrary'

const { CheckableTag } = Tag
const { Text, Title } = Typography

interface Props {
  onDragStart: (nodeType: string, category: string) => void
}

export default function NodePanel({ onDragStart }: Props) {
  const [nodes, setNodes] = useState<NodeTypeRegistry[]>([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [activeCategory, setActiveCategory] = useState('全部')

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
        commonTypeKeys: DEFAULT_COMMON_NODE_TYPES,
      }),
    [activeCategory, keyword, nodes],
  )

  const handleNodeDragStart = (event: DragEvent<HTMLDivElement>, node: NodeTypeRegistry) => {
    event.dataTransfer.setData('application/canvas-node-type', node.typeKey)
    event.dataTransfer.setData('application/canvas-node-category', node.category)
    event.dataTransfer.effectAllowed = 'move'
    onDragStart(node.typeKey, node.category)
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
        padding: 12,
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
        background: '#fafafa',
      }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div>
          <Title level={5} style={{ margin: 0, fontSize: 14 }}>
            节点库
          </Title>
          <Text type="secondary" style={{ fontSize: 12 }}>
            搜索用途后拖入画布
          </Text>
        </div>
        <Input.Search
          allowClear
          size="small"
          placeholder="搜索节点名称或用途"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {categories.map((category) => (
            <CheckableTag
              key={category}
              checked={activeCategory === category}
              onChange={(checked) => setActiveCategory(checked ? category : '全部')}
            >
              {category}
            </CheckableTag>
          ))}
        </div>
      </div>

      {view.commonNodes.length > 0 && (
        <section style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <Text strong style={{ fontSize: 12, color: '#595959' }}>
            常用节点
          </Text>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {view.commonNodes.map((node) => {
              const categoryColor = CATEGORY_SOLID[node.category] ?? '#722ed1'

              return (
                <div
                  key={`common-${node.typeKey}`}
                  draggable
                  onDragStart={(event) => handleNodeDragStart(event, node)}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    maxWidth: '100%',
                    padding: '4px 10px',
                    borderRadius: 999,
                    border: `1px solid ${categoryColor}26`,
                    background: '#fff',
                    cursor: 'grab',
                    userSelect: 'none',
                  }}
                >
                  <span
                    aria-hidden="true"
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      background: categoryColor,
                      flex: '0 0 8px',
                    }}
                  />
                  <Text ellipsis style={{ maxWidth: 120, fontSize: 12 }}>
                    {node.typeName}
                  </Text>
                </div>
              )
            })}
          </div>
        </section>
      )}

      <section style={{ display: 'flex', flexDirection: 'column', gap: 8, minHeight: 0 }}>
        <Text strong style={{ fontSize: 12, color: '#595959' }}>
          全部节点
        </Text>
        {view.filteredNodes.length === 0 ? (
          <div
            style={{
              padding: '20px 12px',
              border: '1px dashed #d9d9d9',
              borderRadius: 6,
              background: '#fff',
            }}
          >
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配的节点" />
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {view.filteredNodes.map((node) => (
              <NodeLibraryItem
                key={node.typeKey}
                node={node}
                summary={getNodeSummary(node)}
                categoryColor={CATEGORY_SOLID[node.category] ?? '#722ed1'}
                onDragStart={handleNodeDragStart}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
