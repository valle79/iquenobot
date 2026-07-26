import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryService } from '@/services/product.service'
import type { CreateCategoryRequest } from '@/types/product'
import { toast } from 'sonner'
import { useState } from 'react'
import { STALE_TIMES } from '@/config/constants'

export function useCategories() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['categories', page, search],
    queryFn: () =>
      search
        ? categoryService.search(search, { page, size: 20 })
        : categoryService.getAll({ page, size: 20 }),
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    categories: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page,
    setPage,
    search,
    setSearch,
    isLoading,
  }
}

export function useCategoryMutations() {
  const queryClient = useQueryClient()

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['categories'] })

  const createMutation = useMutation({
    mutationFn: (dto: CreateCategoryRequest) => categoryService.create(dto),
    onSuccess: () => { invalidate(); toast.success('Categoría creada') },
    onError: () => toast.error('Error al crear categoría'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: CreateCategoryRequest }) =>
      categoryService.update(id, dto),
    onSuccess: () => { invalidate(); toast.success('Categoría actualizada') },
    onError: () => toast.error('Error al actualizar categoría'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => categoryService.delete(id),
    onSuccess: () => { invalidate(); toast.success('Categoría eliminada') },
    onError: () => toast.error('Error al eliminar categoría'),
  })

  return { createMutation, updateMutation, deleteMutation }
}

export function useActiveCategories() {
  return useQuery({
    queryKey: ['categories', 'active'],
    queryFn: () => categoryService.getActive(),
    staleTime: STALE_TIMES.HIGH,
  })
}
