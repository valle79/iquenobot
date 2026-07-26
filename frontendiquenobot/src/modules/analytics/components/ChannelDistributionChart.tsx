import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import { MessageCircle, Globe, Mail } from 'lucide-react'

const CHANNELS = [
  { key: 'whatsapp', label: 'WhatsApp', color: '#25D366', icon: MessageCircle },
  { key: 'web', label: 'Web Chat', color: '#6366f1', icon: Globe },
  { key: 'email', label: 'Email', color: '#3b82f6', icon: Mail },
]

interface ChannelDistributionChartProps {
  stats: {
    whatsappConversations: number
    webConversations: number
    emailConversations: number
  }
}

export function ChannelDistributionChart({ stats }: ChannelDistributionChartProps) {
  const data = [
    { name: 'WhatsApp', value: stats.whatsappConversations, color: '#25D366' },
    { name: 'Web Chat', value: stats.webConversations, color: '#6366f1' },
    { name: 'Email', value: stats.emailConversations, color: '#3b82f6' },
  ].filter(d => d.value > 0)

  if (data.length === 0) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Distribución por Canal</h3>
        <p className="text-sm text-gray-400">Sin datos de canales disponibles.</p>
      </div>
    )
  }

  const total = data.reduce((sum, d) => sum + d.value, 0)

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Distribución por Canal</h3>
      <ResponsiveContainer width="100%" height={280}>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={60}
            outerRadius={100}
            dataKey="value"
          >
            {data.map((entry, index) => (
              <Cell key={index} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value: number) => [`${value} (${((value / total) * 100).toFixed(1)}%)`, 'Conversaciones']}
            contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '13px' }}
          />
          <Legend
            formatter={(value: string) => <span className="text-sm text-gray-700 dark:text-gray-300">{value}</span>}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="mt-4 grid grid-cols-3 gap-4">
        {CHANNELS.map(ch => {
          const Icon = ch.icon
          const val = stats[`${ch.key}Conversations` as keyof typeof stats] as number
          return (
            <div key={ch.key} className="flex items-center gap-2 rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
              <Icon size={16} className="shrink-0" style={{ color: ch.color }} />
              <div className="min-w-0">
                <p className="truncate text-xs text-gray-500">{ch.label}</p>
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{val}</p>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
