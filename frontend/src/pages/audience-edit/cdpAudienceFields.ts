import type { AudienceDataSourceType, AudienceSourceField } from '../../services/audienceApi'

export function isCdpAudienceSource(
  type?: string,
): type is Extract<AudienceDataSourceType, 'CDP_TAG' | 'CDP_PROFILE' | 'CDP_IDENTITY'> {
  return type === 'CDP_TAG' || type === 'CDP_PROFILE' || type === 'CDP_IDENTITY'
}

export function toQueryBuilderFields(fields: AudienceSourceField[]) {
  return fields.map(field => ({
    name: field.name,
    label: field.label || field.name,
    value: field.name,
  }))
}
