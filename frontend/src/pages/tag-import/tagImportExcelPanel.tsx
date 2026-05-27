/**
 * 页面职责：标签导入 Excel 面板，说明离线文件导入字段和模板格式。
 *
 * 维护说明：当前组件只呈现说明和入口，真实上传流程可在后续扩展。
 */
import { useState } from 'react'
import { Alert, Button, Card, Space, Typography, Upload, message } from 'antd'
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { tagImportApi } from '../../services/api'
import type { TagImportResult } from './tagImportTypes'

/** Excel 面板常用文本组件别名。 */
const { Paragraph, Text } = Typography

/** 标签导入 Excel 模板说明面板。 */
export default function TagImportExcelPanel() {
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<TagImportResult>()

  /** 上传 Excel 文件并展示后端返回的导入批次统计。 */
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

  /** 拦截 Upload 默认提交流程，改为调用项目封装的导入接口。 */
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
