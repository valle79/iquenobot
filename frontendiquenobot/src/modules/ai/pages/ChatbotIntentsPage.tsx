import { useState } from 'react'
import { Plus, MoreHorizontal, Trash2, Pencil, Brain } from 'lucide-react'
import { useChatbotIntents, useChatbotIntentMutations } from '../hooks/useChatbotIntents'
import { IntentsFormModal } from '../components/IntentsFormModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { ChatbotIntentDto } from '@/types/chatbot'

export default function ChatbotIntentsPage() {
  const { intents, totalElements, totalPages, page, setPage, search, setSearch, isLoading } = useChatbotIntents()
  const { createMutation, updateMutation, deleteMutation } = useChatbotIntentMutations()
  const [deleteTarget, setDeleteTarget] = useState<ChatbotIntentDto | null>(null)
  const [formTarget, setFormTarget] = useState<ChatbotIntentDto | null>(null)
  const [formOpen, setFormOpen] = useState(false)

  const columns: ColumnDef<ChatbotIntentDto>[] = [
    {
      header: 'Nombre',
      accessorKey: 'intentName',
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Brain size={16} className="text-brand-500" />
          <span className="font-medium text-gray-900 dark:text-gray-100">{row.original.intentName}</span>
        </div>
      ),
    },
    {
      header: 'Descripción',
      accessorKey: 'description',
      cell: ({ row }) => (
        <span className="max-w-[200px] truncate text-sm text-gray-500">
          {row.original.description || '—'}
        </span>
      ),
    },
    {
      header: 'Frases de ejemplo',
      accessorKey: 'trainingPhrases',
      cell: ({ row }) => {
        const phrases = (() => { try { const a = JSON.parse(row.original.trainingPhrases); return Array.isArray(a) ? a : []; } catch { return []; } })()
        return (
          <span className="max-w-[200px] truncate text-sm text-gray-500">
            {phrases.slice(0, 3).join(', ')}{phrases.length > 3 ? '...' : ''}
          </span>
        )
      },
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
          <Brain size={24} className="text-brand-500" />
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Auto-Respuestas</h1>
            <p className="mt-1 text-sm text-gray-500">Cuando el cliente escriba algo relacionado, el bot responde automáticamente ({totalElements} total)</p>
          </div>
        </div>
        <Button onClick={() => { setFormTarget(null); setFormOpen(true) }}>
          <Plus size={18} /> Nueva auto-respuesta
        </Button>
      </div>

      <SearchBar placeholder="Buscar auto-respuestas..." value={search} onSearch={setSearch} className="max-w-xs" />

      <DataTable
        columns={columns} data={intents} loading={isLoading}
        pageCount={totalPages} pageIndex={page} onPageChange={setPage}
        totalRecords={totalElements} emptyMessage="No hay auto-respuestas. Crea una para que el bot responda automáticamente."
      />

      <ConfirmDialog
        open={!!deleteTarget} onClose={() => setDeleteTarget(null)}
        onConfirm={() => { if (deleteTarget) deleteMutation.mutate(deleteTarget.id); setDeleteTarget(null) }}
        title="Eliminar auto-respuesta"
        message={`¿Eliminar "${deleteTarget?.intentName}" permanentemente?`}
        confirmLabel="Eliminar" loading={deleteMutation.isPending}
      />

      <IntentsFormModal open={formOpen} intent={formTarget} onClose={() => { setFormOpen(false); setFormTarget(null) }} />
    </div>
  )
}
