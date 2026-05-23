import { useEffect, useState } from 'react'
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { cdpApi, type CdpUserDetail, type CdpUserTag, type CdpUserTagHistory } from '../../services/cdpApi'
import { buildTagWritePayload, formatDateTime, tagColor } from '../cdp-users/cdpPresentation'

const { Title, Text } = Typography

export default function CdpUserDetailPage() {
  const { userId = '' } = useParams()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<CdpUserDetail | null>(null)
  const [tags, setTags] = useState<CdpUserTag[]>([])
  const [history, setHistory] = useState<CdpUserTagHistory[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const load = async () => {
    const [u, t, h] = await Promise.all([
      cdpApi.getUser(userId),
      cdpApi.listUserTags(userId),
      cdpApi.listUserTagHistory(userId),
    ])
    setDetail(u.data)
    setTags(t.data ?? [])
    setHistory(h.data ?? [])
  }

  useEffect(() => { if (userId) load() }, [userId])

  const saveTag = async () => {
    const values = await form.validateFields()
    await cdpApi.addUserTag(userId, buildTagWritePayload(values))
    message.success('标签已写入')
    setModalOpen(false)
    form.resetFields()
    load()
  }

  const removeTag = async (tagCode: string) => {
    await cdpApi.removeUserTag(userId, tagCode)
    message.success('标签已移除')
    load()
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <Title level={4} style={{ margin: 0 }}>用户详情</Title>
      </Space>

      <Card style={{ marginBottom: 16 }}>
        <Space direction="vertical" size={4}>
          <Text strong>{detail?.displayName || userId}</Text>
          <Text type="secondary">User ID: {userId}</Text>
          <Text type="secondary">最近活跃: {formatDateTime(detail?.lastSeenAt)}</Text>
          <Text type="secondary">手机号: {detail?.phone || '-'}</Text>
          <Text type="secondary">邮箱: {detail?.email || '-'}</Text>
        </Space>
      </Card>

      <Card title="当前标签" extra={<Button size="small" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>打标签</Button>} style={{ marginBottom: 16 }}>
        <Space wrap>
          {tags.map(tag => (
            <Popconfirm key={tag.tagCode} title="移除该标签？" onConfirm={() => removeTag(tag.tagCode)}>
              <Tag color={tagColor(tag.tagCode)} style={{ cursor: 'pointer' }}>{tag.tagName || tag.tagCode}: {tag.tagValue || '-'}</Tag>
            </Popconfirm>
          ))}
          {tags.length === 0 && <Text type="secondary">暂无标签</Text>}
        </Space>
      </Card>

      <Card title="标签历史">
        <Table rowKey={(_, index) => String(index)} dataSource={history} pagination={false} size="small"
          columns={[
            { title: '标签', dataIndex: 'tagCode' },
            { title: '操作', dataIndex: 'operation' },
            { title: '旧值', dataIndex: 'oldValue' },
            { title: '新值', dataIndex: 'newValue' },
            { title: '来源', dataIndex: 'sourceType' },
            { title: '时间', dataIndex: 'operatedAt', render: formatDateTime },
          ]} />
      </Card>

      <Modal title="打标签" open={modalOpen} onOk={saveTag} onCancel={() => setModalOpen(false)} okText="保存" cancelText="取消">
        <Form form={form} layout="vertical">
          <Form.Item name="tagCode" label="标签编码" rules={[{ required: true, message: '请输入标签编码' }]}>
            <Input placeholder="high_value" />
          </Form.Item>
          <Form.Item name="tagValue" label="标签值">
            <Input placeholder="true / 100 / 字符串" />
          </Form.Item>
          <Form.Item name="reason" label="原因">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
