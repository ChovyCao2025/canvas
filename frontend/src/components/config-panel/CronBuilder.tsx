/**
 * 组件职责：Cron 可视化编辑器，将常见频率选择转换为后端可保存的 cron 表达式。
 *
 * 维护说明：组件同时支持从已有 cron 回填 UI 状态，避免用户编辑时丢失当前配置。
 */
import { useEffect, useMemo, useState } from 'react'
import { Select } from 'antd'
import { ClockCircleOutlined } from '@ant-design/icons'

/** 可视化 Cron 编辑器支持的简单频率类型。 */
type Freq = 'daily' | 'weekly' | 'monthly' | 'hourly'

/**
 * 简单模式支持的频率枚举。
 */
const FREQ_OPTIONS = [
  { value: 'daily',   label: 'daily' },
  { value: 'weekly',  label: 'weekly' },
  { value: 'monthly', label: 'monthly' },
  { value: 'hourly',  label: 'hourly' },
]
/** Cron 星期选择项，0 表示周日，1-6 表示周一到周六。 */
const WEEKDAY_OPTIONS = [
  { value: 1, label: '1' }, { value: 2, label: '2' },
  { value: 3, label: '3' }, { value: 4, label: '4' },
  { value: 5, label: '5' }, { value: 6, label: '6' },
  { value: 0, label: '0' },
]
/** 小时下拉选项，覆盖 00-23。 */
const HOUR_OPTIONS   = Array.from({ length: 24 }, (_, h) => ({ value: h, label: String(h).padStart(2, '0') }))
/** 分钟下拉选项，覆盖 00-59。 */
const MINUTE_OPTIONS = Array.from({ length: 60 }, (_, m) => ({ value: m, label: String(m).padStart(2, '0') }))
/** 月内日期下拉选项，仅覆盖 1-28 以避开不同月份天数差异。 */
const DAY_OPTIONS    = Array.from({ length: 28 }, (_, i) => ({ value: i + 1, label: `${i + 1} 日` }))

/** 尝试将 cron 字符串解析为简单模式；返回 null 表示超出简单模式范围 */
function parseCron(cron: string): { freq: Freq; hour: number; minute: number; weekday: number; dayOfMonth: number } | null {
  const p = cron.trim().split(/\s+/)
  if (p.length !== 5) return null
  const [min, h, dom, , dow] = p
  // 简单整数检测
  const isInt = (s: string) => /^\d+$/.test(s)
  if (min === '0' && h === '*' && dom === '*') return { freq: 'hourly', hour: 9, minute: 0, weekday: 1, dayOfMonth: 1 }
  if (isInt(min) && isInt(h) && dom === '*' && dow === '*')
    return { freq: 'daily',   hour: +h, minute: +min, weekday: 1, dayOfMonth: 1 }
  if (isInt(min) && isInt(h) && dom === '*' && isInt(dow))
    return { freq: 'weekly',  hour: +h, minute: +min, weekday: +dow, dayOfMonth: 1 }
  if (isInt(min) && isInt(h) && isInt(dom) && dow === '*')
    return { freq: 'monthly', hour: +h, minute: +min, weekday: 1, dayOfMonth: +dom }
  return null  // 超出简单模式（多时间点/间隔等）
}

/** 根据 UI 状态生成 cron 表达式 */
function buildCron(freq: Freq, hour: number, minute: number, weekday: number, dayOfMonth: number): string {
  switch (freq) {
    case 'hourly':  return `${minute} * * * *`
    case 'daily':   return `${minute} ${hour} * * *`
    case 'weekly':  return `${minute} ${hour} * * ${weekday}`
    case 'monthly': return `${minute} ${hour} ${dayOfMonth} * *`
  }
}

/** CronBuilder 组件入参，支持受控值和可选的系统字典选项。 */
interface Props {
  /** 当前 cron 值。 */
  value?: string

  /** cron 变更回调。 */
  onChange?: (cron: string) => void
  frequencyOptions?: { label: string; value: string }[]
  weekdayOptions?: { label: string; value: number }[]
}

// 绿框选择器（频率/周几/几号）
function GreenSelect(props: React.ComponentProps<typeof Select>) {
  return (
    <div style={{ border: '1.5px solid #52c41a', borderRadius: 8, overflow: 'hidden', display: 'inline-flex' }}>
      <Select variant="borderless" size="small" style={{ minWidth: 80 }} {...props} />
    </div>
  )
}

// 蓝框时间选择器
function TimeSelect({ hour, minute, onHourChange, onMinuteChange }: {
  hour: number; minute: number
  onHourChange: (h: number) => void; onMinuteChange: (m: number) => void
}) {
  return (
    <div style={{
      border: '1.5px solid #1677ff', borderRadius: 8,
      display: 'inline-flex', alignItems: 'center',
      padding: '0 4px 0 8px', background: '#fff',
    }}>
      <ClockCircleOutlined style={{ color: '#8c8c8c', fontSize: 13, marginRight: 4 }} />
      <Select
        variant="borderless" size="small" value={hour}
        onChange={onHourChange} options={HOUR_OPTIONS}
        style={{ width: 68 }} dropdownStyle={{ minWidth: 68 }}
      />
      <span style={{ color: '#8c8c8c', fontWeight: 600, margin: '0 1px' }}>:</span>
      <Select
        variant="borderless" size="small" value={minute}
        onChange={onMinuteChange} options={MINUTE_OPTIONS}
        style={{ width: 68 }} dropdownStyle={{ minWidth: 68 }}
      />
    </div>
  )
}

/** Cron 可视化编辑器组件。 */
export default function CronBuilder({ value, onChange, frequencyOptions, weekdayOptions }: Props) {
  // 初始值优先尝试解析为“简单模式”状态
  const init   = value ? parseCron(value) : null
  // 简单模式状态
  const [freq,       setFreq]       = useState<Freq>(init?.freq       ?? 'daily')
  const [hour,       setHour]       = useState(init?.hour             ?? 9)
  const [minute,     setMinute]     = useState(init?.minute           ?? 0)
  const [weekday,    setWeekday]    = useState(init?.weekday          ?? 1)
  const [dayOfMonth, setDayOfMonth] = useState(init?.dayOfMonth       ?? 1)

  // 高级模式：初始 cron 无法映射为简单模式时自动打开
  const [advanced,   setAdvanced]   = useState(!init && !!value)
  const [manualCron, setManualCron] = useState(value ?? '')
  // 从高级模式尝试回退简单模式失败时，提示并保持高级模式
  const [parseError, setParseError] = useState(false)

  useEffect(() => {
    const parsed = value ? parseCron(value) : null
    if (parsed) {
      setFreq(parsed.freq)
      setHour(parsed.hour)
      setMinute(parsed.minute)
      setWeekday(parsed.weekday)
      setDayOfMonth(parsed.dayOfMonth)
      setAdvanced(false)
      setParseError(false)
      setManualCron(value ?? '')
      return
    }
    setAdvanced(!!value)
    setManualCron(value ?? '')
    setParseError(false)
  }, [value])

  /** 当前简单模式状态实时生成的 cron 表达式。 */
  const computed = useMemo(
    () => buildCron(freq, hour, minute, weekday, dayOfMonth),
    [freq, hour, minute, weekday, dayOfMonth]
  )

  // 简单模式中任一选项变化时，都重算并抛出 cron
  const emitComputed = (f = freq, h = hour, m = minute, wd = weekday, dom = dayOfMonth) => {
    if (!advanced) onChange?.(buildCron(f, h, m, wd, dom))
  }

  /** 切换执行频率并同步输出 cron。 */
  const handleFreq = (v: Freq)   => { setFreq(v);       emitComputed(v) }
  /** 修改小时并同步输出 cron。 */
  const handleHour = (v: number) => { setHour(v);       emitComputed(freq, v) }
  /** 修改分钟并同步输出 cron。 */
  const handleMin  = (v: number) => { setMinute(v);     emitComputed(freq, hour, v) }
  /** 修改周几并同步输出 cron。 */
  const handleWd   = (v: number) => { setWeekday(v);    emitComputed(freq, hour, minute, v) }
  /** 修改月内日期并同步输出 cron。 */
  const handleDom  = (v: number) => { setDayOfMonth(v); emitComputed(freq, hour, minute, weekday, v) }

  /** 进入高级模式，允许用户直接编辑 cron 字符串。 */
  const openAdvanced = () => {
    // 进入高级模式时带入当前简单模式生成值，便于继续手改
    setManualCron(computed)
    setAdvanced(true)
    setParseError(false)
  }

  /** 尝试从高级模式回到简单模式，无法解析时保留高级编辑。 */
  const closeAdvanced = () => {
    const p = parseCron(manualCron)
    if (p) {
      // 能解析回简单模式：同步所有 UI 状态
      setFreq(p.freq); setHour(p.hour); setMinute(p.minute)
      setWeekday(p.weekday); setDayOfMonth(p.dayOfMonth)
      setParseError(false)
      setAdvanced(false)
      onChange?.(manualCron)
    } else {
      // 超出简单模式（如多时间点），提示用户，不强制关闭
      setParseError(true)
    }
  }

  return (
    <div>
      {/* ── 简单模式（自然语言句式）── */}
      {!advanced && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap',
          background: '#f8f9fb', borderRadius: 10, padding: '12px 16px',
        }}>
          <span style={{ color: '#8c8c8c', fontSize: 14 }}>用户</span>

          <GreenSelect
            value={freq} onChange={v => handleFreq(v as Freq)}
            options={frequencyOptions?.length ? frequencyOptions : FREQ_OPTIONS}
          />

          {freq === 'weekly' && (
            <GreenSelect
              value={weekday}
              onChange={v => handleWd(+(v as number))}
              options={weekdayOptions?.length ? weekdayOptions : WEEKDAY_OPTIONS}
            />
          )}
          {freq === 'monthly' && (
            <GreenSelect value={dayOfMonth} onChange={v => handleDom(+(v as number))} options={DAY_OPTIONS} />
          )}

          {freq !== 'hourly' && (
            <TimeSelect
              hour={hour} minute={minute}
              onHourChange={handleHour} onMinuteChange={handleMin}
            />
          )}

          <span style={{ color: '#8c8c8c', fontSize: 14 }}>进入旅程</span>
        </div>
      )}

      {/* ── 高级模式（Cron 表达式）── */}
      {advanced && (
        <div style={{ background: '#f8f9fb', borderRadius: 10, padding: '12px 16px' }}>
          <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>
            Cron 表达式&ensp;<span style={{ color: '#bbb' }}>（分 时 日 月 周）</span>
          </div>
          <input
            value={manualCron}
            onChange={e => { setManualCron(e.target.value); onChange?.(e.target.value); setParseError(false) }}
            placeholder="如 0 9 * * 1-5"
            style={{
              width: '100%', padding: '6px 10px', borderRadius: 6,
              border: '1px solid #d9d9d9', fontFamily: 'monospace', fontSize: 13,
              outline: 'none', boxSizing: 'border-box',
            }}
          />
          {parseError && (
            <div style={{ fontSize: 11, color: '#ff4d4f', marginTop: 4 }}>
              当前 Cron 配置超出简单模式范围（如多时间点），将保持高级模式
            </div>
          )}
          <div style={{ fontSize: 11, color: '#bbb', marginTop: 6 }}>
            简单模式生成：<code style={{ color: '#595959' }}>{computed}</code>
          </div>
        </div>
      )}

      {/* ── 高级模式切换 ── */}
      <div style={{ marginTop: 8, display: 'flex', justifyContent: 'flex-end' }}>
        {!advanced ? (
          <span
            onClick={openAdvanced}
            style={{ fontSize: 12, color: '#bbb', cursor: 'pointer',
                     userSelect: 'none', borderBottom: '1px dashed #e0e0e0', lineHeight: 1.6 }}
          >
            高级模式（Cron 表达式）
          </span>
        ) : (
          <span
            onClick={closeAdvanced}
            style={{ fontSize: 12, color: parseError ? '#ff4d4f' : '#1677ff',
                     cursor: 'pointer', userSelect: 'none' }}
          >
            {parseError ? '⚠ 当前配置超出简单模式，点击继续锁定高级模式' : '↩ 回到简单模式'}
          </span>
        )}
      </div>
    </div>
  )
}
