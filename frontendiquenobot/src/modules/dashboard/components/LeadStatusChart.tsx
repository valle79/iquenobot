import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import type { LeadStats } from '@/types/dashboard'

const COLORS = ['#22c55e', '#3b82f6', '#a855f7', '#f59e0b', '#ef4444']

interface LeadStatusChartProps {
  stats: LeadStats
}

export function LeadStatusChart({ stats }: LeadStatusChartProps) {
  const data = [
    { name: 'Nuevos', value: stats.newLeads },
    { name: 'Contactados', value: stats.contactedLeads },
    { name: 'Calificados', value: stats.qualifiedLeads },
    { name: 'Convertidos', value: stats.convertedLeads },
    { name: 'Perdidos', value: stats.lostLeads },
  ].filter((d) => d.value > 0)

  if (data.length === 0) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Estado de Leads</h3>
        <p className="text-sm text-gray-400">Sin datos</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Estado de Leads</h3>
      <ResponsiveContainer width="100%" height={200}>
        <PieChart>
          <Pie data={data} cx="50%" cy="50%" outerRadius={70} dataKey="value" label={({ percent }) => `${(percent * 100).toFixed(0)}%`}>
            {data.map((_, index) => (
              <Cell key={index} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
