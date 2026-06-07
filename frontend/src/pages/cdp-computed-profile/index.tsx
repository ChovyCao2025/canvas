import { useEffect, useState } from 'react'
import {
  Button,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import {
  HistoryOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  cdpApi,
  type ComputedProfileAttributeRow,
  type ComputedProfileChangeLogRow,
  type ComputedProfilePreviewResult,
  type ComputedProfileRunRow,
} from '../../services/cdpApi'
import {
  buildComputedProfilePayload,
  formatPreviewSummary,
  formatRunStatus,
  formatValueChange,
  profileAttributeStatusColor,
  profileAttributeStatusText,
  type ComputedProfileFormValues,
} from './computedProfilePresentation'

const { Text, Title } = Typography

export default function CdpComputedProfilePage() {
  const [rows, setRows] = useState<ComputedProfileAttributeRow[]>([])
  const [runs, setRuns] = useState<ComputedProfileRunRow[]>([])
  const [changes, setChanges] = useState<ComputedProfileChangeLogRow[]>([])
  const [preview, setPreview] = useState<ComputedProfilePreviewResult | null>(null)
  const [selected, setSelected] = useState<ComputedProfileAttributeRow | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [auditOpen, setAuditOpen] = useState(false)
  const [form] = Form.useForm<ComputedProfileFormValues>()

  const loadRows = async () => {
    setLoading(true)
    try {
      const res = await cdpApi.computedProfiles.list()
      setRows(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRows()
  }, [])

  const openCreate = () => {
    form.setFieldsValue({
      attrCode: '',
      displayName: '',
      valueType: 'STRING',
      computeType: 'RULE',
      refreshMode: 'MANUAL',
      expressionText: JSON.stringify({ field: 'paidCount', op: '>=', value: 2, then: 'VIP' }, null, 2),
    })
    setPreview(null)
    setModalOpen(true)
  }

  const create = async () => {
    const values = await form.validateFields()
    setSaving(true)
    try {
      await cdpApi.computedProfiles.create(buildComputedProfilePayload(values))
      message.success('计算属性已创建')
      setModalOpen(false)
      await loadRows()
    } finally {
      setSaving(false)
    }
  }

  const previewRow = async (row: ComputedProfileAttributeRow) => {
    const res = await cdpApi.computedProfiles.preview(row.id)
    setSelected(row)
    setPreview(res.data)
    setAuditOpen(true)
  }

  const runRow = async (row: ComputedProfileAttributeRow) => {
    await cdpApi.computedProfiles.run(row.id)
    message.success('运行已提交')
    await loadAudit(row)
  }

  const activate = async (row: ComputedProfileAttributeRow) => {
    await cdpApi.computedProfiles.activate(row.id)
    message.success('已启用')
    await loadRows()
  }

  const pause = async (row: ComputedProfileAttributeRow) => {
    await cdpApi.computedProfiles.pause(row.id)
    message.success('已暂停')
    await loadRows()
  }

  const loadAudit = async (row: ComputedProfileAttributeRow) => {
    setSelected(row)
    setAuditOpen(true)
    const [runRes, changeRes] = await Promise.all([
      cdpApi.computedProfiles.runs(row.id),
      cdpApi.computedProfiles.changes(row.id),
    ])
    setRuns(runRes.data)
    setChanges(changeRes.data)
  }

  const columns: ColumnsType<ComputedProfileAttributeRow> = [
    { title: '属性编码', dataIndex: 'attrCode', width: 180, render: value => <Text code>{value}</Text> },
    { title: '名称', dataIndex: 'displayName', width: 160 },
    { title: '值类型', dataIndex: 'valueType', width: 90, render: value => <Tag>{value}</Tag> },
    { title: '计算类型', dataIndex: 'computeType', width: 90, render: value => <Tag>{value}</Tag> },
    { title: '刷新', dataIndex: 'refreshMode', width: 90, render: value => <Tag>{value}</Tag> },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: status => <Tag color={profileAttributeStatusColor(status)}>{profileAttributeStatusText(status)}</Tag>,
    },
    {
      title: '表达式',
      dataIndex: 'expressionJson',
      ellipsis: true,
      render: value => <Text code style={{ whiteSpace: 'normal', wordBreak: 'break-all' }}>{value}</Text>,
    },
    {
      title: '操作',
      width: 290,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small" wrap>
          <Button size="small" onClick={() => previewRow(row)}>预览</Button>
          {row.status === 'ACTIVE' ? (
            <Button size="small" icon={<PauseCircleOutlined />} onClick={() => pause(row)}>暂停</Button>
          ) : (
            <Button size="small" icon={<PlayCircleOutlined />} onClick={() => activate(row)}>启用</Button>
          )}
          <Button size="small" icon={<ThunderboltOutlined />} disabled={row.status !== 'ACTIVE'} onClick={() => runRow(row)}>运行</Button>
          <Button size="small" icon={<HistoryOutlined />} onClick={() => loadAudit(row)}>审计</Button>
        </Space>
      ),
    },
  ]

  const runColumns: ColumnsType<ComputedProfileRunRow> = [
    { title: 'Run ID', dataIndex: 'id', width: 90 },
    { title: '状态', dataIndex: 'status', width: 90, render: value => <Tag>{formatRunStatus(value)}</Tag> },
    { title: '扫描', dataIndex: 'scannedCount', width: 80 },
    { title: '命中', dataIndex: 'matchedCount', width: 80 },
    { title: '变更', dataIndex: 'changedCount', width: 80 },
    { title: '未变', dataIndex: 'unchangedCount', width: 80 },
    { title: '事件 ID', dataIndex: 'sourceEventId', width: 180, render: value => value || '-' },
    { title: '开始时间', dataIndex: 'startedAt', width: 170, render: value => value || '-' },
  ]

  const changeColumns: ColumnsType<ComputedProfileChangeLogRow> = [
    { title: '用户 ID', dataIndex: 'userId', width: 160 },
    { title: '变化', width: 260, render: (_, row) => formatValueChange(row.oldValue, row.newValue) },
    { title: 'Run ID', dataIndex: 'sourceRunId', width: 90 },
    { title: '变更时间', dataIndex: 'changedAt', width: 170, render: value => value || '-' },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>计算画像属性</Title>
          <Text type="secondary">创建、预览、运行和审计写回到用户画像 JSON 的计算属性。</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadRows} loading={loading}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建属性</Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={rows}
        columns={columns}
        loading={loading}
        pagination={{ pageSize: 10, hideOnSinglePage: true }}
        scroll={{ x: 1180 }}
      />

      <Modal
        title="新建计算属性"
        open={modalOpen}
        onOk={create}
        confirmLoading={saving}
        onCancel={() => setModalOpen(false)}
        width={760}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="attrCode" label="属性编码" rules={[{ required: true, message: '请输入属性编码' }]}>
            <Input placeholder="lifecycle_stage" />
          </Form.Item>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true, message: '请输入显示名称' }]}>
            <Input placeholder="生命周期阶段" />
          </Form.Item>
          <Space size={12} style={{ width: '100%' }} align="start">
            <Form.Item name="valueType" label="值类型" rules={[{ required: true }]}>
              <Select style={{ width: 140 }} options={[
                { value: 'STRING', label: 'STRING' },
                { value: 'NUMBER', label: 'NUMBER' },
                { value: 'BOOLEAN', label: 'BOOLEAN' },
                { value: 'JSON', label: 'JSON' },
              ]} />
            </Form.Item>
            <Form.Item name="computeType" label="计算类型" rules={[{ required: true }]}>
              <Select style={{ width: 140 }} options={[
                { value: 'RULE', label: 'RULE' },
                { value: 'EXPR', label: 'EXPR' },
              ]} />
            </Form.Item>
            <Form.Item name="refreshMode" label="刷新模式" rules={[{ required: true }]}>
              <Select style={{ width: 140 }} options={[
                { value: 'MANUAL', label: 'MANUAL' },
                { value: 'EVENT', label: 'EVENT' },
              ]} />
            </Form.Item>
          </Space>
          <Form.Item name="expressionText" label="表达式 JSON" rules={[{ required: true, message: '请输入表达式 JSON' }]}>
            <Input.TextArea autoSize={{ minRows: 8 }} style={{ fontFamily: 'monospace' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selected ? `${selected.displayName} 审计` : '审计'}
        open={auditOpen}
        onClose={() => setAuditOpen(false)}
        width={760}
      >
        {preview && (
          <div style={{ marginBottom: 18 }}>
            <Text strong>预览</Text>
            <div style={{ marginTop: 8 }}>{formatPreviewSummary(preview)}</div>
            <Table
              size="small"
              rowKey="userId"
              dataSource={preview.samples}
              pagination={false}
              style={{ marginTop: 8 }}
              columns={[
                { title: '用户 ID', dataIndex: 'userId' },
                { title: '变化', render: (_, row) => formatValueChange(row.oldValue, row.newValue) },
              ]}
            />
          </div>
        )}

        <Title level={5}>运行历史</Title>
        <Table rowKey="id" size="small" dataSource={runs} columns={runColumns} pagination={false} scroll={{ x: 850 }} />

        <Title level={5} style={{ marginTop: 24 }}>变更记录</Title>
        <Table rowKey="id" size="small" dataSource={changes} columns={changeColumns} pagination={false} />
      </Drawer>
    </div>
  )
}
