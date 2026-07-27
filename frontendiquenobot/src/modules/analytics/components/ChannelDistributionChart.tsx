import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import {
  MessageCircle,
  Globe,
  Mail,
  Smartphone,
  Send,
  Instagram,
  MessageSquare,
} from 'lucide-react'
import type { ChannelType } from '@/types/enums'

const CHANNEL_CONFIG: Record<string, { label: string; color: string; icon: typeof MessageCircle }> = {
  WHATSAPP: { label: 'WhatsApp', color: '#25D366', icon: MessageCircle },
  MESSENGER: { label: 'Messenger', color: '#0084FF', icon: MessageSquare },
  INSTAGRAM: { label: 'Instagram', color: '#E4405F', icon: Instagram },
  EMAIL: { label: 'Email', color: '#3b82f6', icon: Mail },
  WEBCHAT: { label: 'Web Chat', color: '#6366f1', icon: Globe },
  SMS: { label: 'SMS', color: '#f59e0b', icon: Send },
  TELEGRAM: { label: 'Telegram', color: '#0088cc', icon: Send },
  API: { label: 'API', color: '#6b7280', icon: Globe },
  TWITTER: { label: 'Twitter', color: '#1DA1F2', icon: MessageSquare },
}

interface ChannelData {
  channel: ChannelType
  count: number
}

interface ChannelDistributionChartProps {
  data: ChannelData[]
}

export function ChannelDistributionChart({ data }: ChannelDistributionChartProps) {
  const chartData = data
    .filter((d) => d.count > 0)
    .map((d) => ({
      name: CHANNEL_CONFIG[d.channel]?.label ?? d.channel,
      value: d.count,
      color: CHANNEL_CONFIG[d.channel]?.color ?? '#6b7280',
      channel: d.channel,
    }))

  if (chartData.length === 0) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Distribución por Canal</h3>
        <p className="text-sm text-gray-400">Sin datos de canales disponibles.</p>
      </div>
    )
  }

  const total = chartData.reduce((sum, d) => sum + d.value, 0)

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Distribución por Canal</h3>
      <ResponsiveContainer width="100%" height={280}>
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={60}
            outerRadius={100}
            dataKey="value"
          >
            {chartData.map((entry, index) => (
              <Cell key={index} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value: number) => [`${value} (${((value / total) * 100).toFixed(1)}%)`, 'Conversaciones']}
            contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '13px' }}
          />
          <Legend
            formatter={(value: string) => (
              <span className="text-sm text-gray-700 dark:text-gray-300">{value}</span>
            )}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="mt-4 grid grid-cols-3 gap-3">
        {chartData.map((d) => {
          const config = CHANNEL_CONFIG[d.channel]
          const Icon = config?.icon ?? Globe
          return (
            <div key={d.channel} className="flex items-center gap-2 rounded-lg bg-gray-50 p-3 dark:bg-gray-900">
              <Icon size={16} className="shrink-0" style={{ color: d.color }} />
              <div className="min-w-0">
                <p className="truncate text-xs text-gray-500">{d.name}</p>
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{d.value}</p>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
