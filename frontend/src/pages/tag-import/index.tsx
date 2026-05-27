/**
 * 页面职责：标签导入页入口，组合导入源、Excel 模板说明和批次列表。
 *
 * 维护说明：具体导入源和批次行为拆到子组件中维护。
 */
import { Tabs, Typography } from 'antd'
import TagImportApiDoc from './tagImportApiDoc'
import TagImportBatchList from './tagImportBatchList'
import TagImportExcelPanel from './tagImportExcelPanel'
import TagImportSourcePanel from './tagImportSourcePanel'

/** 页面标题组件别名。 */
const { Title } = Typography

/** 标签导入页面入口，组合来源、Excel 和批次三个子视图。 */
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
