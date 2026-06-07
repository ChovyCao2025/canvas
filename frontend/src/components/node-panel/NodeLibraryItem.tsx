/**
 * 组件职责：节点库单个条目组件，负责拖拽数据、节点名称和摘要展示。
 *
 * 维护说明：拖拽 payload 与画布编辑器 onDrop 协议保持一致。
 */
import type { DragEvent } from 'react'
import { Popover, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'

import type { NodePaletteItem } from './nodeLibrary'

/** 节点条目中的文本组件别名。 */
const { Text } = Typography

/** 节点库条目组件入参。 */
interface Props {
  /** 当前节点分类的主题色，用于左侧色条和 hover 描边。 */
  categoryColor: string

  /** 后端节点类型注册信息。 */
  node: NodePaletteItem

  /** 节点摘要说明。 */
  detail: string

  /** 拖拽开始回调，由父组件写入 dataTransfer。 */
  onDragStart: (event: DragEvent<HTMLDivElement>, node: NodePaletteItem) => void
}

/** 节点库条目，点击右侧箭头查看说明，拖拽整行可放到画布。 */
export default function NodeLibraryItem({
  categoryColor,
  node,
  detail,
  onDragStart,
}: Props) {
  return (
    <div
      draggable
      role="group"
      tabIndex={0}
      aria-label={`${node.typeName}节点库条目，可拖拽到画布，查看说明按钮可打开详情`}
      onDragStart={(event) => onDragStart(event, node)}
      style={{
        display: 'flex',
        alignItems: 'stretch',
        gap: 0,
        border: '1px solid #e6e9ef',
        borderRadius: 10,
        background: '#fff',
        cursor: 'grab',
        overflow: 'hidden',
        userSelect: 'none',
        transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
      }}
      onMouseEnter={(event) => {
        // 轻量 hover 反馈直接改当前元素样式，避免为每个条目维护 hover state。
        event.currentTarget.style.borderColor = categoryColor
        event.currentTarget.style.boxShadow = `0 0 0 1px ${categoryColor}1f`
      }}
      onMouseLeave={(event) => {
        event.currentTarget.style.borderColor = '#f0f0f0'
        event.currentTarget.style.boxShadow = 'none'
      }}
      onFocus={(event) => {
        event.currentTarget.style.borderColor = categoryColor
        event.currentTarget.style.boxShadow = `0 0 0 2px ${categoryColor}33`
      }}
      onBlur={(event) => {
        event.currentTarget.style.borderColor = '#f0f0f0'
        event.currentTarget.style.boxShadow = 'none'
      }}
    >
      <div
        aria-hidden="true"
        style={{
          width: 4,
          flex: '0 0 4px',
          background: categoryColor,
          opacity: 0.9,
        }}
      />
      <div
        style={{
          flex: 1,
          minWidth: 0,
          padding: '12px 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
        }}
      >
        <div style={{ flex: 1, minWidth: 0, display: 'flex', alignItems: 'center' }}>
          <Text
            strong
            style={{
              display: 'block',
              color: '#1f1f1f',
              fontSize: 14,
              lineHeight: '22px',
            }}
          >
            {node.typeName}
          </Text>
        </div>
        <Popover
          trigger="click"
          placement="rightTop"
          content={
            <div style={{ maxWidth: 260 }}>
              <Text strong style={{ display: 'block', marginBottom: 4 }}>
                {node.typeName}
              </Text>
              <Text style={{ color: '#595959', fontSize: 12, lineHeight: '20px' }}>
                {detail}
              </Text>
            </div>
          }
        >
          <button
            type="button"
            aria-label={`查看${node.typeName}说明`}
            onClick={(event) => event.stopPropagation()}
            style={{
              width: 20,
              height: 20,
              flex: '0 0 20px',
              border: 'none',
              background: 'transparent',
              color: '#b6c0cd',
              fontSize: 12,
              lineHeight: '20px',
              textAlign: 'center',
              padding: 0,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <RightOutlined style={{ fontSize: 12 }} />
          </button>
        </Popover>
      </div>
    </div>
  )
}
