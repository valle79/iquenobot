import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Plus, Search, MoreHorizontal, Ban, CheckCircle, Trash2, MessageCircle, Pencil, Upload } from 'lucide-react'
import { useContacts, useBlockContact, useUnblockContact, useDeleteContact } from '../hooks/useContacts'
import { ContactFormModal } from '../components/ContactFormModal'
import { ImportContactsModal } from '../components/ImportContactsModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { ContactDto } from '@/types/contact'

export default function ContactsPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const {
    contacts, totalElements, totalPages, page, setPage,
    search, setSearch, statusFilter, setStatusFilter,
    isLoading, refetch,
  } = useContacts()
  const blockMutation = useBlockContact()
  const unblockMutation = useUnblockContact()
  const deleteMutation = useDeleteContact()

  const [deleteTarget, setDeleteTarget] = useState<ContactDto | null>(null)
  const [blockTarget, setBlockTarget] = useState<ContactDto | null>(null)
  const [formTarget, setFormTarget] = useState<ContactDto | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [importOpen, setImportOpen] = useState(false)

  const statusOptions = [
    { value: '', label: 'Todos los estados' },
    { value: 'ACTIVE', label: 'Activos' },
    { value: 'INACTIVE', label: 'Inactivos' },
    { value: 'BLOCKED', label: 'Bloqueados' },
    { value: 'ARCHIVED', label: 'Archivados' },
  ]

  const columns: ColumnDef<ContactDto>[] = [
    {
      header: 'Contacto',
      accessorKey: 'fullName',
      cell: ({ row }) => {
        const c = row.original
        return (
          <div className="flex items-center gap-3">
            <Avatar name={c.fullName} src={c.avatarUrl} size="sm" />
            <div>
              <p className="font-medium text-gray-900 dark:text-gray-100">{c.displayName || c.fullName}</p>
              <p className="text-xs text-gray-500">{c.email || c.phone}</p>
            </div>
          </div>
        )
      },
    },
    {
      header: 'Teléfono',
      accessorKey: 'phone',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">{row.original.phone || '—'}</span>
      ),
    },
    {
      header: 'Email',
      accessorKey: 'email',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">{row.original.email || '—'}</span>
      ),
    },
    {
      header: 'Empresa',
      accessorKey: 'company',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">{row.original.company || '—'}</span>
      ),
    },
    {
      header: 'Estado',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge
          variant={
            row.original.status === 'ACTIVE' ? 'success'
            : row.original.status === 'BLOCKED' ? 'error'
            : row.original.status === 'ARCHIVED' ? 'neutral'
            : 'warning'
          }
          size="sm"
        >
          {row.original.status === 'ACTIVE' ? 'Activo'
           : row.original.status === 'BLOCKED' ? 'Bloqueado'
           : row.original.status === 'ARCHIVED' ? 'Archivado'
           : 'Inactivo'}
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
        const contact = row.original

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
              {
                label: 'Ver detalle',
                icon: Search,
                onClick: () => navigate(`/contacts/${contact.id}`),
              },
              {
                label: 'Editar',
                icon: Pencil,
                onClick: () => { setFormTarget(contact); setFormOpen(true) },
              },
              {
                label: 'Iniciar conversación',
                icon: MessageCircle,
                onClick: () => navigate(`/conversations/new?contactId=${contact.id}`),
              },
              { type: 'separator' },
              ...(contact.status !== 'BLOCKED'
                ? [{ label: 'Bloquear', icon: Ban, danger: true as const, onClick: () => setBlockTarget(contact) }]
                : [{ label: 'Desbloquear', icon: CheckCircle, onClick: () => unblockMutation.mutate(contact.id) }]),
              { type: 'separator' },
              {
                label: 'Eliminar',
                icon: Trash2,
                danger: true,
                onClick: () => setDeleteTarget(contact),
              },
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
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.contacts')}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona tus contactos ({totalElements} total)
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setImportOpen(true)}>
            <Upload size={18} />
            Importar CSV
          </Button>
          <Button onClick={() => { setFormTarget(null); setFormOpen(true) }}>
            <Plus size={18} />
            Nuevo contacto
          </Button>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <SearchBar
          placeholder="Buscar contactos..."
          value={search}
          onSearch={setSearch}
          className="max-w-xs"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="h-9 rounded-lg border border-gray-200 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
        >
          {statusOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </div>

      <DataTable
        columns={columns}
        data={contacts}
        loading={isLoading}
        pageCount={totalPages}
        pageIndex={page}
        onPageChange={setPage}
        totalRecords={totalElements}
        emptyMessage="No se encontraron contactos"
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id)
          setDeleteTarget(null)
        }}
        title="Eliminar contacto"
        message={`¿Estás seguro de eliminar a ${deleteTarget?.displayName || deleteTarget?.fullName}? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar"
        loading={deleteMutation.isPending}
      />

      <ConfirmDialog
        open={!!blockTarget}
        onClose={() => setBlockTarget(null)}
        onConfirm={() => {
          if (blockTarget) blockMutation.mutate({ id: blockTarget.id })
          setBlockTarget(null)
        }}
        title="Bloquear contacto"
        message={`¿Bloquear a ${blockTarget?.displayName || blockTarget?.fullName}? No podrá recibir mensajes.`}
        confirmLabel="Bloquear"
        loading={blockMutation.isPending}
      />

      <ContactFormModal open={formOpen} contact={formTarget} onClose={() => { setFormOpen(false); setFormTarget(null) }} />
      <ImportContactsModal open={importOpen} onClose={() => setImportOpen(false)} />
    </div>
  )
}
