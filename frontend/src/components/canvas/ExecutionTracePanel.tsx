import { useState, useCallback, useEffect } from 'react'
import {
  Modal, Select, Table, Tag, Tooltip, Typography,
  Space, Button, message,
} from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import http from '../../services/api'
import type { R } from '../../types'

const { Text } = Typography

export type TraceStatus = 0 | 1 | 2 | 3 // 执行中 / 成功 / 失败 / 跳过

export interface NodeTrace {
  nodeId:      string
  nodeType:    string
  nodeName:    string
  status:      TraceStatus
  durationMs?: number
  errorMsg?:   string
}

/** 节点状态 → React Flow 节点颜色（设计文档 14.2节色彩规范） */
export const TRACE_NODE_COLOR: Record<TraceStatus, string> = {
  0: '#faad14',  // 执行中 → 黄
  1: '#52c41a',  // 成功   → 绿
  2: '#f5222d',  // 失败   → 红
  3: '#d9d9d9',  // 跳过   → 灰
}

const STATUS_LABEL: Record<TraceStatus, [string, string]> = {
  0: ['processing', '执行中'],
  1: ['success',    '成功'],
  2: ['error',      '失败'],
  3: ['default',    '跳过'],
}

interface Props {
  canvasId: number
  onTraceLoaded: (colorMap: Record<string, string>) => void
  /** 测试运行完成后传入 executionId，自动打开面板并加载该次轨迹 */
  triggerExecutionId?: string | null
}

export default function ExecutionTracePanel({ canvasId, onTraceLoaded, triggerExecutionId }: Props) {
  const [executions, setExecutions] = useState<any[]>([])
  const [traces,     setTraces]     = useState<NodeTrace[]>([])
  const [visible,    setVisible]    = useState(false)
  const [loading,    setLoading]    = useState(false)
  const [selectedExecId, setSelectedExecId] = useState<string | null>(null)

  const open = useCallback(async () => {
    setVisible(true)
    setLoading(true)
    try {
      const res = await http.get<R<any[]>, R<any[]>>(`/canvas/${canvasId}/executions?size=20`)
      setExecutions(res.data ?? [])
    } catch { /* ignore */ } finally { setLoading(false) }
  }, [canvasId])

  // 测试运行完成后自动打开并加载
  useEffect(() => {
    if (!triggerExecutionId) return
    const run = async () => {
      setVisible(true)
      setLoading(true)
      try {
        const res = await http.get<R<any[]>, R<any[]>>(`/canvas/${canvasId}/executions?size=20`)
        setExecutions(res.data ?? [])
      } catch { /* ignore */ } finally { setLoading(false) }
      await loadTrace(triggerExecutionId)
    }
    run()
  }, [triggerExecutionId]) // eslint-disable-line react-hooks/exhaustive-deps

  const loadTrace = useCallback(async (executionId: string) => {
    setSelectedExecId(executionId)
    setLoading(true)
    try {
      const res = await http.get<R<NodeTrace[]>, R<NodeTrace[]>>(
        `/canvas/execution/${executionId}/trace`)
      const data = res.data ?? []
      setTraces(data)

      // 构建节点颜色 map 传给父组件（画布叠加显示）
      const colorMap: Record<string, string> = {}
      data.forEach(t => { colorMap[t.nodeId] = TRACE_NODE_COLOR[t.status] })
      onTraceLoaded(colorMap)
    } catch {
      message.error('加载执行轨迹失败')
    } finally { setLoading(false) }
  }, [onTraceLoaded])

  const clearTrace = () => {
    setSelectedExecId(null)
    setTraces([])
    onTraceLoaded({})  // 清除画布叠色
  }

  const columns = [
    { title: '节点名称', dataIndex: 'nodeName', ellipsis: true },
    { title: '类型',     dataIndex: 'nodeType',  width: 140 },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (s: TraceStatus) => {
        const [color, label] = STATUS_LABEL[s]
        return <Tag color={color}>{label}</Tag>
      },
    },
    {
      title: '耗时', dataIndex: 'durationMs', width: 80,
      render: (v?: number) => v != null ? `${v}ms` : '-',
    },
    {
      title: '错误', dataIndex: 'errorMsg', ellipsis: true,
      render: (v?: string) => v ? <Text type="danger" style={{ fontSize: 11 }}>{v}</Text> : '-',
    },
  ]

  return (
    <>
      <Tooltip title="查看执行轨迹">
        <Button icon={<EyeOutlined />} onClick={open} size="small">轨迹</Button>
      </Tooltip>

      <Modal
        title="执行轨迹"
        open={visible}
        onCancel={() => { setVisible(false); clearTrace() }}
        footer={null}
        width={700}
      >
        <Space style={{ marginBottom: 12 }} wrap>
          <Select
            style={{ width: 320 }}
            placeholder="选择执行记录"
            loading={loading}
            value={selectedExecId}
            onChange={loadTrace}
            options={executions.map(e => ({
              label: `${e.id?.slice(0, 8)} | ${e.triggerType} | ${e.createdAt?.slice(0, 16)}`,
              value: e.id,
            }))}
          />
          {selectedExecId && (
            <Button size="small" onClick={clearTrace}>清除叠色</Button>
          )}
        </Space>

        {selectedExecId && (
          <Table
            rowKey="nodeId"
            dataSource={traces}
            columns={columns}
            size="small"
            pagination={false}
            loading={loading}
          />
        )}

        {!selectedExecId && !loading && (
          <Text type="secondary" style={{ fontSize: 12 }}>
            选择一次执行记录后，画布节点将按执行状态着色（绿=成功 / 红=失败 / 灰=跳过 / 黄=执行中）
          </Text>
        )}
      </Modal>
    </>
  )
}
