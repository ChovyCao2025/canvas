import { Button, Drawer, Space, Spin, Tag } from 'antd'
import type { CanvasVersion } from '../../types'

interface CanvasVersionHistoryDrawerProps {
  open: boolean
  loading: boolean
  versions: CanvasVersion[]
  onClose: () => void
  onRevert: (versionId: number) => void
  onDiff: (versionId: number) => void
}

export default function CanvasVersionHistoryDrawer({
  open,
  loading,
  versions,
  onClose,
  onRevert,
  onDiff,
}: CanvasVersionHistoryDrawerProps) {
  return (
    <Drawer title="版本历史" placement="right" width={320} open={open} onClose={onClose}>
      <Spin spinning={loading}>
        {versions.map((version, index) => {
          const isCurrent = index === 0
          return (
            <div key={version.id} style={{
              padding: '12px 0', borderBottom: '1px solid #f0f0f0',
              borderLeft: isCurrent ? '3px solid #1677ff' : '3px solid #d9d9d9',
              paddingLeft: 12, marginBottom: 4,
              background: isCurrent ? '#f0f5ff' : 'transparent',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, color: isCurrent ? '#1677ff' : '#262626' }}>
                  V{version.version}{isCurrent ? '（当前草稿）' : ''}
                </span>
                <Tag color={version.status === 1 ? 'green' : version.status === 2 ? 'default' : 'blue'}>
                  {version.status === 1 ? '已发布' : version.status === 2 ? '已下线' : '草稿'}
                </Tag>
              </div>
              <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>
                {version.createdAt ? new Date(version.createdAt).toLocaleString('zh-CN') : ''} · {version.createdBy}
              </div>
              {!isCurrent && (
                <Space size={8} style={{ marginTop: 6 }}>
                  <Button
                    size="small"
                    type="link"
                    style={{ paddingLeft: 0 }}
                    onClick={() => onDiff(version.id)}
                  >
                    对比当前草稿
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    style={{ paddingLeft: 0 }}
                    onClick={() => onRevert(version.id)}
                  >
                    回退到此版本
                  </Button>
                </Space>
              )}
            </div>
          )
        })}
        {versions.length === 0 && !loading && (
          <div style={{ textAlign: 'center', color: '#8c8c8c', marginTop: 40 }}>暂无版本记录</div>
        )}
      </Spin>
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, padding: '12px 16px',
        borderTop: '1px solid #f0f0f0', background: '#fafafa', fontSize: 12, color: '#aaa',
      }}>
        ⓘ 回退将覆盖当前草稿，不影响已发布的线上版本
      </div>
    </Drawer>
  )
}
