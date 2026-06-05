/**
 * 页面职责：维护测试用户集，并对单个用户发起 dry-run、跳过副作用复跑或管理员真实复跑。
 */
import { useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import {
  EyeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  executionRerunApi,
  type JsonObject,
  type RerunAudit,
  type RerunMode,
  type RerunResult,
  type TestUser,
  type TestUserPreview,
  type TestUserSet,
} from '../../services/executionRerunApi'

const { Title, Text } = Typography

const MODE_LABELS: Record<RerunMode, string> = {
  DRY_RUN: 'Dry-run',
  SKIP_SIDE_EFFECTS: '跳过副作用',
  ADMIN_REPLAY: '管理员真实复跑',
}

const MODE_COLORS: Record<string, string> = {
  DRY_RUN: 'blue',
  SKIP_SIDE_EFFECTS: 'cyan',
  ADMIN_REPLAY: 'volcano',
}

function parseJsonObject(value: string | undefined, field: string): JsonObject {
  const text = value?.trim()
  if (!text) return {}
  try {
    const parsed = JSON.parse(text)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error(`${field} must be a JSON object`)
    }
    return parsed as JsonObject
  } catch (error) {
    throw new Error(`${field} JSON 格式不正确: ${(error as Error).message}`)
  }
}

function prettyJson(value: unknown): string {
  if (value === null || value === undefined || value === '') return '{}'
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }
  return JSON.stringify(value, null, 2)
}

function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export default function TestUsersPage() {
  const [sets, setSets] = useState<TestUserSet[]>([])
  const [selectedSetId, setSelectedSetId] = useState<number>()
  const [users, setUsers] = useState<TestUser[]>([])
  const [selectedUserId, setSelectedUserId] = useState<number>()
  const [preview, setPreview] = useState<TestUserPreview | null>(null)
  const [audits, setAudits] = useState<RerunAudit[]>([])
  const [rerunResult, setRerunResult] = useState<RerunResult | null>(null)
  const [setModalOpen, setSetModalOpen] = useState(false)
  const [userModalOpen, setUserModalOpen] = useState(false)
  const [previewOpen, setPreviewOpen] = useState(false)
  const [loadingSets, setLoadingSets] = useState(false)
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [loadingAudits, setLoadingAudits] = useState(false)
  const [submittingSet, setSubmittingSet] = useState(false)
  const [submittingUser, setSubmittingUser] = useState(false)
  const [submittingRerun, setSubmittingRerun] = useState(false)
  const [setForm] = Form.useForm()
  const [userForm] = Form.useForm()
  const [rerunForm] = Form.useForm()
  const [auditForm] = Form.useForm()

  const selectedUser = useMemo(
    () => users.find(user => user.id === selectedUserId),
    [users, selectedUserId],
  )

  const loadSets = async () => {
    setLoadingSets(true)
    try {
      const res = await executionRerunApi.listSets()
      const rows = res.data ?? []
      setSets(rows)
      setSelectedSetId(current => current ?? rows[0]?.id)
    } finally {
      setLoadingSets(false)
    }
  }

  const loadUsers = async (setId = selectedSetId) => {
    if (!setId) {
      setUsers([])
      return
    }
    setLoadingUsers(true)
    try {
      const res = await executionRerunApi.listUsers(setId)
      const rows = res.data ?? []
      setUsers(rows)
      setSelectedUserId(current => current ?? rows[0]?.id)
    } finally {
      setLoadingUsers(false)
    }
  }

  const loadAudits = async () => {
    const canvasId = auditForm.getFieldValue('auditCanvasId')
    setLoadingAudits(true)
    try {
      const res = await executionRerunApi.listAudits(canvasId ? Number(canvasId) : undefined)
      setAudits(res.data ?? [])
    } finally {
      setLoadingAudits(false)
    }
  }

  useEffect(() => { loadSets() }, [])
  useEffect(() => { loadUsers(selectedSetId) }, [selectedSetId])
  useEffect(() => {
    if (!selectedUser) return
    rerunForm.setFieldsValue({
      userId: selectedUser.userId,
      testUserId: selectedUser.id,
      inputParamsJson: prettyJson(selectedUser.inputParams),
    })
  }, [selectedUser, rerunForm])

  const createSet = async () => {
    const values = await setForm.validateFields()
    setSubmittingSet(true)
    try {
      const res = await executionRerunApi.createSet(values)
      message.success('测试用户集已创建')
      setSetModalOpen(false)
      setForm.resetFields()
      await loadSets()
      setSelectedSetId(res.data.id)
    } finally {
      setSubmittingSet(false)
    }
  }

  const createUser = async () => {
    if (!selectedSetId) return
    const values = await userForm.validateFields()
    setSubmittingUser(true)
    try {
      const payload = {
        userId: values.userId,
        displayName: values.displayName,
        profile: parseJsonObject(values.profileJson, 'profile'),
        inputParams: parseJsonObject(values.inputParamsJson, 'inputParams'),
      }
      const res = await executionRerunApi.createUser(selectedSetId, payload)
      message.success('测试用户已创建')
      setUserModalOpen(false)
      userForm.resetFields()
      await loadUsers(selectedSetId)
      setSelectedUserId(res.data.id)
    } catch (error) {
      message.error((error as Error).message)
    } finally {
      setSubmittingUser(false)
    }
  }

  const openPreview = async (user: TestUser) => {
    setSelectedUserId(user.id)
    const res = await executionRerunApi.previewUser(user.id)
    setPreview(res.data)
    setPreviewOpen(true)
  }

  const submitRerun = async () => {
    const values = await rerunForm.validateFields()
    setSubmittingRerun(true)
    try {
      const inputParams = parseJsonObject(values.inputParamsJson, 'inputParams')
      const res = await executionRerunApi.rerunCanvas(Number(values.canvasId), {
        userId: values.userId,
        testUserId: values.testUserId,
        originalExecutionId: values.originalExecutionId,
        mode: values.mode,
        reason: values.reason,
        inputParams,
        graphJson: values.graphJson,
      })
      setRerunResult(res.data)
      message.success('复跑请求已完成')
      auditForm.setFieldsValue({ auditCanvasId: Number(values.canvasId) })
      await loadAudits()
    } catch (error) {
      message.error((error as Error).message)
    } finally {
      setSubmittingRerun(false)
    }
  }

  const userColumns: ColumnsType<TestUser> = [
    {
      title: '用户',
      dataIndex: 'userId',
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => setSelectedUserId(row.id)}>
            {row.userId}
          </Button>
          {row.displayName ? <Text type="secondary">{row.displayName}</Text> : null}
        </Space>
      ),
    },
    {
      title: '参数',
      dataIndex: 'inputParams',
      ellipsis: true,
      render: value => <Text code>{prettyJson(value).slice(0, 80)}</Text>,
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 170, render: formatDateTime },
    {
      title: '操作',
      width: 120,
      render: (_, row) => (
        <Space size={4}>
          <Button size="small" icon={<EyeOutlined />} onClick={() => openPreview(row)}>
            预览
          </Button>
        </Space>
      ),
    },
  ]

  const auditColumns: ColumnsType<RerunAudit> = [
    { title: 'Audit ID', dataIndex: 'id', width: 90 },
    { title: '画布', dataIndex: 'canvasId', width: 90 },
    { title: '用户', dataIndex: 'userId', ellipsis: true },
    {
      title: '模式',
      dataIndex: 'mode',
      width: 130,
      render: value => <Tag color={MODE_COLORS[value] ?? 'default'}>{MODE_LABELS[value as RerunMode] ?? value}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: value => <Tag color={value === 'SUCCESS' ? 'green' : value === 'FAILED' ? 'red' : 'gold'}>{value}</Tag>,
    },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
    { title: '操作人', dataIndex: 'operator', width: 120 },
    { title: '更新时间', dataIndex: 'updatedAt', width: 170, render: formatDateTime },
  ]

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space>
          <TeamOutlined />
          <Title level={4} style={{ margin: 0 }}>测试用户与单用户复跑</Title>
        </Space>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => { loadSets(); loadAudits() }}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setSetModalOpen(true)}>
            新建用户集
          </Button>
        </Space>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={9}>
          <Card
            title="测试用户"
            extra={
              <Button
                size="small"
                icon={<PlusOutlined />}
                disabled={!selectedSetId}
                onClick={() => {
                  userForm.setFieldsValue({ profileJson: '{}', inputParamsJson: '{}' })
                  setUserModalOpen(true)
                }}
              >
                新建用户
              </Button>
            }
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              <Select
                style={{ width: '100%' }}
                loading={loadingSets}
                value={selectedSetId}
                placeholder="选择测试用户集"
                options={sets.map(set => ({ value: set.id, label: set.name }))}
                onChange={value => {
                  setSelectedSetId(value)
                  setSelectedUserId(undefined)
                }}
              />
              <Table
                rowKey="id"
                size="small"
                loading={loadingUsers}
                columns={userColumns}
                dataSource={users}
                pagination={{ pageSize: 8 }}
                rowSelection={{
                  type: 'radio',
                  selectedRowKeys: selectedUserId ? [selectedUserId] : [],
                  onChange: keys => setSelectedUserId(Number(keys[0])),
                }}
              />
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={15}>
          <Card title="发起复跑">
            <Form
              form={rerunForm}
              layout="vertical"
              initialValues={{
                mode: 'DRY_RUN',
                inputParamsJson: '{}',
              }}
            >
              <Row gutter={12}>
                <Col xs={24} md={8}>
                  <Form.Item name="canvasId" label="画布 ID" rules={[{ required: true, message: '请输入画布 ID' }]}>
                    <InputNumber min={1} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item name="userId" label="用户 ID" rules={[{ required: true, message: '请输入用户 ID' }]}>
                    <Input />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item name="testUserId" label="测试用户 ID">
                    <InputNumber min={1} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={12}>
                <Col xs={24} md={8}>
                  <Form.Item name="mode" label="复跑模式" rules={[{ required: true }]}>
                    <Select
                      options={[
                        { value: 'DRY_RUN', label: 'Dry-run' },
                        { value: 'SKIP_SIDE_EFFECTS', label: '跳过副作用' },
                        { value: 'ADMIN_REPLAY', label: '管理员真实复跑' },
                      ]}
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={16}>
                  <Form.Item name="originalExecutionId" label="原执行 ID">
                    <Input />
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item
                name="reason"
                label="复跑原因"
                rules={[
                  { required: true, message: '请输入复跑原因' },
                  { min: 10, message: '至少 10 个字符' },
                ]}
              >
                <Input />
              </Form.Item>
              <Form.Item name="inputParamsJson" label="输入参数 JSON">
                <Input.TextArea rows={6} spellCheck={false} />
              </Form.Item>
              <Form.Item name="graphJson" label="草稿 graphJson">
                <Input.TextArea rows={3} spellCheck={false} />
              </Form.Item>
              <Space>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  loading={submittingRerun}
                  onClick={submitRerun}
                >
                  执行复跑
                </Button>
                {rerunResult ? (
                  <Tag color={rerunResult.status === 'SUCCESS' ? 'green' : 'red'}>
                    Audit #{rerunResult.auditId} {rerunResult.status}
                  </Tag>
                ) : null}
              </Space>
            </Form>
          </Card>
        </Col>

        <Col span={24}>
          <Card
            title="复跑审计"
            extra={
              <Space>
                <Form form={auditForm} layout="inline">
                  <Form.Item name="auditCanvasId" style={{ marginBottom: 0 }}>
                    <InputNumber min={1} placeholder="画布 ID" />
                  </Form.Item>
                </Form>
                <Button icon={<ReloadOutlined />} loading={loadingAudits} onClick={loadAudits}>
                  查询
                </Button>
              </Space>
            }
          >
            <Table
              rowKey="id"
              columns={auditColumns}
              dataSource={audits}
              loading={loadingAudits}
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title="新建测试用户集"
        open={setModalOpen}
        onCancel={() => setSetModalOpen(false)}
        onOk={createSet}
        confirmLoading={submittingSet}
        destroyOnClose
      >
        <Form form={setForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="新建测试用户"
        open={userModalOpen}
        onCancel={() => setUserModalOpen(false)}
        onOk={createUser}
        confirmLoading={submittingUser}
        destroyOnClose
        width={720}
      >
        <Form form={userForm} layout="vertical">
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="userId" label="用户 ID" rules={[{ required: true, message: '请输入用户 ID' }]}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="displayName" label="显示名">
                <Input />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="profileJson" label="profile JSON">
            <Input.TextArea rows={5} spellCheck={false} />
          </Form.Item>
          <Form.Item name="inputParamsJson" label="inputParams JSON">
            <Input.TextArea rows={5} spellCheck={false} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={preview?.displayName || preview?.userId}
        open={previewOpen}
        width={720}
        onClose={() => setPreviewOpen(false)}
      >
        {preview ? (
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="测试用户 ID">{preview.id}</Descriptions.Item>
              <Descriptions.Item label="用户 ID">{preview.userId}</Descriptions.Item>
            </Descriptions>
            <Text strong>执行上下文</Text>
            <Input.TextArea value={prettyJson(preview.context)} rows={10} readOnly spellCheck={false} />
            <Text strong>输入参数</Text>
            <Input.TextArea value={prettyJson(preview.inputParams)} rows={6} readOnly spellCheck={false} />
          </Space>
        ) : null}
      </Drawer>
    </div>
  )
}
