import { Handle, Position, type NodeProps } from '@xyflow/react'

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
        border:         `2px dashed ${d.color}`,
        borderRadius:   8,
        background:     `${d.color}0d`,
        display:        'flex',
        flexDirection:  'column',
        alignItems:     'center',
        justifyContent: 'center',
        cursor:         'default',
        userSelect:     'none',
        pointerEvents:  'all',
      }}
    >
      <Handle
        type="target"
        position={Position.Top}
        id="input"
        // 只保留连线锚点，不显示可见圆点
        style={{ opacity: 0, pointerEvents: 'none', width: 1, height: 1 }}
      />
      <span style={{ fontSize: 12, fontWeight: 600, color: d.color }}>{d.label}</span>
      <span style={{ fontSize: 11, color: '#8c8c8c', marginTop: 4 }}>拖节点到这里</span>
      <span style={{ fontSize: 18, color: d.color, lineHeight: 1, marginTop: 4 }}>＋</span>
    </div>
  )
}
