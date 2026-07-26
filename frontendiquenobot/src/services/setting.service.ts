import { api } from '@/core/api/client'
import type { SettingDto, UpdateSettingsRequest } from '@/types/setting'
import type { ApiResponse } from '@/types/api'

export const settingService = {
  getAll: async (): Promise<SettingDto[]> => {
    const response = await api.get<ApiResponse<SettingDto[]>>('/settings')
    return response.data.data
  },

  getByCategory: async (category: string): Promise<SettingDto[]> => {
    const response = await api.get<ApiResponse<SettingDto[]>>(`/settings/${category}`)
    return response.data.data
  },

  update: async (dto: UpdateSettingsRequest): Promise<SettingDto[]> => {
    const response = await api.put<ApiResponse<SettingDto[]>>('/settings', dto)
    return response.data.data
  },
}
