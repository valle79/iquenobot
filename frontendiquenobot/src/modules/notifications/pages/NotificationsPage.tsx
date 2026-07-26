import { useTranslation } from 'react-i18next'
import { Bell, MessageCircle, UserPlus, AlertTriangle, Info, CheckCheck, RefreshCw } from 'lucide-react'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Button } from '@/shared/atoms/Button/Button'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { ErrorState } from '@/shared/molecules/ErrorState'
import { EmptyState } from '@/shared/molecules/EmptyState'
import { dayjs } from '@/config/dayjs'
import { useNotifications, useMarkAsRead, useMarkAllAsRead } from '../hooks/useNotifications'
import type { NotificationDto } from '@/types/notification'

const typeIcons: Record<string, typeof Bell> = {
  NEW_MESSAGE: MessageCircle,
  NEW_CONVERSATION: MessageCircle,
  CONVERSATION_ASSIGNED: UserPlus,
  LEAD_ASSIGNED: UserPlus,
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
  LEAD_ASSIGNED: 'bg-purple-100 text-purple-600 dark:bg-purple-900/20 dark:text-purple-400',
  SYSTEM_ALERT: 'bg-red-100 text-red-600 dark:bg-red-900/20 dark:text-red-400',
  LOW_STOCK_ALERT: 'bg-amber-100 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400',
  WELCOME: 'bg-green-100 text-green-600 dark:bg-green-900/20 dark:text-green-400',
}

function NotificationItem({ notification, onMarkRead }: { notification: NotificationDto; onMarkRead: (id: string) => void }) {
  const Icon = typeIcons[notification.type] ?? Bell
  const colorClass = typeColors[notification.type] ?? 'bg-gray-100 text-gray-500 dark:bg-gray-800'

  return (
    <div
      className={`flex gap-4 border-b border-gray-100 p-4 transition-colors last:border-b-0 hover:bg-gray-50 dark:border-gray-800 dark:hover:bg-gray-800/50 ${!notification.read ? 'bg-brand-50/50 dark:bg-brand-900/5' : ''}`}
      onClick={() => { if (!notification.read) onMarkRead(notification.id) }}
    >
      <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${colorClass}`}>
        <Icon size={18} />
      </div>
      <div className="flex-1">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{notification.title}</p>
            <p className="text-sm text-gray-500">{notification.message}</p>
          </div>
          {!notification.read && <span className="h-2 w-2 rounded-full bg-brand-600" />}
        </div>
        <div className="mt-1 flex items-center gap-2">
          <p className="text-xs text-gray-400">{dayjs(notification.createdAt).fromNow()}</p>
          <Badge variant={notification.priority === 'HIGH' || notification.priority === 'URGENT' ? 'warning' : 'neutral'} size="sm">
            {notification.priority}
          </Badge>
        </div>
      </div>
    </div>
  )
}

function NotificationsSkeleton() {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Skeleton width={200} height={28} />
          <Skeleton width={250} height={16} className="mt-2" />
        </div>
      </div>
      <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex gap-4 border-b border-gray-100 p-4 last:border-b-0 dark:border-gray-800">
            <Skeleton variant="circular" width={40} height={40} />
            <div className="flex-1 space-y-2">
              <Skeleton width="60%" />
              <Skeleton width="80%" />
              <Skeleton width="30%" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function NotificationsPage() {
  const { t } = useTranslation()
  const { data, isLoading, isError, error, refetch } = useNotifications()
  const markAsReadMutation = useMarkAsRead()
  const markAllAsReadMutation = useMarkAllAsRead()

  if (isLoading) return <NotificationsSkeleton />

  if (isError) {
    return (
      <ErrorState
        title="Error al cargar notificaciones"
        message={error instanceof Error ? error.message : 'Error desconocido'}
        onRetry={() => refetch()}
      />
    )
  }

  const notifications = data?.content ?? []
  const unreadCount = notifications.filter((n) => !n.read).length

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.notifications')}</h1>
          <p className="mt-1 text-sm text-gray-500">
            {unreadCount > 0 ? `Tienes ${unreadCount} notificaciones sin leer` : 'No hay notificaciones nuevas'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={() => refetch()}>
            <RefreshCw size={16} />
          </Button>
          {unreadCount > 0 && (
            <Button variant="ghost" size="sm" onClick={() => markAllAsReadMutation.mutate()} loading={markAllAsReadMutation.isPending}>
              <CheckCheck size={16} />
              Marcar todas como leídas
            </Button>
          )}
        </div>
      </div>

      {notifications.length === 0 ? (
        <EmptyState
          icon={<Bell size={48} />}
          title="Sin notificaciones"
          description="No hay notificaciones para mostrar."
        />
      ) : (
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
          {notifications.map((n) => (
            <NotificationItem
              key={n.id}
              notification={n}
              onMarkRead={(id) => markAsReadMutation.mutate(id)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
