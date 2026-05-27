/**
 * 页面职责：标签导入批次列表，展示最近导入任务的状态、成功数和失败数。
 *
 * 维护说明：用于快速定位导入失败批次并查看错误明细。
 */
import { useEffect, useState } from 'react'
import { Alert, Button, Space, Table, Tag, Typography, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { tagImportApi } from '../../services/api'
import type { BatchErrorLoadingState, BatchErrorState, TagImportBatch, TagImportError } from './tagImportTypes'

/** 批次列表中的文本组件别名。 */
const { Text } = Typography

/** 将批次状态转换为颜色明确的 Tag。 */
function renderBatchStatus(status: string) {
  const normalized = status.toUpperCase()
  if (normalized === 'SUCCESS') return <Tag color="green">SUCCESS</Tag>
  if (normalized === 'FAILED') return <Tag color="red">FAILED</Tag>
  if (normalized === 'PARTIAL_SUCCESS') return <Tag color="orange">PARTIAL_SUCCESS</Tag>
  return <Tag color="blue">{status}</Tag>
}

/** 标签导入批次列表组件，支持展开失败明细。 */
export default function TagImportBatchList() {
  const [data, setData] = useState<TagImportBatch[]>([])
  const [loading, setLoading] = useState(false)
  const [errorMap, setErrorMap] = useState<BatchErrorState>({})
  const [errorLoading, setErrorLoading] = useState<BatchErrorLoadingState>({})

  /** 加载最近导入批次。 */
  const fetchBatches = async () => {
    setLoading(true)
    try {
      const res = await tagImportApi.batches()
      setData(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchBatches()
  }, [])

  /** 按需加载某个批次的失败明细；已加载或加载中时直接复用。 */
  const loadErrors = async (batchId: number) => {
    if (errorMap[batchId] || errorLoading[batchId]) return
    setErrorLoading(current => ({ ...current, [batchId]: true }))
    try {
      const res = await tagImportApi.errors(batchId)
      setErrorMap(current => ({ ...current, [batchId]: res.data }))
    } catch {
      message.error('加载失败明细失败')
    } finally {
      setErrorLoading(current => ({ ...current, [batchId]: false }))
    }
  }

  /** 批次主表列。 */
  const columns: ColumnsType<TagImportBatch> = [
    { title: '批次 ID', dataIndex: 'id', width: 96 },
    { title: '来源', dataIndex: 'sourceType', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 160,
      render: (value: string) => renderBatchStatus(value),
    },
    { title: '总行数', dataIndex: 'totalRows', width: 90 },
    { title: '成功', dataIndex: 'successRows', width: 90 },
    { title: '失败', dataIndex: 'failedRows', width: 90 },
    { title: '创建时间', dataIndex: 'createdAt', width: 180 },
  ]

  /** 展开行中的失败明细列。 */
  const errorColumns: ColumnsType<TagImportError> = [
    { title: '行号', dataIndex: 'rowNo', width: 90 },
    { title: '错误码', dataIndex: 'errorCode', width: 140 },
    { title: '错误信息', dataIndex: 'errorMsg' },
    {
      title: '原始数据',
      dataIndex: 'rawPayload',
      ellipsis: true,
      render: (value?: string) => value ? <Text code ellipsis={{ tooltip: value }}>{value}</Text> : '-',
    },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ReloadOutlined />} onClick={() => void fetchBatches()} loading={loading}>
          刷新
        </Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={false}
        expandable={{
          onExpand: (expanded, record) => {
            if (expanded) {
              void loadErrors(record.id)
            }
          },
          expandedRowRender: (record) => {
            const errors = errorMap[record.id]
            const childLoading = !!errorLoading[record.id]

            if (childLoading) {
              return <Alert type="info" showIcon message="正在加载失败明细" />
            }

            if (!errors || errors.length === 0) {
              return <Alert type="success" showIcon message="该批次暂无失败明细" />
            }

            return (
              <Table
                rowKey="id"
                size="small"
                columns={errorColumns}
                dataSource={errors}
                pagination={false}
              />
            )
          },
          rowExpandable: (record) => record.failedRows > 0,
        }}
      />
    </div>
  )
}
