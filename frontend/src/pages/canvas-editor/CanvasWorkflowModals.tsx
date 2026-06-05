import { Input, Modal, Slider, Space } from 'antd'

interface CanvasWorkflowModalsProps {
  testModalOpen: boolean
  testRunning: boolean
  testUserId: string
  testPayload: string
  onCancelTest: () => void
  onRunTest: () => Promise<void>
  onTestUserIdChange: (value: string) => void
  onTestPayloadChange: (value: string) => void
  canaryModalOpen: boolean
  canaryPercent: number
  onCancelCanary: () => void
  onStartCanary: () => Promise<void>
  onCanaryPercentChange: (value: number) => void
}

export default function CanvasWorkflowModals({
  testModalOpen,
  testRunning,
  testUserId,
  testPayload,
  onCancelTest,
  onRunTest,
  onTestUserIdChange,
  onTestPayloadChange,
  canaryModalOpen,
  canaryPercent,
  onCancelCanary,
  onStartCanary,
  onCanaryPercentChange,
}: CanvasWorkflowModalsProps) {
  return (
    <>
      <Modal
        title="测试运行"
        open={testModalOpen}
        confirmLoading={testRunning}
        okText="运行"
        cancelText="取消"
        onCancel={onCancelTest}
        onOk={() => { void onRunTest() }}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#666' }}>用户 ID</div>
            <Input value={testUserId} onChange={event => onTestUserIdChange(event.target.value)} />
          </div>
          <div>
            <div style={{ marginBottom: 4, fontSize: 12, color: '#666' }}>Payload（JSON）</div>
            <Input.TextArea
              rows={4}
              value={testPayload}
              onChange={event => onTestPayloadChange(event.target.value)}
              style={{ fontFamily: 'monospace' }}
            />
          </div>
        </Space>
      </Modal>

      <Modal
        title="灰度发布"
        open={canaryModalOpen}
        onOk={() => { void onStartCanary() }}
        onCancel={onCancelCanary}
        okText="确认灰度发布"
        width={460}
      >
        <div style={{ padding: '8px 0' }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>
            灰度比例：<span style={{ color: '#1890ff', fontSize: 18 }}>{canaryPercent}%</span>
          </div>
          <Slider
            min={1}
            max={99}
            value={canaryPercent}
            onChange={value => {
              if (typeof value === 'number') onCanaryPercentChange(value)
            }}
            marks={{ 1: '1%', 10: '10%', 30: '30%', 50: '50%', 99: '99%' }}
            style={{ marginBottom: 24 }}
          />
          <div style={{ color: '#8c8c8c', fontSize: 12, lineHeight: '20px' }}>
            <div>· {canaryPercent}% 的用户将收到新版本画布</div>
            <div>· 用户分配基于 hash，同一用户始终命中同一版本</div>
            <div>· 灰度期间可随时晋升全量或回滚</div>
          </div>
        </div>
      </Modal>
    </>
  )
}
