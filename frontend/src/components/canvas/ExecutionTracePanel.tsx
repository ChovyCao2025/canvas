/**
 * 组件职责：执行轨迹面板，展示单次调试/运行的节点执行结果和上下文信息。
 *
 * 维护说明：面板数据来自后端 dry-run 或执行记录，前端只做状态归类和可读化展示。
 */
import { useState, useCallback } from 'react'
import {
  Modal, Select, Table, Tag, Tooltip, Typography,
  Button, message, Collapse,
} from 'antd'
import { EyeOutlined } from '@ant-design/icons'
import http from '../../services/api'
import type { R } from '../../types'

/** 轨迹面板内使用的文本组件别名。 */
const { Text } = Typography

/**
 * 节点轨迹状态码（与后端 trace.status 对齐）。
 */
export type TraceStatus = 0 | 1 | 2 | 3 // 执行中 / 成功 / 失败 / 跳过

/**
 * 单个节点的执行轨迹结构。
 */
export interface NodeTrace {
  /** 节点 ID。 */
  nodeId: string

  /** 节点类型。 */
  nodeType: string

  /** 节点名称。 */
  nodeName: string

  /** 执行状态。 */
  status: TraceStatus

  /** 执行耗时（毫秒）。 */
  durationMs?: number

  /** 错误信息。 */
  errorMsg?: string

  /** 节点输出（通常是 JSON 字符串）。 */
  outputData?: string
}

/**
 * 节点着色：轨迹状态 -> 节点边框/头部颜色。
 */
export const TRACE_NODE_COLOR: Record<TraceStatus, string> = {
  0: '#faad14',
  1: '#52c41a',
  2: '#f5222d',
  3: '#d9d9d9',
}

/**
 * 轨迹状态的标签展示配置。
 */
const STATUS_LABEL: Record<TraceStatus, [string, string]> = {
  0: ['processing', '执行中'],
  1: ['success',    '成功'],
  2: ['error',      '失败'],
  3: ['default',    '跳过'],
}

/** 执行轨迹面板组件入参。 */
interface Props {
  /** 当前画布 ID。 */
  canvasId: number

  /**
   * 把“nodeId -> color”映射回传给编辑器父组件，
   * 让画布上的节点按本次执行结果着色。
   */
  onTraceLoaded: (colorMap: Record<string, string>) => void
}

/**
 * 执行轨迹面板：
 * 1) 加载最近执行记录；
 * 2) 选择某次执行后加载节点轨迹；
 * 3) 把轨迹颜色映射同步给画布主视图。
 */
export default function ExecutionTracePanel({ canvasId, onTraceLoaded }: Props) {
  // 执行记录下拉选项数据
  const [executions,    setExecutions]    = useState<any[]>([])
  // 当前执行的节点轨迹列表
  const [traces,        setTraces]        = useState<NodeTrace[]>([])
  // 弹窗与加载状态
  const [visible,       setVisible]       = useState(false)
  const [loading,       setLoading]       = useState(false)
  // 当前选中的 executionId
  const [selectedExecId, setSelectedExecId] = useState<string | null>(null)

  // 打开弹窗并加载最近执行记录
  const open = useCallback(async () => {
    setVisible(true)
    setLoading(true)
    try {
      const res = await http.get<R<any[]>, R<any[]>>(`/canvas/${canvasId}/executions?size=20`)
      setExecutions(res.data ?? [])
    } catch { /* ignore */ } finally { setLoading(false) }
  }, [canvasId])

  // 加载某次执行的节点轨迹，并把颜色映射回传父层
  const loadTrace = useCallback(async (executionId: string) => {
    setSelectedExecId(executionId)
    setLoading(true)
    try {
      const res = await http.get<R<NodeTrace[]>, R<NodeTrace[]>>(
        `/canvas/${canvasId}/execution/${executionId}/trace`)
      const data = res.data ?? []
      setTraces(data)
      // nodeId -> color 映射供画布渲染使用
      const colorMap: Record<string, string> = {}
      data.forEach(t => { colorMap[t.nodeId] = TRACE_NODE_COLOR[t.status] })
      onTraceLoaded(colorMap)
    } catch {
      message.error('加载执行轨迹失败')
    } finally { setLoading(false) }
  }, [canvasId, onTraceLoaded])

  // 关闭/切换执行记录时重置轨迹高亮
  const clearTrace = () => {
    setSelectedExecId(null)
    setTraces([])
    onTraceLoaded({})
  }

  const columns = [
    { title: '节点名称', dataIndex: 'nodeName', width: 140, ellipsis: true },
    { title: '类型', dataIndex: 'nodeType', width: 130, ellipsis: true },
    {
      title: '状态', dataIndex: 'status', width: 72,
      render: (s: TraceStatus) => {
        const [color, label] = STATUS_LABEL[s]
        return <Tag color={color}>{label}</Tag>
      },
    },
    {
      title: '耗时', dataIndex: 'durationMs', width: 72,
      render: (v?: number) => v != null ? `${v}ms` : '-',
    },
    {
      title: '错误', dataIndex: 'errorMsg', width: 120, ellipsis: true,
      render: (v?: string) => v ? <Text type="danger" style={{ fontSize: 11 }}>{v}</Text> : '-',
    },
    {
      title: '输出', dataIndex: 'outputData', ellipsis: true,
      render: (v?: string) => {
        if (!v) return '-'
        try {
          // 优先按 JSON 美化展示，方便排查节点输出字段
          const parsed = JSON.parse(v)
          return (
            <Collapse size="small" ghost items={[{
              key: '1',
              label: <Text style={{ fontSize: 11 }}>查看输出</Text>,
              children: (
                <pre style={{ fontSize: 11, maxHeight: 200, overflow: 'auto', margin: 0, background: '#f6f8fa', padding: 8, borderRadius: 4 }}>
                  {JSON.stringify(parsed, null, 2)}
                </pre>
              ),
            }]} />
          )
        } catch {
          // 非 JSON 时兜底显示原始字符串截断
          return <Text style={{ fontSize: 11 }}>{v.slice(0, 80)}</Text>
        }
      },
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
        width={820}
      >
        <Select
            showSearch
            optionFilterProp="label"
            style={{ width: 480 }}
            placeholder="选择执行记录"
            loading={loading}
            value={selectedExecId}
            onChange={loadTrace}
            labelRender={(props) => {
              const exec = executions.find(e => e.id === props.value);
              if (!exec) return props.value;
              const typeText = exec.triggerType === 'EVENT' ? '事件触发' : '试运行';
              return `[${typeText}] ${exec.id?.slice(0, 8)}`;
            }}
            options={executions.map(e => {
              const typeText = e.triggerType === 'EVENT' ? '事件触发' : '测试运行';
              // 格式化时间
              let formattedTime = e.createdAt;
              try {
                const date = new Date(e.createdAt);
                if (!isNaN(date.getTime())) {
                  formattedTime = date.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                    hour12: false,
                  }).replace(/\//g, '-');
                }
              } catch { /* 保持原样 */ }

              return {
                label: (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, whiteSpace: 'nowrap' }}>
                      <Tag color={e.triggerType === 'EVENT' ? 'blue' : 'orange'} style={{ margin: 0, minWidth: 64, textAlign: 'center' }}>
                        {typeText}
                      </Tag>
                      <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{e.id?.slice(0, 8)}</span>
                      <span style={{ fontSize: 11, color: '#999' }}>📅 {formattedTime}</span>
                    </div>
                ),
                value: e.id,
              };
            })}
        />

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
          <Text type="secondary" style={{ fontSize: 12, paddingLeft: 20 }}>
            <br />
            选择一次执行记录后，画布节点将按执行状态着色（绿=成功 / 红=失败 / 灰=跳过 / 黄=执行中）
          </Text>
        )}
      </Modal>
    </>
  )
}
