import { HomeOutlined } from '@ant-design/icons'
import { Typography } from 'antd'

export default function HomePage() {
  return (
    <div style={{
      minHeight: '80vh',
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      gap: 12,
    }}>
      <HomeOutlined style={{ fontSize: 48, color: '#d0d5e0' }} />
      <Typography.Text type="secondary" style={{ fontSize: 16 }}>
        首页内容建设中
      </Typography.Text>
    </div>
  )
}
