import { useState, useMemo } from 'react'
import { Select } from 'antd'
import { ClockCircleOutlined } from '@ant-design/icons'

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
const HOUR_OPTIONS   = Array.from({ length: 24 }, (_, h) => ({ value: h, label: String(h).padStart(2, '0') }))
const MINUTE_OPTIONS = [0, 15, 30, 45].map(m => ({ value: m, label: String(m).padStart(2, '0') }))
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

interface Props {
  value?: string
  onChange?: (cron: string) => void
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
        style={{ width: 52 }} dropdownStyle={{ minWidth: 60 }}
      />
      <span style={{ color: '#8c8c8c', fontWeight: 600, margin: '0 1px' }}>:</span>
      <Select
        variant="borderless" size="small" value={minute}
        onChange={onMinuteChange} options={MINUTE_OPTIONS}
        style={{ width: 52 }} dropdownStyle={{ minWidth: 60 }}
      />
    </div>
  )
}

export default function CronBuilder({ value, onChange }: Props) {
  const init   = value ? parseCron(value) : null
  const [freq,       setFreq]       = useState<Freq>(init?.freq       ?? 'daily')
  const [hour,       setHour]       = useState(init?.hour             ?? 9)
  const [minute,     setMinute]     = useState(init?.minute           ?? 0)
  const [weekday,    setWeekday]    = useState(init?.weekday          ?? 1)
  const [dayOfMonth, setDayOfMonth] = useState(init?.dayOfMonth       ?? 1)

  // 高级模式：初始 cron 无法解析时自动展开
  const [advanced,   setAdvanced]   = useState(!init && !!value)
  const [manualCron, setManualCron] = useState(value ?? '')
  // 高级模式回退到简单模式时，如无法解析则锁定在高级模式
  const [parseError, setParseError] = useState(false)

  const computed = useMemo(
    () => buildCron(freq, hour, minute, weekday, dayOfMonth),
    [freq, hour, minute, weekday, dayOfMonth]
  )

  // UI 变化 → emit cron
  const emitComputed = (f = freq, h = hour, m = minute, wd = weekday, dom = dayOfMonth) => {
    if (!advanced) onChange?.(buildCron(f, h, m, wd, dom))
  }

  const handleFreq = (v: Freq)   => { setFreq(v);       emitComputed(v) }
  const handleHour = (v: number) => { setHour(v);       emitComputed(freq, v) }
  const handleMin  = (v: number) => { setMinute(v);     emitComputed(freq, hour, v) }
  const handleWd   = (v: number) => { setWeekday(v);    emitComputed(freq, hour, minute, v) }
  const handleDom  = (v: number) => { setDayOfMonth(v); emitComputed(freq, hour, minute, weekday, v) }

  const openAdvanced = () => {
    setManualCron(computed)
    setAdvanced(true)
    setParseError(false)
  }

  const closeAdvanced = () => {
    const p = parseCron(manualCron)
    if (p) {
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
            options={FREQ_OPTIONS}
          />

          {freq === 'weekly' && (
            <GreenSelect value={weekday} onChange={v => handleWd(+(v as number))} options={WEEKDAY_OPTIONS} />
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
