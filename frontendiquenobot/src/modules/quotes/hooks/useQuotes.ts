import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { quoteService } from '@/services/quote.service'
import { useDebounce } from '@/hooks/useDebounce'
import { PAGINATION, STALE_TIMES } from '@/config/constants'
import { toast } from 'sonner'
import type { QuoteFilters } from '@/types/quote'

export function useQuotes() {
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState<QuoteFilters>({})
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search, 400)

  const query = useQuery({
    queryKey: ['quotes', page, debouncedSearch, filters],
    queryFn: () =>
      quoteService.getAll(
        { ...filters, query: debouncedSearch || undefined },
        { page, size: PAGINATION.DEFAULT_SIZE },
      ),
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    quotes: query.data?.content ?? [],
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    page,
    setPage,
    search,
    setSearch,
    filters,
    setFilters: (next: QuoteFilters) => {
      setPage(0)
      setFilters(next)
    },
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useResendQuote() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, phone }: { id: string; phone?: string }) =>
      phone ? quoteService.resendTo(id, phone) : quoteService.resend(id),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['quotes'] })
      if (result.success) {
        toast.success(result.message ?? 'Cotización reenviada')
      } else {
        toast.error(result.message ?? 'No se pudo reenviar la cotización')
      }
    },
    onError: (error: unknown) => {
      const message =
        typeof (error as { response?: { data?: { message?: string } } })?.response?.data?.message === 'string'
          ? (error as { response: { data: { message: string } } }).response.data.message
          : 'Error al reenviar la cotización'
      toast.error(message)
    },
  })
}

export function useRegenerateQuote() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => quoteService.regenerate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quotes'] })
      toast.success('PDF regenerado exitosamente')
    },
    onError: () => toast.error('Error al regenerar el PDF'),
  })
}

export function useCancelQuote() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => quoteService.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quotes'] })
      toast.success('Cotización anulada exitosamente')
    },
    onError: () => toast.error('Error al anular la cotización'),
  })
}
