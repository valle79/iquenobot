import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import type { QuoteFilters, QuoteStatus } from '@/types/quote'

const STATUS_OPTIONS: Array<{ value: QuoteStatus; label: string }> = [
  { value: 'GENERADA', label: 'Generada' },
  { value: 'SENT', label: 'Enviada' },
  { value: 'REENVIADA', label: 'Reenviada' },
  { value: 'ACCEPTED', label: 'Aceptada' },
  { value: 'REJECTED', label: 'Rechazada' },
  { value: 'ANULADA', label: 'Anulada' },
  { value: 'EXPIRED', label: 'Vencida' },
]

interface QuoteFiltersBarProps {
  filters: QuoteFilters
  onChange: (filters: QuoteFilters) => void
  users: Array<{ id: string; fullName: string }>
}

export function QuoteFiltersBar({ filters, onChange, users }: QuoteFiltersBarProps) {
  const [advancedOpen, setAdvancedOpen] = useState(false)

  const set = (patch: QuoteFilters) => onChange({ ...filters, ...patch })

  const inputClass =
    'h-9 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100'

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-950">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <input
          className={inputClass}
          placeholder="Cliente..."
          value={filters.customer ?? ''}
          onChange={(e) => set({ customer: e.target.value })}
        />
        <input
          className={inputClass}
          placeholder="Nº de cotización (C-0000001)"
          value={filters.quoteNumber ?? ''}
          onChange={(e) => set({ quoteNumber: e.target.value })}
        />
        <input
          className={inputClass}
          placeholder="Teléfono / WhatsApp"
          value={filters.phone ?? ''}
          onChange={(e) => set({ phone: e.target.value })}
        />
        <select
          className={inputClass}
          value={filters.status ?? ''}
          onChange={(e) => set({ status: e.target.value })}
        >
          <option value="">Todos los estados</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s.value} value={s.value}>{s.label}</option>
          ))}
        </select>
        <input
          type="date"
          className={inputClass}
          value={filters.dateFrom ?? ''}
          onChange={(e) => set({ dateFrom: e.target.value || undefined })}
        />
        <input
          type="date"
          className={inputClass}
          value={filters.dateTo ?? ''}
          onChange={(e) => set({ dateTo: e.target.value || undefined })}
        />
        <select
          className={inputClass}
          value={filters.generatedBy ?? ''}
          onChange={(e) => set({ generatedBy: e.target.value })}
        >
          <option value="">Generado por: Todos</option>
          <option value="BOT">Bot</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>{u.fullName}</option>
          ))}
        </select>
      </div>

      <div className="mt-3">
        <button
          type="button"
          onClick={() => setAdvancedOpen(!advancedOpen)}
          className="flex items-center gap-1 text-xs font-medium text-gray-500 hover:text-gray-700 dark:hover:text-gray-300"
        >
          <ChevronDown size={14} className={`transition-transform ${advancedOpen ? 'rotate-180' : ''}`} />
          Filtros avanzados (rango de importes)
        </button>
        {advancedOpen && (
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:max-w-md">
            <input
              type="number"
              min="0"
              step="0.01"
              className={inputClass}
              placeholder="Importe mínimo (S/)"
              value={filters.minTotal ?? ''}
              onChange={(e) => set({ minTotal: e.target.value ? Number(e.target.value) : undefined })}
            />
            <input
              type="number"
              min="0"
              step="0.01"
              className={inputClass}
              placeholder="Importe máximo (S/)"
              value={filters.maxTotal ?? ''}
              onChange={(e) => set({ maxTotal: e.target.value ? Number(e.target.value) : undefined })}
            />
          </div>
        )}
      </div>
    </div>
  )
}
