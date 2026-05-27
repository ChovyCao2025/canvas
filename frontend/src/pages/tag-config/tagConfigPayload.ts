/**
 * 页面职责：标签定义提交 payload 归一化工具。
 *
 * 维护说明：把表单中的可选值转换成后端期望的 null/数字格式。
 */
/** 将 Ant Design 表单值转换为后端标签定义保存接口的 payload。 */
export function normalizeTagDefinitionPayload(values: any) {
  return {
    ...values,
    // Switch 在表单中是 boolean，后端按 1/0 存储启用状态。
    enabled: values.enabled ? 1 : 0,
    manualEnabled: values.manualEnabled ? 1 : 0,
    // 空 TTL 用 null 表示永久有效，避免 undefined 在 JSON 序列化时被省略。
    defaultTtlDays: values.defaultTtlDays ?? null,
    // 旧数据可能没有写策略，保存时补默认 UPSERT 以兼容后端必填约束。
    writePolicy: values.writePolicy || 'UPSERT',
  }
}
