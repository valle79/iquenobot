import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Cell } from 'recharts'
import type { LeadStats } from '@/types/dashboard'

const COLORS = ['#6366f1', '#3b82f6', '#22c55e', '#f59e0b', '#ef4444']

interface LeadFunnelChartProps {
  stats: LeadStats
}

export function LeadFunnelChart({ stats }: LeadFunnelChartProps) {
  const data = [
    { name: 'Nuevos', value: stats.newLeads, percentage: stats.totalLeads > 0 ? Math.round((stats.newLeads / stats.totalLeads) * 100) : 0 },
    { name: 'Contactados', value: stats.contactedLeads, percentage: stats.totalLeads > 0 ? Math.round((stats.contactedLeads / stats.totalLeads) * 100) : 0 },
    { name: 'Calificados', value: stats.qualifiedLeads, percentage: stats.totalLeads > 0 ? Math.round((stats.qualifiedLeads / stats.totalLeads) * 100) : 0 },
    { name: 'Convertidos', value: stats.convertedLeads, percentage: stats.totalLeads > 0 ? Math.round((stats.convertedLeads / stats.totalLeads) * 100) : 0 },
    { name: 'Perdidos', value: stats.lostLeads, percentage: stats.totalLeads > 0 ? Math.round((stats.lostLeads / stats.totalLeads) * 100) : 0 },
  ]

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Embudo de Leads</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis type="number" tick={{ fontSize: 12 }} stroke="#9ca3af" />
          <YAxis dataKey="name" type="category" tick={{ fontSize: 12 }} stroke="#9ca3af" width={100} />
          <Tooltip
            contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '13px' }}
            formatter={(value: number) => [value, 'Cantidad']}
          />
          <Bar dataKey="value" radius={[0, 4, 4, 0]}>
            {data.map((_, index) => (
              <Cell key={index} fill={COLORS[index % COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
      <div className="mt-4 flex items-center justify-between text-sm">
        <span className="text-gray-500">Tasa de conversión</span>
        <span className="font-semibold text-gray-900 dark:text-gray-100">{stats.conversionRate.toFixed(1)}%</span>
      </div>
    </div>
  )
}
