import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Plus, Search, MoreHorizontal, UserPlus, TrendingUp, Target, Trash2, Pencil } from 'lucide-react'
import { useLeads, useDeleteLead, useUpdateLeadStatus } from '../hooks/useLeads'
import { LeadFormModal } from '../components/LeadFormModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { LeadDto } from '@/types/contact'

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'info' | 'neutral'> = {
  NEW: 'info',
  CONTACTED: 'warning',
  QUALIFIED: 'success',
  CONVERTED: 'success',
  LOST: 'error',
  DISQUALIFIED: 'neutral',
}

const statusLabels: Record<string, string> = {
  NEW: 'Nuevo',
  CONTACTED: 'Contactado',
  QUALIFIED: 'Calificado',
  CONVERTED: 'Convertido',
  LOST: 'Perdido',
  DISQUALIFIED: 'Descalificado',
}

export default function LeadsPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const {
    leads, totalElements, totalPages, page, setPage,
    search, setSearch, statusFilter, setStatusFilter,
    isLoading, refetch,
  } = useLeads()
  const deleteMutation = useDeleteLead()
  const updateStatus = useUpdateLeadStatus()
  const [deleteTarget, setDeleteTarget] = useState<LeadDto | null>(null)
  const [formTarget, setFormTarget] = useState<LeadDto | null>(null)
  const [formOpen, setFormOpen] = useState(false)

  const columns: ColumnDef<LeadDto>[] = [
    {
      header: 'Lead',
      accessorKey: 'title',
      cell: ({ row }) => {
        const l = row.original
        return (
          <div className="flex items-center gap-3">
            <Avatar name={l.contact?.fullName || l.title} src={l.contact?.avatarUrl} size="sm" />
            <div>
              <p className="font-medium text-gray-900 dark:text-gray-100">{l.title}</p>
              <p className="text-xs text-gray-500">{l.contact?.fullName || 'Sin contacto'}</p>
            </div>
          </div>
        )
      },
    },
    {
      header: 'Score',
      accessorKey: 'score',
      cell: ({ row }) => {
        const score = row.original.score ?? 0
        return (
          <div className="flex items-center gap-2">
            <div className="h-2 w-16 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
              <div
                className={`h-full rounded-full transition-all ${
                  score >= 70 ? 'bg-green-500' : score >= 40 ? 'bg-yellow-500' : 'bg-red-500'
                }`}
                style={{ width: `${score}%` }}
              />
            </div>
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{score}</span>
          </div>
        )
      },
    },
    {
      header: 'Estado',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge variant={statusColors[row.original.status] ?? 'neutral'} size="sm">
          {statusLabels[row.original.status] ?? row.original.status}
        </Badge>
      ),
    },
    {
      header: 'Fuente',
      accessorKey: 'source',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">{row.original.source}</span>
      ),
    },
    {
      header: 'Valor est.',
      accessorKey: 'estimatedValue',
      cell: ({ row }) => (
        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
          ${row.original.estimatedValue?.toLocaleString() ?? '0'}
        </span>
      ),
    },
    {
      header: 'Asignado',
      accessorKey: 'assignedTo',
      cell: ({ row }) => {
        const assigned = row.original.assignedTo
        return assigned ? (
          <div className="flex items-center gap-2">
            <Avatar name={assigned.fullName} src={assigned.avatarUrl} size="xs" />
            <span className="text-sm text-gray-600 dark:text-gray-400">{assigned.fullName}</span>
          </div>
        ) : (
          <Badge variant="warning" size="sm">Sin asignar</Badge>
        )
      },
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
        const lead = row.original

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
              { label: 'Ver detalle', icon: Search, onClick: () => navigate(`/leads/${lead.id}`) },
              { label: 'Editar', icon: Pencil, onClick: () => { setFormTarget(lead); setFormOpen(true) } },
              { label: 'Asignar', icon: UserPlus, onClick: () => navigate(`/leads/${lead.id}`) },
              { type: 'separator' },
              ...(lead.status === 'NEW'
                ? [{ label: 'Contactado', icon: Target, onClick: () => updateStatus.mutate({ id: lead.id, action: 'contacted' }) }]
                : []),
              ...(lead.status === 'CONTACTED'
                ? [{ label: 'Calificar', icon: TrendingUp, onClick: () => updateStatus.mutate({ id: lead.id, action: 'qualified', params: { score: 70 } }) }]
                : []),
              { type: 'separator' },
              { label: 'Eliminar', icon: Trash2, danger: true as const, onClick: () => setDeleteTarget(lead) },
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
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.leads')}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona tus oportunidades de venta ({totalElements} total)
          </p>
        </div>
        <Button onClick={() => { setFormTarget(null); setFormOpen(true) }}>
          <Plus size={18} />
          Nuevo lead
        </Button>
      </div>

      <div className="flex items-center gap-3">
        <SearchBar
          placeholder="Buscar leads..."
          value={search}
          onSearch={setSearch}
          className="max-w-xs"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="h-9 rounded-lg border border-gray-200 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        >
          <option value="">Todos los estados</option>
          <option value="NEW">Nuevos</option>
          <option value="CONTACTED">Contactados</option>
          <option value="QUALIFIED">Calificados</option>
          <option value="CONVERTED">Convertidos</option>
          <option value="LOST">Perdidos</option>
          <option value="DISQUALIFIED">Descalificados</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        data={leads}
        loading={isLoading}
        pageCount={totalPages}
        pageIndex={page}
        onPageChange={setPage}
        totalRecords={totalElements}
        emptyMessage="No se encontraron leads"
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id)
          setDeleteTarget(null)
        }}
        title="Eliminar lead"
        message={`¿Estás seguro de eliminar "${deleteTarget?.title}"?`}
        confirmLabel="Eliminar"
        loading={deleteMutation.isPending}
      />

      <LeadFormModal open={formOpen} lead={formTarget} onClose={() => { setFormOpen(false); setFormTarget(null) }} />
    </div>
  )
}
