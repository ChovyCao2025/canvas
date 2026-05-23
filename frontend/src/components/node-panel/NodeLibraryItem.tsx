import type { DragEvent } from 'react'
import { Popover, Typography } from 'antd'

import type { NodeTypeRegistry } from '../../types'

const { Paragraph, Text } = Typography

interface Props {
  categoryColor: string
  node: NodeTypeRegistry
  summary: string
  onDragStart: (event: DragEvent<HTMLDivElement>, node: NodeTypeRegistry) => void
}

export default function NodeLibraryItem({
  categoryColor,
  node,
  summary,
  onDragStart,
}: Props) {
  const detail = (node.description ?? '').trim() || summary

  return (
    <div
      draggable
      onDragStart={(event) => onDragStart(event, node)}
      style={{
        display: 'flex',
        alignItems: 'stretch',
        gap: 0,
        border: '1px solid #f0f0f0',
        borderRadius: 6,
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
        }}
      />
      <div
        style={{
          flex: 1,
          minWidth: 0,
          padding: '8px 10px',
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <div style={{ flex: 1, minWidth: 0 }}>
          <Text
            strong
            style={{
              display: 'block',
              color: '#1f1f1f',
              fontSize: 13,
              lineHeight: '20px',
            }}
          >
            {node.typeName}
          </Text>
          <Paragraph
            ellipsis={{ rows: 1, tooltip: summary }}
            style={{
              margin: 0,
              color: '#8c8c8c',
              fontSize: 12,
              lineHeight: '18px',
            }}
          >
            {summary}
          </Paragraph>
        </div>
        <Popover
          trigger="click"
          placement="leftTop"
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
              borderRadius: '50%',
              border: '1px solid #d9d9d9',
              background: '#fff',
              color: '#8c8c8c',
              fontSize: 12,
              lineHeight: '18px',
              textAlign: 'center',
              padding: 0,
              cursor: 'pointer',
            }}
          >
            ?
          </button>
        </Popover>
      </div>
    </div>
  )
}
