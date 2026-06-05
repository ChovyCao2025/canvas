import type { ReactNode } from 'react'
import ErrorBoundary from '../layout/ErrorBoundary'

export default function AppErrorBoundary({ children }: { children: ReactNode }) {
  return (
    <ErrorBoundary
      routeName="页面"
      title="页面加载失败"
      subTitle="当前页面渲染异常，重试后仍失败请返回首页。"
      retryLabel="重试"
      homeHref="/home"
      homeLabel="返回首页"
    >
      {children}
    </ErrorBoundary>
  )
}
