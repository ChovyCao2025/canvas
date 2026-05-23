import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import 'antd/dist/reset.css'

/**
 * 前端应用启动入口。
 * `ConfigProvider(locale=zhCN)` 统一 Ant Design 的中文本地化文案。
 * `React.StrictMode` 仅在开发环境做额外检查，不影响生产行为。
 * 全局样式 reset 在这里一次性注入。
 *
 * 维护约定：
 * - 只放“全局初始化”逻辑（主题、多语言、全局 provider）；
 * - 页面级或业务级状态不要放在该文件，避免入口臃肿。
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
  // 启动顺序：StrictMode -> ConfigProvider -> App（由外到内逐层注入能力）。
  // 开发阶段双渲染检查仅用于发现副作用，不会进入生产构建。
  // StrictMode 会在开发态帮助发现副作用问题
  <React.StrictMode>
    {/* ConfigProvider 放在最外层，统一所有 antd 子组件语言与格式 */}
    {/* 例如日期组件、分页、空态文案等都会使用中文包。 */}
    <ConfigProvider locale={zhCN}>
      {/* App 内部再按路由和权限拆分实际页面 */}
      {/* 这样入口文件始终保持“全局初始化”职责单一 */}
      {/* 若后续接入主题切换，也建议在这里加最外层 provider。 */}
      <App />
    </ConfigProvider>
  </React.StrictMode>,
)
