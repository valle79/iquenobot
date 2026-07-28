import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { contactService } from '@/services/contact.service'
import { useDebounce } from '@/hooks/useDebounce'
import { PAGINATION, STALE_TIMES } from '@/config/constants'
import { toast } from 'sonner'

export function useContacts() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const debouncedSearch = useDebounce(search, 400)

  const query = useQuery({
    queryKey: ['contacts', page, debouncedSearch, statusFilter],
    queryFn: async () => {
      if (statusFilter) {
        return contactService.getByStatus(statusFilter, { page, size: PAGINATION.DEFAULT_SIZE })
      }
      if (debouncedSearch) {
        return contactService.search(debouncedSearch, { page, size: PAGINATION.DEFAULT_SIZE })
      }
      return contactService.getAll({ page, size: PAGINATION.DEFAULT_SIZE })
    },
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    contacts: query.data?.content ?? [],
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

export function useCreateContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: import('@/types/contact').CreateContactRequest) => contactService.create(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      toast.success('Contacto creado')
    },
    onError: () => toast.error('Error al crear contacto'),
  })
}

export function useUpdateContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: Partial<import('@/types/contact').CreateContactRequest> }) =>
      contactService.update(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      toast.success('Contacto actualizado')
    },
    onError: () => toast.error('Error al actualizar contacto'),
  })
}

export function useBlockContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) => contactService.block(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      toast.success('Contacto bloqueado')
    },
    onError: () => toast.error('Error al bloquear contacto'),
  })
}

export function useUnblockContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => contactService.unblock(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      toast.success('Contacto desbloqueado')
    },
    onError: () => toast.error('Error al desbloquear contacto'),
  })
}

export function useDeleteContact() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => contactService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      toast.success('Contacto eliminado')
    },
    onError: () => toast.error('Error al eliminar contacto'),
  })
}

export function useImportContacts() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (contacts: Record<string, string>[]) => contactService.importContacts(contacts),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
    },
  })
}
