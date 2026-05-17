import { Handle, Position, type NodeProps } from '@xyflow/react'

export type PlaceholderData = {
  _placeholder: true
  sourceId:     string
  handleId:     string
  label:        string
  color:        string
}

// 与真实节点尺寸一致，碰撞检测才准确
export const PLACEHOLDER_W = 200
export const PLACEHOLDER_H = 76

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
        style={{ opacity: 0, pointerEvents: 'none', width: 1, height: 1 }}
      />
      <span style={{ fontSize: 12, fontWeight: 600, color: d.color }}>{d.label}</span>
      <span style={{ fontSize: 11, color: '#8c8c8c', marginTop: 4 }}>拖节点到这里</span>
      <span style={{ fontSize: 18, color: d.color, lineHeight: 1, marginTop: 4 }}>＋</span>
    </div>
  )
}
