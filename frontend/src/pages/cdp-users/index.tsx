import { useEffect, useState } from 'react'
import { Button, Input, Space, Table, Tag, Typography, Modal, Form, message } from 'antd'
import { TagsOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { cdpApi, type CanvasUserRow } from '../../services/cdpApi'
import { buildBatchTagPayload, formatDateTime, formatExecutionStatus, tagColor } from './cdpPresentation'

const { Title } = Typography

export default function CdpUsersPage() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [rows, setRows] = useState<CanvasUserRow[]>([])
  const [loading, setLoading] = useState(false)
  const [batchOpen, setBatchOpen] = useState(false)
  const [form] = Form.useForm()

  const load = async (searchKeyword = '') => {
    setLoading(true)
    try {
      const res = await cdpApi.listUsers(searchKeyword || undefined)
      setRows(res.data ?? [])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const submitBatch = async () => {
    const values = await form.validateFields()
    await cdpApi.createBatchTagOperation(buildBatchTagPayload(values))
    message.success('批量打标任务已创建')
    setBatchOpen(false)
    form.resetFields()
  }

  const columns: ColumnsType<CanvasUserRow> = [
    { title: '用户 ID', dataIndex: 'userId', render: v => <Button type="link" onClick={() => navigate(`/cdp/users/${encodeURIComponent(v)}`)}>{v}</Button> },
    { title: '执行次数', dataIndex: 'executionCount', width: 100, align: 'right' },
    { title: '最近状态', dataIndex: 'latestStatus', width: 100, render: v => { const s = formatExecutionStatus(v); return <Tag color={s.color}>{s.label}</Tag> } },
    { title: '当前标签', dataIndex: 'tags', render: (tags?: CanvasUserRow['tags']) => tags?.map(tag => <Tag key={tag.tagCode} color={tagColor(tag.tagCode)}>{tag.tagName || tag.tagCode}</Tag>) },
    { title: '最近进入', dataIndex: 'lastEnteredAt', width: 180, render: formatDateTime },
  ]

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
    </div>
  )
}
