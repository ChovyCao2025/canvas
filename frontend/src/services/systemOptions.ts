/**
 * 服务职责：系统字典分类常量和管理端字典 API 封装。
 *
 * 维护说明：前台读取 meta 接口，后台维护 admin 接口，两类调用在这里保持边界清晰。
 */
import http from './api'
import type { PageResult, R, StubOption, SystemOption } from '../types'

/**
 * 前端会直接展示或批量加载的系统字典分类。
 *
 * 维护边界：
 * - value 必须与后端 SystemOption.category 完全一致；
 * - label 面向管理员筛选和配置面板展示；
 * - 新增节点 schema 下拉项时，应优先在这里补充分类，避免页面硬编码。
 */
export const SYSTEM_OPTION_CATEGORIES = [
  { value: 'condition_operator', label: '条件操作符' },
  { value: 'audience_condition_operator', label: '人群规则操作符' },
  { value: 'logic_relation', label: '逻辑关系' },
  { value: 'query_combinator', label: '人群组合关系' },
  { value: 'param_type', label: '参数类型' },
  { value: 'event_attr_type', label: '事件属性类型' },
  { value: 'http_method', label: 'HTTP 方法' },
  { value: 'tag_type', label: '标签类型' },
  { value: 'audience_data_source_type', label: '人群数据源类型' },
  { value: 'audience_evaluation_strategy', label: '人群计算策略' },
  { value: 'audience_engine_type', label: '人群规则引擎' },
  { value: 'user_role', label: '用户角色' },
  { value: 'context_value_type', label: '上下文值类型' },
  { value: 'delay_unit', label: '延迟单位' },
  { value: 'cron_frequency', label: 'Cron 频率' },
  { value: 'weekday', label: '周几' },
  { value: 'schedule_type', label: '定时触发类型' },
  { value: 'tagger_mode', label: 'Tagger 模式' },
  { value: 'threshold_mode', label: '阈值触发条件' },
  { value: 'aggregate_evaluate_mode', label: '聚合评估方式' },
  { value: 'approval_timeout_action', label: '审批超时动作' },
  { value: 'canvas_invoke_mode', label: '画布调用模式' },
  { value: 'direct_return_build_type', label: '直调返回构建方式' },
  { value: 'coupon_type', label: '券类型' },
  { value: 'reach_scene', label: '触达场景' },
  { value: 'biz_line', label: '业务线' },
  { value: 'biz_line_api', label: '业务线接口' },
  { value: 'behavior_strategy_type', label: '行为策略类型' },
  { value: 'message_code_in_app', label: '端内消息编码' },
  { value: 'message_code_mq', label: 'MQ 消息编码' },
  { value: 'mq_topic_legacy', label: 'MQ 主题兼容选项' },
  { value: 'canvas_trigger_type', label: '画布触发类型' },
  { value: 'start_trigger_type', label: 'START 触发方式' },
  { value: 'behavior_trigger_type', label: '行为触发方式' },
]

/** 系统字典接口集合：前台读取 meta，后台维护 admin。 */
export const systemOptionsApi = {
  /** 查询单个分类的启用字典项，供配置面板下拉使用。 */
  meta: (category: string) =>
    http.get<R<StubOption[]>, R<StubOption[]>>('/meta/options', { params: { category } }),

  /**
   * 批量查询多个分类的启用字典项。
   *
   * 后端按重复 query key 接收 categories，因此这里自定义序列化格式为
   * categories=a&categories=b，而不是 axios 默认的 categories[]=a。
   */
  metaBatch: (categories: string[]) =>
    http.get<R<Record<string, StubOption[]>>, R<Record<string, StubOption[]>>>('/meta/options/batch', {
      params: { categories },
      paramsSerializer: params => (params.categories as string[])
        .map(category => `categories=${encodeURIComponent(category)}`)
        .join('&'),
    }),

  /** 管理端分页查询字典项，支持分类、启用状态和关键字筛选。 */
  adminList: (params?: { category?: string; enabled?: number; keyword?: string }) =>
    http.get<R<PageResult<SystemOption>>, R<PageResult<SystemOption>>>('/admin/system-options', { params }),

  /** 更新字典项；前端以 Partial 传递仅修改的字段。 */
  update: (id: number, body: Partial<SystemOption>) =>
    http.put<R<void>, R<void>>(`/admin/system-options/${id}`, body),
}
