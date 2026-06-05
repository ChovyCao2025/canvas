/**
 * 组件职责：React Flow 自定义边组件，提供悬浮高亮、删除按钮和分支标签展示。
 *
 * 维护说明：边的真实含义由 sourceHandle 与节点配置共同决定，组件本身不改写关系。
 */
import { useState } from 'react'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useCanvasActions } from '../../context/CanvasActionsContext'

/**
 * 自定义边组件：
 * - hover/selected 高亮
 * - 显示边标签
 * - selected 时显示“删除连线”快捷按钮
 * - 删除行为通过 CanvasActionsContext 回调到编辑器状态层
 *
 * 交互设计说明：
 * - 用一条“加宽透明 path”接管 hover 命中，提升鼠标可点性；
 * - 删除按钮仅在 selected 渲染，避免常驻按钮遮挡画布。
 */
export default function HoverEdge({
  id, sourceX, sourceY, targetX, targetY,
  sourcePosition, targetPosition, sourceHandleId,
  selected, label, style, markerEnd,
}: EdgeProps) {
  // hovered 仅用于视觉高亮，不影响 ReactFlow 选中态管理
  const [hovered, setHovered] = useState(false)
  const { deleteEdge, startInsertOnEdge, canInsertOnEdge } = useCanvasActions()

  // ReactFlow 根据两端坐标计算贝塞尔曲线路径和标签中心点
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX, sourceY, sourcePosition,
    targetX, targetY, targetPosition,
  })

  // hovered 或 selected 任意成立都进入高亮态
  const highlighted = hovered || !!selected
  const deleteOffsetY = labelY + 30
  const isPlaceholderHelperEdge = id.startsWith('__phe_')

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
          // 高亮时加粗+主色，未高亮保持中性灰
          // 颜色变化保持轻量，避免画布大量边同时重绘造成视觉闪烁
          strokeWidth: highlighted ? 2 : 1.5,
          stroke: highlighted ? '#1677ff' : '#b1b1b7',
          transition: 'stroke .15s',
        }}
      />

      <EdgeLabelRenderer>
        {/* 边标签：展示分支条件或业务语义 */}
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

        {highlighted && canInsertOnEdge && !isPlaceholderHelperEdge && (
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
            aria-label={`插入到连线${sourceHandleId ? ` ${sourceHandleId}` : ''}`}
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
          <button
            type="button"
            className="nopan nodrag"
            aria-label={`删除连线${sourceHandleId ? ` ${sourceHandleId}` : ''}`}
            style={{
              position: 'absolute',
              transform: `translate(-50%,-50%) translate(${labelX}px,${deleteOffsetY}px)`,
              pointerEvents: 'all',
              background: 'rgba(28,28,28,0.88)',
              border: 'none',
              borderRadius: 7,
              padding: '4px 10px',
              boxShadow: '0 2px 8px rgba(0,0,0,.28)',
              display: 'flex', alignItems: 'center', gap: 5,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
            onMouseDown={e => e.stopPropagation()}
            // 先 stopPropagation，避免点击删除时触发边选中切换
            onClick={e => { e.stopPropagation(); deleteEdge(id) }}
          >
            <DeleteOutlined style={{ fontSize: 11, color: '#ff7875' }} />
            <span style={{ fontSize: 11, color: '#d9d9d9', userSelect: 'none' }}>删除连线</span>
          </button>
        )}
      </EdgeLabelRenderer>
    </>
  )
}
