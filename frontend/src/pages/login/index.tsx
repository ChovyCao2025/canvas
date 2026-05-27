/**
 * 页面职责：登录页，负责账号密码提交、认证态写入和登录后跳转。
 *
 * 维护说明：认证状态的持久化由 AuthContext 统一处理，页面只处理表单交互。
 */
import { useState } from 'react'
import { Form, Input, Button, Card, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../../services/api'
import { useAuth } from '../../context/AuthContext'

/** 页面标题组件别名，避免 JSX 中重复书写 Typography.Title。 */
const { Title } = Typography

/**
 * 登录页。
 * 成功后会把 token/user 写入 `AuthContext` 与 localStorage。
 *
 * 注意：
 * - 登录页不依赖 AppLayout，避免把侧边栏样式引入匿名页面；
 * - 登录失败提示优先展示后端 message，便于定位账号/密码问题。
 */
export default function LoginPage() {
  // loading 仅控制按钮态，避免重复提交登录请求
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login } = useAuth()

  // 登录成功后写入全局认证态并跳转首页
  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      // 调用后端登录接口，拿到 token + 用户信息
      const res = await authApi.login(values.username, values.password)
      // 写入 AuthContext（内部会同步到 localStorage）
      login(res.data)
      // replace=true：登录后回退不会返回登录页
      navigate('/', { replace: true })
    } catch (err: any) {
      // 优先使用后端返回文案，便于区分“账号不存在/密码错误/账号禁用”等场景
      message.error(err?.response?.data?.message ?? '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    // 居中卡片式登录布局，避免与主应用侧边栏样式耦合
    <div style={{
      minHeight: '100vh', display: 'flex',
      alignItems: 'center', justifyContent: 'center',
      background: '#f0f2f5',
    }}>
      <Card style={{ width: 380 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 32 }}>
          营销画布
        </Title>

        {/* Form 的校验与提交都在组件内闭环，便于后续替换为 SSO 登录 */}
        <Form layout="vertical" onFinish={onFinish} autoComplete="off">
          <Form.Item name="username" label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}>
            <Input size="large" placeholder="admin" />
          </Form.Item>
          <Form.Item name="password" label="密码"
            rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password size="large" placeholder="••••••••" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" size="large" block loading={loading}>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
