import { useEffect, useState } from 'react'
import { Collapse, Tooltip, Typography, Spin } from 'antd'
import { metaApi } from '../../services/api'
import type { NodeTypeRegistry } from '../../types'
import { CATEGORY_SOLID } from '../canvas/constants'

const { Text } = Typography

/**
 * 节点面板 props：
 * onDragStart 用于通知编辑器“开始拖拽某类节点”。
 */
interface Props {
  onDragStart: (nodeType: string, category: string) => void
}

/**
 * 左侧节点面板：
 * 1. 拉取后端 node-type 元数据；
 * 2. 按 category 分组；
 * 3. 通过 HTML5 DnD 把节点类型信息放入 dataTransfer。
 * 4. drop 区按约定 MIME key 解析节点类型并创建实例。
 *
 * 这层不参与节点实例化逻辑，只负责提供“可拖拽元数据”。
 */
export default function NodePanel({ onDragStart }: Props) {
  // groups 结构：{ [category]: NodeTypeRegistry[] }
  const [groups, setGroups] = useState<Record<string, NodeTypeRegistry[]>>({})
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // 初始化加载可用节点类型，并按分类聚合
    metaApi.getNodeTypes().then((res) => {
      const map: Record<string, NodeTypeRegistry[]> = {}
      for (const nt of res.data) {
        if (!map[nt.category]) map[nt.category] = []
        map[nt.category].push(nt)
      }
      setGroups(map)
    }).finally(() => setLoading(false))
  }, [])

  // 首屏加载元数据时显示轻量 loading
  if (loading) return <div style={{ padding: 16 }}><Spin /></div>

  // 把分组数据转换成 Collapse 组件可消费的 items 结构
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
            title={nt.description || nt.typeName}
            placement="right"
            mouseEnterDelay={0.5}
          >
            <div
              draggable
              onDragStart={(e) => {
                // 约定：编辑器 drop 区读取这两个 MIME key 来创建节点
                // key 名字在编辑器和面板之间是“协议”，不要随意改动。
                e.dataTransfer.setData('application/canvas-node-type', nt.typeKey)
                e.dataTransfer.setData('application/canvas-node-category', nt.category)
                e.dataTransfer.effectAllowed = 'move'
                onDragStart(nt.typeKey, nt.category)
              }}
              style={{
                padding: '5px 8px', borderRadius: 4, cursor: 'grab',
                background: '#fafafa',
                border: '1px solid #f0f0f0',
                fontSize: 12, userSelect: 'none',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              }}
              onMouseEnter={e => (e.currentTarget.style.borderColor = CATEGORY_SOLID[category] ?? '#722ed1')}
              onMouseLeave={e => (e.currentTarget.style.borderColor = '#f0f0f0')}
            >
              {nt.typeName}
            </div>
          </Tooltip>
        ))}
      </div>
    ),
  }))

  return (
    // 面板高度跟随容器，内部自己滚动
    <div style={{ height: '100%', overflowY: 'auto' }}>
      <Collapse items={items} defaultActiveKey={Object.keys(groups)} ghost size="small" />
    </div>
  )
}
