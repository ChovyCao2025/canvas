import type { DragEvent } from 'react'
import { Popover, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'

import type { NodeTypeRegistry } from '../../types'

const { Text } = Typography

interface Props {
  categoryColor: string
  node: NodeTypeRegistry
  detail: string
  onDragStart: (event: DragEvent<HTMLDivElement>, node: NodeTypeRegistry) => void
}

export default function NodeLibraryItem({
  categoryColor,
  node,
  detail,
  onDragStart,
}: Props) {
  return (
    <div
      draggable
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
        event.currentTarget.style.borderColor = categoryColor
        event.currentTarget.style.boxShadow = `0 0 0 1px ${categoryColor}1f`
      }}
      onMouseLeave={(event) => {
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
