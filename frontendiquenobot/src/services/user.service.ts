import { api } from '@/core/api/client'
import type { UserDto, CreateUserRequest, UpdateUserRequest } from '@/types/auth'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export const userService = {
  getAll: async (params?: PaginationParams): Promise<PagedResponse<UserDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<UserDto>>>('/auth/users', { params })
    return response.data.data
  },

  getById: async (id: string): Promise<UserDto> => {
    const response = await api.get<ApiResponse<UserDto>>(`/auth/users/${id}`)
    return response.data.data
  },

  create: async (dto: CreateUserRequest): Promise<UserDto> => {
    const response = await api.post<ApiResponse<UserDto>>('/auth/users', dto)
    return response.data.data
  },

  update: async (id: string, dto: UpdateUserRequest): Promise<UserDto> => {
    const response = await api.put<ApiResponse<UserDto>>(`/auth/users/${id}`, dto)
    return response.data.data
  },
}
