import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userService } from '@/services/user.service'
import { PAGINATION, STALE_TIMES } from '@/config/constants'
import { useDebounce } from '@/hooks/useDebounce'
import { toast } from 'sonner'
import type { CreateUserRequest, UpdateUserRequest } from '@/types/auth'

function getErrorMessage(error: unknown): string {
  const err = error as { response?: { data?: { message?: string } }; message?: string }
  return err.response?.data?.message ?? err.message ?? 'Error inesperado'
}

export function useUsers() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState<string>('')
  const debouncedSearch = useDebounce(search, 400)

  const query = useQuery({
    queryKey: ['users', page, debouncedSearch],
    queryFn: () => userService.getAll({ page, size: PAGINATION.DEFAULT_SIZE }),
    staleTime: STALE_TIMES.MEDIUM,
  })

  let filtered = query.data?.content ?? []
  if (debouncedSearch) {
    const q = debouncedSearch.toLowerCase()
    filtered = filtered.filter((u) => u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
  }
  if (roleFilter) {
    filtered = filtered.filter((u) => u.role === roleFilter)
  }

  return {
    users: filtered,
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    page,
    setPage,
    search,
    setSearch,
    roleFilter,
    setRoleFilter,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useCreateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: CreateUserRequest) => userService.create(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success('Usuario creado exitosamente')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: UpdateUserRequest }) => userService.update(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success('Usuario actualizado')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
}
