import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { roleService } from '@/services/role.service'
import { PAGINATION, STALE_TIMES } from '@/config/constants'
import { toast } from 'sonner'
import type { CreateRoleRequest, UpdateRoleRequest } from '@/types/role'

export function useRoles() {
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['roles', page],
    queryFn: () => roleService.getAll({ page, size: PAGINATION.DEFAULT_SIZE }),
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    roles: query.data?.content ?? [],
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    page,
    setPage,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useRole(id: string | undefined) {
  return useQuery({
    queryKey: ['role', id],
    queryFn: () => roleService.getById(id!),
    enabled: !!id,
  })
}

export function useCreateRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: CreateRoleRequest) => roleService.create(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      toast.success('Rol creado exitosamente')
    },
  })
}

export function useUpdateRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: UpdateRoleRequest }) => roleService.update(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      toast.success('Rol actualizado exitosamente')
    },
  })
}

export function useDeleteRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => roleService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      toast.success('Rol eliminado exitosamente')
    },
  })
}
