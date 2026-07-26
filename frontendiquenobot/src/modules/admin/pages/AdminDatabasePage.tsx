import { useState, useEffect } from 'react'
import { Database, Server, HardDrive, Activity, Wifi, Clock } from 'lucide-react'
import { adminService, type SystemStats } from '@/services/admin.service'

function formatUptime(millis: number): string {
  const seconds = Math.floor(millis / 1000)
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  return `${days}d ${hours}h ${mins}m`
}

export default function AdminDatabasePage() {
  const [stats, setStats] = useState<SystemStats | null>(null)

  useEffect(() => {
    adminService.getSystemStats().then(setStats).catch(() => {})
  }, [])

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Base de Datos</h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <Database className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">PostgreSQL</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Estado" value={stats?.database?.status ?? 'desconocido'} />
            <InfoRow label="Conexiones activas" value={String(stats?.database?.activeConnections ?? '-')} />
            <InfoRow label="Conexiones máximas" value={String(stats?.database?.maxConnections ?? '-')} />
            <InfoRow label="Disco usado" value={stats?.database?.diskUsageMb ? `${(stats.database.diskUsageMb / 1024).toFixed(1)} GB` : '-'} />
          </div>
        </div>

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
            <InfoRow label="Memoria" value={stats?.server?.memoryUsage ? `${stats.server.memoryUsage.toFixed(0)} / ${stats.server.memoryMax.toFixed(0)} MB` : '-'} />
            <InfoRow label="Hilos activos" value={String(stats?.server?.activeThreads ?? '-')} />
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <HardDrive className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Almacenamiento</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Storage usado" value={stats?.storageUsedMb ? `${(stats.storageUsedMb / 1024).toFixed(1)} GB` : '0 GB'} />
            <InfoRow label="Conversaciones" value={String(stats?.totalConversations ?? 0)} />
            <InfoRow label="Mensajes" value={String(stats?.totalMessages ?? 0)} />
            <InfoRow label="Contactos" value={String(stats?.totalContacts ?? 0)} />
            <InfoRow label="Productos" value={String(stats?.totalProducts ?? 0)} />
          </div>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
          <div className="mb-4 flex items-center gap-2">
            <Wifi className="text-blue-600" size={20} />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Integraciones</h2>
          </div>
          <div className="space-y-3">
            <InfoRow label="Evolution API" value={stats?.evolutionApi?.status ?? 'disconnected'} />
            <InfoRow label="Último check" value={stats?.evolutionApi?.lastCheck ?? '-'} />
            <InfoRow label="Solicitudes IA" value={String(stats?.aiTotalRequests ?? 0)} />
            <InfoRow label="Tokens IA" value={String(stats?.aiTotalTokens ?? 0)} />
          </div>
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
