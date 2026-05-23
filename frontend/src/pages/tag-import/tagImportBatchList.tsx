import { useEffect, useState } from 'react'
import { Alert, Button, Space, Table, Tag, Typography, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { tagImportApi } from '../../services/api'
import type { BatchErrorLoadingState, BatchErrorState, TagImportBatch, TagImportError } from './tagImportTypes'

const { Text } = Typography

function renderBatchStatus(status: string) {
  const normalized = status.toUpperCase()
  if (normalized === 'SUCCESS') return <Tag color="green">SUCCESS</Tag>
  if (normalized === 'FAILED') return <Tag color="red">FAILED</Tag>
  if (normalized === 'PARTIAL_SUCCESS') return <Tag color="orange">PARTIAL_SUCCESS</Tag>
  return <Tag color="blue">{status}</Tag>
}

export default function TagImportBatchList() {
  const [data, setData] = useState<TagImportBatch[]>([])
  const [loading, setLoading] = useState(false)
  const [errorMap, setErrorMap] = useState<BatchErrorState>({})
  const [errorLoading, setErrorLoading] = useState<BatchErrorLoadingState>({})

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
