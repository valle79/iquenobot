import { useState, useEffect } from 'react'
import { BarChart3, TrendingUp, Users, MessageCircle, Activity, Building } from 'lucide-react'
import { adminService, type SystemStats } from '@/services/admin.service'

export default function AdminAnalyticsPage() {
  const [stats, setStats] = useState<SystemStats | null>(null)

  useEffect(() => {
    adminService.getSystemStats().then(setStats).catch(() => {})
  }, [])

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Analíticas Globales</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard icon={Building} label="Empresas" value={stats?.totalTenants ?? 0} />
        <MetricCard icon={Users} label="Usuarios" value={stats?.totalUsers ?? 0} />
        <MetricCard icon={MessageCircle} label="Conversaciones" value={stats?.totalConversations ?? 0} />
        <MetricCard icon={Activity} label="Mensajes" value={stats?.totalMessages ?? 0} />
        <MetricCard icon={Users} label="Contactos" value={stats?.totalContacts ?? 0} />
        <MetricCard icon={TrendingUp} label="Leads" value={stats?.totalLeads ?? 0} />
        <MetricCard icon={BarChart3} label="Productos" value={stats?.totalProducts ?? 0} />
        <MetricCard icon={Activity} label="Solicitudes IA" value={stats?.aiTotalRequests ?? 0} />
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
        <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Distribución de Empresas</h2>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Distribucion label="Activas" value={stats?.activeTenants ?? 0} color="green" total={stats?.totalTenants ?? 1} />
          <Distribucion label="Prueba" value={stats?.trialTenants ?? 0} color="yellow" total={stats?.totalTenants ?? 1} />
          <Distribucion label="Vencidas" value={stats?.expiredTenants ?? 0} color="red" total={stats?.totalTenants ?? 1} />
          <Distribucion label="Suspendidas" value={stats?.suspendedTenants ?? 0} color="orange" total={stats?.totalTenants ?? 1} />
        </div>
      </div>
    </div>
  )
}

function MetricCard({ icon: Icon, label, value }: { icon: React.ComponentType<{ size?: number }>; label: string; value: number }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-950">
      <div className="flex items-center gap-3">
        <div className="rounded-lg bg-brand-50 p-2.5 text-brand-600 dark:bg-brand-900/20 dark:text-brand-400">
          <Icon size={20} />
        </div>
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
          <p className="text-xl font-bold text-gray-900 dark:text-white">{value.toLocaleString()}</p>
        </div>
      </div>
    </div>
  )
}

function Distribucion({ label, value, color, total }: { label: string; value: number; color: string; total: number }) {
  const pct = total > 0 ? (value / total) * 100 : 0
  const colors: Record<string, string> = {
    green: 'bg-green-500',
    yellow: 'bg-yellow-500',
    red: 'bg-red-500',
    orange: 'bg-orange-500',
  }
  return (
    <div>
      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-500 dark:text-gray-400">{label}</span>
        <span className="font-medium text-gray-900 dark:text-white">{value}</span>
      </div>
      <div className="mt-1 h-2 w-full rounded-full bg-gray-100 dark:bg-gray-800">
        <div className={`h-2 rounded-full ${colors[color] ?? 'bg-gray-500'}`} style={{ width: `${Math.min(pct, 100)}%` }} />
      </div>
      <p className="mt-0.5 text-xs text-gray-400">{pct.toFixed(1)}%</p>
    </div>
  )
}
