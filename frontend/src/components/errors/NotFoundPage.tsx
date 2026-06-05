import { Button, Result } from 'antd'

export default function NotFoundPage() {
  return (
    <Result
      status="404"
      title={<h1 style={{ margin: 0, fontSize: 'inherit', fontWeight: 'inherit' }}>页面不存在</h1>}
      subTitle="请确认地址是否正确，或返回旅程管理继续操作。"
      extra={<Button type="primary" href="/canvas">返回旅程管理</Button>}
    />
  )
}
