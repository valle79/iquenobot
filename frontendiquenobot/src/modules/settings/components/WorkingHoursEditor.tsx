import { Toggle } from '@/shared/atoms/Toggle/Toggle'
import type { WorkingHoursSchedule } from '@/types/orchestrator'
import { DAY_LABELS, DAY_ORDER } from '@/types/orchestrator'
import { Clock, Copy } from 'lucide-react'
import { cn } from '@/shared/utils'

interface WorkingHoursEditorProps {
  schedule: WorkingHoursSchedule
  onChange: (schedule: WorkingHoursSchedule) => void
}

const TIME_OPTIONS = Array.from({ length: 48 }, (_, i) => {
  const h = Math.floor(i / 2)
  const m = (i % 2) * 30
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
})

function DayRow({
  day,
  config,
  onChange,
}: {
  day: string
  config: { start: string; end: string; active: boolean }
  onChange: (start: string, end: string, active: boolean) => void
}) {
  return (
    <div className={cn(
      'flex items-center gap-4 rounded-lg border px-4 py-3 transition-colors',
      config.active
        ? 'border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950'
        : 'border-gray-100 bg-gray-50 dark:border-gray-800 dark:bg-gray-900',
    )}>
      <div className="w-28 shrink-0">
        <Toggle
          checked={config.active}
          onChange={(checked) => onChange(config.start, config.end, checked)}
          size="sm"
        />
      </div>
      <span className={cn(
        'w-24 text-sm font-medium',
        config.active ? 'text-gray-900 dark:text-gray-100' : 'text-gray-400 dark:text-gray-600',
      )}>
        {DAY_LABELS[day]}
      </span>
      {config.active ? (
        <div className="flex items-center gap-2">
          <select
            value={config.start}
            onChange={(e) => onChange(e.target.value, config.end, config.active)}
            className="h-9 rounded-lg border border-gray-300 bg-white px-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
          >
            {TIME_OPTIONS.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <span className="text-sm text-gray-400">—</span>
          <select
            value={config.end}
            onChange={(e) => onChange(config.start, e.target.value, config.active)}
            className="h-9 rounded-lg border border-gray-300 bg-white px-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
          >
            {TIME_OPTIONS.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
      ) : (
        <span className="text-sm text-gray-400 dark:text-gray-600">Cerrado</span>
      )}
    </div>
  )
}

export function WorkingHoursEditor({ schedule, onChange }: WorkingHoursEditorProps) {
  const updateDay = (day: string, start: string, end: string, active: boolean) => {
    onChange({ ...schedule, [day]: { start, end, active } })
  }

  const copyMondayToAll = () => {
    const monday = schedule.monday
    if (!monday) return
    const updated: WorkingHoursSchedule = {}
    for (const d of DAY_ORDER) {
      updated[d] = { ...monday }
    }
    onChange(updated)
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <Clock size={14} />
          <span>Horario por día</span>
        </div>
        <button
          type="button"
          onClick={copyMondayToAll}
          className="flex items-center gap-1.5 text-xs text-brand-600 hover:text-brand-700 dark:text-brand-400"
        >
          <Copy size={12} />
          Copiar lunes a todos
        </button>
      </div>
      {DAY_ORDER.map((day) => {
        const config = schedule[day] ?? { start: '09:00', end: '18:00', active: false }
        return (
          <DayRow
            key={day}
            day={day}
            config={config}
            onChange={(start, end, active) => updateDay(day, start, end, active)}
          />
        )
      })}
    </div>
  )
}
