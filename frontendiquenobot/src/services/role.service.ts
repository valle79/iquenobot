import { api } from '@/core/api/client'
import type { RoleDto, CreateRoleRequest, UpdateRoleRequest } from '@/types/role'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export const roleService = {
  getAll: async (params?: PaginationParams): Promise<PagedResponse<RoleDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<RoleDto>>>('/roles', { params })
    return response.data.data
  },

  getById: async (id: string): Promise<RoleDto> => {
    const response = await api.get<ApiResponse<RoleDto>>(`/roles/${id}`)
    return response.data.data
  },

  create: async (dto: CreateRoleRequest): Promise<RoleDto> => {
    const response = await api.post<ApiResponse<RoleDto>>('/roles', dto)
    return response.data.data
  },

  update: async (id: string, dto: UpdateRoleRequest): Promise<RoleDto> => {
    const response = await api.put<ApiResponse<RoleDto>>(`/roles/${id}`, dto)
    return response.data.data
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/roles/${id}`)
  },
}
