import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'
import { adminService } from '@/services/admin.service'
import { useDebounce } from '@/hooks/useDebounce'
import type { CreateTenantRequest } from '@/types/auth'

export function useAdminTenants() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const debouncedSearch = useDebounce(search, 400)

  const queryKey = ['admin-tenants', page, debouncedSearch, statusFilter]

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey,
    queryFn: () =>
      adminService.getTenants({
        page,
        size: 20,
        search: debouncedSearch || undefined,
        status: statusFilter || undefined,
      }),
  })

  return {
    tenants: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page,
    setPage,
    search,
    setSearch,
    statusFilter,
    setStatusFilter,
    isLoading,
    isError,
    refetch,
  }
}

export function useSystemStats() {
  return useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => adminService.getSystemStats(),
  })
}

export function useCreateTenant() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateTenantRequest) => adminService.createTenant(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] })
      toast.success('Empresa creada exitosamente')
    },
    onError: () => {
      toast.error('Error al crear la empresa')
    },
  })
}

export function useUpdateTenantStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      adminService.updateTenantStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] })
      toast.success('Estado actualizado exitosamente')
    },
    onError: () => {
      toast.error('Error al actualizar el estado')
    },
  })
}

export function useUpdateTenant() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: Partial<CreateTenantRequest> }) =>
      adminService.updateTenant(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] })
      toast.success('Empresa actualizada exitosamente')
    },
    onError: () => {
      toast.error('Error al actualizar la empresa')
    },
  })
}

export function useDeleteTenant() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => adminService.deleteTenant(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] })
      toast.success('Empresa eliminada exitosamente')
    },
    onError: () => {
      toast.error('Error al eliminar la empresa')
    },
  })
}
