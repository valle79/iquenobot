import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Building2, Globe, Mail, Phone, Calendar, Shield } from 'lucide-react'
import { useAuthStore } from '@/core/auth/auth.store'
import { api } from '@/core/api/client'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { toast } from 'sonner'
import { dayjs } from '@/config/dayjs'
import type { ApiResponse } from '@/types/api'
import type { TenantDto } from '@/types/auth'

export default function TenantSettingsPage() {
  const { tenant: storeTenant } = useAuthStore()
  const queryClient = useQueryClient()

  const { data: tenant, isLoading } = useQuery({
    queryKey: ['tenant', 'my'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TenantDto>>('/auth/tenant/my')
      return res.data.data
    },
    initialData: storeTenant ?? undefined,
  })

  const updateMutation = useMutation({
    mutationFn: async (dto: Partial<TenantDto>) => {
      const res = await api.put<ApiResponse<TenantDto>>('/auth/tenant/my', dto)
      return res.data.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenant'] })
      toast.success('Empresa actualizada')
    },
    onError: () => toast.error('Error al actualizar empresa'),
  })

  if (isLoading && !tenant) {
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <Skeleton width={250} height={32} />
        <Skeleton width={180} height={20} />
        <div className="space-y-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} width="100%" height={48} />)}
        </div>
      </div>
    )
  }

  if (!tenant) {
    return <p className="text-gray-500">No se pudo cargar la información de la empresa.</p>
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <Building2 size={24} className="text-brand-500" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Mi Empresa</h1>
          <p className="mt-1 text-sm text-gray-500">Configuración y detalles de tu empresa</p>
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 dark:bg-brand-900/20">
              <Building2 size={24} className="text-brand-500" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{tenant.companyName}</h2>
              <p className="text-sm text-gray-500">{tenant.subdomain}.iquenobot.com</p>
            </div>
          </div>
          <Badge variant={tenant.status === 'ACTIVE' ? 'success' : tenant.status === 'SUSPENDED' ? 'error' : 'neutral'} size="sm">
            {tenant.status === 'ACTIVE' ? 'Activo' : tenant.status === 'SUSPENDED' ? 'Suspendido' : 'Inactivo'}
          </Badge>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="flex items-center gap-3 rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
            <Mail size={16} className="text-gray-400" />
            <div>
              <p className="text-xs text-gray-500">Email de contacto</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{tenant.contactEmail}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
            <Phone size={16} className="text-gray-400" />
            <div>
              <p className="text-xs text-gray-500">Teléfono</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{tenant.contactPhone || '—'}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
            <Globe size={16} className="text-gray-400" />
            <div>
              <p className="text-xs text-gray-500">Plan</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{tenant.subscriptionPlan || 'Gratuito'}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
            <Calendar size={16} className="text-gray-400" />
            <div>
              <p className="text-xs text-gray-500">Suscripción hasta</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                {tenant.subscriptionExpiresAt ? dayjs(tenant.subscriptionExpiresAt).format('DD/MM/YYYY') : '—'}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="mb-4 flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-gray-500">
          <Shield size={16} /> Límites del plan
        </h3>
        <div className="grid gap-3 md:grid-cols-3">
          <div className="rounded-lg border border-gray-200 p-3 text-center dark:border-gray-700">
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{tenant.maxUsers ?? '∞'}</p>
            <p className="text-xs text-gray-500">Usuarios máx.</p>
          </div>
          <div className="rounded-lg border border-gray-200 p-3 text-center dark:border-gray-700">
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{tenant.maxConversations ?? '∞'}</p>
            <p className="text-xs text-gray-500">Conversaciones máx.</p>
          </div>
          <div className="rounded-lg border border-gray-200 p-3 text-center dark:border-gray-700">
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{tenant.features ? '✓' : '—'}</p>
            <p className="text-xs text-gray-500">Funciones extra</p>
          </div>
        </div>
      </div>
    </div>
  )
}
