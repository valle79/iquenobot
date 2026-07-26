import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import type { ProductStats } from '@/types/dashboard'

interface ProductStatsChartProps {
  stats: ProductStats
}

export function ProductStatsChart({ stats }: ProductStatsChartProps) {
  const data = [
    { name: 'Activos', value: stats.activeProducts },
    { name: 'Sin stock', value: stats.outOfStockProducts },
    { name: 'Stock bajo', value: stats.lowStockProducts },
    { name: 'Destacados', value: stats.featuredProducts },
  ]

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Productos</h3>
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
          <Bar dataKey="value" fill="#22c55e" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
