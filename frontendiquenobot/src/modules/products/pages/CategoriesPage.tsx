import { useState } from 'react'
import { Plus, MoreHorizontal, Trash2, Pencil, ArrowUpDown } from 'lucide-react'
import { useCategories, useCategoryMutations } from '../hooks/useCategories'
import { CategoryFormModal } from '../components/CategoryFormModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { CategoryDto } from '@/types/product'

export default function CategoriesPage() {
  const {
    categories, totalElements, totalPages, page, setPage,
    search, setSearch, isLoading,
  } = useCategories()
  const { createMutation, updateMutation, deleteMutation } = useCategoryMutations()
  const [deleteTarget, setDeleteTarget] = useState<CategoryDto | null>(null)
  const [formTarget, setFormTarget] = useState<CategoryDto | null>(null)
  const [formOpen, setFormOpen] = useState(false)

  const columns: ColumnDef<CategoryDto>[] = [
    { header: 'Nombre', accessorKey: 'name' },
    {
      header: 'Descripción',
      accessorKey: 'description',
      cell: ({ row }) => (
        <span className="text-sm text-gray-500 line-clamp-1">{row.original.description || '—'}</span>
      ),
    },
    {
      header: 'Orden',
      accessorKey: 'displayOrder',
      cell: ({ row }) => (
        <span className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
          <ArrowUpDown size={14} /> {row.original.displayOrder}
        </span>
      ),
    },
    {
      header: 'Productos',
      accessorKey: 'productCount',
      cell: ({ row }) => (
        <span className="text-sm font-medium">{row.original.productCount}</span>
      ),
    },
    {
      header: 'Activo',
      accessorKey: 'active',
      cell: ({ row }) => (
        <Badge variant={row.original.active ? 'success' : 'neutral'} size="sm">
          {row.original.active ? 'Sí' : 'No'}
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
        const cat = row.original
        return (
          <Dropdown
            open={menuOpen} onOpenChange={setMenuOpen} align="end"
            trigger={
              <Button variant="ghost" size="sm" icon><MoreHorizontal size={16} /></Button>
            }
            items={[
              { label: 'Editar', icon: Pencil, onClick: () => { setFormTarget(cat); setFormOpen(true) } },
              { type: 'separator' },
              { label: 'Eliminar', icon: Trash2, danger: true as const, onClick: () => setDeleteTarget(cat) },
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
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Categorías</h1>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona las categorías de productos ({totalElements} total)
          </p>
        </div>
        <Button onClick={() => { setFormTarget(null); setFormOpen(true) }}>
          <Plus size={18} />
          Nueva categoría
        </Button>
      </div>

      <div className="flex items-center gap-3">
        <SearchBar
          placeholder="Buscar categorías..."
          value={search}
          onSearch={setSearch}
          className="max-w-xs"
        />
      </div>

      <DataTable
        columns={columns}
        data={categories}
        loading={isLoading}
        pageCount={totalPages}
        pageIndex={page}
        onPageChange={setPage}
        totalRecords={totalElements}
        emptyMessage="No se encontraron categorías"
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id)
          setDeleteTarget(null)
        }}
        title="Eliminar categoría"
        message={`¿Eliminar "${deleteTarget?.name}" permanentemente?`}
        confirmLabel="Eliminar"
        loading={deleteMutation.isPending}
      />

      <CategoryFormModal open={formOpen} category={formTarget} onClose={() => { setFormOpen(false); setFormTarget(null) }} />
    </div>
  )
}
