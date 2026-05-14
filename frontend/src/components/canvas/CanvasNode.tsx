import { memo, useRef, useState } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Button, Tooltip } from 'antd'
import { CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import type { CanvasNodeData } from './constants'
import { CATEGORY_COLORS, TRIGGER_TYPES, TERMINAL_TYPES } from './constants'
import { useCanvasActions } from '../../context/CanvasActionsContext'

const CanvasNode = memo(({ data, id, selected }: NodeProps) => {
  const d = data as CanvasNodeData & { traceColor?: string }
  const [hovered, setHovered] = useState(false)
  const { deleteNode, copyNode } = useCanvasActions()

  const bg = d.traceColor ?? CATEGORY_COLORS[d.category] ?? '#722ed1'
  const isTrigger  = TRIGGER_TYPES.has(d.nodeType)
  const isTerminal = TERMINAL_TYPES.has(d.nodeType)
  const isStart    = d.nodeType === 'START'
  const isEnd      = d.nodeType === 'END'
  const isIf       = d.nodeType === 'IF_CONDITION'
  const branches   = (d.bizConfig?.branches as { label?: string }[] | undefined) ?? []
  const isSelector = d.nodeType === 'SELECTOR'

  // START / END 节点：圆形样式
  if (isStart || isEnd) {
    const color = isStart ? '#52c41a' : '#f5222d'
    return (
      <div
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        style={{ position: 'relative' }}
      >
        <div style={{
          width: 80, height: 80, borderRadius: '50%',
          background: color,
          border: `3px solid ${selected ? '#1677ff' : '#fff'}`,
          boxShadow: '0 2px 8px rgba(0,0,0,.2)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontWeight: 700, fontSize: 14,
          position: 'relative',
        }}>
          {isStart
            ? <Handle type="source" position={Position.Bottom} id="default"
                style={{ background: '#fff', border: '2px solid #52c41a', width: 10, height: 10 }} />
            : <Handle type="target" position={Position.Top} id="input"
                style={{ background: '#fff', border: '2px solid #f5222d', width: 10, height: 10 }} />
          }
          {isStart ? '开始' : '结束'}
        </div>
        {/* 悬浮操作按钮——叠在节点右上角内部，无 gap 问题 */}
        {hovered && (
          <div style={{ position: 'absolute', top: 2, right: 2, display: 'flex', gap: 2, zIndex: 10 }}>
            <Tooltip title="复制" mouseEnterDelay={0.5}>
              <Button size="small" shape="circle"
                icon={<CopyOutlined style={{ fontSize: 9 }} />}
                style={{ width: 18, height: 18, minWidth: 18, padding: 0, opacity: 0.85 }}
                onClick={e => { e.stopPropagation(); copyNode(id) }} />
            </Tooltip>
            <Tooltip title="删除" mouseEnterDelay={0.5}>
              <Button size="small" danger shape="circle"
                icon={<DeleteOutlined style={{ fontSize: 9 }} />}
                style={{ width: 18, height: 18, minWidth: 18, padding: 0, opacity: 0.85 }}
                onClick={e => { e.stopPropagation(); deleteNode(id) }} />
            </Tooltip>
          </div>
        )}
      </div>
    )
  }

  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        width: 200, borderRadius: 8, position: 'relative',
        border: `2px solid ${selected ? '#1677ff' : 'transparent'}`,
        boxShadow: `0 2px 8px rgba(0,0,0,${hovered ? '.25' : '.15'})`,
        overflow: 'hidden',
      }}
    >
      {!isTrigger && (
        <Handle type="target" position={Position.Top} id="input"
          style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }} />
      )}

      {/* 头部色块 + 操作按钮叠加其上 */}
      <div style={{ background: bg, padding: '6px 10px', display: 'flex', alignItems: 'center', gap: 6, position: 'relative' }}>
        <span style={{ color: '#fff', fontSize: 11, fontWeight: 600, lineHeight: 1.2, flex: 1 }}>
          {d.category} · {d.nodeType}
        </span>
        {/* 按钮叠在 header 右侧，始终与节点同一 DOM，无 gap */}
        {hovered && (
          <div style={{ display: 'flex', gap: 2 }} onClick={e => e.stopPropagation()}>
            <Tooltip title="复制" mouseEnterDelay={0.5}>
              <Button size="small" shape="circle"
                icon={<CopyOutlined style={{ fontSize: 9 }} />}
                style={{ width: 18, height: 18, minWidth: 18, padding: 0,
                         background: 'rgba(255,255,255,0.9)', border: 'none' }}
                onClick={e => { e.stopPropagation(); copyNode(id) }} />
            </Tooltip>
            <Tooltip title="删除" mouseEnterDelay={0.5}>
              <Button size="small" danger shape="circle"
                icon={<DeleteOutlined style={{ fontSize: 9 }} />}
                style={{ width: 18, height: 18, minWidth: 18, padding: 0, opacity: 0.9 }}
                onClick={e => { e.stopPropagation(); deleteNode(id) }} />
            </Tooltip>
          </div>
        )}
      </div>

      <div style={{ background: '#fff', padding: '8px 10px', fontSize: 13, color: '#262626', lineHeight: 1.4, minHeight: 36 }}>
        {d.name || '未命名'}
      </div>

      {!isTerminal && !isIf && !isSelector && (
        <Handle type="source" position={Position.Bottom} id="default"
          style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }} />
      )}

      {isIf && (<>
        <Handle type="source" position={Position.Bottom} id="success"
          style={{ left: '30%', background: '#52c41a', border: '2px solid #fff', width: 10, height: 10 }} />
        <Handle type="source" position={Position.Bottom} id="fail"
          style={{ left: '70%', background: '#f5222d', border: '2px solid #fff', width: 10, height: 10 }} />
      </>)}

      {isSelector && (<>
        {branches.map((_, i) => (
          <Handle key={i} type="source" position={Position.Bottom}
            id={`branch-${i}`}
            style={{
              left: `${(i + 1) / (branches.length + 2) * 100}%`,
              background: '#1677ff', border: '2px solid #fff', width: 10, height: 10,
            }} />
        ))}
        <Handle type="source" position={Position.Bottom} id="else"
          style={{
            left: `${(branches.length + 1) / (branches.length + 2) * 100}%`,
            background: '#8c8c8c', border: '2px solid #fff', width: 10, height: 10,
          }} />
      </>)}
    </div>
  )
})

CanvasNode.displayName = 'CanvasNode'
export default CanvasNode
