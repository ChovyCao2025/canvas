import type { R } from '../types'
import type {
  MessageTemplate,
  MessageTemplateDraft,
  TemplatePreviewResult,
} from '../pages/message-templates/messageTemplateCenter'
import http from './api'

export const messageTemplateApi = {
  search: (params?: { keyword?: string; channel?: string }) =>
    http.get<R<MessageTemplate[]>, R<MessageTemplate[]>>('/message-templates', { params }),

  create: (payload: MessageTemplateDraft) =>
    http.post<R<MessageTemplate>, R<MessageTemplate>>('/message-templates', payload),

  preview: (templateCode: string, context: Record<string, unknown>) =>
    http.post<R<TemplatePreviewResult>, R<TemplatePreviewResult>>(
      `/message-templates/${templateCode}/preview`,
      context,
    ),
}
