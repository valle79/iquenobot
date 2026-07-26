import { Calendar, RefreshCw } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'
import { Select } from '@/shared/atoms/Select/Select'

interface AnalyticsFilterBarProps {
  period: string
  channel: string
  onPeriodChange: (period: string) => void
  onChannelChange: (channel: string) => void
  onRefresh: () => void
}

const PERIOD_OPTIONS = [
  { value: '7d', label: 'Últimos 7 días' },
  { value: '30d', label: 'Últimos 30 días' },
  { value: '90d', label: 'Últimos 90 días' },
  { value: '1y', label: 'Último año' },
]

const CHANNEL_OPTIONS = [
  { value: 'all', label: 'Todos los canales' },
  { value: 'whatsapp', label: 'WhatsApp' },
  { value: 'web', label: 'Web Chat' },
  { value: 'email', label: 'Email' },
]

export function AnalyticsFilterBar({ period, channel, onPeriodChange, onChannelChange, onRefresh }: AnalyticsFilterBarProps) {
  return (
    <div className="flex flex-wrap items-center gap-3 rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-950">
      <Calendar size={18} className="text-gray-400" />
      <Select value={period} onChange={e => onPeriodChange(e.target.value)} options={PERIOD_OPTIONS} />
      <span className="hidden text-sm text-gray-300 sm:inline">|</span>
      <Select value={channel} onChange={e => onChannelChange(e.target.value)} options={CHANNEL_OPTIONS} />
      <div className="ml-auto">
        <Button variant="ghost" size="sm" onClick={onRefresh}>
          <RefreshCw size={16} />
          Actualizar
        </Button>
      </div>
    </div>
  )
}
