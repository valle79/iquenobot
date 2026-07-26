import { api } from '@/core/api/client'
import { BaseService } from './base.service'
import type { ConversationDto, ConversationMessageDto, CreateConversationRequest, SendMessageRequest } from '@/types/chat'
import type { ApiResponse, PagedResponse } from '@/types/api'

class ConversationService extends BaseService<ConversationDto, CreateConversationRequest> {
  constructor() {
    super('/conversations')
  }

  async getActive(params?: { page?: number; size?: number }): Promise<PagedResponse<ConversationDto>> {
    return this.getByPath('active', params)
  }

  async getUnassigned(params?: { page?: number; size?: number }): Promise<PagedResponse<ConversationDto>> {
    return this.getByPath('unassigned', params)
  }

  async getMyConversations(params?: { page?: number; size?: number }): Promise<PagedResponse<ConversationDto>> {
    return this.getByPath('my-conversations', params)
  }

  async getByStatus(status: string, params?: { page?: number; size?: number }): Promise<PagedResponse<ConversationDto>> {
    return this.getByPath(`status/${status}`, params)
  }

  async getMessages(conversationId: string, params?: { page?: number; size?: number }): Promise<PagedResponse<ConversationMessageDto>> {
    const response = await api.get<ApiResponse<PagedResponse<ConversationMessageDto>>>(
      `${this.endpoint}/${conversationId}/messages`,
      { params },
    )
    return response.data.data
  }

  async sendMessage(dto: SendMessageRequest): Promise<ConversationMessageDto> {
    const response = await api.post<ApiResponse<ConversationMessageDto>>(`${this.endpoint}/messages`, dto)
    return response.data.data
  }

  async assign(conversationId: string, userId: string): Promise<void> {
    await this.action(`${conversationId}/assign/${userId}`)
  }

  async unassign(conversationId: string): Promise<void> {
    await this.action(`${conversationId}/unassign`)
  }

  async resolve(conversationId: string): Promise<void> {
    await this.action(`${conversationId}/resolve`)
  }

  async close(conversationId: string): Promise<void> {
    await this.action(`${conversationId}/close`)
  }

  async reopen(conversationId: string): Promise<void> {
    await this.action(`${conversationId}/reopen`)
  }

  async markAsRead(conversationId: string): Promise<void> {
    await this.action(`${conversationId}/mark-as-read`)
  }
}

export const conversationService = new ConversationService()
