import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { ConversationStats } from '@/types/dashboard'

interface ConversationsChartProps {
  stats: ConversationStats
}

export function ConversationsChart({ stats }: ConversationsChartProps) {
  const data = [
    { name: 'Hoy', value: stats.conversationsToday },
    { name: 'Esta semana', value: stats.conversationsThisWeek },
    { name: 'Este mes', value: stats.conversationsThisMonth },
  ]

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
        Conversaciones
      </h3>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data}>
          <XAxis dataKey="name" tick={{ fontSize: 12 }} stroke="#9ca3af" />
          <YAxis tick={{ fontSize: 12 }} stroke="#9ca3af" />
          <Tooltip
            contentStyle={{
              borderRadius: '8px',
              border: '1px solid #e5e7eb',
              fontSize: '13px',
            }}
          />
          <Bar dataKey="value" fill="var(--color-brand-500, #6366f1)" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
