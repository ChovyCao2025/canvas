import { useState } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useCanvasActions } from '../../context/CanvasActionsContext'

export default function HoverEdge({
  id, sourceX, sourceY, targetX, targetY,
  sourcePosition, targetPosition, sourceHandleId,
  selected, label, style, markerEnd,
}: EdgeProps) {
  const [hovered, setHovered] = useState(false)
  const { deleteEdge, startInsertOnEdge } = useCanvasActions()

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX, sourceY, sourcePosition,
    targetX, targetY, targetPosition,
  })

  const highlighted = hovered || !!selected
  const canInsertOnEdge = !sourceHandleId || sourceHandleId === 'default'
  const deleteOffsetY = canInsertOnEdge ? labelY + 30 : labelY

  return (
    <>
      {/* 加宽的透明 path 用于 hover 检测（纯 SVG，不跨层） */}
      <path
        d={edgePath} fill="none" strokeWidth={20} stroke="transparent"
        style={{ cursor: 'pointer' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      />

      <BaseEdge path={edgePath} markerEnd={markerEnd}
        style={{
          ...style,
          strokeWidth: highlighted ? 2 : 1.5,
          stroke: highlighted ? '#1677ff' : '#b1b1b7',
          transition: 'stroke .15s',
        }}
      />

      <EdgeLabelRenderer>
        {/* 边标签 */}
        {label && (
          <div style={{
            position: 'absolute',
            transform: `translate(-50%,-50%) translate(${labelX}px,${labelY - 16}px)`,
            fontSize: 11, background: '#fff', padding: '1px 6px',
            border: '1px solid #e8e8e8', borderRadius: 10,
            pointerEvents: 'none', color: '#595959',
            boxShadow: '0 1px 3px rgba(0,0,0,.08)',
          }}>
            {String(label)}
          </div>
        )}

        {highlighted && canInsertOnEdge && (
          <button
            type="button"
            className="nopan nodrag"
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${labelY}px)`,
              width: 26,
              height: 26,
              borderRadius: '50%',
              border: '1px solid #8b5cf6',
              background: '#fff',
              color: '#8b5cf6',
              boxShadow: '0 2px 8px rgba(139,92,246,.18)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              pointerEvents: 'all',
              cursor: 'pointer',
            }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            onMouseDown={e => e.stopPropagation()}
            onClick={e => {
              e.stopPropagation()
              startInsertOnEdge(id)
            }}
          >
            <PlusOutlined style={{ fontSize: 12 }} />
          </button>
        )}

        {/*
          删除按钮只在 selected 时渲染。
          selected 由 ReactFlow 内部管理（点击边即选中），
          完全绕开 SVG ↔ HTML 跨层 hover 问题。
        */}
        {selected && (
          <div
            className="nopan nodrag"
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${deleteOffsetY}px)`,
              pointerEvents: 'all',
              background: 'rgba(28,28,28,0.88)',
              borderRadius: 7,
              padding: '4px 10px',
              boxShadow: '0 2px 8px rgba(0,0,0,.28)',
              display: 'flex', alignItems: 'center', gap: 5,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
            onMouseDown={e => e.stopPropagation()}
            onClick={e => { e.stopPropagation(); deleteEdge(id) }}
          >
            <DeleteOutlined style={{ fontSize: 11, color: '#ff7875' }} />
            <span style={{ fontSize: 11, color: '#d9d9d9', userSelect: 'none' }}>删除连线</span>
          </div>
        )}
      </EdgeLabelRenderer>
    </>
  )
}
