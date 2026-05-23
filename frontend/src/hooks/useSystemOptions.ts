import { useEffect, useMemo, useState } from 'react'
import { systemOptionsApi } from '../services/systemOptions'
import type { SelectOption, StubOption, SystemOption } from '../types'

export function toSelectOptions(options: Array<SystemOption | StubOption | SelectOption>) {
  return options
    .map((option: any) => ({
      value: String(option.optionKey ?? option.key ?? option.value ?? ''),
      label: String(option.label ?? option.optionKey ?? option.key ?? option.value ?? ''),
    }))
    .filter(option => option.value !== '')
}

export function mergeCurrentValueOption(options: SelectOption[], currentValue: unknown): SelectOption[] {
  if (currentValue === undefined || currentValue === null || currentValue === '') return options
  const value = String(currentValue)
  if (options.some(option => String(option.value) === value)) return options
  return [{ value, label: `已禁用：${value}` }, ...options]
}

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

  const options = useMemo(
    () => mergeCurrentValueOption(toSelectOptions(raw), currentValue),
    [raw, currentValue],
  )

  return { raw, options, loading, error }
}

export function useSystemOptionsBatch(categories: string[]) {
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

  const options = useMemo(() => {
    return Object.fromEntries(
      Object.entries(raw).map(([category, rows]) => [category, toSelectOptions(rows)]),
    ) as Record<string, SelectOption[]>
  }, [raw])

  return { raw, options, loading, error }
}
