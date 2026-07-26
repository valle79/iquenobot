import { api } from '@/core/api/client'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export class BaseService<T, CreateDTO = Partial<T>, UpdateDTO = Partial<T>> {
  constructor(protected endpoint: string) {}

  async getAll(params?: PaginationParams): Promise<PagedResponse<T>> {
    const response = await api.get<ApiResponse<PagedResponse<T>>>(this.endpoint, { params })
    return response.data.data
  }

  async getById(id: string): Promise<T> {
    const response = await api.get<ApiResponse<T>>(`${this.endpoint}/${id}`)
    return response.data.data
  }

  async getByPath(path: string, params?: PaginationParams): Promise<PagedResponse<T>> {
    const response = await api.get<ApiResponse<PagedResponse<T>>>(`${this.endpoint}/${path}`, { params })
    return response.data.data
  }

  async create(dto: CreateDTO): Promise<T> {
    const response = await api.post<ApiResponse<T>>(this.endpoint, dto)
    return response.data.data
  }

  async update(id: string, dto: UpdateDTO): Promise<T> {
    const response = await api.put<ApiResponse<T>>(`${this.endpoint}/${id}`, dto)
    return response.data.data
  }

  async delete(id: string): Promise<void> {
    await api.delete(`${this.endpoint}/${id}`)
  }

  async action(path: string, params?: Record<string, string | number>): Promise<void> {
    await api.put(`${this.endpoint}/${path}`, null, { params })
  }
}
