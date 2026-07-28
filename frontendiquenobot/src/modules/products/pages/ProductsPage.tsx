import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Plus, MoreHorizontal, Package, Trash2, Eye, Pencil, Minus, Plus as PlusIcon, Upload } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { useProducts, useDeleteProduct, useAdjustStock } from '../hooks/useProducts'
import { ProductFormModal } from '../components/ProductFormModal'
import { ImportProductsModal } from '../components/ImportProductsModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { ProductDto } from '@/types/product'

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'neutral'> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  OUT_OF_STOCK: 'error',
  DISCONTINUED: 'warning',
}

export default function ProductsPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const {
    products, totalElements, totalPages, page, setPage,
    search, setSearch, statusFilter, setStatusFilter,
    isLoading,
  } = useProducts()
  const deleteMutation = useDeleteProduct()
  const adjustStockMutation = useAdjustStock()
  const [deleteTarget, setDeleteTarget] = useState<ProductDto | null>(null)
  const [formTarget, setFormTarget] = useState<ProductDto | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [importOpen, setImportOpen] = useState(false)

  const columns: ColumnDef<ProductDto>[] = [
    {
      header: 'Producto',
      accessorKey: 'name',
      cell: ({ row }) => {
        const p = row.original
        return (
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
              {p.imageUrl ? (
                <img src={p.imageUrl} alt={p.name} className="h-10 w-10 rounded-lg object-cover" />
              ) : (
                <Package size={18} className="text-gray-400" />
              )}
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-gray-100">{p.name}</p>
              <p className="text-xs text-gray-500">{p.sku || 'Sin SKU'}</p>
            </div>
          </div>
        )
      },
    },
    {
      header: 'Precio',
      accessorKey: 'price',
      cell: ({ row }) => (
        <div>
          <p className="font-medium text-gray-900 dark:text-gray-100">
            ${row.original.price.toLocaleString()}
          </p>
          {row.original.compareAtPrice && row.original.compareAtPrice > row.original.price && (
            <p className="text-xs text-gray-400 line-through">
              ${row.original.compareAtPrice.toLocaleString()}
            </p>
          )}
        </div>
      ),
    },
    {
      header: 'Stock',
      accessorKey: 'stockQuantity',
      cell: ({ row }) => {
        const product = row.original
        const qty = product.stockQuantity
        const threshold = product.lowStockThreshold ?? 5
        const isLow = qty <= threshold
        return (
          <div className="flex items-center gap-1">
            <button
              onClick={() => adjustStockMutation.mutate({ id: product.id, action: 'decrease', quantity: 1 })}
              disabled={qty <= 0 || adjustStockMutation.isPending}
              className="flex h-6 w-6 items-center justify-center rounded border border-gray-200 text-gray-500 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-30 dark:border-gray-600 dark:hover:bg-gray-800"
            >
              <Minus size={12} />
            </button>
            <span className={`mx-1 min-w-[24px] text-center text-sm font-medium tabular-nums ${isLow ? 'text-red-600' : 'text-gray-700 dark:text-gray-300'}`}>
              {qty}
            </span>
            <button
              onClick={() => adjustStockMutation.mutate({ id: product.id, action: 'increase', quantity: 1 })}
              disabled={adjustStockMutation.isPending}
              className="flex h-6 w-6 items-center justify-center rounded border border-gray-200 text-gray-500 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-30 dark:border-gray-600 dark:hover:bg-gray-800"
            >
              <PlusIcon size={12} />
            </button>
          </div>
        )
      },
    },
    {
      header: 'Categoría',
      accessorKey: 'category',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">
          {row.original.category?.name || 'Sin categoría'}
        </span>
      ),
    },
    {
      header: 'Estado',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge variant={statusColors[row.original.status] ?? 'neutral'} size="sm">
          {row.original.status === 'ACTIVE' ? 'Activo'
           : row.original.status === 'INACTIVE' ? 'Inactivo'
           : row.original.status === 'OUT_OF_STOCK' ? 'Sin stock'
           : 'Descontinuado'}
        </Badge>
      ),
    },
    {
      header: 'Creado',
      accessorKey: 'createdAt',
      cell: ({ row }) => (
        <span className="text-sm text-gray-500">{dayjs(row.original.createdAt).format('DD/MM/YYYY')}</span>
      ),
    },
    {
      header: 'Acciones',
      id: 'actions',
      cell: ({ row }) => {
        const [menuOpen, setMenuOpen] = useState(false)
        const product = row.original

        return (
          <Dropdown
            open={menuOpen}
            onOpenChange={setMenuOpen}
            align="end"
            trigger={
              <Button variant="ghost" size="sm" icon>
                <MoreHorizontal size={16} />
              </Button>
            }
            items={[
              { label: 'Ver detalle', icon: Eye, onClick: () => navigate(`/products/${product.id}`) },
              { label: 'Editar', icon: Pencil, onClick: () => { setFormTarget(product); setFormOpen(true) } },
              { type: 'separator' },
              { label: 'Eliminar', icon: Trash2, danger: true as const, onClick: () => setDeleteTarget(product) },
            ]}
          />
        )
      },
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.products')}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona tu catálogo de productos ({totalElements} total)
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setImportOpen(true)}>
            <Upload size={18} />
            Importar
          </Button>
          <Button onClick={() => { setFormTarget(null); setFormOpen(true) }}>
            <Plus size={18} />
            Nuevo producto
          </Button>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <SearchBar
          placeholder="Buscar productos..."
          value={search}
          onSearch={setSearch}
          className="max-w-xs"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="h-9 rounded-lg border border-gray-200 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        >
          <option value="">Todos</option>
          <option value="ACTIVE">Activos</option>
          <option value="INACTIVE">Inactivos</option>
          <option value="OUT_OF_STOCK">Sin stock</option>
          <option value="DISCONTINUED">Descontinuados</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        data={products}
        loading={isLoading}
        pageCount={totalPages}
        pageIndex={page}
        onPageChange={setPage}
        totalRecords={totalElements}
        emptyMessage="No se encontraron productos"
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id)
          setDeleteTarget(null)
        }}
        title="Eliminar producto"
        message={`¿Eliminar "${deleteTarget?.name}" permanentemente?`}
        confirmLabel="Eliminar"
        loading={deleteMutation.isPending}
      />

      <ProductFormModal open={formOpen} product={formTarget} onClose={() => { setFormOpen(false); setFormTarget(null) }} />
      <ImportProductsModal open={importOpen} onClose={() => { setImportOpen(false); queryClient.invalidateQueries({ queryKey: ['products'] }) }} />
    </div>
  )
}
