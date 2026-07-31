import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  Download,
  Send,
  RefreshCw,
  XCircle,
  History,
  PhoneForwarded,
  FileText,
  Bot,
  User,
  Hash,
  HardDrive,
} from 'lucide-react'
import { quoteService } from '@/services/quote.service'
import { useResendQuote, useRegenerateQuote, useCancelQuote } from '../hooks/useQuotes'
import { ResendToNumberModal } from '../components/ResendToNumberModal'
import { QuoteHistoryModal } from '../components/QuoteHistoryModal'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { dayjs } from '@/config/dayjs'

const statusColors: Record<string, 'success' | 'warning' | 'error' | 'info' | 'neutral'> = {
  GENERADA: 'info',
  DRAFT: 'neutral',
  SENT: 'info',
  REENVIADA: 'success',
  ACCEPTED: 'success',
  REJECTED: 'error',
  ANULADA: 'neutral',
  EXPIRED: 'warning',
}

const statusLabels: Record<string, string> = {
  GENERADA: 'Generada',
  DRAFT: 'Borrador',
  SENT: 'Enviada',
  REENVIADA: 'Reenviada',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Rechazada',
  ANULADA: 'Anulada',
  EXPIRED: 'Vencida',
}

const money = (value: number, currency: string) =>
  `${currency === 'USD' ? '$' : 'S/'} ${value.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

const formatSize = (bytes: number | null) => {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function QuoteDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const resendMutation = useResendQuote()
  const regenerateMutation = useRegenerateQuote()
  const cancelMutation = useCancelQuote()

  const [resendToOpen, setResendToOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [regenerateTarget, setRegenerateTarget] = useState(false)
  const [cancelTarget, setCancelTarget] = useState(false)

  const { data: quote, isLoading } = useQuery({
    queryKey: ['quote', id],
    queryFn: () => quoteService.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton width={200} height={28} />
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-4">
            <Skeleton width="100%" height={220} />
            <Skeleton width="100%" height={160} />
          </div>
          <Skeleton width="100%" height={280} />
        </div>
      </div>
    )
  }

  if (!quote) {
    return (
      <div className="flex flex-col items-center py-20">
        <p className="text-gray-500">Cotización no encontrada</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate('/quotes')}>
          Volver a cotizaciones
        </Button>
      </div>
    )
  }

  const isCancelled = quote.status === 'ANULADA'
  const isBot = quote.generatedBy === null
  const contactId = quote.contactId

  const actions = (
    <div className="flex flex-wrap items-center gap-2">
      <Button variant="outline" size="sm" onClick={() => quoteService.download(quote.id, quote.fileName)}>
        <Download size={14} />
        Descargar PDF
      </Button>
      <Button size="sm" disabled={isCancelled} onClick={() => resendMutation.mutate({ id: quote.id })}>
        <Send size={14} />
        Reenviar
      </Button>
      <Button variant="outline" size="sm" disabled={isCancelled} onClick={() => setResendToOpen(true)}>
        <PhoneForwarded size={14} />
        Otro número
      </Button>
      <Button variant="outline" size="sm" onClick={() => setHistoryOpen(true)}>
        <History size={14} />
        Historial
      </Button>
      <Button variant="outline" size="sm" disabled={isCancelled} onClick={() => setRegenerateTarget(true)}>
        <RefreshCw size={14} />
        Regenerar
      </Button>
      <Button variant="danger" size="sm" disabled={isCancelled} onClick={() => setCancelTarget(true)}>
        <XCircle size={14} />
        Anular
      </Button>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="sm" onClick={() => navigate('/quotes')}>
            <ArrowLeft size={16} />
          </Button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{quote.quoteNumber}</h1>
              <Badge variant={statusColors[quote.status] ?? 'neutral'}>{statusLabels[quote.status] ?? quote.status}</Badge>
            </div>
            <p className="mt-1 text-sm text-gray-500">
              Generada {dayjs(quote.createdAt).format('DD/MM/YYYY HH:mm')}
            </p>
          </div>
        </div>
        {actions}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <div className="mb-4 flex items-center gap-2">
              <FileText size={16} className="text-gray-400" />
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">Detalle de la cotización</h3>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left text-xs font-medium uppercase text-gray-500 dark:border-gray-700">
                    <th className="pb-2 pr-4">Item</th>
                    <th className="pb-2 pr-4">SKU</th>
                    <th className="pb-2 pr-4 text-right">Cantidad</th>
                    <th className="pb-2 pr-4 text-right">Precio Unitario</th>
                    <th className="pb-2 text-right">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {quote.items.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="py-4 text-center text-gray-400">Sin ítems</td>
                    </tr>
                  ) : (
                    quote.items.map((item, i) => (
                      <tr key={i} className="border-b border-gray-100 dark:border-gray-800">
                        <td className="py-3 pr-4 font-medium text-gray-900 dark:text-gray-100">{item.name}</td>
                        <td className="py-3 pr-4 text-gray-500">{item.sku ?? '—'}</td>
                        <td className="py-3 pr-4 text-right text-gray-700 dark:text-gray-300">{item.quantity}</td>
                        <td className="py-3 pr-4 text-right text-gray-700 dark:text-gray-300">{money(item.unitPrice, quote.currency)}</td>
                        <td className="py-3 text-right font-medium text-gray-900 dark:text-gray-100">
                          {money(item.quantity * item.unitPrice, quote.currency)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={4} className="pt-3 text-right text-gray-500">Subtotal</td>
                    <td className="pt-3 text-right font-medium text-gray-900 dark:text-gray-100">{money(quote.subtotal, quote.currency)}</td>
                  </tr>
                  {quote.discount > 0 && (
                    <tr>
                      <td colSpan={4} className="pt-1 text-right text-gray-500">Descuento</td>
                      <td className="pt-1 text-right font-medium text-red-600">- {money(quote.discount, quote.currency)}</td>
                    </tr>
                  )}
                  <tr>
                    <td colSpan={4} className="pt-1 text-right text-gray-500">IGV (18%)</td>
                    <td className="pt-1 text-right font-medium text-gray-900 dark:text-gray-100">{money(quote.igv, quote.currency)}</td>
                  </tr>
                  <tr>
                    <td colSpan={4} className="pt-2 text-right text-base font-semibold text-gray-900 dark:text-gray-100">Total</td>
                    <td className="pt-2 text-right text-base font-bold text-gray-900 dark:text-gray-100">{money(quote.total, quote.currency)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {quote.observations && (
            <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
              <h3 className="mb-3 text-base font-semibold text-gray-900 dark:text-gray-100">Observaciones</h3>
              <p className="whitespace-pre-wrap text-sm text-gray-700 dark:text-gray-300">{quote.observations}</p>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Cliente</h3>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{quote.customerName ?? 'Sin cliente'}</p>
            {quote.customerPhone && <p className="mt-1 text-sm text-gray-500">📱 {quote.customerPhone}</p>}
            {quote.whatsappPhone && quote.whatsappPhone !== quote.customerPhone && (
              <p className="text-sm text-gray-500">WhatsApp: {quote.whatsappPhone}</p>
            )}
            {contactId && (
              <Button variant="ghost" size="sm" className="mt-2 px-0" onClick={() => navigate(`/contacts/${contactId}`)}>
                Ver contacto
              </Button>
            )}
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Documento</h3>
            <div className="space-y-2 text-sm">
              <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                <HardDrive size={14} className="text-gray-400" />
                <span>Tamaño: {formatSize(quote.fileSize)}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                <Hash size={14} className="text-gray-400" />
                <span className="break-all">Hash: {quote.fileHash ? quote.fileHash.slice(0, 24) + '…' : '—'}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                <Send size={14} className="text-gray-400" />
                <span>Reenvíos: {quote.resendCount}</span>
              </div>
              {quote.lastResentAt && (
                <p className="text-xs text-gray-400">
                  Último reenvío: {dayjs(quote.lastResentAt).format('DD/MM/YYYY HH:mm')}
                </p>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-4 text-base font-semibold text-gray-900 dark:text-gray-100">Generado por</h3>
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
                {isBot ? <Bot size={16} className="text-brand-500" /> : <User size={16} className="text-gray-500" />}
              </div>
              <div>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {quote.generatedByName}
                  {isBot && <Badge variant="info" size="sm" className="ml-2">Bot</Badge>}
                </p>
                <p className="text-xs text-gray-400">
                  {isBot ? 'Generada automáticamente por el bot de WhatsApp' : 'Generada por un usuario del sistema'}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={regenerateTarget}
        onClose={() => setRegenerateTarget(false)}
        onConfirm={() => {
          regenerateMutation.mutate(quote.id)
          setRegenerateTarget(false)
        }}
        title="Regenerar PDF"
        message={`Se regenerará el PDF de "${quote.quoteNumber}" con los datos actuales y reemplazará el almacenado. ¿Continuar?`}
        confirmLabel="Regenerar"
        variant="primary"
        loading={regenerateMutation.isPending}
      />

      <ConfirmDialog
        open={cancelTarget}
        onClose={() => setCancelTarget(false)}
        onConfirm={() => {
          cancelMutation.mutate(quote.id, { onSuccess: () => navigate('/quotes') })
          setCancelTarget(false)
        }}
        title="Anular cotización"
        message={`¿Estás seguro de anular la cotización "${quote.quoteNumber}"? Esta acción no se puede deshacer.`}
        confirmLabel="Anular"
        loading={cancelMutation.isPending}
      />

      <ResendToNumberModal
        open={resendToOpen}
        quoteNumber={quote.quoteNumber}
        onClose={() => setResendToOpen(false)}
        loading={resendMutation.isPending}
        onConfirm={(phone) => {
          resendMutation.mutate({ id: quote.id, phone })
          setResendToOpen(false)
        }}
      />

      <QuoteHistoryModal
        open={historyOpen}
        quoteNumber={quote.quoteNumber}
        history={quote.history}
        loading={false}
        onClose={() => setHistoryOpen(false)}
      />
    </div>
  )
}
