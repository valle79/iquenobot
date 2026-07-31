import { Modal } from '@/shared/atoms/Modal/Modal'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { dayjs } from '@/config/dayjs'
import { FileText, RefreshCw, Send, XCircle, Download, AlertTriangle, Calendar } from 'lucide-react'
import type { QuoteHistoryItem } from '@/types/quote'

interface QuoteHistoryModalProps {
  open: boolean
  quoteNumber: string
  history: QuoteHistoryItem[]
  loading?: boolean
  onClose: () => void
}

const ACTION_META: Record<string, { label: string; icon: typeof Send; variant: 'success' | 'info' | 'warning' | 'error' | 'neutral' }> = {
  GENERATED: { label: 'Generación', icon: FileText, variant: 'info' },
  SENT: { label: 'Envío inicial', icon: Send, variant: 'success' },
  RESENT: { label: 'Reenvío', icon: Send, variant: 'success' },
  REGENERATED: { label: 'Regeneración', icon: RefreshCw, variant: 'warning' },
  DOWNLOADED: { label: 'Descarga', icon: Download, variant: 'neutral' },
  CANCELLED: { label: 'Anulación', icon: XCircle, variant: 'error' },
  ERROR: { label: 'Error', icon: AlertTriangle, variant: 'error' },
}

export function QuoteHistoryModal({ open, quoteNumber, history, loading, onClose }: QuoteHistoryModalProps) {
  return (
    <Modal open={open} onClose={onClose} title="Historial de la cotización" size="full"
      description={`Auditoría completa de la cotización ${quoteNumber}`}>
      {loading ? (
        <div className="py-8 text-center text-sm text-gray-500">Cargando historial...</div>
      ) : history.length === 0 ? (
        <div className="py-8 text-center text-sm text-gray-500">Sin eventos registrados</div>
      ) : (
        <ol className="relative space-y-6 border-l-2 border-gray-100 pl-6 dark:border-gray-800">
          {history.map((entry) => {
            const meta = ACTION_META[entry.action] ?? { label: entry.action, icon: Calendar, variant: 'neutral' as const }
            const Icon = meta.icon
            return (
              <li key={entry.id} className="relative">
                <span
                  className={`absolute -left-[31px] flex h-5 w-5 items-center justify-center rounded-full border-2 border-white bg-gray-100 dark:border-gray-950 ${
                    meta.variant === 'error' ? 'text-red-500' : 'text-gray-500'
                  }`}
                >
                  <Icon size={10} />
                </span>
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant={meta.variant} size="sm">{meta.label}</Badge>
                  <span className="text-xs text-gray-400">
                    {dayjs(entry.createdAt).format('DD/MM/YYYY HH:mm')}
                  </span>
                  {entry.channel && (
                    <span className="text-xs text-gray-400">· Canal: {entry.channel}</span>
                  )}
                </div>
                <p className="mt-1 text-sm text-gray-700 dark:text-gray-300">
                  <span className="font-medium">{entry.actorName}</span>
                  {entry.details ? ` — ${entry.details}` : ''}
                </p>
                {entry.channelMessageId && (
                  <p className="text-xs text-gray-400">ID mensaje: {entry.channelMessageId}</p>
                )}
              </li>
            )
          })}
        </ol>
      )}
    </Modal>
  )
}
