import { Button, Result } from 'antd'

export default function ForbiddenPage() {
  return (
    <Result
      status="403"
      title={<h1 style={{ margin: 0, fontSize: 'inherit', fontWeight: 'inherit' }}>无权限访问</h1>}
      subTitle="当前账号没有访问该页面的权限。"
      extra={<Button type="primary" href="/home">返回首页</Button>}
    />
  )
}
