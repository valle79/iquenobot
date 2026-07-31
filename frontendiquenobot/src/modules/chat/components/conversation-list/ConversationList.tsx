import { useState, useMemo, memo } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Search,
  MessageCircle,
  Inbox,
  UserCheck,
  Users,
  MessageSquare,
  Mail,
  Globe,
  Send,
  Instagram,
  Smartphone,
} from 'lucide-react'

import { useConversations } from '@/modules/chat/hooks/useConversations'
import { useChatStore } from '@/modules/chat/stores/chat.store'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { cn } from '@/shared/utils'
import { dayjs } from '@/config/dayjs'

import type { ConversationDto } from '@/types/chat'
import type { ChannelType } from '@/types/enums'

const CHANNEL_ICONS: Record<ChannelType, typeof MessageCircle> = {
  WHATSAPP: MessageCircle,
  MESSENGER: MessageSquare,
  INSTAGRAM: Instagram,
  EMAIL: Mail,
  WEBCHAT: Globe,
  SMS: Send,
  TELEGRAM: Send,
  TWITTER: MessageSquare,
  API: Globe,
}

const CHANNEL_COLORS: Record<ChannelType, string> = {
  WHATSAPP: 'text-green-600',
  MESSENGER: 'text-blue-500',
  INSTAGRAM: 'text-pink-500',
  EMAIL: 'text-blue-600',
  WEBCHAT: 'text-indigo-500',
  SMS: 'text-amber-500',
  TELEGRAM: 'text-sky-600',
  TWITTER: 'text-sky-500',
  API: 'text-gray-500',
}

type FilterTab = 'all' | 'mine' | 'unassigned'

const ConversationItem = memo(function ConversationItem({
  conversation,
  isActive,
}: {
  conversation: ConversationDto
  isActive: boolean
}) {
  const contact = conversation.contact
  const lastMessage = conversation.lastMessage
  const unread = conversation.unreadCount ?? 0

// ===============================
// DISPLAY NAME PRIORITY (WhatsApp-like)
// ===============================
const displayName =
  contact?.displayName?.trim() ||
  contact?.fullName?.trim() ||
  conversation?.subject?.trim() ||
  contact?.phone ||
  'Contacto sin nombre'

  const avatarName = displayName
  const avatarUrl = contact?.avatarUrl

  const statusVariant =
    conversation.status === 'OPEN'
      ? 'info'
      : conversation.status === 'IN_PROGRESS'
        ? 'warning'
        : conversation.status === 'RESOLVED'
          ? 'success'
          : conversation.status === 'CLOSED'
            ? 'neutral'
            : 'neutral'

  return (
    <div
      className={cn(
        'flex cursor-pointer gap-3 border-b border-gray-100 px-4 py-3 transition-colors last:border-b-0 dark:border-gray-800',
        isActive
          ? 'bg-brand-50 dark:bg-brand-900/10'
          : 'hover:bg-gray-50 dark:hover:bg-gray-800/50',
      )}
    >
      <Avatar
        name={avatarName}
        src={avatarUrl}
        size="md"
      />

      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between">
          <span className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
            {displayName}
          </span>

          {conversation.lastMessageAt && (
            <span className="ml-2 shrink-0 text-xs text-gray-400">
              {dayjs(conversation.lastMessageAt).fromNow()}
            </span>
          )}
        </div>

        <div className="mt-0.5 flex items-center justify-between">
          <p className="truncate text-sm text-gray-500 dark:text-gray-400">
            {lastMessage?.content ?? 'Sin mensajes'}
          </p>

          {unread > 0 && (
            <span className="ml-2 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-600 px-1.5 text-[11px] font-bold text-white">
              {unread > 99 ? '99+' : unread}
            </span>
          )}
        </div>

        <div className="mt-1 flex items-center gap-2">
          <Badge variant={statusVariant} size="sm">
            {conversation.status === 'IN_PROGRESS'
              ? 'En curso'
              : conversation.status === 'RESOLVED'
                ? 'Resuelto'
                : conversation.status === 'CLOSED'
                  ? 'Cerrado'
                  : conversation.status === 'PENDING'
                    ? 'Pendiente'
                    : conversation.status === 'SPAM'
                      ? 'Spam'
                      : 'Abierto'}
          </Badge>

{(() => {
  const ChannelIcon = CHANNEL_ICONS[conversation.channel]
  const channelColor = CHANNEL_COLORS[conversation.channel]

  return ChannelIcon ? (
    <div aria-label={conversation.channel} title={conversation.channel}>
      <ChannelIcon
        size={12}
        className={channelColor}
      />
    </div>
  ) : null
})()}

          {conversation.priority === 'HIGH' && (
            <Badge variant="warning" size="sm" dot>
              Alta
            </Badge>
          )}

          {conversation.priority === 'URGENT' && (
            <Badge variant="error" size="sm" dot>
              Urgente
            </Badge>
          )}

          {conversation.botConversation && (
            <Badge variant="info" size="sm">
              Bot
            </Badge>
          )}
        </div>
      </div>
    </div>
  )
})

export function ConversationList() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { id: activeId } = useParams()

  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<FilterTab>('all')

  const { conversations } = useChatStore()

  useConversations({
    ...(filter === 'mine' ? { assigned: 'mine' } : {}),
    ...(filter === 'unassigned' ? { assigned: 'unassigned' } : {}),
  })

  const filteredConversations = useMemo(() => {
    if (!search.trim()) return conversations

    const q = search.toLowerCase()

    return conversations.filter((c) => {
      const contact = c.contact

const name = (
  contact?.displayName?.trim() ||
  contact?.fullName?.trim() ||
  c.subject?.trim() ||
  contact?.phone ||
  ''
).toLowerCase()

      const email = contact?.email?.toLowerCase() ?? ''
      const phone = contact?.phone ?? ''

      return (
        name.includes(q) ||
        email.includes(q) ||
        phone.includes(q)
      )
    })
  }, [conversations, search])

  const filters: {
    key: FilterTab
    label: string
    icon: typeof Inbox
  }[] = [
    { key: 'all', label: 'Todas', icon: Inbox },
    { key: 'mine', label: 'Mis conv.', icon: UserCheck },
    { key: 'unassigned', label: 'Sin asignar', icon: Users },
  ]

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-gray-200 p-4 dark:border-gray-700">
        <div className="relative">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            size={16}
          />

          <input
            type="text"
            placeholder={t('chat.searchConversations')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 pl-9 pr-3 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
          />
        </div>
      </div>

      <div className="flex gap-1 border-b border-gray-200 px-4 py-2 dark:border-gray-700">
        {filters.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setFilter(key)}
            className={cn(
              'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors',
              filter === key
                ? 'bg-brand-100 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'
                : 'text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800',
            )}
          >
            <Icon size={14} />
            {label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto">
        {filteredConversations.length === 0 ? (
          <div className="flex flex-col items-center py-12 text-center">
            <MessageCircle
              className="mb-3 text-gray-300 dark:text-gray-600"
              size={40}
            />

            <p className="text-sm text-gray-500">
              {t('chat.noMessages')}
            </p>
          </div>
        ) : (
          filteredConversations.map((conv) => (
            <div
              key={conv.id}
              onClick={() => navigate(`/conversations/${conv.id}`)}
            >
              <ConversationItem
                conversation={conv}
                isActive={conv.id === activeId}
              />
            </div>
          ))
        )}
      </div>
    </div>
  )
}