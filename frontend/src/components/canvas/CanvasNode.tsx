import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Tooltip } from 'antd'
import { CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import type { CanvasNodeData } from './constants'
import { CATEGORY_COLORS, TRIGGER_TYPES, TERMINAL_TYPES } from './constants'
import { useCanvasActions } from '../../context/CanvasActionsContext'

// 注入全局样式（模块级，只执行一次）
if (typeof document !== 'undefined' && !document.getElementById('canvas-node-hover-style')) {
  const s = document.createElement('style')
  s.id = 'canvas-node-hover-style'
  s.textContent = `
    .cnv-node-wrap .cnv-node-actions { opacity: 0; pointer-events: none; transform: translateX(-50%) translateY(4px); transition: opacity .15s, transform .15s; }
    .cnv-node-wrap:hover .cnv-node-actions { opacity: 1; pointer-events: all; transform: translateX(-50%) translateY(0); }
    .cnv-node-wrap:hover .cnv-node-inner { box-shadow: 0 4px 14px rgba(0,0,0,.18) !important; }
    .cnv-node-action-btn { background: none; border: none; cursor: pointer; padding: 3px 7px; border-radius: 4px; display: flex; align-items: center; font-size: 13px; transition: background .12s; }
    .cnv-node-action-btn:hover { background: rgba(255,255,255,.15); }
  `
  document.head.appendChild(s)
}

function ActionBar({ onCopy, onDelete }: { onCopy: () => void; onDelete: () => void }) {
  return (
    <div
      className="cnv-node-actions"
      style={{
        position: 'absolute', top: -36, left: '50%',
        display: 'flex', alignItems: 'center', gap: 1,
        background: 'rgba(28,28,28,0.88)',
        borderRadius: 7, padding: '4px 6px',
        boxShadow: '0 3px 10px rgba(0,0,0,.3)',
        whiteSpace: 'nowrap',
      }}
      onMouseDown={e => e.stopPropagation()}
    >
      <Tooltip title="复制" mouseEnterDelay={0.6}>
        <button
          className="cnv-node-action-btn"
          style={{ color: '#d9d9d9' }}
          onClick={e => { e.stopPropagation(); onCopy() }}
        >
          <CopyOutlined />
        </button>
      </Tooltip>
      <div style={{ width: 1, height: 12, background: 'rgba(255,255,255,.18)', margin: '0 2px' }} />
      <Tooltip title="删除" mouseEnterDelay={0.6}>
        <button
          className="cnv-node-action-btn"
          style={{ color: '#ff7875' }}
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
  const { deleteNode, copyNode } = useCanvasActions()

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
      <div className="cnv-node-wrap" style={{ position: 'relative', display: 'inline-block' }}>
        <ActionBar onCopy={() => copyNode(id)} onDelete={() => deleteNode(id)} />
        <div style={{
          width: 80, height: 80, borderRadius: '50%', background: color,
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
    <div className="cnv-node-wrap" style={{ position: 'relative' }}>
      <ActionBar onCopy={() => copyNode(id)} onDelete={() => deleteNode(id)} />
      <div
        className="cnv-node-inner"
        style={{
          width: 200, borderRadius: 8,
          border: `2px solid ${selected ? '#1677ff' : 'transparent'}`,
          boxShadow: '0 2px 8px rgba(0,0,0,.12)',
          overflow: 'hidden', transition: 'box-shadow .15s',
        }}
      >
        {!isTrigger && (
          <Handle type="target" position={Position.Top} id="input"
            style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }} />
        )}
        <div style={{ background: bg, padding: '6px 10px' }}>
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
