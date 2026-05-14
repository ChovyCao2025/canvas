import { useRef, useState } from 'react'
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
  const leaveTimer = useRef<ReturnType<typeof setTimeout>>()
  const { deleteEdge } = useCanvasActions()

  const enter = () => {
    clearTimeout(leaveTimer.current)
    setHovered(true)
  }
  const leave = () => {
    // 延迟 120ms 再隐藏，给鼠标移到按钮留出时间
    leaveTimer.current = setTimeout(() => setHovered(false), 120)
  }

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX, sourceY, sourcePosition,
    targetX, targetY, targetPosition,
  })

  return (
    <>
      {/* 宽透明区域捕获 hover */}
      <path
        d={edgePath}
        fill="none"
        strokeWidth={18}
        stroke="transparent"
        style={{ cursor: 'pointer' }}
        onMouseEnter={enter}
        onMouseLeave={leave}
      />
      <BaseEdge
        path={edgePath}
        markerEnd={markerEnd}
        style={{ ...style, strokeWidth: hovered ? 2.5 : 1.5, stroke: hovered ? '#1677ff' : '#b1b1b7' }}
      />
      <EdgeLabelRenderer>
        {/* 边 label */}
        {label && (
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${labelY}px)`,
              fontSize: 11, background: '#fff', padding: '1px 6px',
              border: '1px solid #e8e8e8', borderRadius: 3,
              pointerEvents: 'none', color: '#595959',
            }}
          >
            {String(label)}
          </div>
        )}
        {/* hover 删除按钮 */}
        {hovered && (
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${labelY + (label ? 18 : 0)}px)`,
              pointerEvents: 'all',
            }}
            onMouseEnter={enter}
            onMouseLeave={leave}
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
