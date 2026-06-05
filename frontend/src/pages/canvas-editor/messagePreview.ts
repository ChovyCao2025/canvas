export interface MessagePreviewInput {
  canvasId: number
  nodeId: string
  userId: string
  graphJson: string
  contextJson: string
}

export function buildMessagePreviewPayload(input: MessagePreviewInput) {
  return {
    canvasId: input.canvasId,
    nodeId: input.nodeId,
    userId: input.userId,
    graphJson: input.graphJson,
    context: parsePreviewContext(input.contextJson),
  }
}

function parsePreviewContext(contextJson: string): Record<string, unknown> {
  if (!contextJson.trim()) return {}
  const parsed = JSON.parse(contextJson)
  if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
    return parsed as Record<string, unknown>
  }
  throw new Error('message preview context must be a JSON object')
}
