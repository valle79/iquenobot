import { useState } from 'react'
import { Plus, Search, Building2, Users, Activity, AlertTriangle, MoreHorizontal, Pencil, Trash2, Ban, CheckCircle, Play } from 'lucide-react'
import { useAdminTenants, useSystemStats, useUpdateTenantStatus, useDeleteTenant } from '../hooks/useAdminTenants'
import { CreateTenantModal } from '../components/CreateTenantModal'
import { EditTenantModal } from '../components/EditTenantModal'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { ErrorState } from '@/shared/molecules/ErrorState'
import { EmptyState } from '@/shared/molecules/EmptyState'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { dayjs } from '@/config/dayjs'
import type { TenantDto } from '@/types/auth'

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'neutral'> = {
  ACTIVE: 'success',
  TRIAL: 'warning',
  INACTIVE: 'error',
  SUSPENDED: 'error',
  EXPIRED: 'neutral',
}

const statusLabels: Record<string, string> = {
  ACTIVE: 'Activa',
  TRIAL: 'Prueba',
  INACTIVE: 'Inactiva',
  SUSPENDED: 'Suspendida',
  EXPIRED: 'Vencida',
}

function StatsCard({ icon: Icon, label, value, loading }: { icon: any; label: string; value: number | string; loading: boolean }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-100 text-brand-600 dark:bg-brand-900/20 dark:text-brand-400">
          <Icon size={20} />
        </div>
        <div>
          <p className="text-xs text-gray-500">{label}</p>
          {loading ? (
            <Skeleton width={60} height={24} className="mt-1" />
          ) : (
            <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
          )}
        </div>
      </div>
    </div>
  )
}

function TenantsSkeleton() {
  return (
    <div className="space-y-4">
      {Array.from({ length: 5 }).map((_, i) => (
        <Skeleton key={i} width="100%" height={72} />
      ))}
    </div>
  )
}

const statusOptions = [
  { value: '', label: 'Todos' },
  { value: 'ACTIVE', label: 'Activas' },
  { value: 'TRIAL', label: 'Prueba' },
  { value: 'INACTIVE', label: 'Inactivas' },
  { value: 'SUSPENDED', label: 'Suspendidas' },
]

export default function AdminTenantsPage() {
  const { tenants, totalElements, page, setPage, search, setSearch, statusFilter, setStatusFilter, isLoading, isError, refetch } = useAdminTenants()
  const { data: stats, isLoading: statsLoading } = useSystemStats()
  const updateStatus = useUpdateTenantStatus()
  const deleteTenant = useDeleteTenant()

  const [createOpen, setCreateOpen] = useState(false)
  const [editTenant, setEditTenant] = useState<TenantDto | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<TenantDto | null>(null)
  const [openMenuId, setOpenMenuId] = useState<string | null>(null)

  if (isError) {
    return (
      <ErrorState
        title="Error al cargar empresas"
        message="No pudimos cargar la lista de empresas."
        onRetry={() => refetch()}
      />
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Empresas</h1>
          <p className="mt-1 text-sm text-gray-500">Gestiona todas las empresas del sistema</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus size={18} />
          Nueva empresa
        </Button>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-4 gap-4">
        <StatsCard icon={Building2} label="Total empresas" value={stats?.totalTenants ?? 0} loading={statsLoading} />
        <StatsCard icon={Activity} label="Activas" value={stats?.activeTenants ?? 0} loading={statsLoading} />
        <StatsCard icon={Users} label="Total usuarios" value={stats?.totalUsers ?? 0} loading={statsLoading} />
        <StatsCard icon={AlertTriangle} label="Prueba" value={stats?.trialTenants ?? 0} loading={statsLoading} />
      </div>

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar empresas..."
            className="h-9 w-full rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
          />
        </div>
        {statusOptions.map((opt) => (
          <button
            key={opt.value}
            onClick={() => setStatusFilter(opt.value)}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
              statusFilter === opt.value
                ? 'bg-brand-100 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-400'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* Tenants list */}
      {isLoading ? (
        <TenantsSkeleton />
      ) : tenants.length === 0 ? (
        <EmptyState
          icon={<Building2 size={48} />}
          title="No hay empresas"
          description={search || statusFilter ? 'No se encontraron empresas con los filtros actuales.' : 'Aún no se ha creado ninguna empresa.'}
        />
      ) : (
        <div className="space-y-3">
          {tenants.map((tenant) => (
            <div
              key={tenant.id}
              className="flex items-center justify-between rounded-xl border border-gray-200 bg-white p-4 transition-colors hover:border-gray-300 dark:border-gray-700 dark:bg-gray-900 dark:hover:border-gray-600"
            >
              <div className="flex items-center gap-4 min-w-0">
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-600 dark:bg-brand-900/20">
                  <Building2 size={20} />
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="font-medium text-gray-900 dark:text-gray-100 truncate">{tenant.companyName}</p>
                    <span className="rounded-md bg-gray-100 px-2 py-0.5 text-xs text-gray-500 dark:bg-gray-800 flex-shrink-0">@{tenant.subdomain}</span>
                    <Badge variant={statusColors[tenant.status] ?? 'neutral'} size="sm">
                      {statusLabels[tenant.status] ?? tenant.status}
                    </Badge>
                  </div>
                  <p className="mt-0.5 text-xs text-gray-500 truncate">{tenant.contactEmail} · Creada {dayjs(tenant.createdAt).format('DD/MM/YYYY')}</p>
                </div>
              </div>

              <div className="flex-shrink-0 ml-4">
                <Dropdown
                  open={openMenuId === tenant.id}
                  onOpenChange={(open) => setOpenMenuId(open ? tenant.id : null)}
                  trigger={
                    <button className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800">
                      <MoreHorizontal size={18} />
                    </button>
                  }
                  items={[
                    {
                      label: 'Editar',
                      icon: Pencil,
                      onClick: () => { setOpenMenuId(null); setEditTenant(tenant) },
                    },
                    ...(tenant.status === 'ACTIVE'
                      ? [{ label: 'Suspender', icon: Ban, onClick: () => { setOpenMenuId(null); updateStatus.mutate({ id: tenant.id, status: 'SUSPENDED' }) }, danger: true as const }]
                      : [{ label: 'Activar', icon: Play, onClick: () => { setOpenMenuId(null); updateStatus.mutate({ id: tenant.id, status: 'ACTIVE' }) } }]
                    ),
                    { type: 'separator' as const },
                    {
                      label: 'Eliminar',
                      icon: Trash2,
                      onClick: () => { setOpenMenuId(null); setDeleteTarget(tenant) },
                      danger: true as const,
                    },
                  ]}
                />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {!isLoading && tenants.length > 0 && (
        <div className="flex items-center justify-between border-t border-gray-200 pt-4 dark:border-gray-700">
          <p className="text-sm text-gray-500">{totalElements} empresas en total</p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="rounded-lg px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-50 dark:text-gray-400 dark:hover:bg-gray-800"
            >
              Anterior
            </button>
            <span className="text-sm text-gray-500">Página {page + 1}</span>
            <button
              onClick={() => setPage(page + 1)}
              className="rounded-lg px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

      {/* Modals */}
      <CreateTenantModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <EditTenantModal open={!!editTenant} tenant={editTenant} onClose={() => setEditTenant(null)} />
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            deleteTenant.mutate(deleteTarget.id)
            setDeleteTarget(null)
          }
        }}
        title="Eliminar empresa"
        message={`¿Estás seguro de eliminar ${deleteTarget?.companyName}? Los usuarios de esta empresa ya no podrán acceder.`}
        confirmLabel="Eliminar"
        loading={deleteTenant.isPending}
      />
    </div>
  )
}
