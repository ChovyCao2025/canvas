import { useEffect, useRef, useState } from 'react'
import {
  Button, Input, Space, Tag, Tooltip, message,
  Typography, Spin, Divider,
} from 'antd'
import {
  SaveOutlined, CloudUploadOutlined,
  ArrowLeftOutlined, HistoryOutlined,
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { canvasApi } from '../../services/api'
import type { CanvasDetail } from '../../types'

const { Title, Text } = Typography

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已下线', color: 'red' },
}

export default function CanvasEditorPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const canvasId = Number(id)

  const [detail, setDetail] = useState<CanvasDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [canvasName, setCanvasName] = useState('')
  // graphJson 的状态将在 Phase 2 由 xflow 管理
  const graphJsonRef = useRef<string>('{"nodes":[]}')

  useEffect(() => {
    canvasApi.get(canvasId).then((res) => {
      setDetail(res.data)
      setCanvasName(res.data.canvas.name)
      graphJsonRef.current = res.data.graphJson
    }).finally(() => setLoading(false))
  }, [canvasId])

  const handleSave = async () => {
    setSaving(true)
    try {
      await canvasApi.update(canvasId, {
        name: canvasName,
        graphJson: graphJsonRef.current,
        updatedBy: 'admin',
      })
      message.success('保存成功')
      // 刷新 detail
      const res = await canvasApi.get(canvasId)
      setDetail(res.data)
    } finally {
      setSaving(false)
    }
  }

  const handlePublish = async () => {
    await canvasApi.publish(canvasId)
    message.success('发布成功')
    const res = await canvasApi.get(canvasId)
    setDetail(res.data)
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  const status = detail?.canvas.status ?? 0
  const { label: statusLabel, color: statusColor } = STATUS_MAP[status]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>

      {/* ── 顶部工具栏 ── */}
      <div style={{
        display: 'flex', alignItems: 'center', padding: '0 16px',
        height: 56, borderBottom: '1px solid #f0f0f0', background: '#fff',
        flexShrink: 0,
      }}>
        <Tooltip title="返回列表">
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/canvas')}
          />
        </Tooltip>

        <Divider type="vertical" />

        <Input
          value={canvasName}
          onChange={(e) => setCanvasName(e.target.value)}
          variant="borderless"
          style={{ width: 300, fontWeight: 500, fontSize: 16 }}
          placeholder="画布名称"
        />

        <Tag color={statusColor} style={{ marginLeft: 8 }}>{statusLabel}</Tag>

        <div style={{ flex: 1 }} />

        <Space>
          <Button icon={<HistoryOutlined />}>版本历史</Button>
          <Button icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
            保存
          </Button>
          {status !== 1 && (
            <Button type="primary" icon={<CloudUploadOutlined />} onClick={handlePublish}>
              发布
            </Button>
          )}
        </Space>
      </div>

      {/* ── 主体区域：三栏布局 ── */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>

        {/* 左侧：节点面板（Phase 2 接入 xflow NsNodeCollapsePanel） */}
        <div style={{
          width: 240, borderRight: '1px solid #f0f0f0',
          background: '#fafafa', padding: 12, overflowY: 'auto',
        }}>
          <Text type="secondary" style={{ fontSize: 12 }}>节点面板</Text>
          <div style={{ marginTop: 8, color: '#bbb', fontSize: 12 }}>
            Phase 2 接入 xflow 节点拖拽面板
          </div>
        </div>

        {/* 中央：画布区域（Phase 2 接入 xflow XFlowCanvas） */}
        <div style={{
          flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: '#f5f5f5', position: 'relative',
        }}>
          <div style={{ textAlign: 'center', color: '#bbb' }}>
            <Title level={4} style={{ color: '#bbb' }}>画布区域</Title>
            <Text type="secondary">Phase 2 接入 @antv/xflow 可视化编辑器</Text>
            <div style={{ marginTop: 16, fontSize: 12, color: '#d9d9d9' }}>
              当前 graph JSON 已从后端加载，保存功能正常工作
            </div>
          </div>
        </div>

        {/* 右侧：配置面板（Phase 2 接入 xflow NsJsonSchemaForm） */}
        <div style={{
          width: 300, borderLeft: '1px solid #f0f0f0',
          background: '#fff', padding: 12,
        }}>
          <Text type="secondary" style={{ fontSize: 12 }}>节点配置</Text>
          <div style={{ marginTop: 8, color: '#bbb', fontSize: 12 }}>
            选中节点后显示 Schema 驱动表单
          </div>
        </div>
      </div>
    </div>
  )
}
