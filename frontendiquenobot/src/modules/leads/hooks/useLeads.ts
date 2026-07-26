import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { leadService } from '@/services/lead.service'
import { useDebounce } from '@/hooks/useDebounce'
import { PAGINATION, STALE_TIMES } from '@/config/constants'
import { toast } from 'sonner'

export function useLeads() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const debouncedSearch = useDebounce(search, 400)

  const query = useQuery({
    queryKey: ['leads', page, debouncedSearch, statusFilter],
    queryFn: async () => {
      if (statusFilter) {
        return leadService.getByStatus(statusFilter, { page, size: PAGINATION.DEFAULT_SIZE })
      }
      if (debouncedSearch) {
        return leadService.search(debouncedSearch, { page, size: PAGINATION.DEFAULT_SIZE })
      }
      return leadService.getAll({ page, size: PAGINATION.DEFAULT_SIZE })
    },
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    leads: query.data?.content ?? [],
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    page,
    setPage,
    search,
    setSearch,
    statusFilter,
    setStatusFilter,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useCreateLead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: import('@/types/contact').CreateLeadRequest) => leadService.create(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] })
      toast.success('Lead creado')
    },
    onError: () => toast.error('Error al crear lead'),
  })
}

export function useUpdateLead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: Partial<import('@/types/contact').CreateLeadRequest> }) =>
      leadService.update(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] })
      toast.success('Lead actualizado')
    },
    onError: () => toast.error('Error al actualizar lead'),
  })
}

export function useUpdateLeadStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, action, params }: { id: string; action: string; params?: Record<string, string | number> }) =>
      leadService.updateStatus(id, action, params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] })
      toast.success('Estado actualizado')
    },
    onError: () => toast.error('Error al actualizar estado'),
  })
}

export function useAssignLead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ leadId, userId }: { leadId: string; userId: string }) =>
      leadService.assign(leadId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] })
      toast.success('Lead asignado')
    },
    onError: () => toast.error('Error al asignar lead'),
  })
}

export function useDeleteLead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => leadService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] })
      toast.success('Lead eliminado')
    },
    onError: () => toast.error('Error al eliminar lead'),
  })
}
