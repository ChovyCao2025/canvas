import { useState } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import { Button } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import { useCanvasActions } from '../../context/CanvasActionsContext'

export default function HoverEdge({
  id, sourceX, sourceY, targetX, targetY,
  sourcePosition, targetPosition,
  label, style, markerEnd,
}: EdgeProps) {
  const [hovered, setHovered] = useState(false)
  const { deleteEdge } = useCanvasActions()

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX, sourceY, sourcePosition,
    targetX, targetY, targetPosition,
  })

  return (
    <>
      {/* 加宽的透明区域捕获 hover（SVG path 细线难 hover） */}
      <path
        d={edgePath}
        fill="none"
        strokeWidth={16}
        stroke="transparent"
        style={{ cursor: 'pointer' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      />
      <BaseEdge
        path={edgePath}
        markerEnd={markerEnd}
        style={{ ...style, strokeWidth: hovered ? 2.5 : 1.5, stroke: hovered ? '#1677ff' : '#b1b1b7' }}
      />
      <EdgeLabelRenderer>
        {/* 边 label（分组/分支名） */}
        {label && (
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${labelY}px)`,
              fontSize: 11, background: '#fff', padding: '1px 5px',
              border: '1px solid #e8e8e8', borderRadius: 3, pointerEvents: 'none',
              color: '#595959',
            }}
            className="nodrag nopan"
          >
            {String(label)}
          </div>
        )}
        {/* hover 时显示删除按钮 */}
        {hovered && (
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${labelY + (label ? 16 : 0)}px)`,
              pointerEvents: 'all',
            }}
            className="nodrag nopan"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
          >
            <Button
              size="small" danger shape="circle"
              icon={<DeleteOutlined style={{ fontSize: 10 }} />}
              style={{ width: 20, height: 20, minWidth: 20, padding: 0 }}
              onClick={e => { e.stopPropagation(); deleteEdge(id) }}
            />
          </div>
        )}
      </EdgeLabelRenderer>
    </>
  )
}
