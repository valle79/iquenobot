import { useState, useEffect } from 'react'
import { Building, Users, Activity, AlertTriangle, TrendingUp, Clock, Server, Database, Wifi } from 'lucide-react'
import { adminService, type SystemStats } from '@/services/admin.service'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes.toFixed(1)} MB`
  return `${(bytes / 1024).toFixed(1)} GB`
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<SystemStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadStats()
  }, [])

  const loadStats = async () => {
    try {
      const data = await adminService.getSystemStats()
      setStats(data)
    } catch {
      console.error('Error loading system stats')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-600 border-t-transparent" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Panel de Administración</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={Building} label="Total Empresas" value={stats?.totalTenants ?? 0} color="blue" />
        <StatCard icon={Activity} label="Empresas Activas" value={stats?.activeTenants ?? 0} color="green" />
        <StatCard icon={Users} label="Total Usuarios" value={stats?.totalUsers ?? 0} color="purple" />
        <StatCard icon={AlertTriangle} label="Empresas Vencidas" value={stats?.expiredTenants ?? 0} color="red" />
        <StatCard icon={TrendingUp} label="Prueba" value={stats?.trialTenants ?? 0} color="yellow" />
        <StatCard icon={Building} label="Suspendidas" value={stats?.suspendedTenants ?? 0} color="orange" />
        <StatCard icon={Activity} label="Conversaciones" value={stats?.totalConversations ?? 0} color="cyan" />
        <StatCard icon={Users} label="Contactos" value={stats?.totalContacts ?? 0} color="indigo" />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <Server className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Servidor</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Estado" value={stats?.server?.status ?? 'desconocido'} />
            <InfoRow label="Versión" value={stats?.server?.version ?? '-'} />
            <InfoRow label="Uptime" value={stats?.server?.uptime ?? '-'} />
            <InfoRow label="CPU" value={stats?.server?.cpuUsage ? `${stats.server.cpuUsage.toFixed(2)}%` : '-'} />
            <InfoRow label="Memoria" value={stats?.server?.memoryUsage ? `${stats.server.memoryUsage.toFixed(0)} MB / ${stats.server.memoryMax.toFixed(0)} MB` : '-'} />
            <InfoRow label="Hilos activos" value={String(stats?.server?.activeThreads ?? '-')} />
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <Database className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Base de Datos</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Estado" value={stats?.database?.status ?? 'desconocido'} />
            <InfoRow label="Conexiones activas" value={String(stats?.database?.activeConnections ?? '-')} />
            <InfoRow label="Conexiones máximas" value={String(stats?.database?.maxConnections ?? '-')} />
            <InfoRow label="Disco usado" value={stats?.database?.diskUsageMb ? formatBytes(stats.database.diskUsageMb) : '-'} />
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <Wifi className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Evolution API</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Estado" value={stats?.evolutionApi?.status ?? 'desconocido'} />
            <InfoRow label="Última verificación" value={stats?.evolutionApi?.lastCheck ?? '-'} />
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <Clock className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Almacenamiento</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Storage usado" value={stats?.storageUsedMb ? formatBytes(stats.storageUsedMb) : '0 MB'} />
            <InfoRow label="Productos" value={String(stats?.totalProducts ?? 0)} />
            <InfoRow label="Solicitudes IA" value={String(stats?.aiTotalRequests ?? 0)} />
            <InfoRow label="Tokens IA" value={String(stats?.aiTotalTokens ?? 0)} />
          </div>
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon: Icon, label, value, color }: { icon: React.ComponentType<{ size?: number }>; label: string; value: number; color: string }) {
  const colorClasses: Record<string, string> = {
    blue: 'bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400',
    green: 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400',
    purple: 'bg-purple-50 text-purple-700 dark:bg-purple-900/20 dark:text-purple-400',
    red: 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400',
    yellow: 'bg-yellow-50 text-yellow-700 dark:bg-yellow-900/20 dark:text-yellow-400',
    orange: 'bg-orange-50 text-orange-700 dark:bg-orange-900/20 dark:text-orange-400',
    cyan: 'bg-cyan-50 text-cyan-700 dark:bg-cyan-900/20 dark:text-cyan-400',
    indigo: 'bg-indigo-50 text-indigo-700 dark:bg-indigo-900/20 dark:text-indigo-400',
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-950">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
          <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
            {value.toLocaleString()}
          </p>
        </div>
        <div className={`rounded-lg p-3 ${colorClasses[color] ?? colorClasses.blue}`}>
          <Icon size={24} />
        </div>
      </div>
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between border-b border-gray-100 pb-2 last:border-0 dark:border-gray-800">
      <span className="text-sm text-gray-500 dark:text-gray-400">{label}</span>
      <span className="text-sm font-medium text-gray-900 dark:text-white">{value}</span>
    </div>
  )
}
