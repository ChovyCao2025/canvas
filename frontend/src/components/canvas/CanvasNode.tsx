import { memo, useState } from 'react'
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

  const ActionButtons = () => (
    <div
      style={{
        position: 'absolute', top: -28, right: 0,
        display: 'flex', gap: 3, zIndex: 10,
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <Tooltip title="复制节点">
        <Button size="small" shape="circle"
          icon={<CopyOutlined style={{ fontSize: 10 }} />}
          style={{ width: 22, height: 22, minWidth: 22, padding: 0, background: '#fff' }}
          onClick={e => { e.stopPropagation(); copyNode(id) }}
        />
      </Tooltip>
      <Tooltip title="删除节点">
        <Button size="small" danger shape="circle"
          icon={<DeleteOutlined style={{ fontSize: 10 }} />}
          style={{ width: 22, height: 22, minWidth: 22, padding: 0 }}
          onClick={e => { e.stopPropagation(); deleteNode(id) }}
        />
      </Tooltip>
    </div>
  )

  // START / END 节点：圆形样式
  if (isStart || isEnd) {
    const color = isStart ? '#52c41a' : '#f5222d'
    return (
      <div
        style={{ position: 'relative' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        {hovered && <ActionButtons />}
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
      </div>
    )
  }

  return (
    <div
      style={{ position: 'relative' }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {hovered && <ActionButtons />}
      <div style={{
        width: 200, borderRadius: 8,
        border: `2px solid ${selected ? '#1677ff' : 'transparent'}`,
        boxShadow: `0 2px 8px rgba(0,0,0,${hovered ? '.25' : '.15'})`,
        overflow: 'hidden',
      }}>
        {!isTrigger && (
          <Handle type="target" position={Position.Top} id="input"
            style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }} />
        )}

        <div style={{ background: bg, padding: '6px 10px', display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ color: '#fff', fontSize: 11, fontWeight: 600, lineHeight: 1.2 }}>
            {d.category} · {d.nodeType}
          </span>
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
    </div>
  )
})

CanvasNode.displayName = 'CanvasNode'
export default CanvasNode
