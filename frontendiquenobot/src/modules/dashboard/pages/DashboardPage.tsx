import { useTranslation } from 'react-i18next'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { ErrorState } from '@/shared/molecules/ErrorState'
import { EmptyState } from '@/shared/molecules/EmptyState'
import { useDashboard, useUserActivity } from '../hooks/useDashboard'
import { KpiCards } from '../components/KpiCards'
import { ConversationsChart } from '../components/ConversationsChart'
import { LeadStatusChart } from '../components/LeadStatusChart'
import { ProductStatsChart } from '../components/ProductStatsChart'
import { UserActivityTable } from '../components/UserActivityTable'

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div>
        <Skeleton width={200} height={28} />
        <Skeleton width={300} height={16} className="mt-2" />
      </div>
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <Skeleton width={120} height={16} />
            <Skeleton width={80} height={36} className="mt-2" />
          </div>
        ))}
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        {Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <Skeleton width={160} height={20} />
            <Skeleton width="100%" height={200} className="mt-4" />
          </div>
        ))}
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { t } = useTranslation()
  const { data: overview, isLoading, isError, error, refetch } = useDashboard()
  const { data: userActivity } = useUserActivity()

  if (isLoading) return <DashboardSkeleton />

  if (isError) {
    return (
      <ErrorState
        title="Error al cargar el dashboard"
        message={error instanceof Error ? error.message : 'Error desconocido'}
        onRetry={() => refetch()}
      />
    )
  }

  if (!overview) {
    return (
      <EmptyState
        title="Sin datos"
        description="No hay información disponible para el dashboard."
      />
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.dashboard')}</h1>
        <p className="mt-1 text-sm text-gray-500">Bienvenido al panel de control</p>
      </div>

      <KpiCards
        conversations={overview.conversationStats}
        contacts={overview.contactStats}
        leads={overview.leadStats}
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <ConversationsChart stats={overview.conversationStats} />
        <LeadStatusChart stats={overview.leadStats} />
        <ProductStatsChart stats={overview.productStats} />
      </div>

      <UserActivityTable data={userActivity ?? []} />
    </div>
  )
}
