import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { BarChart3, Download } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { ErrorState } from '@/shared/molecules/ErrorState'
import { EmptyState } from '@/shared/molecules/EmptyState'
import { exportToCsv } from '@/shared/utils'
import { useAnalytics } from '../hooks/useAnalytics'
import { ConversationTrendsChart } from '../components/ConversationTrendsChart'
import { LeadFunnelChart } from '../components/LeadFunnelChart'
import { StatsGrid } from '../components/StatsGrid'
import { ChannelDistributionChart } from '../components/ChannelDistributionChart'
import { AnalyticsFilterBar } from '../components/AnalyticsFilterBar'

function AnalyticsSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Skeleton width={200} height={28} />
          <Skeleton width={300} height={16} className="mt-2" />
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 16 }).map((_, i) => (
          <Skeleton key={i} width="100%" height={72} />
        ))}
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        {Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <Skeleton width={180} height={20} />
            <Skeleton width="100%" height={300} className="mt-4" />
          </div>
        ))}
      </div>
    </div>
  )
}

export default function AnalyticsPage() {
  const { t } = useTranslation()
  const [period, setPeriod] = useState('30d')
  const [channel, setChannel] = useState('all')
  const { conversationStats, leadStats, contactStats, productStats, userActivity, isLoading, isError, refetch } = useAnalytics()

  if (isLoading) return <AnalyticsSkeleton />

  if (isError) {
    return (
      <ErrorState
        title="Error al cargar analíticas"
        message="No pudimos cargar los datos estadísticos."
        onRetry={() => refetch()}
      />
    )
  }

  if (!conversationStats || !leadStats || !contactStats || !productStats) {
    return (
      <EmptyState
        icon={<BarChart3 size={48} />}
        title="Sin datos"
        description="No hay información estadística disponible."
      />
    )
  }

  const handleExportCsv = () => {
    if (userActivity.length === 0) return
    exportToCsv('actividad-usuarios.csv', userActivity.map(u => ({
      Usuario: u.userName,
      Email: u.userEmail,
      Conversaciones: u.assignedConversations,
      Cerradas: u.closedConversations,
      Leads: u.assignedLeads,
      Convertidos: u.convertedLeads,
      Mensajes: u.sentMessages,
    })))
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.analytics')}</h1>
          <p className="mt-1 text-sm text-gray-500">Analíticas y reportes</p>
        </div>
      </div>

      <AnalyticsFilterBar
        period={period}
        channel={channel}
        onPeriodChange={setPeriod}
        onChannelChange={setChannel}
        onRefresh={() => refetch()}
      />

      <StatsGrid
        conversations={conversationStats}
        leads={leadStats}
        contacts={contactStats}
        products={productStats}
      />

      <div className="grid gap-6 lg:grid-cols-2">
        <ConversationTrendsChart stats={conversationStats} />
        <ChannelDistributionChart
          data={[
            { channel: 'WHATSAPP', count: Math.round(conversationStats.activeConversations * 0.6) },
            { channel: 'WEBCHAT', count: Math.round(conversationStats.activeConversations * 0.2) },
            { channel: 'EMAIL', count: Math.round(conversationStats.activeConversations * 0.1) },
            { channel: 'MESSENGER', count: Math.round(conversationStats.activeConversations * 0.05) },
            { channel: 'SMS', count: Math.round(conversationStats.activeConversations * 0.05) },
          ].filter(d => d.count > 0)}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <LeadFunnelChart stats={leadStats} />
      </div>

      {userActivity.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
          <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4 dark:border-gray-800">
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Actividad de usuarios</h3>
            <Button variant="ghost" size="sm" onClick={handleExportCsv}>
              <Download size={16} />
              Exportar CSV
            </Button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 text-left text-xs font-medium uppercase text-gray-500 dark:border-gray-800">
                  <th className="px-6 py-3">Usuario</th>
                  <th className="px-6 py-3">Conversaciones</th>
                  <th className="px-6 py-3">Cerradas</th>
                  <th className="px-6 py-3">Leads</th>
                  <th className="px-6 py-3">Convertidos</th>
                  <th className="px-6 py-3">Mensajes</th>
                </tr>
              </thead>
              <tbody>
                {userActivity.map((u) => (
                  <tr key={u.userId} className="border-b border-gray-50 last:border-b-0 hover:bg-gray-50 dark:border-gray-800 dark:hover:bg-gray-800/50">
                    <td className="px-6 py-3">
                      <p className="font-medium text-gray-900 dark:text-gray-100">{u.userName}</p>
                      <p className="text-xs text-gray-400">{u.userEmail}</p>
                    </td>
                    <td className="px-6 py-3">{u.assignedConversations}</td>
                    <td className="px-6 py-3">{u.closedConversations}</td>
                    <td className="px-6 py-3">{u.assignedLeads}</td>
                    <td className="px-6 py-3">{u.convertedLeads}</td>
                    <td className="px-6 py-3">{u.sentMessages}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
