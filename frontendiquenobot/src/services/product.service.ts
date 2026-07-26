import { api } from '@/core/api/client'
import type { ProductDto, CreateProductRequest, CategoryDto, CreateCategoryRequest } from '@/types/product'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'

export const productService = {
  getAll: async (params?: PaginationParams): Promise<PagedResponse<ProductDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<ProductDto>>>('/products', { params })
    return response.data.data
  },

  getById: async (id: string): Promise<ProductDto> => {
    const response = await api.get<ApiResponse<ProductDto>>(`/products/${id}`)
    return response.data.data
  },

  search: async (query: string, params?: PaginationParams): Promise<PagedResponse<ProductDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<ProductDto>>>('/products/search', { params: { ...params, query } })
    return response.data.data
  },

  getByStatus: async (status: string, params?: PaginationParams): Promise<PagedResponse<ProductDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<ProductDto>>>(`/products/status/${status}`, { params })
    return response.data.data
  },

  getByCategory: async (categoryId: string, params?: PaginationParams): Promise<PagedResponse<ProductDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<ProductDto>>>(`/products/category/${categoryId}`, { params })
    return response.data.data
  },

  getFeatured: async (): Promise<ProductDto[]> => {
    const response = await api.get<ApiResponse<ProductDto[]>>('/products/featured')
    return response.data.data
  },

  getLowStock: async (): Promise<ProductDto[]> => {
    const response = await api.get<ApiResponse<ProductDto[]>>('/products/low-stock')
    return response.data.data
  },

  create: async (dto: CreateProductRequest): Promise<ProductDto> => {
    const response = await api.post<ApiResponse<ProductDto>>('/products', dto)
    return response.data.data
  },

  update: async (id: string, dto: CreateProductRequest): Promise<ProductDto> => {
    const response = await api.put<ApiResponse<ProductDto>>(`/products/${id}`, dto)
    return response.data.data
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/products/${id}`)
  },

  adjustStock: async (id: string, action: 'increase' | 'decrease', quantity: number): Promise<ProductDto> => {
    const response = await api.put<ApiResponse<ProductDto>>(`/products/${id}/stock/${action}`, null, { params: { quantity } })
    return response.data.data
  },
}

export const categoryService = {
  getAll: async (params?: PaginationParams): Promise<PagedResponse<CategoryDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<CategoryDto>>>('/categories', { params })
    return response.data.data
  },

  getById: async (id: string): Promise<CategoryDto> => {
    const response = await api.get<ApiResponse<CategoryDto>>(`/categories/${id}`)
    return response.data.data
  },

  getActive: async (): Promise<CategoryDto[]> => {
    const response = await api.get<ApiResponse<CategoryDto[]>>('/categories/active')
    return response.data.data
  },

  search: async (query: string, params?: PaginationParams): Promise<PagedResponse<CategoryDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<CategoryDto>>>('/categories/search', { params: { ...params, query } })
    return response.data.data
  },

  create: async (dto: CreateCategoryRequest): Promise<CategoryDto> => {
    const response = await api.post<ApiResponse<CategoryDto>>('/categories', dto)
    return response.data.data
  },

  update: async (id: string, dto: CreateCategoryRequest): Promise<CategoryDto> => {
    const response = await api.put<ApiResponse<CategoryDto>>(`/categories/${id}`, dto)
    return response.data.data
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/categories/${id}`)
  },
}
