import { api } from '@/core/api/client'
import type { ChatbotIntentDto, CreateChatbotIntentRequest, ChatbotFlowDto, CreateChatbotFlowRequest, ChatbotPreviewRequest, ChatbotPreviewResponse } from '@/types/chatbot'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export const chatbotService = {
  // Modo prueba (preview)
  sendPreviewMessage: async (dto: ChatbotPreviewRequest): Promise<ChatbotPreviewResponse> => {
    const res = await api.post<ApiResponse<ChatbotPreviewResponse>>('/chatbot/preview', dto)
    return res.data.data
  },
  // Intents
  getIntents: async (params?: PaginationParams): Promise<PagedResponse<ChatbotIntentDto>> => {
    const res = await api.get<ApiResponse<PagedResponse<ChatbotIntentDto>>>('/chatbot/intents', { params })
    return res.data.data
  },
  getIntent: async (id: string): Promise<ChatbotIntentDto> => {
    const res = await api.get<ApiResponse<ChatbotIntentDto>>(`/chatbot/intents/${id}`)
    return res.data.data
  },
  createIntent: async (dto: CreateChatbotIntentRequest): Promise<ChatbotIntentDto> => {
    const res = await api.post<ApiResponse<ChatbotIntentDto>>('/chatbot/intents', dto)
    return res.data.data
  },
  updateIntent: async (id: string, dto: CreateChatbotIntentRequest): Promise<ChatbotIntentDto> => {
    const res = await api.put<ApiResponse<ChatbotIntentDto>>(`/chatbot/intents/${id}`, dto)
    return res.data.data
  },
  deleteIntent: async (id: string): Promise<void> => {
    await api.delete(`/chatbot/intents/${id}`)
  },
  getIntentCount: async (): Promise<{ total: number; active: number }> => {
    const res = await api.get<ApiResponse<{ total: number; active: number }>>('/chatbot/intents/count')
    return res.data.data
  },

  // Flows
  getFlows: async (params?: PaginationParams): Promise<PagedResponse<ChatbotFlowDto>> => {
    const res = await api.get<ApiResponse<PagedResponse<ChatbotFlowDto>>>('/chatbot/flows', { params })
    return res.data.data
  },
  getFlow: async (id: string): Promise<ChatbotFlowDto> => {
    const res = await api.get<ApiResponse<ChatbotFlowDto>>(`/chatbot/flows/${id}`)
    return res.data.data
  },
  createFlow: async (dto: CreateChatbotFlowRequest): Promise<ChatbotFlowDto> => {
    const res = await api.post<ApiResponse<ChatbotFlowDto>>('/chatbot/flows', dto)
    return res.data.data
  },
  updateFlow: async (id: string, dto: CreateChatbotFlowRequest): Promise<ChatbotFlowDto> => {
    const res = await api.put<ApiResponse<ChatbotFlowDto>>(`/chatbot/flows/${id}`, dto)
    return res.data.data
  },
  deleteFlow: async (id: string): Promise<void> => {
    await api.delete(`/chatbot/flows/${id}`)
  },
  getFlowCount: async (): Promise<{ total: number; active: number }> => {
    const res = await api.get<ApiResponse<{ total: number; active: number }>>('/chatbot/flows/count')
    return res.data.data
  },
}
