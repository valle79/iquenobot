import { api } from '@/core/api/client'
import type { ApiResponse, PagedResponse, PaginationParams } from '@/types/api'
import type {
  QuoteDetailDto,
  QuoteFilters,
  QuoteHistoryItem,
  QuoteResendResult,
  QuoteSummaryDto,
} from '@/types/quote'

export const quoteService = {
  getAll: async (filters: QuoteFilters, params?: PaginationParams): Promise<PagedResponse<QuoteSummaryDto>> => {
    const response = await api.get<ApiResponse<PagedResponse<QuoteSummaryDto>>>('/quotes', {
      params: { ...params, ...cleanFilters(filters) },
    })
    return response.data.data
  },

  getById: async (id: string): Promise<QuoteDetailDto> => {
    const response = await api.get<ApiResponse<QuoteDetailDto>>(`/quotes/${id}`)
    return response.data.data
  },

  getHistory: async (id: string): Promise<QuoteHistoryItem[]> => {
    const response = await api.get<ApiResponse<QuoteHistoryItem[]>>(`/quotes/${id}/history`)
    return response.data.data
  },

  download: async (id: string, filename?: string | null): Promise<void> => {
    const response = await api.get<Blob>(`/quotes/${id}/download`, { responseType: 'blob' })
    const disposition = response.headers['content-disposition'] as string | undefined
    let name = filename ?? `cotizacion.pdf`
    if (disposition) {
      const match = disposition.match(/filename="?([^";]+)"?/i)
      if (match?.[1]) name = match[1]
    }
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  },

  resend: async (id: string): Promise<QuoteResendResult> => {
    const response = await api.post<ApiResponse<QuoteResendResult>>(`/quotes/${id}/resend`)
    return response.data.data
  },

  resendTo: async (id: string, phone: string): Promise<QuoteResendResult> => {
    const response = await api.post<ApiResponse<QuoteResendResult>>(`/quotes/${id}/resend-to`, { phone })
    return response.data.data
  },

  regenerate: async (id: string): Promise<QuoteDetailDto> => {
    const response = await api.post<ApiResponse<QuoteDetailDto>>(`/quotes/${id}/regenerate`)
    return response.data.data
  },

  cancel: async (id: string): Promise<void> => {
    await api.delete(`/quotes/${id}`)
  },
}

function cleanFilters(filters: QuoteFilters): Record<string, unknown> {
  const clean: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      clean[key] = value
    }
  }
  return clean
}
