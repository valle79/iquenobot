import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Bell,
  BellRing,
  MessageCircle,
  UserPlus,
  AlertTriangle,
  Info,
  CheckCheck,
  RefreshCw,
  MoreVertical,
  MailOpen,
  Mail,
  Trash2,
  CalendarClock,
  ArrowUpRight,
  Inbox,
} from 'lucide-react'
import { cn } from '@/shared/utils'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Button } from '@/shared/atoms/Button/Button'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { Dropdown, type DropdownItem } from '@/shared/atoms/Dropdown/Dropdown'
import { ErrorState } from '@/shared/molecules/ErrorState'
import { EmptyState } from '@/shared/molecules/EmptyState'
import { dayjs } from '@/config/dayjs'
import {
  useNotifications,
  useUnreadCount,
  useMarkAsRead,
  useMarkAsUnread,
  useMarkAllAsRead,
  useDeleteNotification,
} from '../hooks/useNotifications'
import type { NotificationDto } from '@/types/notification'

const PAGE_SIZE = 20

const typeIcons: Record<string, typeof Bell> = {
  NEW_MESSAGE: MessageCircle,
  NEW_CONVERSATION: MessageCircle,
  CONVERSATION_ASSIGNED: UserPlus,
  CONVERSATION_ESCALATED: UserPlus,
  LEAD_ASSIGNED: UserPlus,
  LEAD_CREATED: UserPlus,
  LEAD_STATUS_CHANGED: Info,
  TASK_REMINDER: Bell,
  SYSTEM_ALERT: AlertTriangle,
  MENTION: MessageCircle,
  LOW_STOCK_ALERT: AlertTriangle,
  NEW_ORDER: Bell,
  ORDER_STATUS_CHANGED: Info,
  WELCOME: Info,
  ANNOUNCEMENT: Bell,
  CUSTOM: Info,
}

const typeColors: Record<string, string> = {
  NEW_MESSAGE: 'bg-blue-100 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400',
  NEW_CONVERSATION: 'bg-blue-100 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400',
  CONVERSATION_ASSIGNED: 'bg-purple-100 text-purple-600 dark:bg-purple-900/20 dark:text-purple-400',
  CONVERSATION_ESCALATED:
    'bg-purple-100 text-purple-600 dark:bg-purple-900/20 dark:text-purple-400',
  LEAD_ASSIGNED: 'bg-purple-100 text-purple-600 dark:bg-purple-900/20 dark:text-purple-400',
  LEAD_CREATED: 'bg-purple-100 text-purple-600 dark:bg-purple-900/20 dark:text-purple-400',
  LEAD_STATUS_CHANGED: 'bg-blue-100 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400',
  SYSTEM_ALERT: 'bg-red-100 text-red-600 dark:bg-red-900/20 dark:text-red-400',
  LOW_STOCK_ALERT: 'bg-amber-100 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400',
  WELCOME: 'bg-green-100 text-green-600 dark:bg-green-900/20 dark:text-green-400',
}

const typeLabels: Record<string, string> = {
  NEW_MESSAGE: 'Nuevo mensaje',
  NEW_CONVERSATION: 'Nueva conversación',
  CONVERSATION_ASSIGNED: 'Conversación asignada',
  CONVERSATION_ESCALATED: 'Conversación escalada',
  LEAD_ASSIGNED: 'Lead asignado',
  LEAD_CREATED: 'Lead creado',
  LEAD_STATUS_CHANGED: 'Estado de lead',
  TASK_REMINDER: 'Recordatorio',
  SYSTEM_ALERT: 'Alerta del sistema',
  MENTION: 'Mención',
  LOW_STOCK_ALERT: 'Stock bajo',
  NEW_ORDER: 'Nuevo pedido',
  ORDER_STATUS_CHANGED: 'Estado de pedido',
  WELCOME: 'Bienvenida',
  ANNOUNCEMENT: 'Anuncio',
  CUSTOM: 'Notificación',
}

type PriorityBadge = { variant: 'error' | 'warning' | 'info' | 'neutral'; label: string }

const priorityBadge = {
  URGENT: { variant: 'error', label: 'Urgente' },
  HIGH: { variant: 'warning', label: 'Alta' },
  NORMAL: { variant: 'info', label: 'Normal' },
  LOW: { variant: 'neutral', label: 'Baja' },
} satisfies Record<string, PriorityBadge>

type Filter = 'all' | 'unread'

function dayGroup(createdAt: string): string {
  const date = dayjs.utc(createdAt)
  const today = dayjs.utc().startOf('day')
  if (date.isSame(today, 'day')) return 'Hoy'
  if (date.isSame(today.subtract(1, 'day'), 'day')) return 'Ayer'
  if (date.isAfter(today.subtract(7, 'day'))) return 'Esta semana'
  return 'Anterior'
}

function groupByDay(items: NotificationDto[]): Array<{ label: string; items: NotificationDto[] }> {
  const groups = new Map<string, NotificationDto[]>()
  for (const item of items) {
    const label = dayGroup(item.createdAt)
    const current = groups.get(label) ?? []
    current.push(item)
    groups.set(label, current)
  }
  return [
    { label: 'Hoy', items: groups.get('Hoy') ?? [] },
    { label: 'Ayer', items: groups.get('Ayer') ?? [] },
    { label: 'Esta semana', items: groups.get('Esta semana') ?? [] },
    { label: 'Anterior', items: groups.get('Anterior') ?? [] },
  ].filter((group) => group.items.length > 0)
}

interface NotificationItemProps {
  notification: NotificationDto
  onMarkRead: (id: string) => void
  onMarkUnread: (id: string) => void
  onDelete: (id: string) => void
}

function NotificationItem({
  notification,
  onMarkRead,
  onMarkUnread,
  onDelete,
}: NotificationItemProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const navigate = useNavigate()
  const Icon = typeIcons[notification.type] ?? Bell
  const colorClass = typeColors[notification.type] ?? 'bg-gray-100 text-gray-500 dark:bg-gray-800'
  const priority = priorityBadge[notification.priority ?? 'NORMAL'] ?? priorityBadge.NORMAL
  const isQuote = notification.message.startsWith('«')

  const menuItems: DropdownItem[] = [
    {
      label: notification.read ? 'Marcar como no leída' : 'Marcar como leída',
      icon: notification.read ? Mail : MailOpen,
      onClick: () =>
        notification.read ? onMarkUnread(notification.id) : onMarkRead(notification.id),
    },
    { type: 'separator' },
    {
      label: 'Eliminar',
      icon: Trash2,
      danger: true,
      onClick: () => onDelete(notification.id),
    },
  ]

  const handleOpen = () => {
    if (!notification.read) onMarkRead(notification.id)
    if (notification.actionUrl) navigate(notification.actionUrl)
  }

  return (
    <div
      className={cn(
        'group flex gap-4 border-b border-gray-100 p-4 transition-colors last:border-b-0 dark:border-gray-800',
        'first:rounded-t-xl last:rounded-b-xl',
        !notification.read && 'bg-brand-50/60 dark:bg-brand-900/10',
      )}
      onClick={() => {
        if (!notification.read) onMarkRead(notification.id)
      }}
    >
      <div className="flex flex-col items-center gap-2">
        {!notification.read && <span className="bg-brand-600 mt-1 h-2 w-2 shrink-0 rounded-full" />}
        {notification.imageUrl ? (
          <img
            src={notification.imageUrl}
            alt=""
            className="h-10 w-10 shrink-0 rounded-full object-cover ring-2 ring-gray-100 dark:ring-gray-800"
          />
        ) : (
          <div
            className={cn(
              'flex h-10 w-10 shrink-0 items-center justify-center rounded-full',
              colorClass,
            )}
          >
            <Icon size={18} />
          </div>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <p className="truncate text-sm font-semibold text-gray-900 dark:text-gray-100">
            {notification.title}
          </p>
          <div onClick={(e) => e.stopPropagation()} className="relative shrink-0">
            <Dropdown
              open={menuOpen}
              onOpenChange={setMenuOpen}
              align="end"
              items={menuItems}
              trigger={
                <Button
                  variant="ghost"
                  size="sm"
                  icon
                  className="opacity-0 group-hover:opacity-100 focus-visible:opacity-100"
                >
                  <MoreVertical size={16} />
                </Button>
              }
            />
          </div>
        </div>

        <p
          className={cn(
            'mt-0.5 line-clamp-2 text-sm',
            isQuote ? 'text-brand-700 dark:text-brand-400' : 'text-gray-600 dark:text-gray-400',
          )}
        >
          {notification.message}
        </p>

        <div className="mt-2 flex flex-wrap items-center gap-2">
          <span className="text-xs text-gray-400">
            {dayjs.utc(notification.createdAt).fromNow()}
          </span>
          <Badge variant={priority.variant} size="sm">
            {priority.label}
          </Badge>
          <Badge variant="neutral" size="sm">
            {typeLabels[notification.type] ?? notification.type}
          </Badge>
        </div>
      </div>

      {notification.actionUrl && (
        <Button
          variant="outline"
          size="sm"
          className="mt-1 shrink-0 self-start"
          onClick={(e) => {
            e.stopPropagation()
            handleOpen()
          }}
        >
          {notification.actionLabel || 'Atender'}
          <ArrowUpRight size={14} />
        </Button>
      )}
    </div>
  )
}

function NotificationsSkeleton() {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-3 gap-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} variant="rectangular" height={80} />
        ))}
      </div>
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="flex gap-4 border-b border-gray-100 p-4 last:border-b-0 dark:border-gray-800"
          >
            <Skeleton variant="circular" width={40} height={40} />
            <div className="flex-1 space-y-2">
              <Skeleton width="50%" />
              <Skeleton width="80%" />
              <Skeleton width="35%" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function NotificationsPage() {
  const { t } = useTranslation()
  const [filter, setFilter] = useState<Filter>('all')
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<NotificationDto[]>([])
  const pageRef = useRef(0)

  const { data, isLoading, isError, error, refetch, isFetching } = useNotifications(
    page,
    PAGE_SIZE,
    filter === 'unread',
  )
  const unreadCountQuery = useUnreadCount()
  const markAsReadMutation = useMarkAsRead()
  const markAsUnreadMutation = useMarkAsUnread()
  const markAllAsReadMutation = useMarkAllAsRead()
  const deleteMutation = useDeleteNotification()

  useEffect(() => {
    pageRef.current = -1
    setItems([])
    setPage(0)
  }, [filter])

  useEffect(() => {
    if (!data) {
      return
    }

    if (data.page === 0) {
      // reemplaza completamente la lista
      setItems(data.content)
    } else if (data.page > pageRef.current) {
      // agrega páginas adicionales
      setItems((prev) => [...prev, ...data.content])
    }

    pageRef.current = data.page
  }, [data])

  const handleRefetch = async () => {
    pageRef.current = 0

    // NO borrar items
    await refetch()
  }

  const grouped = useMemo(() => groupByDay(items), [items])
  const unreadCount = unreadCountQuery.data ?? items.filter((n) => !n.read).length
  const highCount = items.filter((n) => n.priority === 'HIGH' || n.priority === 'URGENT').length
  const todayCount = items.filter((n) => dayGroup(n.createdAt) === 'Hoy').length
  const totalElements = data?.totalElements ?? 0
  const hasMore = data ? page + 1 < data.totalPages : false

  const stats = [
    {
      label: 'Sin leer',
      value: unreadCount,
      icon: BellRing,
      color: 'bg-red-50 text-red-600 dark:bg-red-900/20 dark:text-red-400',
    },
    {
      label: 'Alta prioridad',
      value: highCount,
      icon: AlertTriangle,
      color: 'bg-amber-50 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400',
    },
    {
      label: 'De hoy',
      value: todayCount,
      icon: CalendarClock,
      color: 'bg-blue-50 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400',
    },
  ]

  if (isLoading && items.length === 0) return <NotificationsSkeleton />

  if (isError && items.length === 0) {
    return (
      <ErrorState
        title="Error al cargar notificaciones"
        message={error instanceof Error ? error.message : 'Error desconocido'}
        onRetry={() => handleRefetch()}
      />
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-brand-600 dark:bg-brand-500 flex h-11 w-11 items-center justify-center rounded-xl text-white shadow-sm">
            <Bell size={22} />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
              {t('navigation.notifications')}
            </h1>
            <p className="mt-0.5 text-sm text-gray-500">
              {unreadCount > 0
                ? `${unreadCount} sin leer · ${totalElements} en total`
                : 'Estás al día, sin notificaciones pendientes'}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" icon onClick={handleRefetch} title="Actualizar">
            <RefreshCw size={16} className={cn(isFetching && 'animate-spin')} />
          </Button>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => markAllAsReadMutation.mutate()}
              loading={markAllAsReadMutation.isPending}
            >
              <CheckCheck size={16} />
              Marcar todas como leídas
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        {stats.map((stat) => (
          <div
            key={stat.label}
            className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-950"
          >
            <div
              className={cn(
                'flex h-10 w-10 shrink-0 items-center justify-center rounded-lg',
                stat.color,
              )}
            >
              <stat.icon size={18} />
            </div>
            <div className="min-w-0">
              <p className="text-xl leading-tight font-bold text-gray-900 dark:text-gray-100">
                {stat.value}
              </p>
              <p className="truncate text-xs text-gray-500">{stat.label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="flex items-center gap-1 rounded-lg bg-gray-100 p-1 dark:bg-gray-800/60">
        {(['all', 'unread'] as Filter[]).map((option) => (
          <button
            key={option}
            onClick={() => setFilter(option)}
            className={cn(
              'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
              filter === option
                ? 'bg-white text-gray-900 shadow-sm dark:bg-gray-700 dark:text-gray-100'
                : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300',
            )}
          >
            {option === 'all' ? 'Todas' : 'No leídas'}
          </button>
        ))}
      </div>

      {items.length === 0 ? (
        <EmptyState
          icon={<Inbox size={48} />}
          title={filter === 'unread' ? 'Todo leído' : 'Sin notificaciones'}
          description={
            filter === 'unread'
              ? 'No tienes notificaciones sin leer. Cuando te asignen una conversación aparecerá aquí.'
              : 'No hay notificaciones para mostrar. Las asignaciones de conversaciones y los mensajes aparecerán aquí.'
          }
        />
      ) : (
        <div className="space-y-6">
          {grouped.map((group) => (
            <div key={group.label}>
              <div className="mb-2 flex items-center gap-2 px-1">
                <h2 className="text-xs font-semibold tracking-wider text-gray-400 uppercase">
                  {group.label}
                </h2>
                <div className="h-px flex-1 bg-gray-200 dark:bg-gray-800" />
              </div>
              <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
                {group.items.map((notification) => (
                  <NotificationItem
                    key={notification.id}
                    notification={notification}
                    onMarkRead={(id) => markAsReadMutation.mutate(id)}
                    onMarkUnread={(id) => markAsUnreadMutation.mutate(id)}
                    onDelete={(id) => deleteMutation.mutate(id)}
                  />
                ))}
              </div>
            </div>
          ))}

          {hasMore && (
            <div className="flex justify-center pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((prev) => prev + 1)}
                loading={isFetching && page > 0}
              >
                Cargar más
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
