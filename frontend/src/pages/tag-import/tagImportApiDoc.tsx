import { Card, Descriptions, Typography } from 'antd'

const { Paragraph, Text } = Typography

const SAMPLE_PAYLOAD = `{
  "idType": "mobile",
  "idValue": "13800138000",
  "tagCode": "user_level",
  "tagValue": "vip",
  "tagTime": "2026-05-23 10:00:00"
}`

export default function TagImportApiDoc() {
  return (
    <Card size="small" bordered={false} style={{ padding: 0 }}>
      <Paragraph style={{ marginBottom: 12 }}>
        通过服务端接口向标签中心推送单条或多条标签记录。请求体中的每条记录至少需要包含 ID 类型、ID 值、标签编码和标签值。
      </Paragraph>
      <Card size="small" title="示例 JSON" style={{ marginBottom: 16 }}>
        <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{SAMPLE_PAYLOAD}</pre>
      </Card>
      <Descriptions bordered column={1} size="small">
        <Descriptions.Item label="idType">
          <Text code>identity_types.code</Text>，如 <Text code>mobile</Text>、<Text code>openid</Text>
        </Descriptions.Item>
        <Descriptions.Item label="idValue">对应 ID 类型的具体值</Descriptions.Item>
        <Descriptions.Item label="tagCode">
          标签编码，对应标签中心中的 <Text code>tagCode</Text>
        </Descriptions.Item>
        <Descriptions.Item label="tagValue">标签值，支持字符串形式传入</Descriptions.Item>
        <Descriptions.Item label="tagTime">标签时间，可选，建议使用标准日期时间字符串</Descriptions.Item>
      </Descriptions>
    </Card>
  )
}
