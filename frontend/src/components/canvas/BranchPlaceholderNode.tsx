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
