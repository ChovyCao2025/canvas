/**
 * Hook 职责：系统选项 Hook，将后端字典项转换为 Ant Design Select 可直接消费的 options。
 *
 * 维护说明：包含单分类和多分类两种加载方式，并保留当前值以兼容已下线/未返回的历史选项。
 */
import { useEffect, useMemo, useState } from 'react'
import { systemOptionsApi } from '../services/systemOptions'
import type { SelectOption, StubOption, SystemOption } from '../types'

/** 将后端不同字典 DTO 统一转换为 antd Select 的 { value, label } 结构。 */
export function toSelectOptions(options: Array<SystemOption | StubOption | SelectOption>) {
  return options
    .map((option: any) => ({
      value: String(option.optionKey ?? option.key ?? option.value ?? ''),
      label: String(option.label ?? option.optionKey ?? option.key ?? option.value ?? ''),
    }))
    .filter(option => option.value !== '')
}

/**
 * 把当前已保存值补回下拉选项。
 *
 * 典型场景：历史配置引用的字典项后来被禁用，后端 meta 接口不再返回它；
 * 如果不补回，编辑页会显示空值，用户无法判断当前真实配置。
 */
export function mergeCurrentValueOption(options: SelectOption[], currentValue: unknown): SelectOption[] {
  if (currentValue === undefined || currentValue === null || currentValue === '') return options
  const value = String(currentValue)
  if (options.some(option => String(option.value) === value)) return options
  return [{ value, label: `已禁用：${value}` }, ...options]
}

/** 加载单个系统字典分类，并返回原始数据、Select options、加载态和错误。 */
export function useSystemOptions(category: string | undefined, currentValue?: unknown) {
  const [raw, setRaw] = useState<StubOption[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!category) {
      setRaw([])
      return
    }
    let mounted = true
    setLoading(true)
    setError(null)
    // mounted 标记避免异步返回晚于组件卸载时继续 setState。
    systemOptionsApi.meta(category)
      .then(res => {
        if (mounted) setRaw(res.data)
      })
      .catch(err => {
        if (mounted) setError(err instanceof Error ? err : new Error(String(err)))
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => { mounted = false }
  }, [category])

  /** 最终下拉选项，补齐当前值后再交给调用方渲染。 */
  const options = useMemo(
    () => mergeCurrentValueOption(toSelectOptions(raw), currentValue),
    [raw, currentValue],
  )

  return { raw, options, loading, error }
}

/**
 * 批量加载多个系统字典分类。
 *
 * 先去重并排序，保证调用方传入数组引用变化时，只要实际分类集合不变就不会重复请求。
 */
export function useSystemOptionsBatch(categories: string[]) {
  /** 去重后的稳定分类列表，避免调用方每次传新数组都触发请求。 */
  const stableCategories = useMemo(
    () => Array.from(new Set(categories.filter(Boolean))).sort(),
    [categories.join('|')],
  )
  const [raw, setRaw] = useState<Record<string, StubOption[]>>({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (stableCategories.length === 0) {
      setRaw({})
      return
    }
    let mounted = true
    setLoading(true)
    setError(null)
    systemOptionsApi.metaBatch(stableCategories)
      .then(res => {
        if (mounted) setRaw(res.data)
      })
      .catch(err => {
        if (mounted) setError(err instanceof Error ? err : new Error(String(err)))
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => { mounted = false }
  }, [stableCategories.join('|')])

  /** 按分类聚合后的 Select options，调用方可直接按 category 读取。 */
  const options = useMemo(() => {
    // 保持返回结构与 raw 一一对应，调用方可用 category 直接索引对应下拉项。
    return Object.fromEntries(
      Object.entries(raw).map(([category, rows]) => [category, toSelectOptions(rows)]),
    ) as Record<string, SelectOption[]>
  }, [raw])

  return { raw, options, loading, error }
}
