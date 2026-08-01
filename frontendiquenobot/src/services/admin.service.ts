import { api } from '@/core/api/client'
import type { ApiResponse, PagedResponse } from '@/types/api'
import type { TenantDto, CreateTenantRequest } from '@/types/auth'

export interface ServerInfo {
  status: string
  version: string
  uptime: string
  cpuUsage: number
  memoryUsage: number
  memoryMax: number
  activeThreads: number
}

export interface DatabaseInfo {
  status: string
  activeConnections: number
  maxConnections: number
  diskUsageMb: number
}

export interface IntegrationInfo {
  status: string
  lastCheck: string
}

export interface SystemStats {
  totalTenants: number
  activeTenants: number
  suspendedTenants: number
  trialTenants: number
  expiredTenants: number
  totalUsers: number
  totalConversations: number
  totalMessages: number
  totalContacts: number
  totalLeads: number
  totalProducts: number
  storageUsedMb: number
  aiTotalRequests: number
  aiTotalTokens: number
  server: ServerInfo
  database: DatabaseInfo
  evolutionApi: IntegrationInfo
}

export interface TenantQueryParams {
  search?: string
  status?: string
  page?: number
  size?: number
}

export interface PlanDto {
  id: string
  name: string
  code: string
  description: string
  monthlyPrice: number
  yearlyPrice: number
  maxUsers: number
  maxConversations: number
  maxContacts: number
  maxStorageMb: number
  features: string
  active: boolean
  publicPlan: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreatePlanRequest {
  name: string
  code: string
  description?: string
  monthlyPrice: number
  yearlyPrice: number
  maxUsers: number
  maxConversations: number
  maxContacts: number
  maxStorageMb: number
  features?: string
  active?: boolean
  publicPlan?: boolean
  sortOrder?: number
}

export interface SettingDto {
  id: string
  category: string
  key: string
  value: string
  type: string
  description: string
}

export interface BackupInfo {
  fileName: string
  sizeBytes: number
  createdAt: string
}

export const adminService = {
  getTenants: async (params?: TenantQueryParams): Promise<PagedResponse<TenantDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<TenantDto>>>('/admin/tenants', { params })
    return response.data.data
  },

  getTenant: async (id: string): Promise<TenantDto> => {
    const response = await api.get<ApiResponse<TenantDto>>(`/admin/tenants/${id}`)
    return response.data.data
  },

  createTenant: async (request: CreateTenantRequest): Promise<TenantDto> => {
    const response = await api.post<ApiResponse<TenantDto>>('/admin/tenants', request)
    return response.data.data
  },

  updateTenantStatus: async (id: string, status: string): Promise<TenantDto> => {
    const response = await api.put<ApiResponse<TenantDto>>(`/admin/tenants/${id}/status`, null, {
      params: { status },
    })
    return response.data.data
  },

  updateTenant: async (id: string, request: Partial<CreateTenantRequest>): Promise<TenantDto> => {
    const response = await api.put<ApiResponse<TenantDto>>(`/admin/tenants/${id}`, request)
    return response.data.data
  },

  deleteTenant: async (id: string): Promise<void> => {
    await api.delete(`/admin/tenants/${id}`)
  },

  getSystemStats: async (): Promise<SystemStats> => {
    const response = await api.get<ApiResponse<SystemStats>>('/admin/stats')
    return response.data.data
  },

  getPlans: async (params?: { page?: number; size?: number }): Promise<PagedResponse<PlanDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<PlanDto>>>('/admin/plans', { params })
    return response.data.data
  },

  getPlan: async (id: string): Promise<PlanDto> => {
    const response = await api.get<ApiResponse<PlanDto>>(`/admin/plans/${id}`)
    return response.data.data
  },

  createPlan: async (request: CreatePlanRequest): Promise<PlanDto> => {
    const response = await api.post<ApiResponse<PlanDto>>('/admin/plans', request)
    return response.data.data
  },

  updatePlan: async (id: string, request: Partial<CreatePlanRequest>): Promise<PlanDto> => {
    const response = await api.put<ApiResponse<PlanDto>>(`/admin/plans/${id}`, request)
    return response.data.data
  },

  deletePlan: async (id: string): Promise<void> => {
    await api.delete(`/admin/plans/${id}`)
  },

  getSettings: async (category: string): Promise<SettingDto[]> => {
    const response = await api.get<ApiResponse<SettingDto[]>>(`/admin/settings/${category}`)
    return response.data.data
  },

  updateSettings: async (category: string, settings: Record<string, string>): Promise<SettingDto[]> => {
    const response = await api.put<ApiResponse<SettingDto[]>>(`/admin/settings/${category}`, settings)
    return response.data.data
  },

  testSmtp: async (to: string): Promise<void> => {
    await api.post<ApiResponse<null>>('/admin/settings/smtp/test', { to })
  },

  getBackups: async (): Promise<BackupInfo[]> => {
    const response = await api.get<ApiResponse<BackupInfo[]>>('/admin/settings/backups')
    return response.data.data
  },

  createBackup: async (): Promise<BackupInfo> => {
    const response = await api.post<ApiResponse<BackupInfo>>('/admin/settings/backups')
    return response.data.data
  },
}
