import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import type { ConversationStats } from '@/types/dashboard'

interface ConversationTrendsChartProps {
  stats: ConversationStats
}

export function ConversationTrendsChart({ stats }: ConversationTrendsChartProps) {
  const data = [
    { name: 'Hoy', value: stats.conversationsToday },
    { name: 'Esta semana', value: stats.conversationsThisWeek },
    { name: 'Este mes', value: stats.conversationsThisMonth },
  ]

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Tendencia de Conversaciones</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis dataKey="name" tick={{ fontSize: 12 }} stroke="#9ca3af" />
          <YAxis tick={{ fontSize: 12 }} stroke="#9ca3af" />
          <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '13px' }} />
          <Bar dataKey="value" fill="#6366f1" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
      <div className="mt-4 grid grid-cols-3 gap-4 text-center">
        <div>
          <p className="text-xs text-gray-500">Abiertas</p>
          <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">{stats.openConversations}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500">Cerradas</p>
          <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">{stats.closedConversations}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500">Pendientes</p>
          <p className="text-lg font-semibold text-gray-900 dark:text-gray-100">{stats.pendingConversations}</p>
        </div>
      </div>
    </div>
  )
}
