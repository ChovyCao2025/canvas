import { useEffect, useState } from 'react'
import { Button, Form, Input, Modal, Select, Table, Tag, Tooltip, message, Typography } from 'antd'
import { PlusOutlined, StopOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { adminApi } from '../../services/api'
import type { SysUser } from '../../services/api'

const { Title } = Typography

/**
 * 管理后台用户管理页。
 *
 * 页面职责：
 * 1) 展示用户列表；
 * 2) 提供新建用户入口；
 * 3) 提供禁用用户操作（软禁用，不删除记录）。
 */
export default function AdminUsersPage() {
  // 列表与弹窗状态
  const [users, setUsers] = useState<SysUser[]>([])
  const [loading, setLoading] = useState(false)
  const [createVisible, setCreateVisible] = useState(false)
  const [form] = Form.useForm()

  // 拉取全部用户：作为列表唯一数据源，创建/禁用后都回调它刷新
  const fetchUsers = async () => {
    setLoading(true)
    try {
      const res = await adminApi.listUsers()
      setUsers(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchUsers() }, [])

  // 创建用户：先做表单校验，再调用后端接口
  const handleCreate = async () => {
    const values = await form.validateFields()
    await adminApi.createUser(values)
    message.success('创建成功')
    setCreateVisible(false)
    form.resetFields()
    fetchUsers()
  }

  // 禁用用户（软禁用）
  const handleDisable = async (id: number) => {
    Modal.confirm({
      title: '确认禁用该用户？',
      okType: 'danger',
      onOk: async () => {
        await adminApi.disableUser(id)
        message.success('已禁用')
        fetchUsers()
      },
    })
  }

  // 表格列定义与后端 SysUser 字段一一对应
  // 便于读代码时从前端字段快速映射到后端模型。
  const columns: ColumnsType<SysUser> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户名', dataIndex: 'username' },
    { title: '显示名', dataIndex: 'displayName' },
    {
      title: '角色',
      dataIndex: 'role',
      render: (r) => <Tag color={r === 'ADMIN' ? 'blue' : 'default'}>{r}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      render: (v) => v === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
    },
    {
      title: '操作',
      render: (_, record) => record.enabled === 1 ? (
        <Tooltip title="禁用">
          <Button size="small" danger icon={<StopOutlined />}
            onClick={() => handleDisable(record.id)} />
        </Tooltip>
      ) : null,
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>用户管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>
          新建用户
        </Button>
      </div>

      <Table rowKey="id" dataSource={users} columns={columns} loading={loading} pagination={false} />

      <Modal title="新建用户" open={createVisible}
        onOk={handleCreate} onCancel={() => { setCreateVisible(false); form.resetFields() }}
        okText="创建" cancelText="取消">
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, min: 6 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="displayName" label="显示名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true }]} initialValue="OPERATOR">
            <Select options={[
              { label: 'ADMIN（管理员）', value: 'ADMIN' },
              { label: 'OPERATOR（运营）', value: 'OPERATOR' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
