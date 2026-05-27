/**
 * 组件职责：分支占位节点组件，显示“拖节点到这里连接”的虚拟落点。
 *
 * 维护说明：它只参与编辑器交互反馈，不代表真实可执行节点。
 */
import { Handle, Position, type NodeProps } from '@xyflow/react'

/** 分支占位节点 data，记录它代表哪个源节点和出口 handle。 */
export type PlaceholderData = {
  /** 标记该节点是占位节点而非真实业务节点。 */
  _placeholder: true

  /** 需要回写后继关系的源节点 ID。 */
  sourceId: string

  /** 源节点出口 handle ID。 */
  handleId: string

  /** 占位文案。 */
  label: string

  /** 占位边框主题色。 */
  color: string
}

// 与真实节点尺寸一致，碰撞检测才准确。
// 否则拖拽投放时会出现“看起来落在框里，但判定没命中”的错位。
export const PLACEHOLDER_W = 200

/** 分支占位节点高度，与真实画布节点的命中区域保持接近。 */
export const PLACEHOLDER_H = 76

/**
 * 分支占位节点。
 *
 * 用途：
 * - 当多分支节点某个出口还没接后继时，先渲染一个“可投放占位”；
 * - 用户把节点拖到该占位上后，编辑器会回写真实连线并移除占位节点。
 */
export default function BranchPlaceholderNode({ data }: NodeProps) {
  const d = data as PlaceholderData
  return (
    <div
      style={{
        width:          PLACEHOLDER_W,
        height:         PLACEHOLDER_H,
        border:         `1px dashed ${d.color}66`,
        borderRadius:   10,
        background:     '#fafbfc',
        display:        'flex',
        flexDirection:  'column',
        alignItems:     'center',
        justifyContent: 'center',
        cursor:         'default',
        userSelect:     'none',
        pointerEvents:  'all',
        boxSizing:      'border-box',
        position:       'relative',
        boxShadow:      'inset 0 0 0 1px rgba(255,255,255,0.7)',
      }}
    >
      <div
        style={{
          position: 'absolute',
          inset: '0 auto 0 0',
          width: 4,
          borderTopLeftRadius: 10,
          borderBottomLeftRadius: 10,
          background: d.color,
          opacity: 0.75,
        }}
      />
      <Handle
        type="target"
        position={Position.Top}
        id="input"
        // 只保留连线锚点，不显示可见圆点
        style={{ opacity: 0, pointerEvents: 'none', width: 1, height: 1 }}
      />
      <span
        style={{
          width: 26,
          height: 26,
          borderRadius: '50%',
          border: `1px solid ${d.color}55`,
          background: `${d.color}10`,
          color: d.color,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 16,
          lineHeight: 1,
        }}
      >
        ＋
      </span>
      <span style={{ fontSize: 12, fontWeight: 600, color: d.color, marginTop: 6 }}>{d.label}</span>
      <span style={{ fontSize: 11, color: '#94a3b8', marginTop: 3 }}>拖到这里</span>
    </div>
  )
}
