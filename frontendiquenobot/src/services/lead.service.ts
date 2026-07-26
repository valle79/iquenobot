import { api } from '@/core/api/client'
import type { LeadDto, CreateLeadRequest } from '@/types/contact'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export const leadService = {
  getAll: async (params?: PaginationParams): Promise<PagedResponse<LeadDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<LeadDto>>>('/leads', { params })
    return response.data.data
  },

  getById: async (id: string): Promise<LeadDto> => {
    const response = await api.get<ApiResponse<LeadDto>>(`/leads/${id}`)
    return response.data.data
  },

  getByStatus: async (status: string, params?: PaginationParams): Promise<PagedResponse<LeadDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<LeadDto>>>(`/leads/status/${status}`, { params })
    return response.data.data
  },

  getBySource: async (source: string, params?: PaginationParams): Promise<PagedResponse<LeadDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<LeadDto>>>(`/leads/source/${source}`, { params })
    return response.data.data
  },

  getAssigned: async (userId: string, params?: PaginationParams): Promise<PagedResponse<LeadDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<LeadDto>>>(`/leads/assigned/${userId}`, { params })
    return response.data.data
  },

  getUnassigned: async (params?: PaginationParams): Promise<PagedResponse<LeadDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<LeadDto>>>('/leads/unassigned', { params })
    return response.data.data
  },

  getHighScore: async (minScore: number = 70): Promise<LeadDto[]> => {
    const response = await api.get<ApiResponse<LeadDto[]>>('/leads/high-score', { params: { minScore } })
    return response.data.data
  },

  getStale: async (days: number = 7): Promise<LeadDto[]> => {
    const response = await api.get<ApiResponse<LeadDto[]>>('/leads/stale', { params: { days } })
    return response.data.data
  },

  search: async (query: string, params?: PaginationParams): Promise<PagedResponse<LeadDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<LeadDto>>>('/leads/search', { params: { ...params, query } })
    return response.data.data
  },

  create: async (dto: CreateLeadRequest): Promise<LeadDto> => {
    const response = await api.post<ApiResponse<LeadDto>>('/leads', dto)
    return response.data.data
  },

  update: async (id: string, dto: CreateLeadRequest): Promise<LeadDto> => {
    const response = await api.put<ApiResponse<LeadDto>>(`/leads/${id}`, dto)
    return response.data.data
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/leads/${id}`)
  },

  assign: async (leadId: string, userId: string): Promise<LeadDto> => {
    const response = await api.put<ApiResponse<LeadDto>>(`/leads/${leadId}/assign/${userId}`)
    return response.data.data
  },

  updateStatus: async (leadId: string, action: string, params?: Record<string, string | number>): Promise<LeadDto> => {
    const response = await api.put<ApiResponse<LeadDto>>(`/leads/${leadId}/${action}`, null, { params })
    return response.data.data
  },
}
