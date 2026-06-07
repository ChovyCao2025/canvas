import { useEffect, useMemo, useState } from 'react'
import { Button, Descriptions, Drawer, Form, Input, InputNumber, Modal, Select, Space, Statistic, Switch, Table, Tabs, Tag, Typography, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, StopOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  projectApi,
  type ProjectDetail,
  type ProjectMember,
  type ProjectStats,
  type ProjectSummary,
} from '../../services/api'
import type { Canvas } from '../../types'
import {
  projectRoleLabel,
  projectStatsCards,
  projectStatusColor,
  projectStatusLabel,
} from './projectPresentation'

const { Title } = Typography

interface ProjectFormValues {
  projectKey: string
  projectName: string
  description?: string
  requireReviewBeforePublish?: boolean
  defaultSettingsJson?: string
  quietHoursJson?: string
}

interface MemberFormValues {
  userId: number
  username: string
  role: string
}

export default function ProjectsPage() {
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selected, setSelected] = useState<ProjectDetail | null>(null)
  const [members, setMembers] = useState<ProjectMember[]>([])
  const [canvases, setCanvases] = useState<Canvas[]>([])
  const [stats, setStats] = useState<ProjectStats | null>(null)
  const [form] = Form.useForm<ProjectFormValues>()
  const [editForm] = Form.useForm<ProjectFormValues>()
  const [memberForm] = Form.useForm<MemberFormValues>()

  const fetchProjects = async () => {
    setLoading(true)
    try {
      const res = await projectApi.list()
      setProjects(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProjects() }, [])

  const openDetail = async (project: ProjectSummary) => {
    const [detailRes, membersRes, canvasesRes, statsRes] = await Promise.all([
      projectApi.detail(project.id),
      projectApi.members(project.id),
      projectApi.canvases(project.id, { page: 1, size: 20 }),
      projectApi.stats(project.id),
    ])
    setSelected(detailRes.data)
    setMembers(membersRes.data)
    setCanvases(canvasesRes.data.list)
    setStats(statsRes.data)
    setDetailOpen(true)
  }

  const handleCreate = async () => {
    const values = await form.validateFields()
    await projectApi.create({
      projectKey: values.projectKey,
      projectName: values.projectName,
      description: values.description,
      requireReviewBeforePublish: values.requireReviewBeforePublish ? 1 : 0,
    })
    message.success('项目已创建')
    setCreateOpen(false)
    form.resetFields()
    fetchProjects()
  }

  const handleDisable = async (project: ProjectSummary) => {
    Modal.confirm({
      title: `停用项目 ${project.projectName}？`,
      okType: 'danger',
      onOk: async () => {
        await projectApi.disable(project.id)
        message.success('项目已停用')
        fetchProjects()
        if (selected?.id === project.id) setDetailOpen(false)
      },
    })
  }

  const openEdit = () => {
    if (!selected) return
    editForm.setFieldsValue({
      projectName: selected.projectName,
      description: selected.description || undefined,
      defaultSettingsJson: selected.defaultSettingsJson || undefined,
      requireReviewBeforePublish: Boolean(selected.requireReviewBeforePublish),
      quietHoursJson: selected.quietHoursJson || undefined,
    })
    setEditOpen(true)
  }

  const handleUpdate = async () => {
    if (!selected) return
    const values = await editForm.validateFields()
    const res = await projectApi.update(selected.id, {
      projectName: values.projectName,
      description: values.description,
      defaultSettingsJson: values.defaultSettingsJson,
      requireReviewBeforePublish: values.requireReviewBeforePublish ? 1 : 0,
      quietHoursJson: values.quietHoursJson,
    })
    message.success('项目已更新')
    setSelected(res.data)
    setEditOpen(false)
    fetchProjects()
  }

  const handleSetMember = async () => {
    if (!selected) return
    const values = await memberForm.validateFields()
    await projectApi.setMember(selected.id, values.userId, {
      username: values.username,
      role: values.role,
    })
    message.success('成员已保存')
    memberForm.resetFields()
    const res = await projectApi.members(selected.id)
    setMembers(res.data)
  }

  const handleRemoveMember = (member: ProjectMember) => {
    if (!selected || !member.userId) return
    Modal.confirm({
      title: `移除成员 ${member.username}？`,
      okType: 'danger',
      onOk: async () => {
        await projectApi.removeMember(selected.id, member.userId!)
        message.success('成员已移除')
        const res = await projectApi.members(selected.id)
        setMembers(res.data)
        fetchProjects()
      },
    })
  }

  const projectColumns: ColumnsType<ProjectSummary> = [
    { title: '项目', dataIndex: 'projectName', render: (_, row) => (
      <Button type="link" onClick={() => openDetail(row)} style={{ padding: 0 }}>
        {row.projectName}
      </Button>
    ) },
    { title: 'Key', dataIndex: 'projectKey', width: 160 },
    { title: '状态', dataIndex: 'status', width: 100, render: status => (
      <Tag color={projectStatusColor(status)}>{projectStatusLabel(status)}</Tag>
    ) },
    { title: '成员', dataIndex: 'memberCount', width: 90, render: value => value ?? 0 },
    { title: '画布', dataIndex: 'canvasCount', width: 90, render: value => value ?? 0 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    { title: '操作', width: 90, render: (_, row) => row.status === 'ACTIVE' ? (
      <Button danger size="small" icon={<StopOutlined />} onClick={() => handleDisable(row)} />
    ) : null },
  ]

  const memberColumns: ColumnsType<ProjectMember> = [
    { title: '用户 ID', dataIndex: 'userId', width: 100, render: value => value ?? '-' },
    { title: '用户名', dataIndex: 'username' },
    { title: '角色', dataIndex: 'role', render: role => <Tag>{projectRoleLabel(role)}</Tag> },
    { title: '来源', dataIndex: 'source', width: 100, render: value => value || 'MANUAL' },
    { title: '操作', width: 80, render: (_, row) => row.userId ? (
      <Button danger size="small" icon={<DeleteOutlined />} onClick={() => handleRemoveMember(row)} />
    ) : null },
  ]

  const canvasColumns: ColumnsType<Canvas> = [
    { title: 'ID', dataIndex: 'id', width: 90 },
    { title: '画布名称', dataIndex: 'name' },
    { title: '状态', dataIndex: 'status', width: 100 },
    { title: '文件夹', dataIndex: 'folderName', render: (_, row) => row.folderName || row.folderKey || '-' },
  ]

  const statsItems = useMemo(() => stats ? projectStatsCards(stats) : [], [stats])

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>项目管理</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchProjects}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>新建项目</Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        dataSource={projects}
        columns={projectColumns}
        loading={loading}
        pagination={{ pageSize: 20 }}
      />

      <Modal
        title="新建项目"
        open={createOpen}
        onOk={handleCreate}
        onCancel={() => { setCreateOpen(false); form.resetFields() }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="projectKey" label="项目 Key" rules={[{ required: true }]}>
            <Input placeholder="growth-team" />
          </Form.Item>
          <Form.Item name="projectName" label="项目名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="requireReviewBeforePublish" label="发布前审批" valuePropName="checked" initialValue={false}>
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selected ? selected.projectName : '项目详情'}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={760}
        extra={selected ? <Button icon={<EditOutlined />} onClick={openEdit}>编辑</Button> : null}
      >
        {selected ? (
          <Tabs
            items={[
              {
                key: 'canvases',
                label: '画布',
                children: <Table rowKey="id" dataSource={canvases} columns={canvasColumns} pagination={false} />,
              },
              {
                key: 'members',
                label: '成员',
                children: (
                  <Space direction="vertical" style={{ width: '100%' }} size={16}>
                    <Form form={memberForm} layout="inline">
                      <Form.Item name="userId" label="用户 ID" rules={[{ required: true }]}>
                        <InputNumber min={1} style={{ width: 120 }} />
                      </Form.Item>
                      <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
                        <Input />
                      </Form.Item>
                      <Form.Item name="role" label="角色" rules={[{ required: true }]} initialValue="VIEWER">
                        <Select
                          style={{ width: 140 }}
                          options={[
                            { value: 'PROJECT_ADMIN', label: projectRoleLabel('PROJECT_ADMIN') },
                            { value: 'EDITOR', label: projectRoleLabel('EDITOR') },
                            { value: 'EXECUTOR', label: projectRoleLabel('EXECUTOR') },
                            { value: 'VIEWER', label: projectRoleLabel('VIEWER') },
                          ]}
                        />
                      </Form.Item>
                      <Button type="primary" onClick={handleSetMember}>保存成员</Button>
                    </Form>
                    <Table rowKey="id" dataSource={members} columns={memberColumns} pagination={false} />
                  </Space>
                ),
              },
              {
                key: 'policies',
                label: '默认策略',
                children: (
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="状态">
                      <Tag color={projectStatusColor(selected.status)}>{projectStatusLabel(selected.status)}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="发布前审批">
                      {selected.requireReviewBeforePublish ? '开启' : '关闭'}
                    </Descriptions.Item>
                    <Descriptions.Item label="默认设置 JSON">
                      {selected.defaultSettingsJson || '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="静默期 JSON">
                      {selected.quietHoursJson || '-'}
                    </Descriptions.Item>
                  </Descriptions>
                ),
              },
              {
                key: 'stats',
                label: '统计',
                children: (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 16 }}>
                    {statsItems.map(item => (
                      <Statistic key={item.key} title={item.label} value={item.value} />
                    ))}
                  </div>
                ),
              },
            ]}
          />
        ) : null}
      </Drawer>

      <Modal
        title="编辑项目"
        open={editOpen}
        onOk={handleUpdate}
        onCancel={() => { setEditOpen(false); editForm.resetFields() }}
        okText="保存"
        cancelText="取消"
      >
        <Form form={editForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="projectName" label="项目名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="requireReviewBeforePublish" label="发布前审批" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="defaultSettingsJson" label="默认设置 JSON">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="quietHoursJson" label="静默期 JSON">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
