import { api } from '@/core/api/client'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, AuthResponse, CreateTenantRequest, TenantDto, CreateUserRequest, UserDto } from '@/types/auth'

export const authService = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', credentials)
    return response.data.data
  },

  logout: async (): Promise<void> => {
    try {
      await api.post('/auth/logout')
    } catch {
      // Even if the request fails, we should proceed with logout
    }
  },

  refresh: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await api.post<ApiResponse<AuthResponse>>(
      '/auth/refresh',
      {},
      { headers: { Authorization: `Bearer ${refreshToken}` } },
    )
    return response.data.data
  },

  createTenant: async (request: CreateTenantRequest): Promise<TenantDto> => {
    const response = await api.post<ApiResponse<TenantDto>>('/auth/tenants', request)
    return response.data.data
  },

  createUser: async (request: CreateUserRequest): Promise<UserDto> => {
    const response = await api.post<ApiResponse<UserDto>>('/auth/users', request)
    return response.data.data
  },
}
