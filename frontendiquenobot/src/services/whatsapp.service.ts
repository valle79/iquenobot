import { api } from '@/core/api/client'
import type { ApiResponse } from '@/types/api'

export interface WhatsAppConnectionStatus {
  connected: boolean
  instanceId: string | null
  provider: string | null
  phoneNumber: string | null
  configured: boolean
  error: string | null
}

export interface WhatsAppTestConnectionResult {
  success: boolean
  connected: boolean
  message: string
}

export const whatsappService = {
  getStatus: async (): Promise<WhatsAppConnectionStatus> => {
    const response = await api.get<ApiResponse<WhatsAppConnectionStatus>>('/whatsapp/status')
    return response.data.data
  },

  testConnection: async (): Promise<WhatsAppTestConnectionResult> => {
    const response = await api.post<ApiResponse<WhatsAppTestConnectionResult>>('/whatsapp/test-connection')
    return response.data.data
  },
}
