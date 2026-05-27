/**
 * 页面职责：CDP 用户列表页，支持关键词搜索、用户画像入口和批量打标签操作。
 *
 * 维护说明：批量操作创建后端异步任务，前端通过操作列表展示进度。
 */
import { useEffect, useState } from 'react'
import { Button, Input, Space, Table, Tag, Typography, Modal, Form, message, Card, Drawer, Descriptions, List, Progress } from 'antd'
import { TagsOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { cdpApi, type CanvasUserRow, type CdpTagOperation } from '../../services/cdpApi'
import { buildBatchTagPayload, formatDateTime, formatExecutionStatus, tagColor } from './cdpPresentation'

/** 页面标题组件别名。 */
const { Title } = Typography

/** CDP 用户中心主页面，包含用户检索和批量打标任务跟踪。 */
export default function CdpUsersPage() {
  const navigate = useNavigate()
  // 用户列表与批量任务列表相互独立：搜索用户不会影响批量任务查询。
  const [keyword, setKeyword] = useState('')
  const [rows, setRows] = useState<CanvasUserRow[]>([])
  const [loading, setLoading] = useState(false)
  const [operations, setOperations] = useState<CdpTagOperation[]>([])
  const [batchOpen, setBatchOpen] = useState(false)
  const [watchOperationId, setWatchOperationId] = useState<number | null>(null)
  const [selectedOperation, setSelectedOperation] = useState<CdpTagOperation | null>(null)
  const [form] = Form.useForm()

  /** 加载用户列表；空关键字表示查询最近/默认用户集合。 */
  const load = async (searchKeyword = '') => {
    setLoading(true)
    try {
      const res = await cdpApi.listUsers(searchKeyword || undefined)
      setRows(res.data ?? [])
    } finally {
      setLoading(false)
    }
  }

  /** 加载最近批量打标任务，用于列表展示和详情抽屉同步。 */
  const loadOperations = async () => {
    const res = await cdpApi.listTagOperations(20)
    setOperations(res.data ?? [])
    return res.data ?? []
  }

  // 首次进入同时加载用户列表和最近任务。
  useEffect(() => {
    load()
    loadOperations()
  }, [])

  // 只要有运行中的任务或正在关注的任务，就按 2 秒轮询任务列表。
  useEffect(() => {
    const shouldPoll = watchOperationId != null || operations.some(item => item.status === 'RUNNING')
    if (!shouldPoll) return
    const timer = window.setInterval(() => {
      loadOperations().then(items => {
        if (watchOperationId != null) {
          const current = items.find(item => item.id === watchOperationId)
          if (current && current.status !== 'RUNNING') {
            setWatchOperationId(null)
          }
        }
        if (selectedOperation != null) {
          const current = items.find(item => item.id === selectedOperation.id)
          if (current) setSelectedOperation(current)
        }
      })
    }, 2000)
    return () => window.clearInterval(timer)
  }, [operations, selectedOperation, watchOperationId])

  /** 创建批量打标任务，并立即进入详情抽屉观察进度。 */
  const submitBatch = async () => {
    const values = await form.validateFields()
    const res = await cdpApi.createBatchTagOperation(buildBatchTagPayload(values))
    message.success(`批量打标任务已创建 #${res.data.id}`)
    setWatchOperationId(res.data.id)
    setSelectedOperation(res.data)
    setBatchOpen(false)
    form.resetFields()
    loadOperations()
  }

  /** 打开某个批量任务详情。 */
  const openOperation = async (id: number) => {
    const res = await cdpApi.getBatchTagOperation(id)
    setSelectedOperation(res.data)
  }

  /** 对当前任务中失败的用户创建一次重试任务。 */
  const retryFailedUsers = async () => {
    if (!selectedOperation) return
    const res = await cdpApi.retryFailedTagOperation(selectedOperation.id)
    message.success(`已创建重试任务 #${res.data.id}`)
    setWatchOperationId(res.data.id)
    setSelectedOperation(res.data)
    loadOperations()
  }

  /** 用户列表列定义，用户 ID 链接到用户画像详情页。 */
  const columns: ColumnsType<CanvasUserRow> = [
    { title: '用户 ID', dataIndex: 'userId', render: v => <Button type="link" onClick={() => navigate(`/cdp/users/${encodeURIComponent(v)}`)}>{v}</Button> },
    { title: '执行次数', dataIndex: 'executionCount', width: 100, align: 'right' },
    { title: '最近状态', dataIndex: 'latestStatus', width: 100, render: v => { const s = formatExecutionStatus(v); return <Tag color={s.color}>{s.label}</Tag> } },
    { title: '当前标签', dataIndex: 'tags', render: (tags?: CanvasUserRow['tags']) => tags?.map(tag => <Tag key={tag.tagCode} color={tagColor(tag.tagCode)}>{tag.tagName || tag.tagCode}</Tag>) },
    { title: '最近进入', dataIndex: 'lastEnteredAt', width: 180, render: formatDateTime },
  ]

  /** 批量打标任务列定义，状态使用 CDP 展示工具统一转换。 */
  const operationColumns: ColumnsType<CdpTagOperation> = [
    {
      title: '任务 ID',
      dataIndex: 'id',
      width: 90,
      render: value => <Button type="link" onClick={() => openOperation(value)}>{value}</Button>,
    },
    { title: '操作', dataIndex: 'operationType', width: 120 },
    { title: '标签', dataIndex: 'tagCode', width: 140 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: value => {
        const status = formatExecutionStatus(value === 'PARTIAL_FAILED' ? 'FAILED' : value)
        return <Tag color={status.color}>{value === 'PARTIAL_FAILED' ? '部分失败' : status.label}</Tag>
      },
    },
    { title: '成功/总数', width: 120, render: (_, row) => `${row.successCount}/${row.totalCount}` },
    { title: '失败', dataIndex: 'failCount', width: 80, align: 'right' },
    { title: '创建时间', dataIndex: 'createdAt', width: 180, render: formatDateTime },
    { title: '错误摘要', dataIndex: 'errorMsg', ellipsis: true },
  ]

  // 详情抽屉进度：成功 + 失败都算作已处理。
  const selectedProgress = selectedOperation && selectedOperation.totalCount > 0
    ? Math.round(((selectedOperation.successCount + selectedOperation.failCount) / selectedOperation.totalCount) * 100)
    : 0
  const errorLines = selectedOperation?.errorMsg
    ? selectedOperation.errorMsg.split(';').map(item => item.trim()).filter(Boolean)
    : []

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20, gap: 12, flexWrap: 'wrap' }}>
        <Title level={4} style={{ margin: 0 }}>CDP 用户中心</Title>
        <Space>
          <Input.Search
            placeholder="搜索 userId"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onSearch={value => load(value.trim())}
            style={{ width: 240 }}
          />
          <Button icon={<TagsOutlined />} onClick={() => setBatchOpen(true)}>批量打标</Button>
        </Space>
      </div>
      <Table rowKey="userId" columns={columns} dataSource={rows} loading={loading} />

      <Card
        title="批量任务"
        extra={<Button size="small" onClick={() => loadOperations()}>刷新</Button>}
        style={{ marginTop: 16 }}
      >
        <Table rowKey="id" columns={operationColumns} dataSource={operations} pagination={false} size="small" />
      </Card>

      <Modal title="批量打标" open={batchOpen} onOk={submitBatch} onCancel={() => setBatchOpen(false)} okText="提交" cancelText="取消">
        <Form form={form} layout="vertical" initialValues={{ operationType: 'BATCH_SET' }}>
          <Form.Item name="operationType" label="操作类型" rules={[{ required: true }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item name="tagCode" label="标签编码" rules={[{ required: true, message: '请输入标签编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="tagValue" label="标签值">
            <Input />
          </Form.Item>
          <Form.Item name="userIds" label="用户 ID 列表" rules={[{ required: true, message: '请输入用户 ID' }]}>
            <Input.TextArea rows={6} placeholder="一行一个，或逗号分隔" />
          </Form.Item>
          <Form.Item name="reason" label="原因">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selectedOperation ? `批量任务 #${selectedOperation.id}` : '批量任务详情'}
        open={!!selectedOperation}
        width={680}
        onClose={() => setSelectedOperation(null)}
        extra={selectedOperation ? (
          <Space>
            {selectedOperation.failCount > 0 && (
              <Button size="small" onClick={retryFailedUsers}>
                重试失败用户
              </Button>
            )}
            <Button size="small" onClick={() => openOperation(selectedOperation.id)}>刷新</Button>
          </Space>
        ) : null}
      >
        {selectedOperation && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Card size="small">
              <Descriptions column={2} size="small">
                <Descriptions.Item label="操作类型">{selectedOperation.operationType}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={formatExecutionStatus(selectedOperation.status === 'PARTIAL_FAILED' ? 'FAILED' : selectedOperation.status).color}>
                    {selectedOperation.status === 'PARTIAL_FAILED' ? '部分失败' : formatExecutionStatus(selectedOperation.status).label}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="标签编码">{selectedOperation.tagCode}</Descriptions.Item>
                <Descriptions.Item label="标签值">{selectedOperation.tagValue || '-'}</Descriptions.Item>
                <Descriptions.Item label="创建人">{selectedOperation.createdBy || '-'}</Descriptions.Item>
                <Descriptions.Item label="创建时间">{formatDateTime(selectedOperation.createdAt)}</Descriptions.Item>
              </Descriptions>
            </Card>

            <Card size="small" title="执行进度">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Progress percent={selectedProgress} status={selectedOperation.status === 'PARTIAL_FAILED' ? 'exception' : selectedOperation.status === 'SUCCESS' ? 'success' : 'active'} />
                <Descriptions column={3} size="small">
                  <Descriptions.Item label="总数">{selectedOperation.totalCount}</Descriptions.Item>
                  <Descriptions.Item label="成功">{selectedOperation.successCount}</Descriptions.Item>
                  <Descriptions.Item label="失败">{selectedOperation.failCount}</Descriptions.Item>
                </Descriptions>
              </Space>
            </Card>

            <Card size="small" title="失败明细">
              {errorLines.length === 0 ? (
                <Typography.Text type="secondary">暂无失败明细</Typography.Text>
              ) : (
                <List
                  size="small"
                  dataSource={errorLines}
                  renderItem={item => <List.Item>{item}</List.Item>}
                />
              )}
            </Card>
          </Space>
        )}
      </Drawer>
    </div>
  )
}
