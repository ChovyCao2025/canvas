import { memo, useRef, useState } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Tooltip } from 'antd'
import { CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import type { CanvasNodeData } from './constants'
import { CATEGORY_COLORS, TRIGGER_TYPES, TERMINAL_TYPES } from './constants'
import { useCanvasActions } from '../../context/CanvasActionsContext'

/** 浮动操作工具条 */
function ActionBar({ onCopy, onDelete }: { onCopy: () => void; onDelete: () => void }) {
  return (
    <div style={{
      position: 'absolute', top: -34, left: '50%',
      transform: 'translateX(-50%)',
      display: 'flex', alignItems: 'center', gap: 1,
      background: 'rgba(30,30,30,0.82)', borderRadius: 6,
      padding: '3px 6px', boxShadow: '0 2px 8px rgba(0,0,0,.25)',
      zIndex: 20, pointerEvents: 'all',
    }}>
      <Tooltip title="复制节点" mouseEnterDelay={0.6}>
        <button
          style={{
            background: 'none', border: 'none', cursor: 'pointer',
            color: '#d9d9d9', padding: '2px 5px', borderRadius: 4,
            display: 'flex', alignItems: 'center', fontSize: 12,
            transition: 'color .15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.color = '#fff')}
          onMouseLeave={e => (e.currentTarget.style.color = '#d9d9d9')}
          onClick={e => { e.stopPropagation(); onCopy() }}
        >
          <CopyOutlined />
        </button>
      </Tooltip>
      <div style={{ width: 1, height: 12, background: 'rgba(255,255,255,.2)' }} />
      <Tooltip title="删除节点" mouseEnterDelay={0.6}>
        <button
          style={{
            background: 'none', border: 'none', cursor: 'pointer',
            color: '#ff7875', padding: '2px 5px', borderRadius: 4,
            display: 'flex', alignItems: 'center', fontSize: 12,
            transition: 'color .15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.color = '#ff4d4f')}
          onMouseLeave={e => (e.currentTarget.style.color = '#ff7875')}
          onClick={e => { e.stopPropagation(); onDelete() }}
        >
          <DeleteOutlined />
        </button>
      </Tooltip>
    </div>
  )
}

const CanvasNode = memo(({ data, id, selected }: NodeProps) => {
  const d = data as CanvasNodeData & { traceColor?: string }
  const [hovered, setHovered] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const { deleteNode, copyNode } = useCanvasActions()

  const handleMouseLeave = (e: React.MouseEvent) => {
    // 只有鼠标真正离开整个组件（包括浮层按钮）才隐藏
    if (!containerRef.current?.contains(e.relatedTarget as Node)) {
      setHovered(false)
    }
  }

  const bg = d.traceColor ?? CATEGORY_COLORS[d.category] ?? '#722ed1'
  const isTrigger  = TRIGGER_TYPES.has(d.nodeType)
  const isTerminal = TERMINAL_TYPES.has(d.nodeType)
  const isStart    = d.nodeType === 'START'
  const isEnd      = d.nodeType === 'END'
  const isIf       = d.nodeType === 'IF_CONDITION'
  const branches   = (d.bizConfig?.branches as { label?: string }[] | undefined) ?? []
  const isSelector = d.nodeType === 'SELECTOR'

  if (isStart || isEnd) {
    const color = isStart ? '#52c41a' : '#f5222d'
    return (
      <div
        ref={containerRef}
        style={{ position: 'relative', display: 'inline-block' }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={handleMouseLeave}
      >
        {hovered && (
          <ActionBar onCopy={() => copyNode(id)} onDelete={() => deleteNode(id)} />
        )}
        <div style={{
          width: 80, height: 80, borderRadius: '50%',
          background: color,
          border: `3px solid ${selected ? '#1677ff' : '#fff'}`,
          boxShadow: '0 2px 8px rgba(0,0,0,.2)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontWeight: 700, fontSize: 14, position: 'relative',
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
      ref={containerRef}
      style={{ position: 'relative' }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={handleMouseLeave}
    >
      {hovered && (
        <ActionBar onCopy={() => copyNode(id)} onDelete={() => deleteNode(id)} />
      )}
      <div style={{
        width: 200, borderRadius: 8,
        border: `2px solid ${selected ? '#1677ff' : hovered ? '#d0d0d0' : 'transparent'}`,
        boxShadow: `0 2px 8px rgba(0,0,0,${hovered ? '.22' : '.12'})`,
        overflow: 'hidden', transition: 'box-shadow .15s, border-color .15s',
      }}>
        {!isTrigger && (
          <Handle type="target" position={Position.Top} id="input"
            style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }} />
        )}

        <div style={{ background: bg, padding: '6px 10px', display: 'flex', alignItems: 'center' }}>
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
            <Handle key={i} type="source" position={Position.Bottom} id={`branch-${i}`}
              style={{ left: `${(i + 1) / (branches.length + 2) * 100}%`, background: '#1677ff', border: '2px solid #fff', width: 10, height: 10 }} />
          ))}
          <Handle type="source" position={Position.Bottom} id="else"
            style={{ left: `${(branches.length + 1) / (branches.length + 2) * 100}%`, background: '#8c8c8c', border: '2px solid #fff', width: 10, height: 10 }} />
        </>)}
      </div>
    </div>
  )
})

CanvasNode.displayName = 'CanvasNode'
export default CanvasNode
