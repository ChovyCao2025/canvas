export function normalizeTagDefinitionPayload(values: any) {
  return {
    ...values,
    enabled: values.enabled ? 1 : 0,
    manualEnabled: values.manualEnabled ? 1 : 0,
    defaultTtlDays: values.defaultTtlDays ?? null,
    writePolicy: values.writePolicy || 'UPSERT',
  }
}
