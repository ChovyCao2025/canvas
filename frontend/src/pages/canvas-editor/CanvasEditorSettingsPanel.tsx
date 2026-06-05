import { useMemo } from 'react'
import { DatePicker, Form, Input, InputNumber, Modal, Radio } from 'antd'
import type { FormInstance } from 'antd'
import { CaretRightOutlined, DownOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import CronBuilder from '../../components/config-panel/CronBuilder'
import {
  type CanvasSettingsLike,
  type CanvasTriggerType,
  getExecutionLimitsSummary,
  getTriggerTypeSummary,
} from './settingsPresentation'

const { RangePicker } = DatePicker

const TRIGGER_TYPE_HELP: Record<string, string> = {
  REALTIME: '用户满足条件后立即进入当前画布。',
  SCHEDULED: '按固定周期统一执行，适合批处理场景。',
}

interface SelectOption {
  label: string
  value: string
}

interface WeekdayOption {
  label: string
  value: number
}

interface CanvasEditorSettingsPanelProps {
  open: boolean
  form: FormInstance
  onSave: () => Promise<void>
  onCancel: () => void
  triggerTypeOptions: SelectOption[]
  cronFrequencyOptions: SelectOption[]
  weekdayOptions: WeekdayOption[]
  limitsExpanded: boolean
  onToggleLimits: () => void
}

export default function CanvasEditorSettingsPanel({
  open,
  form,
  onSave,
  onCancel,
  triggerTypeOptions,
  cronFrequencyOptions,
  weekdayOptions,
  limitsExpanded,
  onToggleLimits,
}: CanvasEditorSettingsPanelProps) {
  const watchedTriggerType = Form.useWatch('triggerType', form)
  const watchedCronExpression = Form.useWatch('cronExpression', form)
  const watchedValidRange = Form.useWatch('validRange', form) as [Dayjs | null | undefined, Dayjs | null | undefined] | undefined
  const watchedMaxTotalExecutions = Form.useWatch('maxTotalExecutions', form)
  const watchedPerUserDailyLimit = Form.useWatch('perUserDailyLimit', form)
  const watchedPerUserTotalLimit = Form.useWatch('perUserTotalLimit', form)
  const watchedCooldownSeconds = Form.useWatch('cooldownSeconds', form)
  const watchedControlGroupPercent = Form.useWatch('controlGroupPercent', form)
  const watchedControlGroupSalt = Form.useWatch('controlGroupSalt', form)
  const watchedConversionEventCode = Form.useWatch('conversionEventCode', form)
  const watchedAttributionWindowDays = Form.useWatch('attributionWindowDays', form)
  const watchedProjectKey = Form.useWatch('projectKey', form)
  const watchedProjectName = Form.useWatch('projectName', form)
  const watchedFolderKey = Form.useWatch('folderKey', form)
  const watchedFolderName = Form.useWatch('folderName', form)
  const normalizedTriggerType: CanvasTriggerType = watchedTriggerType === 'SCHEDULED' ? 'SCHEDULED' : 'REALTIME'
  const limitsSectionId = 'canvas-settings-execution-limits'

  const liveSettings = useMemo<CanvasSettingsLike>(() => ({
    triggerType: normalizedTriggerType,
    cronExpression: watchedCronExpression ?? '',
    validStart: watchedValidRange?.[0]?.format('YYYY-MM-DDTHH:mm:ss') ?? undefined,
    validEnd: watchedValidRange?.[1]?.format('YYYY-MM-DDTHH:mm:ss') ?? undefined,
    maxTotalExecutions: watchedMaxTotalExecutions ?? undefined,
    perUserDailyLimit: watchedPerUserDailyLimit ?? undefined,
    perUserTotalLimit: watchedPerUserTotalLimit ?? undefined,
    cooldownSeconds: watchedCooldownSeconds ?? undefined,
    controlGroupPercent: watchedControlGroupPercent ?? undefined,
    controlGroupSalt: watchedControlGroupSalt ?? undefined,
    conversionEventCode: watchedConversionEventCode ?? undefined,
    attributionWindowDays: watchedAttributionWindowDays ?? undefined,
    projectKey: watchedProjectKey ?? undefined,
    projectName: watchedProjectName ?? undefined,
    folderKey: watchedFolderKey ?? undefined,
    folderName: watchedFolderName ?? undefined,
  }), [
    watchedAttributionWindowDays,
    watchedCooldownSeconds,
    watchedControlGroupPercent,
    watchedControlGroupSalt,
    watchedConversionEventCode,
    watchedCronExpression,
    watchedMaxTotalExecutions,
    normalizedTriggerType,
    watchedFolderKey,
    watchedFolderName,
    watchedPerUserDailyLimit,
    watchedPerUserTotalLimit,
    watchedProjectKey,
    watchedProjectName,
    watchedValidRange,
  ])

  return (
    <Modal
      title="画布设置"
      open={open}
      onOk={() => { void onSave() }}
      onCancel={onCancel}
      okText="保存"
      cancelText="取消"
      width={560}
    >
      <Form form={form} layout="vertical" className="canvas-settings-form">
        <section className="canvas-settings-card">
          <div className="canvas-settings-section-header">
            <div>
              <div className="canvas-settings-section-title">触发方式</div>
              <div className="canvas-settings-section-help">决定旅程是实时进入，还是按计划批量执行。</div>
            </div>
            <span className="canvas-settings-summary-tag">
              {getTriggerTypeSummary(liveSettings.triggerType)}
            </span>
          </div>
          <Form.Item name="triggerType" initialValue="REALTIME" className="canvas-settings-trigger-group">
            <Radio.Group className="canvas-settings-trigger-options">
              {triggerTypeOptions.map(option => (
                <Radio key={option.value} value={option.value} className="canvas-settings-trigger-option">
                  <span className="canvas-settings-trigger-option-title">{option.label}</span>
                  <span className="canvas-settings-trigger-option-help">
                    {TRIGGER_TYPE_HELP[option.value] ?? option.value}
                  </span>
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>
          {liveSettings.triggerType === 'SCHEDULED' ? (
            <div className="canvas-settings-inline-panel">
              <div className="canvas-settings-inline-title">执行计划</div>
              <Form.Item
                name="cronExpression"
                rules={[{ required: true, message: '请配置触发时间' }]}
                style={{ marginBottom: 0 }}
              >
                <CronBuilder
                  onChange={cron => form.setFieldValue('cronExpression', cron)}
                  frequencyOptions={cronFrequencyOptions}
                  weekdayOptions={weekdayOptions}
                />
              </Form.Item>
            </div>
          ) : null}
        </section>

        <section className="canvas-settings-card">
          <div className="canvas-settings-section-header">
            <div>
              <div className="canvas-settings-section-title">项目归属</div>
              <div className="canvas-settings-section-help">用于列表筛选和轻量分组。</div>
            </div>
          </div>
          <div className="canvas-settings-grid">
            <Form.Item label="项目 Key" name="projectKey">
              <Input maxLength={128} placeholder="growth" />
            </Form.Item>
            <Form.Item label="项目名称" name="projectName">
              <Input maxLength={255} placeholder="增长运营" />
            </Form.Item>
            <Form.Item label="文件夹 Key" name="folderKey">
              <Input maxLength={128} placeholder="new-user" />
            </Form.Item>
            <Form.Item label="文件夹名称" name="folderName">
              <Input maxLength={255} placeholder="新客旅程" />
            </Form.Item>
          </div>
        </section>

        <section className="canvas-settings-card">
          <button
            type="button"
            className="canvas-settings-collapse"
            onClick={onToggleLimits}
            aria-expanded={limitsExpanded}
            aria-controls={limitsSectionId}
          >
            <div className="canvas-settings-collapse-copy">
              <div className="canvas-settings-section-title">执行限制</div>
              <div className="canvas-settings-section-help">留空表示不限制，可按需控制有效期、频次和总量。</div>
            </div>
            <div className="canvas-settings-collapse-actions">
              <span className="canvas-settings-summary-tag">
                {getExecutionLimitsSummary(liveSettings)}
              </span>
              {limitsExpanded ? <DownOutlined /> : <CaretRightOutlined />}
            </div>
          </button>
          <div
            id={limitsSectionId}
            className="canvas-settings-limits-content"
            hidden={!limitsExpanded}
          >
            <div className="canvas-settings-tip">
              限制仅影响执行窗口与配额，不会改变现有触发逻辑。
            </div>
            <Form.Item label="有效期" name="validRange">
              <RangePicker
                showTime
                format="YYYY-MM-DD HH:mm"
                placeholder={['开始时间', '结束时间']}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <div className="canvas-settings-grid">
              <Form.Item label="总执行次数上限" name="maxTotalExecutions">
                <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
              </Form.Item>
              <Form.Item label="用户每日上限" name="perUserDailyLimit">
                <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
              </Form.Item>
              <Form.Item label="用户总上限" name="perUserTotalLimit">
                <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
              </Form.Item>
              <Form.Item label="冷却秒数" name="cooldownSeconds">
                <InputNumber min={0} style={{ width: '100%' }} placeholder="不限制" />
              </Form.Item>
              <Form.Item label="控制组比例(%)" name="controlGroupPercent">
                <InputNumber min={0} max={50} style={{ width: '100%' }} placeholder="0" />
              </Form.Item>
              <Form.Item label="控制组盐值" name="controlGroupSalt">
                <Input maxLength={64} placeholder="默认盐值" />
              </Form.Item>
              <Form.Item label="转化事件编码" name="conversionEventCode">
                <Input maxLength={128} placeholder="如 ORDER_PAID" />
              </Form.Item>
              <Form.Item label="归因窗口(天)" name="attributionWindowDays">
                <InputNumber min={1} style={{ width: '100%' }} placeholder="7" />
              </Form.Item>
            </div>
          </div>
        </section>
      </Form>
    </Modal>
  )
}
