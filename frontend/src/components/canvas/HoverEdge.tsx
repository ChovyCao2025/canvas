import { useEffect, useRef, useState } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import { DeleteOutlined } from '@ant-design/icons'
import { useCanvasActions } from '../../context/CanvasActionsContext'

export default function HoverEdge({
  id, sourceX, sourceY, targetX, targetY,
  sourcePosition, targetPosition,
  selected, label, style, markerEnd,
}: EdgeProps) {
  const [hovered, setHovered] = useState(false)
  const { deleteEdge } = useCanvasActions()

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX, sourceY, sourcePosition,
    targetX, targetY, targetPosition,
  })

  const leaveTimer = useRef<ReturnType<typeof setTimeout>>()
  const enter = () => { clearTimeout(leaveTimer.current); setHovered(true) }
  const leave = () => { leaveTimer.current = setTimeout(() => setHovered(false), 400) }

  // 组件卸载时清理 timer，防止 stale state update
  useEffect(() => () => clearTimeout(leaveTimer.current), [])

  const show = hovered || !!selected

  return (
    <>
      {/* 透明宽路径：hover 触发区 */}
      <path
        d={edgePath} fill="none" strokeWidth={20} stroke="transparent"
        style={{ cursor: 'pointer' }}
        onMouseEnter={enter} onMouseLeave={leave}
      />
      <BaseEdge path={edgePath} markerEnd={markerEnd}
        style={{
          ...style,
          strokeWidth: show ? 2 : 1.5,
          stroke: show ? '#1677ff' : '#b1b1b7',
          transition: 'stroke .15s',
        }}
      />

      <EdgeLabelRenderer>
        {label && (
          <div style={{
            position: 'absolute',
            transform: `translate(-50%,-50%) translate(${labelX}px,${labelY - 14}px)`,
            fontSize: 11, background: '#fff', padding: '1px 6px',
            border: '1px solid #e8e8e8', borderRadius: 10,
            pointerEvents: 'none', color: '#595959',
            boxShadow: '0 1px 3px rgba(0,0,0,.08)',
          }}>
            {String(label)}
          </div>
        )}

        {/*
          按钮始终在 DOM，opacity 切换（opacity:0 仍接收鼠标事件，visibility:hidden 不行）。
          nopan 阻止 ReactFlow pane 层拦截 click。
          !show 守卫防止不可见时误触。
        */}
        <div
          className="nopan"
          style={{
            position: 'absolute',
            transform: `translate(-50%,-50%) translate(${labelX}px,${labelY + 14}px)`,
            pointerEvents: 'all',
            opacity: show ? 1 : 0,
            transition: 'opacity .15s',
            background: 'rgba(28,28,28,0.88)',
            borderRadius: 7,
            padding: '4px 10px',
            boxShadow: '0 2px 8px rgba(0,0,0,.28)',
            display: 'flex', alignItems: 'center', gap: 5,
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
          onMouseEnter={enter}
          onMouseLeave={leave}
          onClick={e => {
            if (!show) return
            e.stopPropagation()
            deleteEdge(id)
          }}
        >
          <DeleteOutlined style={{ fontSize: 11, color: '#ff7875' }} />
          <span style={{ fontSize: 11, color: '#d9d9d9', userSelect: 'none' }}>删除连线</span>
        </div>
      </EdgeLabelRenderer>
    </>
  )
}
