import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Button, Result } from 'antd'

type ResetKey = string | number | boolean | null | undefined

interface ErrorBoundaryProps {
  children: ReactNode
  routeName: string
  resetKey?: ResetKey
  title?: string
  subTitle?: string
  retryLabel?: string
  homeHref?: string
  homeLabel?: string
}

interface ErrorBoundaryState {
  failed: boolean
}

function fallbackTitle(routeName: string) {
  return `${routeName}加载失败`
}

function safeText(value: string) {
  return value
    .replace(/(token|password|secret|credential|apiKey)=?[^\s,;}]*/gi, '$1=****')
    .replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, '***@***')
    .replace(/1[3-9]\d{9}/g, '1**********')
}

export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { failed: false }

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ERROR_BOUNDARY]', {
      routeName: safeText(this.props.routeName),
      message: safeText(error.message),
      componentStack: safeText(info.componentStack ?? ''),
    })
  }

  componentDidUpdate(prevProps: ErrorBoundaryProps) {
    if (prevProps.resetKey !== this.props.resetKey && this.state.failed) {
      this.setState({ failed: false })
    }
  }

  private reset = () => {
    this.setState({ failed: false })
  }

  render() {
    if (!this.state.failed) return this.props.children

    const routeName = safeText(this.props.routeName)
    const title = this.props.title ?? fallbackTitle(routeName)
    const retryLabel = this.props.retryLabel ?? `重试${routeName}`
    const actions = [
      <Button key="retry" type="primary" aria-label={retryLabel} onClick={this.reset}>{retryLabel}</Button>,
    ]
    if (this.props.homeHref && this.props.homeLabel) {
      actions.push(<Button key="home" href={this.props.homeHref}>{this.props.homeLabel}</Button>)
    }

    return (
      <div role="alert">
        <Result
          status="error"
          title={<h1 style={{ margin: 0, fontSize: 'inherit', fontWeight: 'inherit' }}>{title}</h1>}
          subTitle={this.props.subTitle ?? '当前区域渲染异常，重试后仍失败请返回上一层页面。'}
          extra={actions}
        />
      </div>
    )
  }
}
