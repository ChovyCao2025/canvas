/**
 * 模块职责：应用路由入口，集中声明登录页、主布局页、画布编辑页和后台管理页的访问路径。
 *
 * 维护说明：本文件只负责编排 Provider、懒加载页面和权限路由，不承载具体业务状态。
 */
import { Suspense, lazy } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import { RequireAuth, RequireAdmin } from './auth/guards'

// 页面组件统一 lazy，首屏只加载登录态、路由和当前访问页面所需的代码块。
const AppLayout = lazy(() => import('./components/layout/AppLayout'))
/** 登录页懒加载组件，匿名访问时才下载对应页面代码。 */
const LoginPage = lazy(() => import('./pages/login'))
/** 首页懒加载组件，承载登录后的运营概览。 */
const HomePage = lazy(() => import('./pages/home'))
/** 画布列表页懒加载组件，展示可管理的营销旅程。 */
const CanvasListPage = lazy(() => import('./pages/canvas-list'))
/** 画布编辑器懒加载组件，体积较大所以独立成路由 chunk。 */
const CanvasEditorPage = lazy(() => import('./pages/canvas-editor'))
/** 画布统计页懒加载组件，包含图表依赖，按需加载。 */
const CanvasStatsPage = lazy(() => import('./pages/canvas-stats'))
/** 管理员用户页懒加载组件，仅管理员路由会访问。 */
const AdminUsersPage = lazy(() => import('./pages/admin'))
/** API 配置页懒加载组件，维护外部接口定义。 */
const ApiConfigPage = lazy(() => import('./pages/api-config'))
/** 数据源配置页懒加载组件，维护下拉和远程选项来源。 */
const DataSourceConfigPage = lazy(() => import('./pages/data-source-config'))
/** AB 实验配置页懒加载组件。 */
const AbExperimentPage = lazy(() => import('./pages/ab-experiment'))
/** 标签配置页懒加载组件。 */
const TagConfigPage = lazy(() => import('./pages/tag-config'))
/** 身份类型配置页懒加载组件。 */
const IdentityTypesPage = lazy(() => import('./pages/identity-types'))
/** 标签导入页懒加载组件。 */
const TagImportPage = lazy(() => import('./pages/tag-import'))
/** MQ 定义配置页懒加载组件。 */
const MqConfigPage = lazy(() => import('./pages/mq-config'))
/** 事件定义配置页懒加载组件。 */
const EventConfigPage = lazy(() => import('./pages/event-config'))
/** 人群列表页懒加载组件。 */
const AudienceListPage = lazy(() => import('./pages/audience-list'))
/** 人群编辑页懒加载组件。 */
const AudienceEditPage = lazy(() => import('./pages/audience-edit'))
/** API 文档页懒加载组件。 */
const ApiDocsPage = lazy(() => import('./pages/api-docs'))
/** 系统选项配置页懒加载组件。 */
const SystemOptionsPage = lazy(() => import('./pages/system-options'))
/** CDP 用户列表页懒加载组件。 */
const CdpUsersPage = lazy(() => import('./pages/cdp-users'))
/** CDP 用户详情页懒加载组件。 */
const CdpUserDetailPage = lazy(() => import('./pages/cdp-user-detail'))
/** 画布触达用户页懒加载组件。 */
const CanvasUsersPage = lazy(() => import('./pages/canvas-users'))

/** 全屏加载态，配合 Suspense 覆盖页面 chunk 下载时间。 */
function RouteFallback() {
  return <Spin fullscreen />
}

/** 应用根组件：Provider 注入在外，路由权限和布局分层在内。 */
export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BrowserRouter>
          <Suspense fallback={<RouteFallback />}>
            <Routes>
              {/* 登录页不包权限守卫，避免未登录访问时出现重定向循环。 */}
              <Route path="/login" element={<LoginPage />} />

              {/* 普通登录用户可访问首页、画布列表、CDP 用户和单画布详情页。 */}
              <Route element={<RequireAuth />}>
                <Route element={<AppLayout />}>
                  <Route path="/" element={<Navigate to="/home" replace />} />
                  <Route path="/home" element={<HomePage />} />
                  <Route path="/canvas" element={<CanvasListPage />} />
                  <Route path="/cdp/users" element={<CdpUsersPage />} />
                  <Route path="/cdp/users/:userId" element={<CdpUserDetailPage />} />
                </Route>

                <Route path="/canvas/:id/edit" element={<CanvasEditorPage />} />
                <Route path="/canvas/:id/stats" element={<CanvasStatsPage />} />
                <Route path="/canvas/:id/users" element={<CanvasUsersPage />} />
              </Route>

              {/* 管理配置类页面统一放在管理员守卫下，菜单展示也由 AppLayout 内部按角色收敛。 */}
              <Route element={<RequireAdmin />}>
                <Route element={<AppLayout />}>
                  <Route path="/admin/users" element={<AdminUsersPage />} />
                  <Route path="/api-config" element={<ApiConfigPage />} />
                  <Route path="/data-source-config" element={<DataSourceConfigPage />} />
                  <Route path="/ab-experiments" element={<AbExperimentPage />} />
                  <Route path="/tag-config" element={<TagConfigPage />} />
                  <Route path="/identity-types" element={<IdentityTypesPage />} />
                  <Route path="/tag-import" element={<TagImportPage />} />
                  <Route path="/audiences" element={<AudienceListPage />} />
                  <Route path="/audiences/new" element={<AudienceEditPage />} />
                  <Route path="/audiences/:id/edit" element={<AudienceEditPage />} />
                  <Route path="/mq-config" element={<MqConfigPage />} />
                  <Route path="/event-config" element={<EventConfigPage />} />
                  <Route path="/api-docs" element={<ApiDocsPage />} />
                  <Route path="/system-options" element={<SystemOptionsPage />} />
                </Route>
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
