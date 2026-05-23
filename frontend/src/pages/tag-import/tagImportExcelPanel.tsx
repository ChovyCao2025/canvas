import { useState } from 'react'
import { Alert, Button, Card, Space, Typography, Upload, message } from 'antd'
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { tagImportApi } from '../../services/api'
import type { TagImportResult } from './tagImportTypes'

const { Paragraph, Text } = Typography

export default function TagImportExcelPanel() {
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<TagImportResult>()

  const handleUpload = async (file: File) => {
    setUploading(true)
    try {
      const res = await tagImportApi.uploadExcel(file)
      setResult(res.data)
      message.success('Excel 导入完成')
    } catch {
      message.error('Excel 导入失败')
    } finally {
      setUploading(false)
    }
  }

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    void handleUpload(file as File)
    return false
  }

  return (
    <Card size="small">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Paragraph style={{ margin: 0 }}>
          先下载模板填写数据，再上传 Excel 文件。系统会生成导入批次，并展示成功与失败行数。
        </Paragraph>
        <Space wrap>
          <Button
            icon={<DownloadOutlined />}
            onClick={() => window.open(tagImportApi.excelTemplateUrl, '_blank', 'noopener,noreferrer')}
          >
            下载模板
          </Button>
          <Upload beforeUpload={beforeUpload} showUploadList={false} accept=".xlsx,.xls">
            <Button type="primary" icon={<UploadOutlined />} loading={uploading}>
              上传 Excel
            </Button>
          </Upload>
        </Space>
        <Text type="secondary">支持 `.xlsx` / `.xls`，上传后将直接调用后端导入接口。</Text>
        {result ? (
          <Alert
            type={result.failedRows > 0 ? 'warning' : 'success'}
            showIcon
            message={`批次 #${result.batchId} 已创建`}
            description={`状态：${result.status}；总行数 ${result.totalRows}，成功 ${result.successRows}，失败 ${result.failedRows}`}
          />
        ) : null}
      </Space>
    </Card>
  )
}
