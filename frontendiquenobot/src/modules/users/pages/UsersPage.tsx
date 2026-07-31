import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Search, Clock, Pencil } from 'lucide-react'
import { useUsers } from '../hooks/useUsers'
import { UserCreateModal } from '../components/UserCreateModal'
import { UserEditModal } from '../components/UserEditModal'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { ErrorState } from '@/shared/molecules/ErrorState'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { UserDto } from '@/types/auth'

const roleColors: Record<string, 'info' | 'success' | 'warning' | 'neutral'> = {
  SUPER_ADMIN: 'info',
  TENANT_ADMIN: 'success',
  SUPERVISOR: 'warning',
  AGENT: 'neutral',
}

const roleOptions = [
  { value: '', label: 'Todos los roles' },
  { value: 'TENANT_ADMIN', label: 'Admin' },
  { value: 'SUPERVISOR', label: 'Supervisor' },
  { value: 'AGENT', label: 'Agente' },
]

function UsersSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Skeleton width={200} height={28} />
          <Skeleton width={250} height={16} className="mt-2" />
        </div>
        <Skeleton width={140} height={40} />
      </div>
      <div className="flex gap-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} width={100} height={32} />
        ))}
      </div>
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} width="100%" height={48} />
        ))}
      </div>
    </div>
  )
}

export default function UsersPage() {
  const { t } = useTranslation()
  const { users, totalElements, totalPages, page, setPage, search, setSearch, roleFilter, setRoleFilter, isLoading, isError, refetch } = useUsers()
  const [createOpen, setCreateOpen] = useState(false)
  const [editUser, setEditUser] = useState<UserDto | null>(null)

  const columns: ColumnDef<UserDto>[] = [
    {
      header: 'Usuario',
      accessorKey: 'fullName',
      cell: ({ row }) => {
        const u = row.original
        return (
          <div className="flex items-center gap-3">
            <Avatar name={u.fullName} src={u.avatarUrl} size="sm" />
            <div>
              <p className="font-medium text-gray-900 dark:text-gray-100">{u.fullName}</p>
              <p className="text-xs text-gray-500">{u.email}</p>
            </div>
          </div>
        )
      },
    },
    {
      header: 'Rol',
      accessorKey: 'role',
      cell: ({ row }) => (
        <Badge variant={roleColors[row.original.role] ?? 'neutral'} size="sm">
          {row.original.role === 'TENANT_ADMIN' ? 'Admin'
           : row.original.role === 'SUPERVISOR' ? 'Supervisor'
           : row.original.role === 'AGENT' ? 'Agente'
           : row.original.role}
        </Badge>
      ),
    },
    {
      header: 'Estado',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge
          variant={row.original.status === 'ACTIVE' ? 'success' : row.original.status === 'LOCKED' ? 'error' : 'neutral'}
          size="sm"
        >
          {row.original.status === 'ACTIVE' ? 'Activo'
           : row.original.status === 'INACTIVE' ? 'Inactivo'
           : row.original.status === 'LOCKED' ? 'Bloqueado'
           : row.original.status}
        </Badge>
      ),
    },
    {
      header: 'Teléfono',
      accessorKey: 'phone',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">{row.original.phone || '—'}</span>
      ),
    },
    {
      header: 'Último acceso',
      accessorKey: 'lastLoginAt',
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Clock size={14} className="text-gray-400" />
          <span className="text-sm text-gray-500">
            {row.original.lastLoginAt ? dayjs.utc(row.original.lastLoginAt).fromNow() : 'Nunca'}
          </span>
        </div>
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
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <button
          onClick={() => setEditUser(row.original)}
          className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
          title="Editar usuario"
        >
          <Pencil size={16} />
        </button>
      ),
    },
  ]

  if (isLoading) return <UsersSkeleton />

  if (isError) {
    return (
      <ErrorState
        title="Error al cargar usuarios"
        message="No pudimos cargar la lista de usuarios."
        onRetry={() => refetch()}
      />
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.users')}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona los usuarios del sistema ({totalElements} total)
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus size={18} />
          Nuevo usuario
        </Button>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar usuarios..."
            className="h-9 w-full rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
          />
        </div>
        {roleOptions.map((opt) => (
          <button
            key={opt.value}
            onClick={() => setRoleFilter(opt.value)}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
              roleFilter === opt.value
                ? 'bg-brand-100 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <DataTable
        columns={columns}
        data={users}
        loading={false}
        pageCount={totalPages}
        pageIndex={page}
        onPageChange={setPage}
        totalRecords={totalElements}
        emptyMessage="No se encontraron usuarios"
      />

      <UserCreateModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <UserEditModal open={!!editUser} user={editUser} onClose={() => setEditUser(null)} />
    </div>
  )
}
