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

export interface WhatsAppQRCodeResult {
  base64: string | null
  hasQR: boolean
  error: string | null
}

export interface WhatsAppSetupPayload {
  provider: string
  phoneNumber?: string
}

export interface WhatsAppSetupResult {
  instanceId: string
  configured: boolean
  message: string
}

export const whatsappService = {
  getStatus: async (): Promise<WhatsAppConnectionStatus> => {
    const response = await api.get<ApiResponse<WhatsAppConnectionStatus>>('/whatsapp/status')
    return response.data.data
  },

  setup: async (payload: WhatsAppSetupPayload): Promise<WhatsAppSetupResult> => {
    const response = await api.post<ApiResponse<WhatsAppSetupResult>>('/whatsapp/setup', payload)
    return response.data.data
  },

  testConnection: async (): Promise<WhatsAppTestConnectionResult> => {
    const response = await api.post<ApiResponse<WhatsAppTestConnectionResult>>('/whatsapp/test-connection')
    return response.data.data
  },

  getQRCode: async (): Promise<WhatsAppQRCodeResult> => {
    const response = await api.get<ApiResponse<WhatsAppQRCodeResult>>('/whatsapp/qr-code')
    return response.data.data
  },

  disconnect: async (): Promise<void> => {
    await api.post('/whatsapp/disconnect')
  },
}
