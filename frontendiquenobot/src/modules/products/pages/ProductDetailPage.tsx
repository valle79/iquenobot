import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Package, Edit, Trash2, Tag, Calendar, DollarSign, PackageSearch } from 'lucide-react'
import { productService } from '@/services/product.service'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { dayjs } from '@/config/dayjs'
import { STALE_TIMES } from '@/config/constants'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productService.getById(id!),
    enabled: !!id,
    staleTime: STALE_TIMES.MEDIUM,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" onClick={() => navigate('/products')}>
          <ArrowLeft size={18} />
          Volver
        </Button>
        <Skeleton width={300} height={40} />
        <Skeleton width={200} height={20} />
      </div>
    )
  }

  if (!product) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" onClick={() => navigate('/products')}>
          <ArrowLeft size={18} />
          Volver
        </Button>
        <p className="text-gray-500">Producto no encontrado</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Button variant="ghost" onClick={() => navigate('/products')}>
        <ArrowLeft size={18} />
        Volver
      </Button>

      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-gray-100 dark:bg-gray-800">
              {product.imageUrl ? (
                <img src={product.imageUrl} alt={product.name} className="h-16 w-16 rounded-xl object-cover" />
              ) : (
                <Package size={28} className="text-gray-400" />
              )}
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100">{product.name}</h1>
              <p className="text-sm text-gray-500">{product.sku || 'Sin SKU'}</p>
              <div className="mt-2 flex gap-2">
                <Badge variant={product.status === 'ACTIVE' ? 'success' : product.status === 'OUT_OF_STOCK' ? 'error' : 'neutral'} size="sm">
                  {product.status === 'ACTIVE' ? 'Activo' : product.status === 'INACTIVE' ? 'Inactivo' : product.status === 'OUT_OF_STOCK' ? 'Sin stock' : 'Descontinuado'}
                </Badge>
                {product.featured && <Badge variant="info" size="sm">Destacado</Badge>}
                {product.lowStock && <Badge variant="warning" size="sm">Stock bajo</Badge>}
              </div>
            </div>
          </div>

          <div className="flex gap-2">
            <Button variant="outline" size="sm">
              <Edit size={16} />
              Editar
            </Button>
          </div>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-gray-500">Información</h2>
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <DollarSign size={16} className="text-gray-400" />
              <div>
                <p className="text-xs text-gray-500">Precio</p>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  ${product.price.toLocaleString()}
                  {product.compareAtPrice && product.compareAtPrice > product.price && (
                    <span className="ml-2 text-xs text-gray-400 line-through">
                      ${product.compareAtPrice.toLocaleString()}
                    </span>
                  )}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <PackageSearch size={16} className="text-gray-400" />
              <div>
                <p className="text-xs text-gray-500">Stock</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{product.stockQuantity} unidades</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Tag size={16} className="text-gray-400" />
              <div>
                <p className="text-xs text-gray-500">Categoría</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{product.category?.name || 'Sin categoría'}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-gray-500">Fechas</h2>
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <Calendar size={16} className="text-gray-400" />
              <div>
                <p className="text-xs text-gray-500">Creado</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{dayjs(product.createdAt).format('DD/MM/YYYY HH:mm')}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Calendar size={16} className="text-gray-400" />
              <div>
                <p className="text-xs text-gray-500">Actualizado</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{dayjs(product.updatedAt).format('DD/MM/YYYY HH:mm')}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {product.description && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h2 className="mb-2 text-sm font-semibold uppercase tracking-wider text-gray-500">Descripción</h2>
          <p className="text-sm text-gray-700 dark:text-gray-300">{product.description}</p>
        </div>
      )}
    </div>
  )
}
