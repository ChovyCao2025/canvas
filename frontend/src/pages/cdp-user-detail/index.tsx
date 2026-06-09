/**
 * 页面职责：CDP 用户详情页，展示用户基础属性、标签、标签历史和参与画布。
 *
 * 维护说明：页面也支持手动给用户增删标签，用于运营侧临时修正。
 */
import { useEffect, useState } from 'react'
import { Button, Card, Drawer, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd'
import { ArrowLeftOutlined, BarChartOutlined, PlusOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { cdpApi, type CdpUserCanvasSummary, type CdpUserDetail, type CdpUserTag, type CdpUserTagHistory } from '../../services/cdpApi'
import { contactabilityApi, type ContactabilityReport } from '../../services/contactabilityApi'
import { buildTagWritePayload, formatDateTime, formatExecutionStatus, tagColor } from '../cdp-users/cdpPresentation'
import { contactabilityCheckView, contactabilityStatusView } from './contactabilityPresentation'

/** 详情页标题和辅助文本组件别名。 */
const { Title, Text } = Typography

/** CDP 用户详情主页面，按 userId 聚合画像、标签和画布参与记录。 */
export default function CdpUserDetailPage() {
  const { userId = '' } = useParams()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<CdpUserDetail | null>(null)
  const [tags, setTags] = useState<CdpUserTag[]>([])
  const [history, setHistory] = useState<CdpUserTagHistory[]>([])
  const [canvasRows, setCanvasRows] = useState<CdpUserCanvasSummary[]>([])
  const [selectedCanvas, setSelectedCanvas] = useState<CdpUserCanvasSummary | null>(null)
  const [canvasExecutions, setCanvasExecutions] = useState<any[]>([])
  const [contactabilityChannel, setContactabilityChannel] = useState('SMS')
  const [contactability, setContactability] = useState<ContactabilityReport | null>(null)
  const [contactabilityLoading, setContactabilityLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  /** 并行加载用户洞察和标签历史；两者来自不同接口但同属详情页首屏数据。 */
  const load = async () => {
    const [insight, h] = await Promise.all([
      cdpApi.getUserInsight(userId),
      cdpApi.listUserTagHistory(userId),
    ])
    setDetail(insight.data.profile)
    setTags(insight.data.tags ?? [])
    setCanvasRows(insight.data.canvasRows ?? [])
    setHistory(h.data ?? [])
  }

  /** 加载当前用户在指定渠道的触达策略解释，不消耗频控额度。 */
  const loadContactability = async (channel = contactabilityChannel) => {
    if (!userId) return
    setContactabilityLoading(true)
    try {
      const res = await contactabilityApi.explain({ userId, channel })
      setContactability(res.data)
    } finally {
      setContactabilityLoading(false)
    }
  }

  useEffect(() => {
    if (!userId) return
    load()
    loadContactability(contactabilityChannel)
  }, [userId])

  /** 手动写入单个标签，成功后刷新画像和历史。 */
  const saveTag = async () => {
    const values = await form.validateFields()
    await cdpApi.addUserTag(userId, buildTagWritePayload(values))
    message.success('标签已写入')
    setModalOpen(false)
    form.resetFields()
    load()
  }

  /** 移除当前用户的指定标签。 */
  const removeTag = async (tagCode: string) => {
    await cdpApi.removeUserTag(userId, tagCode)
    message.success('标签已移除')
    load()
  }

  /** 打开某个画布下该用户的执行明细抽屉。 */
  const openCanvasExecutions = async (row: CdpUserCanvasSummary) => {
    setSelectedCanvas(row)
    const res = await cdpApi.listCanvasUserExecutions(row.canvasId, userId)
    setCanvasExecutions(res.data ?? [])
  }

  /** 切换预检渠道并立即刷新解释结果。 */
  const changeContactabilityChannel = (channel: string) => {
    setContactabilityChannel(channel)
    loadContactability(channel)
  }

  const contactabilityStatus = contactability ? contactabilityStatusView(contactability) : null
  const openAnalyticsTimeline = () => {
    const endDate = new Date().toISOString().slice(0, 10)
    const start = new Date()
    start.setDate(start.getDate() - 6)
    const startDate = start.toISOString().slice(0, 10)
    navigate(`/analytics?userId=${encodeURIComponent(userId)}&startDate=${startDate}&endDate=${endDate}`)
  }

  return (
    <div>
      {/* 返回导航和页面标题。 */}
      <Space style={{ marginBottom: 16 }}>
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <Title level={4} style={{ margin: 0 }}>用户详情</Title>
      </Space>

      {/* 用户基础信息卡片，字段缺失时用 '-' 保持布局稳定。 */}
      <Card style={{ marginBottom: 16 }}>
        <Space direction="vertical" size={4}>
          <Text strong>{detail?.displayName || userId}</Text>
          <Text type="secondary">User ID: {userId}</Text>
          <Text type="secondary">最近活跃: {formatDateTime(detail?.lastSeenAt)}</Text>
          <Text type="secondary">手机号: {detail?.phone || '-'}</Text>
          <Text type="secondary">邮箱: {detail?.email || '-'}</Text>
          <Button size="small" icon={<BarChartOutlined />} onClick={openAnalyticsTimeline}>
            查看事件时间线
          </Button>
        </Space>
      </Card>

      <Card
        title="触达可达性"
        extra={(
          <Space>
            <Select
              size="small"
              value={contactabilityChannel}
              onChange={changeContactabilityChannel}
              style={{ width: 96 }}
              options={[
                { value: 'SMS', label: 'SMS' },
                { value: 'EMAIL', label: 'Email' },
                { value: 'PUSH', label: 'Push' },
                { value: 'WECHAT', label: '微信' },
              ]}
            />
            <Button size="small" loading={contactabilityLoading} onClick={() => loadContactability()}>
              刷新
            </Button>
          </Space>
        )}
        style={{ marginBottom: 16 }}
      >
        {contactabilityStatus ? (
          <Space direction="vertical" size={8}>
            <Tag color={contactabilityStatus.color}>{contactabilityStatus.label}</Tag>
            <Space wrap>
              {contactability?.checks.map(check => {
                const view = contactabilityCheckView(check)
                return (
                  <Tag key={check.checkKey} color={view.color}>
                    {view.label}: {view.text}
                  </Tag>
                )
              })}
            </Space>
          </Space>
        ) : (
          <Text type="secondary">{contactabilityLoading ? '加载中' : '-'}</Text>
        )}
      </Card>

      {/* 当前标签支持点击删除，适合运营临时修正用户画像。 */}
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

      {/* 标签历史用于审计，记录来源、旧值、新值和操作时间。 */}
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

      {/* 触达画布列表用于从用户视角反查其参与过的营销旅程。 */}
      <Card title="触达画布" style={{ marginTop: 16 }}>
        <Table
          rowKey="canvasId"
          dataSource={canvasRows}
          pagination={false}
          size="small"
          columns={[
            {
              title: '画布',
              dataIndex: 'canvasName',
              render: (_, row) => (
                <Space>
                  <Button type="link" onClick={() => navigate(`/canvas/${row.canvasId}/users`)}>
                    {row.canvasName}
                  </Button>
                  <Button size="small" onClick={() => openCanvasExecutions(row)}>
                    执行明细
                  </Button>
                </Space>
              ),
            },
            { title: '执行次数', dataIndex: 'executionCount', width: 100, align: 'right' },
            {
              title: '最近状态',
              dataIndex: 'latestStatus',
              width: 100,
              render: value => {
                const status = formatExecutionStatus(value)
                return <Tag color={status.color}>{status.label}</Tag>
              },
            },
            { title: '最近进入', dataIndex: 'lastEnteredAt', width: 180, render: formatDateTime },
          ]}
        />
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

      <Drawer
        title={selectedCanvas ? `${selectedCanvas.canvasName} · 执行明细` : '执行明细'}
        open={!!selectedCanvas}
        width={720}
        onClose={() => setSelectedCanvas(null)}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {selectedCanvas && (
            <Card size="small">
              <Space split={<span>|</span>} wrap>
                <span>执行次数 {selectedCanvas.executionCount}</span>
                <span>成功 {selectedCanvas.successCount}</span>
                <span>失败 {selectedCanvas.failedCount}</span>
                <span>最近进入 {formatDateTime(selectedCanvas.lastEnteredAt)}</span>
              </Space>
            </Card>
          )}
          <Table
            rowKey="id"
            dataSource={canvasExecutions}
            pagination={false}
            size="small"
            columns={[
              { title: '执行 ID', dataIndex: 'id', ellipsis: true },
              {
                title: '状态',
                dataIndex: 'status',
                width: 120,
                render: value => {
                  const status = formatExecutionStatus(value === 2 ? 'SUCCESS' : value === 3 ? 'FAILED' : value === 1 ? 'PAUSED' : value === 0 ? 'RUNNING' : String(value))
                  return <Tag color={status.color}>{status.label}</Tag>
                },
              },
              { title: '触发类型', dataIndex: 'triggerType', width: 140 },
              { title: '创建时间', dataIndex: 'createdAt', width: 180, render: formatDateTime },
              { title: '更新时间', dataIndex: 'updatedAt', width: 180, render: formatDateTime },
            ]}
          />
        </Space>
      </Drawer>
    </div>
  )
}
