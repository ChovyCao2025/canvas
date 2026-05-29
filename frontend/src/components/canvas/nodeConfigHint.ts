/**
 * 节点卡片关键配置摘要。
 *
 * 只展示能帮助排障的稳定配置，避免在画布上看不到已选 MQ 消息类型。
 */
export function getNodeConfigHint(
  nodeType: string,
  bizConfig: Record<string, unknown> | undefined,
): string | null {
  if (!bizConfig) return null

  if (nodeType === 'MQ_TRIGGER' || nodeType === 'SEND_MQ') {
    const messageCode = bizConfig.messageCodeKey
    if (typeof messageCode === 'string' && messageCode.trim()) return `消息: ${messageCode}`

    const topic = bizConfig.topicKey
    if (typeof topic === 'string' && topic.trim()) return `Topic: ${topic}`
  }

  return null
}
