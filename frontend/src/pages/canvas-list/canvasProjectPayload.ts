export interface CanvasCreateFormValues {
  name: string
  description?: string
  projectId?: number
  projectKey?: string
  projectName?: string
  folderKey?: string
  folderName?: string
}

export function buildCanvasCreatePayload(values: CanvasCreateFormValues) {
  return {
    name: values.name,
    ...optionalText('description', values.description),
    ...(values.projectId ? { projectId: values.projectId } : {}),
    ...optionalText('projectKey', values.projectKey),
    ...optionalText('projectName', values.projectName),
    ...optionalText('folderKey', values.folderKey),
    ...optionalText('folderName', values.folderName),
  }
}

function optionalText(key: string, value?: string) {
  const trimmed = value?.trim()
  return trimmed ? { [key]: trimmed } : {}
}
