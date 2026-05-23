import { memo, useEffect, useRef, useState } from 'react'
import { Handle, Position, NodeToolbar, type NodeProps } from '@xyflow/react'
import { Tooltip } from 'antd'
import { CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import type { CanvasNodeData } from './constants'
import { CATEGORY_COLORS, TRIGGER_TYPES, TERMINAL_TYPES } from './constants'
import { getBranchHandles } from './branchHandles'
import { useCanvasActions } from '../../context/CanvasActionsContext'

/**
 * 节点悬停态管理：
 * 鼠标离开后延迟隐藏工具栏，避免用户移动到工具栏时闪烁。
 */
function useHover() {
  const [hovered, setHovered] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout>>()
  const enter = () => { clearTimeout(timer.current); setHovered(true) }
  const leave = () => { timer.current = setTimeout(() => setHovered(false), 300) }
  useEffect(() => () => clearTimeout(timer.current), [])
  return { hovered, enter, leave }
}

/**
 * 节点悬浮操作条（复制 / 删除）。
 */
function Toolbar({ onCopy, onDelete, enter, leave }: {
  onCopy: () => void; onDelete: () => void
  enter: () => void; leave: () => void
}) {
  return (
    <div
      onMouseEnter={enter}
      onMouseLeave={leave}
      style={{
        display: 'flex', alignItems: 'center', gap: 1,
        background: 'rgba(28,28,28,0.88)', borderRadius: 7,
        padding: '4px 6px', boxShadow: '0 3px 10px rgba(0,0,0,.3)',
      }}
    >
      <Tooltip title="复制" mouseEnterDelay={0.6}>
        <button
          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '3px 7px', borderRadius: 4, display: 'flex', alignItems: 'center', fontSize: 13, color: '#d9d9d9' }}
          onMouseDown={e => e.preventDefault()}
          onClick={e => { e.stopPropagation(); onCopy() }}
        ><CopyOutlined /></button>
      </Tooltip>
      <div style={{ width: 1, height: 12, background: 'rgba(255,255,255,.18)', margin: '0 2px' }} />
      <Tooltip title="删除" mouseEnterDelay={0.6}>
        <button
          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '3px 7px', borderRadius: 4, display: 'flex', alignItems: 'center', fontSize: 13, color: '#ff7875' }}
          onMouseDown={e => e.preventDefault()}
          onClick={e => { e.stopPropagation(); onDelete() }}
        ><DeleteOutlined /></button>
      </Tooltip>
    </div>
  )
}

/**
 * ReactFlow 节点渲染组件。
 *
 * 说明：
 * 1. START/END 渲染为圆形节点；
 * 2. 普通节点渲染为矩形卡片；
 * 3. 分支节点会在底部渲染多个 source handle；
 * 4. 所有删除/复制行为通过 CanvasActionsContext 分发。
 */
const CanvasNode = memo(({ data, id, selected }: NodeProps) => {
  const d = data as CanvasNodeData & { traceColor?: string }
  const { deleteNode, copyNode } = useCanvasActions()
  const { hovered, enter, leave } = useHover()

  const bg = d.traceColor ?? CATEGORY_COLORS[d.category] ?? '#722ed1'
  const isTrigger  = TRIGGER_TYPES.has(d.nodeType)
    || (d.nodeType === 'TAGGER' && d.bizConfig?.mode === 'realtime')
  const isTerminal  = TERMINAL_TYPES.has(d.nodeType)
  const isStart     = d.nodeType === 'START'
  const isEnd       = d.nodeType === 'END'
  const isAudienceTagger = d.nodeType === 'TAGGER' && d.bizConfig?.mode === 'audience'

  // 根据节点类型和配置动态计算当前节点应该暴露哪些分支出口
  const branchHandles = getBranchHandles(d.nodeType, d.bizConfig ?? {})
  const isBranching   = branchHandles.length > 0

  // START / END 使用独立视觉样式，且只保留单向连接点
  if (isStart || isEnd) {
    const color = isStart ? '#52c41a' : '#f5222d'
    return (
      <div onMouseEnter={enter} onMouseLeave={leave} style={{ display: 'inline-block' }}>
        <NodeToolbar position={Position.Top} offset={8} isVisible={hovered}>
          <Toolbar onCopy={() => copyNode(id)} onDelete={() => deleteNode(id)} enter={enter} leave={leave} />
        </NodeToolbar>
        <div style={{
          width: 80, height: 80, borderRadius: '50%', background: color,
          border: `3px solid ${selected ? '#1677ff' : '#fff'}`,
          boxShadow: '0 2px 8px rgba(0,0,0,.2)',
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
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
    <div onMouseEnter={enter} onMouseLeave={leave}>
      <NodeToolbar position={Position.Top} offset={8} isVisible={hovered}>
        <Toolbar onCopy={() => copyNode(id)} onDelete={() => deleteNode(id)} enter={enter} leave={leave} />
      </NodeToolbar>
      <div
        style={{
          width: 200, borderRadius: 8,
          border: `2px solid ${selected ? '#1677ff' : 'transparent'}`,
          boxShadow: hovered ? '0 4px 14px rgba(0,0,0,.18)' : '0 2px 8px rgba(0,0,0,.12)',
          overflow: 'hidden', transition: 'box-shadow .15s',
        }}
      >
        {!isTrigger && (
          // 非触发器节点允许从上游连入
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
          {isAudienceTagger && (
            <div style={{ marginTop: 6, fontSize: 11, color: '#8c8c8c' }}>
              人群 ID: {String(d.bizConfig?.audienceId ?? '未配置')}
            </div>
          )}
        </div>
        {!isTerminal && (
          isBranching ? (
            // 多分支节点：每个分支单独暴露 source handle，并显示分支标签
            <div style={{ paddingBottom: 18 }}>
              {branchHandles.map((h, i) => {
                const pct = ((i + 1) / (branchHandles.length + 1)) * 100
                return (
                  <Handle
                    key={h.id}
                    type="source"
                    position={Position.Bottom}
                    id={h.id}
                    style={{
                      left:       `${pct}%`,
                      background: h.color,
                      border:     '2px solid #fff',
                      width:      10,
                      height:     10,
                    }}
                  >
                    <span style={{
                      position:  'absolute',
                      top:       12,
                      left:      '50%',
                      transform: 'translateX(-50%)',
                      fontSize:  9,
                      color:     h.color,
                      whiteSpace: 'nowrap',
                      pointerEvents: 'none',
                    }}>
                      {h.label}
                    </span>
                  </Handle>
                )
              })}
            </div>
          ) : (
            // 单出口节点：统一使用 default handle
            <Handle
              type="source"
              position={Position.Bottom}
              id="default"
              style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }}
            />
          )
        )}
      </div>
    </div>
  )
})

CanvasNode.displayName = 'CanvasNode'
export default CanvasNode
