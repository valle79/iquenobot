import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { productService } from '@/services/product.service'
import { useDebounce } from '@/hooks/useDebounce'
import { PAGINATION, STALE_TIMES } from '@/config/constants'
import { toast } from 'sonner'

export function useProducts() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const debouncedSearch = useDebounce(search, 400)

  const query = useQuery({
    queryKey: ['products', page, debouncedSearch, statusFilter],
    queryFn: async () => {
      if (statusFilter) {
        return productService.getByStatus(statusFilter, { page, size: PAGINATION.DEFAULT_SIZE })
      }
      if (debouncedSearch) {
        return productService.search(debouncedSearch, { page, size: PAGINATION.DEFAULT_SIZE })
      }
      return productService.getAll({ page, size: PAGINATION.DEFAULT_SIZE })
    },
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    products: query.data?.content ?? [],
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

export function useCreateProduct() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto: import('@/types/product').CreateProductRequest) => productService.create(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      toast.success('Producto creado')
    },
    onError: () => toast.error('Error al crear producto'),
  })
}

export function useUpdateProduct() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: Partial<import('@/types/product').CreateProductRequest> }) =>
      productService.update(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      toast.success('Producto actualizado')
    },
    onError: () => toast.error('Error al actualizar producto'),
  })
}

export function useDeleteProduct() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => productService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      toast.success('Producto eliminado')
    },
    onError: () => toast.error('Error al eliminar producto'),
  })
}

export function useAdjustStock() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, action, quantity }: { id: string; action: 'increase' | 'decrease'; quantity: number }) =>
      productService.adjustStock(id, action, quantity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      toast.success('Stock actualizado')
    },
    onError: () => toast.error('Error al ajustar stock'),
  })
}
