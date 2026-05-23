import { HomeOutlined } from '@ant-design/icons'
import { Typography } from 'antd'

/**
 * 首页占位页。
 * 当前仅保留轻量提示，后续可替换为仪表盘入口。
 * 该页面不承载业务操作，主要承担路由落点作用。
 * 保持实现简单，避免与业务页共享复杂状态。
 *
 * 对前端初学者的阅读提示：
 * - 这是一个纯展示组件，不依赖接口和全局状态；
 * - 可以作为最小 React 页面模板参考（结构 + 样式 + 图标）。
 */
export default function HomePage() {
  // 这里不发请求、不依赖 context，确保首页始终可秒开。
  return (
    // 当前阶段作为占位容器，后续可平滑替换为看板组件
    // 保留页面骨架可避免路由切换出现“未实现页面”空白
    <div style={{
      minHeight: '80vh',
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      gap: 12,
    }}>
      {/* 图标 + 文案的最小信息结构，保证空态也有视觉锚点 */}
      {/* 后续若改成仪表盘，可直接替换该容器内部内容。 */}
      {/* HomeOutlined 来自 antd 图标库，和当前 UI 风格保持一致。 */}
      <HomeOutlined style={{ fontSize: 48, color: '#d0d5e0' }} />
      <Typography.Text type="secondary" style={{ fontSize: 16 }}>
        首页内容建设中
      </Typography.Text>
    </div>
  )
}
