import { api } from '@/core/api/client'
import type { ApiResponse } from '@/types/api'
import type { DashboardOverview, ConversationStats, LeadStats, ContactStats, ProductStats, UserActivity } from '@/types/dashboard'

export const dashboardService = {
  getOverview: async (): Promise<DashboardOverview> => {
    const response = await api.get<ApiResponse<DashboardOverview>>('/dashboard/overview')
    return response.data.data
  },

  getConversationStats: async (): Promise<ConversationStats> => {
    const response = await api.get<ApiResponse<ConversationStats>>('/dashboard/conversations/stats')
    return response.data.data
  },

  getLeadStats: async (): Promise<LeadStats> => {
    const response = await api.get<ApiResponse<LeadStats>>('/dashboard/leads/stats')
    return response.data.data
  },

  getContactStats: async (): Promise<ContactStats> => {
    const response = await api.get<ApiResponse<ContactStats>>('/dashboard/contacts/stats')
    return response.data.data
  },

  getProductStats: async (): Promise<ProductStats> => {
    const response = await api.get<ApiResponse<ProductStats>>('/dashboard/products/stats')
    return response.data.data
  },

  getUserActivity: async (): Promise<UserActivity[]> => {
    const response = await api.get<ApiResponse<UserActivity[]>>('/dashboard/users/activity')
    return response.data.data
  },
}
