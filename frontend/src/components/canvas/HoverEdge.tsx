import { useRef, useState } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import { DeleteOutlined } from '@ant-design/icons'
import { useCanvasActions } from '../../context/CanvasActionsContext'

export default function HoverEdge({
  id, sourceX, sourceY, targetX, targetY,
  sourcePosition, targetPosition,
  label, style, markerEnd,
}: EdgeProps) {
  const [hovered, setHovered] = useState(false)
  const btnRef = useRef<HTMLDivElement>(null)
  const { deleteEdge } = useCanvasActions()

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX, sourceY, sourcePosition,
    targetX, targetY, targetPosition,
  })

  // SVG → HTML 跨边界时 relatedTarget 为 null，用延迟兜底
  const leaveTimer = useRef<ReturnType<typeof setTimeout>>()
  const enter = () => { clearTimeout(leaveTimer.current); setHovered(true) }
  const leave = () => { leaveTimer.current = setTimeout(() => setHovered(false), 120) }

  const midY = labelY + (label ? 20 : 0)

  return (
    <>
      <path d={edgePath} fill="none" strokeWidth={18} stroke="transparent"
        style={{ cursor: 'pointer' }} onMouseEnter={enter} onMouseLeave={leave} />
      <BaseEdge path={edgePath} markerEnd={markerEnd}
        style={{ ...style, strokeWidth: hovered ? 2 : 1.5, stroke: hovered ? '#1677ff' : '#b1b1b7', transition: 'stroke .15s' }} />

      <EdgeLabelRenderer>
        {/* 边标签 */}
        {label && (
          <div style={{
            position: 'absolute',
            transform: `translate(-50%,-50%) translate(${labelX}px,${labelY}px)`,
            fontSize: 11, background: '#fff', padding: '1px 6px',
            border: '1px solid #e8e8e8', borderRadius: 10,
            pointerEvents: 'none', color: '#595959',
            boxShadow: '0 1px 3px rgba(0,0,0,.08)',
          }}>
            {String(label)}
          </div>
        )}

        {/* hover 删除按钮——与节点风格统一的深色小工具条 */}
        {hovered && (
          <div
            ref={btnRef}
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${midY}px)`,
              pointerEvents: 'all',
              background: 'rgba(30,30,30,0.82)', borderRadius: 6,
              padding: '3px 8px', boxShadow: '0 2px 8px rgba(0,0,0,.25)',
              display: 'flex', alignItems: 'center', gap: 4,
              cursor: 'pointer',
            }}
            onMouseEnter={enter}
            onMouseLeave={leave}
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
