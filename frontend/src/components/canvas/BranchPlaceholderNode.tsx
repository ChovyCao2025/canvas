import { Handle, Position, type NodeProps } from '@xyflow/react'

export type PlaceholderData = {
  _placeholder: true
  sourceId:     string
  handleId:     string
  label:        string
  color:        string
}

export default function BranchPlaceholderNode({ data }: NodeProps) {
  const d = data as PlaceholderData
  return (
    <div
      style={{
        width:          150,
        height:         52,
        border:         `2px dashed ${d.color}`,
        borderRadius:   8,
        background:     `${d.color}14`,
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
      <span style={{ fontSize: 11, fontWeight: 600, color: d.color }}>{d.label}</span>
      <span style={{ fontSize: 10, color: '#8c8c8c', marginTop: 2 }}>拖节点到这里</span>
      <span style={{ fontSize: 15, color: d.color, lineHeight: 1, marginTop: 2 }}>＋</span>
    </div>
  )
}
