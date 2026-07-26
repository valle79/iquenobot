import { useState } from 'react'
import { Plus, MoreHorizontal, Trash2, Pencil, GitBranch, Zap } from 'lucide-react'
import { useChatbotFlows, useChatbotFlowMutations } from '../hooks/useChatbotFlows'
import { FlowsFormModal } from '../components/FlowsFormModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { ChatbotFlowDto } from '@/types/chatbot'

const triggerColors: Record<string, 'success' | 'warning' | 'info' | 'neutral'> = {
  KEYWORD: 'success',
  PATTERN: 'warning',
  AI: 'info',
  SCHEDULED: 'neutral',
  EVENT: 'neutral',
}

export default function ChatbotFlowsPage() {
  const { flows, totalElements, totalPages, page, setPage, search, setSearch, isLoading } = useChatbotFlows()
  const { createMutation, updateMutation, deleteMutation } = useChatbotFlowMutations()
  const [deleteTarget, setDeleteTarget] = useState<ChatbotFlowDto | null>(null)
  const [formTarget, setFormTarget] = useState<ChatbotFlowDto | null>(null)
  const [formOpen, setFormOpen] = useState(false)

  const columns: ColumnDef<ChatbotFlowDto>[] = [
    {
      header: 'Nombre',
      accessorKey: 'name',
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <GitBranch size={16} className="text-brand-500" />
          <span className="font-medium text-gray-900 dark:text-gray-100">{row.original.name}</span>
        </div>
      ),
    },
    {
      header: 'Disparador',
      accessorKey: 'triggerType',
      cell: ({ row }) => (
        <Badge variant={triggerColors[row.original.triggerType] ?? 'neutral'} size="sm">
          {row.original.triggerType}
        </Badge>
      ),
    },
    {
      header: 'Prioridad',
      accessorKey: 'priority',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">{row.original.priority}</span>
      ),
    },
    {
      header: 'Ejecuciones',
      accessorKey: 'executionCount',
      cell: ({ row }) => (
        <div className="flex gap-3 text-sm">
          <span className="text-green-600">{row.original.successCount} OK</span>
          <span className="text-red-500">{row.original.failureCount} FAIL</span>
        </div>
      ),
    },
    {
      header: 'IA',
      accessorKey: 'useAI',
      cell: ({ row }) => (
        row.original.useAI ? <Badge variant="info" size="sm"><Zap size={12} /> IA</Badge> : <span className="text-sm text-gray-400">—</span>
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
        const item = row.original
        return (
          <Dropdown
            open={menuOpen} onOpenChange={setMenuOpen} align="end"
            trigger={<Button variant="ghost" size="sm" icon><MoreHorizontal size={16} /></Button>}
            items={[
              { label: 'Editar', icon: Pencil, onClick: () => { setFormTarget(item); setFormOpen(true) } },
              { type: 'separator' },
              { label: 'Eliminar', icon: Trash2, danger: true as const, onClick: () => setDeleteTarget(item) },
            ]}
          />
        )
      },
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <GitBranch size={24} className="text-brand-500" />
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Flujos</h1>
            <p className="mt-1 text-sm text-gray-500">Gestiona los flujos del chatbot ({totalElements} total)</p>
          </div>
        </div>
        <Button onClick={() => { setFormTarget(null); setFormOpen(true) }}>
          <Plus size={18} /> Nuevo flujo
        </Button>
      </div>

      <SearchBar placeholder="Buscar flujos..." value={search} onSearch={setSearch} className="max-w-xs" />

      <DataTable
        columns={columns} data={flows} loading={isLoading}
        pageCount={totalPages} pageIndex={page} onPageChange={setPage}
        totalRecords={totalElements} emptyMessage="No se encontraron flujos"
      />

      <ConfirmDialog
        open={!!deleteTarget} onClose={() => setDeleteTarget(null)}
        onConfirm={() => { if (deleteTarget) deleteMutation.mutate(deleteTarget.id); setDeleteTarget(null) }}
        title="Eliminar flujo"
        message={`¿Eliminar "${deleteTarget?.name}" permanentemente?`}
        confirmLabel="Eliminar" loading={deleteMutation.isPending}
      />

      <FlowsFormModal open={formOpen} flow={formTarget} onClose={() => { setFormOpen(false); setFormTarget(null) }} />
    </div>
  )
}
