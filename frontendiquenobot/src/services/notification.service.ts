import { api } from '@/core/api/client'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'
import type { NotificationDto, CreateNotificationRequest } from '@/types/notification'

export const notificationService = {
  getById: async (id: string): Promise<NotificationDto> => {
    const response = await api.get<ApiResponse<NotificationDto>>(`/notifications/${id}`)
    return response.data.data
  },

  getByUser: async (userId: string, params?: PaginationParams): Promise<PagedResponse<NotificationDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<NotificationDto>>>(`/notifications/user/${userId}`, { params })
    return response.data.data
  },

  getUnread: async (userId: string, params?: PaginationParams): Promise<PagedResponse<NotificationDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<NotificationDto>>>(`/notifications/user/${userId}/unread`, { params })
    return response.data.data
  },

  getByUserAndType: async (userId: string, type: string, params?: PaginationParams): Promise<PagedResponse<NotificationDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<NotificationDto>>>(`/notifications/user/${userId}/type/${type}`, { params })
    return response.data.data
  },

  getUnreadCount: async (userId: string): Promise<number> => {
    const response = await api.get<ApiResponse<number>>(`/notifications/user/${userId}/unread/count`)
    return response.data.data
  },

  getUnreadCountByPriority: async (userId: string, priority: string): Promise<number> => {
    const response = await api.get<ApiResponse<number>>(`/notifications/user/${userId}/unread/count/priority/${priority}`)
    return response.data.data
  },

  create: async (dto: CreateNotificationRequest): Promise<NotificationDto> => {
    const response = await api.post<ApiResponse<NotificationDto>>('/notifications', dto)
    return response.data.data
  },

  markAsRead: async (id: string): Promise<NotificationDto> => {
    const response = await api.put<ApiResponse<NotificationDto>>(`/notifications/${id}/read`)
    return response.data.data
  },

  markAsUnread: async (id: string): Promise<NotificationDto> => {
    const response = await api.put<ApiResponse<NotificationDto>>(`/notifications/${id}/unread`)
    return response.data.data
  },

  markAllAsRead: async (userId: string): Promise<void> => {
    await api.put(`/notifications/user/${userId}/read-all`)
  },

  sendNotification: async (id: string): Promise<void> => {
    await api.post(`/notifications/${id}/send`)
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/notifications/${id}`)
  },

  deleteOldRead: async (userId: string, daysOld: number = 30): Promise<void> => {
    await api.delete(`/notifications/user/${userId}/cleanup`, { params: { daysOld } })
  },
}
