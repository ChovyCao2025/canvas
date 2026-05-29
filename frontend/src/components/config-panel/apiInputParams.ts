/**
 * api-input-params 控件辅助逻辑。
 *
 * 该控件被 API_CALL、SEND_MQ、事件定义等复用，写回字段必须以 schema.key 为准。
 */
export function resolveApiInputParamsFieldKey(fieldKey: string): string {
  return fieldKey
}
