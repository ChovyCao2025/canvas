import { useState, useEffect, useMemo } from 'react'
import { Select, Input } from 'antd'

type Freq = 'daily' | 'weekly' | 'monthly' | 'hourly'

const FREQ_OPTIONS = [
  { value: 'daily',   label: '每天' },
  { value: 'weekly',  label: '每周' },
  { value: 'monthly', label: '每月' },
  { value: 'hourly',  label: '每小时' },
]

const WEEKDAY_OPTIONS = [
  { value: 1, label: '周一' }, { value: 2, label: '周二' },
  { value: 3, label: '周三' }, { value: 4, label: '周四' },
  { value: 5, label: '周五' }, { value: 6, label: '周六' },
  { value: 0, label: '周日' },
]

const HOUR_OPTIONS = Array.from({ length: 24 }, (_, h) => ({
  value: h,
  label: `${String(h).padStart(2, '0')}:00`,
}))

const DAY_OPTIONS = Array.from({ length: 28 }, (_, i) => ({
  value: i + 1,
  label: `${i + 1} 日`,
}))

/** 从 cron 字符串解析出结构化状态（仅支持简单模式） */
function parseCron(cron: string): { freq: Freq; hour: number; weekday: number; dayOfMonth: number } | null {
  const parts = cron.trim().split(/\s+/)
  if (parts.length !== 5) return null
  const [min, h, dom, , dow] = parts
  if (min === '0' && h !== '*' && dom === '*' && dow === '*') {
    return { freq: 'daily', hour: parseInt(h), weekday: 1, dayOfMonth: 1 }
  }
  if (min === '0' && h !== '*' && dom === '*' && dow !== '*') {
    return { freq: 'weekly', hour: parseInt(h), weekday: parseInt(dow), dayOfMonth: 1 }
  }
  if (min === '0' && h !== '*' && dom !== '*') {
    return { freq: 'monthly', hour: parseInt(h), weekday: 1, dayOfMonth: parseInt(dom) }
  }
  if (min === '0' && h === '*') {
    return { freq: 'hourly', hour: 9, weekday: 1, dayOfMonth: 1 }
  }
  return null
}

interface Props {
  value?: string
  onChange?: (cron: string) => void
}

export default function CronBuilder({ value, onChange }: Props) {
  const parsed = value ? parseCron(value) : null

  const [freq,       setFreq]       = useState<Freq>(parsed?.freq       ?? 'daily')
  const [hour,       setHour]       = useState(parsed?.hour             ?? 9)
  const [weekday,    setWeekday]    = useState(parsed?.weekday          ?? 1)
  const [dayOfMonth, setDayOfMonth] = useState(parsed?.dayOfMonth       ?? 1)
  const [advanced,   setAdvanced]   = useState(false)
  const [manualCron, setManualCron] = useState(value ?? '')

  /** 根据 UI 状态生成 cron */
  const computed = useMemo(() => {
    switch (freq) {
      case 'hourly':  return `0 * * * *`
      case 'daily':   return `0 ${hour} * * *`
      case 'weekly':  return `0 ${hour} * * ${weekday}`
      case 'monthly': return `0 ${hour} ${dayOfMonth} * *`
    }
  }, [freq, hour, weekday, dayOfMonth])

  // UI 变化时同步 cron（非高级模式）
  useEffect(() => {
    if (!advanced) {
      onChange?.(computed)
    }
  }, [computed, advanced])

  const openAdvanced = () => {
    setManualCron(computed)
    setAdvanced(true)
  }
  const closeAdvanced = () => {
    const p = parseCron(manualCron)
    if (p) { setFreq(p.freq); setHour(p.hour); setWeekday(p.weekday); setDayOfMonth(p.dayOfMonth) }
    setAdvanced(false)
    onChange?.(manualCron)
  }

  const selectStyle = { minWidth: 84 }

  return (
    <div>
      {/* 自然语言句式 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap',
                    padding: '12px 16px', background: '#f8f9fb', borderRadius: 8 }}>
        <span style={{ color: '#8c8c8c', fontSize: 14 }}>用户</span>

        <Select
          value={freq}
          onChange={v => { setFreq(v); setAdvanced(false) }}
          options={FREQ_OPTIONS}
          style={selectStyle}
          size="small"
          variant="outlined"
          styles={{ popup: { root: { minWidth: 90 } } }}
        />

        {freq === 'weekly' && (
          <Select
            value={weekday}
            onChange={v => { setWeekday(v); setAdvanced(false) }}
            options={WEEKDAY_OPTIONS}
            style={selectStyle}
            size="small"
          />
        )}

        {freq === 'monthly' && (
          <Select
            value={dayOfMonth}
            onChange={v => { setDayOfMonth(v); setAdvanced(false) }}
            options={DAY_OPTIONS}
            style={{ minWidth: 72 }}
            size="small"
          />
        )}

        {freq !== 'hourly' && (
          <Select
            value={hour}
            onChange={v => { setHour(v); setAdvanced(false) }}
            options={HOUR_OPTIONS}
            style={{ minWidth: 80 }}
            size="small"
          />
        )}

        <span style={{ color: '#8c8c8c', fontSize: 14 }}>进入旅程</span>
      </div>

      {/* 高级模式入口 / 收起 */}
      <div style={{ marginTop: 8, textAlign: 'right' }}>
        {!advanced ? (
          <span
            onClick={openAdvanced}
            style={{ fontSize: 12, color: '#bbb', cursor: 'pointer',
                     userSelect: 'none', borderBottom: '1px dashed #ddd' }}
          >
            高级模式（Cron 表达式）
          </span>
        ) : (
          <span
            onClick={closeAdvanced}
            style={{ fontSize: 12, color: '#1677ff', cursor: 'pointer', userSelect: 'none' }}
          >
            ▲ 收起高级模式
          </span>
        )}
      </div>

      {/* 高级模式输入框（折叠） */}
      {advanced && (
        <div style={{ marginTop: 8 }}>
          <Input
            value={manualCron}
            onChange={e => { setManualCron(e.target.value); onChange?.(e.target.value) }}
            placeholder="分 时 日 月 周 — 如 0 9 * * 1-5"
            prefix={<span style={{ color: '#8c8c8c', fontSize: 12, marginRight: 4 }}>Cron</span>}
            style={{ fontFamily: 'monospace', fontSize: 13 }}
          />
          <div style={{ fontSize: 11, color: '#bbb', marginTop: 4 }}>
            自动生成：<code style={{ color: '#595959' }}>{computed}</code>
          </div>
        </div>
      )}
    </div>
  )
}
