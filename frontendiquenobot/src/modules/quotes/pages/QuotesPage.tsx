import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import {
  MoreHorizontal,
  Eye,
  Download,
  Send,
  History,
  RefreshCw,
  XCircle,
  FileText,
  PhoneForwarded,
} from 'lucide-react'
import { useQuotes, useResendQuote, useRegenerateQuote, useCancelQuote } from '../hooks/useQuotes'
import { QuoteFiltersBar } from '../components/QuoteFiltersBar'
import { ResendToNumberModal } from '../components/ResendToNumberModal'
import { QuoteHistoryModal } from '../components/QuoteHistoryModal'
import { quoteService } from '@/services/quote.service'
import { useUsers } from '@/modules/users/hooks/useUsers'
import { DataTable } from '@/shared/organisms/DataTable/DataTable'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { ConfirmDialog } from '@/shared/molecules/ConfirmDialog'
import { SearchBar } from '@/shared/molecules/SearchBar'
import { dayjs } from '@/config/dayjs'
import type { ColumnDef } from '@tanstack/react-table'
import type { QuoteHistoryItem, QuoteSummaryDto } from '@/types/quote'

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

const formatMoney = (value: number, currency: string) =>
  `${currency === 'USD' ? '$' : 'S/'} ${(value ?? 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

export default function QuotesPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const {
    quotes, totalElements, totalPages, page, setPage,
    search, setSearch, filters, setFilters, isLoading,
  } = useQuotes()
  const { users } = useUsers()
  const resendMutation = useResendQuote()
  const regenerateMutation = useRegenerateQuote()
  const cancelMutation = useCancelQuote()

  const [cancelTarget, setCancelTarget] = useState<QuoteSummaryDto | null>(null)
  const [regenerateTarget, setRegenerateTarget] = useState<QuoteSummaryDto | null>(null)
  const [resendToTarget, setResendToTarget] = useState<QuoteSummaryDto | null>(null)
  const [historyTarget, setHistoryTarget] = useState<QuoteSummaryDto | null>(null)
  const [history, setHistory] = useState<QuoteHistoryItem[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)

  const openHistory = async (quote: QuoteSummaryDto) => {
    setHistoryTarget(quote)
    setHistory([])
    setHistoryLoading(true)
    try {
      setHistory(await quoteService.getHistory(quote.id))
    } catch {
      setHistory([])
    } finally {
      setHistoryLoading(false)
    }
  }

  const columns: ColumnDef<QuoteSummaryDto>[] = [
    {
      header: 'Nº Cotización',
      accessorKey: 'quoteNumber',
      cell: ({ row }) => (
        <button
          className="flex items-center gap-2 text-sm font-semibold text-brand-600 hover:underline dark:text-brand-400"
          onClick={() => navigate(`/quotes/${row.original.id}`)}
        >
          <FileText size={14} />
          {row.original.quoteNumber}
        </button>
      ),
    },
    {
      header: 'Cliente',
      accessorKey: 'customerName',
      cell: ({ row }) => (
        <div>
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
            {row.original.customerName ?? 'Sin cliente'}
          </p>
          {row.original.generatedByName && (
            <p className="text-xs text-gray-400">
              {row.original.generatedBy === null ? '🤖 ' : ''}{row.original.generatedByName}
            </p>
          )}
        </div>
      ),
    },
    {
      header: 'WhatsApp',
      accessorKey: 'customerPhone',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">
          {row.original.customerPhone ?? row.original.whatsappPhone ?? '—'}
        </span>
      ),
    },
    {
      header: 'Fecha',
      accessorKey: 'createdAt',
      cell: ({ row }) => (
        <span className="text-sm text-gray-600 dark:text-gray-400">
          {dayjs(row.original.createdAt).format('DD/MM/YYYY HH:mm')}
        </span>
      ),
    },
    {
      header: 'Total',
      accessorKey: 'total',
      cell: ({ row }) => (
        <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
          {formatMoney(row.original.total, row.original.currency)}
        </span>
      ),
    },
    {
      header: 'Estado',
      accessorKey: 'status',
      cell: ({ row }) => (
        <Badge variant={statusColors[row.original.status] ?? 'neutral'} size="sm">
          {statusLabels[row.original.status] ?? row.original.status}
        </Badge>
      ),
    },
    {
      header: 'Reenvíos',
      accessorKey: 'resendCount',
      cell: ({ row }) => (
        <div className="text-sm">
          <span className="font-medium text-gray-700 dark:text-gray-300">{row.original.resendCount}</span>
          {row.original.lastResentAt && (
            <span className="block text-xs text-gray-400">
              {dayjs(row.original.lastResentAt).format('DD/MM/YYYY')}
            </span>
          )}
        </div>
      ),
    },
    {
      header: 'Acciones',
      id: 'actions',
      cell: ({ row }) => {
        const [menuOpen, setMenuOpen] = useState(false)
        const quote = row.original
        const isCancelled = quote.status === 'ANULADA'

        return (
          <Dropdown
            open={menuOpen}
            onOpenChange={setMenuOpen}
            align="end"
            trigger={
              <Button variant="ghost" size="sm" icon>
                <MoreHorizontal size={16} />
              </Button>
            }
            items={[
              { label: 'Ver detalle', icon: Eye, onClick: () => navigate(`/quotes/${quote.id}`) },
              {
                label: 'Descargar PDF',
                icon: Download,
                onClick: () => quoteService.download(quote.id, quote.fileName),
              },
              { type: 'separator' },
              ...(!isCancelled
                ? ([
                    {
                      label: 'Reenviar por WhatsApp',
                      icon: Send,
                      onClick: () => resendMutation.mutate({ id: quote.id }),
                    },
                    {
                      label: 'Enviar a otro número',
                      icon: PhoneForwarded,
                      onClick: () => setResendToTarget(quote),
                    },
                  ] as const)
                : []),
              {
                label: 'Ver historial',
                icon: History,
                onClick: () => openHistory(quote),
              },
              { type: 'separator' },
              ...(!isCancelled
                ? ([
                    {
                      label: 'Regenerar PDF',
                      icon: RefreshCw,
                      onClick: () => setRegenerateTarget(quote),
                    },
                    {
                      label: 'Anular',
                      icon: XCircle,
                      danger: true as const,
                      onClick: () => setCancelTarget(quote),
                    },
                  ] as const)
                : []),
            ]}
          />
        )
      },
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.quotes')}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Gestión documental de cotizaciones ({totalElements} total)
          </p>
        </div>
      </div>

      <QuoteFiltersBar filters={filters} onChange={setFilters} users={users} />

      <div className="flex items-center gap-3">
        <SearchBar
          placeholder="Buscar por cliente, número, teléfono..."
          value={search}
          onSearch={setSearch}
          className="max-w-sm"
        />
        {(Object.keys(filters).length > 0 || search) && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => { setFilters({}); setSearch('') }}
          >
            Limpiar filtros
          </Button>
        )}
      </div>

      <DataTable
        columns={columns}
        data={quotes}
        loading={isLoading}
        pageCount={totalPages}
        pageIndex={page}
        onPageChange={setPage}
        totalRecords={totalElements}
        emptyMessage="No se encontraron cotizaciones"
      />

      <ConfirmDialog
        open={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        onConfirm={() => {
          if (cancelTarget) cancelMutation.mutate(cancelTarget.id)
          setCancelTarget(null)
        }}
        title="Anular cotización"
        message={cancelTarget ? `¿Estás seguro de anular la cotización "${cancelTarget.quoteNumber}"? Esta acción no se puede deshacer.` : ''}
        confirmLabel="Anular"
        loading={cancelMutation.isPending}
      />

      <ConfirmDialog
        open={!!regenerateTarget}
        onClose={() => setRegenerateTarget(null)}
        onConfirm={() => {
          if (regenerateTarget) regenerateMutation.mutate(regenerateTarget.id)
          setRegenerateTarget(null)
        }}
        title="Regenerar PDF"
        message={regenerateTarget ? `Se regenerará el PDF de "${regenerateTarget.quoteNumber}" con los datos actuales y reemplazará el almacenado. ¿Continuar?` : ''}
        confirmLabel="Regenerar"
        variant="primary"
        loading={regenerateMutation.isPending}
      />

      <ResendToNumberModal
        open={!!resendToTarget}
        quoteNumber={resendToTarget?.quoteNumber ?? ''}
        onClose={() => setResendToTarget(null)}
        loading={resendMutation.isPending}
        onConfirm={(phone) => {
          if (resendToTarget) resendMutation.mutate({ id: resendToTarget.id, phone })
          setResendToTarget(null)
        }}
      />

      <QuoteHistoryModal
        open={!!historyTarget}
        quoteNumber={historyTarget?.quoteNumber ?? ''}
        history={history}
        loading={historyLoading}
        onClose={() => setHistoryTarget(null)}
      />
    </div>
  )
}
