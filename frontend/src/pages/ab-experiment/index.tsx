import { useEffect, useState } from 'react'
import {
  Button, Table, Tag, Space, Modal, Form, Input, InputNumber, Drawer,
  Switch, message, Typography, Popconfirm,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PartitionOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { abExperimentApi } from '../../services/api'
import type { AbExperimentGroup } from '../../types'

const { Title } = Typography

/**
 * AB 实验定义模型。
 */
interface AbExperiment {
  /** 实验定义 ID。 */
  id: number

  /** 实验名称。 */
  name: string

  /** 实验业务键（运行时分桶依据）。 */
  experimentKey: string

  /** 实验描述。 */
  description?: string

  /** 启用状态：1 启用，0 禁用。 */
  enabled: number
}

/**
 * AB 实验管理页。
 */
export default function AbExperimentPage() {
  // 列表与弹窗状态
  const [data, setData] = useState<AbExperiment[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState<AbExperiment | null>(null)
  const [form] = Form.useForm()
  const [groupForm] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [groupDrawerOpen, setGroupDrawerOpen] = useState(false)
  const [selectedExperiment, setSelectedExperiment] = useState<AbExperiment | null>(null)
  const [groups, setGroups] = useState<AbExperimentGroup[]>([])
  const [groupsLoading, setGroupsLoading] = useState(false)
  const [editingGroup, setEditingGroup] = useState<AbExperimentGroup | null>(null)

  // 拉取实验定义分页列表
  const fetchList = async (p = page) => {
    setLoading(true)
    try {
      const res = await abExperimentApi.list({ page: p, size: 20 })
      setData(res.data.list)
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList(1) }, [])

  // 新建模式
  const openCreate = () => {
    setEditingRecord(null)
    form.resetFields()
    form.setFieldsValue({ enabled: true })
    setModalVisible(true)
  }

  // 编辑模式（表单回填）
  const openEdit = (record: AbExperiment) => {
    setEditingRecord(record)
    form.setFieldsValue({
      ...record,
      enabled: record.enabled === 1,
    })
    setModalVisible(true)
  }

  // 新建/编辑提交
  const handleOk = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      const body = { ...values, enabled: values.enabled ? 1 : 0 }
      if (editingRecord) {
        await abExperimentApi.update(editingRecord.id, body)
        message.success('更新成功')
      } else {
        await abExperimentApi.create(body)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchList(editingRecord ? page : 1)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: number) => {
    await abExperimentApi.delete(id)
    message.success('已删除')
    fetchList(page)
  }

  const loadGroups = async (experiment: AbExperiment) => {
    setGroupsLoading(true)
    try {
      const res = await abExperimentApi.groups(experiment.id, true)
      setGroups(res.data)
    } finally {
      setGroupsLoading(false)
    }
  }

  const openGroups = (record: AbExperiment) => {
    setSelectedExperiment(record)
    setGroupDrawerOpen(true)
    setEditingGroup(null)
    groupForm.resetFields()
    groupForm.setFieldsValue({ enabled: true, sortOrder: 0 })
    loadGroups(record)
  }

  const startEditGroup = (group: AbExperimentGroup) => {
    setEditingGroup(group)
    groupForm.setFieldsValue({
      groupKey: group.groupKey,
      label: group.label,
      sortOrder: group.sortOrder,
      enabled: group.enabled === 1,
    })
  }

  const resetGroupForm = () => {
    setEditingGroup(null)
    groupForm.resetFields()
    groupForm.setFieldsValue({ enabled: true, sortOrder: 0 })
  }

  const saveGroup = async () => {
    if (!selectedExperiment) return
    const values = await groupForm.validateFields()
    const body = {
      groupKey: values.groupKey,
      label: values.label,
      sortOrder: values.sortOrder,
      enabled: values.enabled ? 1 as const : 0 as const,
    }
    if (editingGroup) {
      await abExperimentApi.updateGroup(selectedExperiment.id, editingGroup.id, body)
      message.success('分组已更新')
    } else {
      await abExperimentApi.createGroup(selectedExperiment.id, body)
      message.success('分组已创建')
    }
    resetGroupForm()
    loadGroups(selectedExperiment)
  }

  const disableGroup = async (group: AbExperimentGroup) => {
    if (!selectedExperiment) return
    await abExperimentApi.deleteGroup(selectedExperiment.id, group.id)
    message.success('分组已禁用')
    loadGroups(selectedExperiment)
  }

  const columns: ColumnsType<AbExperiment> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '实验名称', dataIndex: 'name' },
    { title: 'experimentKey', dataIndex: 'experimentKey', ellipsis: true },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (v: number) => (
        <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      width: 160,
      render: (_, record) => (
        <Space size={4}>
          <Button
            size="small"
            icon={<PartitionOutlined />}
            onClick={() => openGroups(record)}
          >
            分组
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          />
          <Popconfirm
            title="确认删除？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            okType="danger"
            cancelText="取消"
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const groupColumns: ColumnsType<AbExperimentGroup> = [
    { title: 'Key', dataIndex: 'groupKey', width: 100 },
    { title: '显示名', dataIndex: 'label' },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: value => value === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>,
    },
    {
      title: '操作',
      width: 120,
      render: (_, record) => (
        <Space size={4}>
          <Button size="small" icon={<EditOutlined />} onClick={() => startEditGroup(record)} />
          <Popconfirm
            title="确认禁用该分组？"
            onConfirm={() => disableGroup(record)}
            okText="禁用"
            okType="danger"
            cancelText="取消"
          >
            <Button size="small" danger icon={<DeleteOutlined />} disabled={record.enabled !== 1} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>AB 实验管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建实验
        </Button>
      </div>

      <Table
        rowKey="id"
        dataSource={data}
        columns={columns}
        loading={loading}
        pagination={{
          total,
          pageSize: 20,
          current: page,
          onChange: (p) => { setPage(p); fetchList(p) },
        }}
      />

      <Modal
        title={editingRecord ? '编辑实验' : '新建实验'}
        open={modalVisible}
        onOk={handleOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        okText={editingRecord ? '保存' : '创建'}
        cancelText="取消"
        width={480}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="实验名称" rules={[{ required: true, message: '请输入实验名称' }]}>
            <Input placeholder="如：新用户发券实验" />
          </Form.Item>
          <Form.Item name="experimentKey" label="experimentKey" rules={[{ required: true, message: '请输入 experimentKey' }]}>
            <Input placeholder="如：exp_new_user_coupon" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={selectedExperiment ? `${selectedExperiment.name} 分组` : '分组管理'}
        open={groupDrawerOpen}
        onClose={() => setGroupDrawerOpen(false)}
        width={620}
      >
        <Form form={groupForm} layout="inline" style={{ marginBottom: 16 }} onFinish={saveGroup}>
          <Form.Item name="groupKey" rules={[{ required: true, message: '请输入 Key' }]}>
            <Input placeholder="Key" style={{ width: 96 }} disabled={!!editingGroup} />
          </Form.Item>
          <Form.Item name="label" rules={[{ required: true, message: '请输入显示名' }]}>
            <Input placeholder="显示名" style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="sortOrder">
            <InputNumber placeholder="排序" style={{ width: 92 }} />
          </Form.Item>
          <Form.Item name="enabled" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">{editingGroup ? '保存' : '新增'}</Button>
            {editingGroup && <Button onClick={resetGroupForm}>取消</Button>}
          </Space>
        </Form>
        <Table
          rowKey="id"
          dataSource={groups}
          columns={groupColumns}
          loading={groupsLoading}
          pagination={false}
        />
      </Drawer>
    </div>
  )
}
