import { Tabs, Typography } from 'antd'
import TagImportApiDoc from './tagImportApiDoc'
import TagImportBatchList from './tagImportBatchList'
import TagImportExcelPanel from './tagImportExcelPanel'
import TagImportSourcePanel from './tagImportSourcePanel'

const { Title } = Typography

export default function TagImportPage() {
  return (
    <div>
      <Title level={4} style={{ marginTop: 0 }}>标签导入</Title>
      <Tabs
        items={[
          { key: 'api-push', label: 'API 推送', children: <TagImportApiDoc /> },
          { key: 'excel', label: 'Excel 导入', children: <TagImportExcelPanel /> },
          { key: 'api-pull', label: 'API 拉取', children: <TagImportSourcePanel /> },
          { key: 'batches', label: '导入批次', children: <TagImportBatchList /> },
        ]}
      />
    </div>
  )
}
