import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Info,
  CheckCheck,
  X,
  RotateCcw,
  UserPlus,
  MoreHorizontal,
  Tag,
  FileText,
  UserCircle,
  AlertTriangle,
  Clock,
} from 'lucide-react'
import { useConversation, useUpdateConversation } from '@/modules/chat/hooks/useConversations'
import { useAuthStore } from '@/core/auth/auth.store'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Button } from '@/shared/atoms/Button/Button'
import { Dropdown } from '@/shared/atoms/Dropdown/Dropdown'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { AssignAgentModal } from '@/modules/chat/components/AssignAgentModal'
import { TagsManager } from '@/modules/chat/components/TagsManager'
import { InternalNotes } from '@/modules/chat/components/InternalNotes'
import { dayjs } from '@/config/dayjs'

interface ChatToolbarProps {
  onToggleInfo: () => void
  showInfo: boolean
}

export function ChatToolbar({ onToggleInfo, showInfo }: ChatToolbarProps) {
  const { t } = useTranslation()
  const { id: conversationId } = useParams<{ id: string }>()
  const { data: conversation, isLoading } = useConversation(conversationId)
  const { resolve, close, reopen } = useUpdateConversation()
  const currentUserId = useAuthStore((s) => s.user?.id)
  const [menuOpen, setMenuOpen] = useState(false)
  const [showAssign, setShowAssign] = useState(false)
  const [showTags, setShowTags] = useState(false)
  const [showNotes, setShowNotes] = useState(false)

  if (!conversationId) return null

  if (isLoading) {
    return (
      <div className="flex h-16 items-center gap-3 border-b border-gray-200 px-4 dark:border-gray-700">
        <Skeleton variant="circular" width={36} height={36} />
        <div className="space-y-1.5">
          <Skeleton width={150} />
          <Skeleton width={100} />
        </div>
      </div>
    )
  }

  const contact = conversation?.contact
  const isResolved = conversation?.status === 'RESOLVED'
  const isClosed = conversation?.status === 'CLOSED'

  return (
    <>
      <div className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-4 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex items-center gap-3">
          <Avatar
            name={contact?.displayName || contact?.fullName || ''}
            src={contact?.avatarUrl}
            size="md"
            status={
              conversation?.assignedUser ? 'online' : 'offline'
            }
          />
          <div>
            <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
              {contact?.displayName || contact?.fullName || 'Sin nombre'}
            </h2>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-500">
                {conversation?.channel ?? ''}
              </span>
              {conversation?.status && (
                <Badge
                  variant={
                    conversation.status === 'OPEN' ? 'info'
                    : conversation.status === 'IN_PROGRESS' ? 'warning'
                    : conversation.status === 'RESOLVED' ? 'success'
                    : conversation.status === 'CLOSED' ? 'neutral'
                    : 'neutral'
                  }
                  size="sm"
                >
                  {conversation.status === 'IN_PROGRESS' ? 'En curso'
                   : conversation.status === 'RESOLVED' ? 'Resuelto'
                   : conversation.status === 'CLOSED' ? 'Cerrado'
                   : conversation.status === 'PENDING' ? 'Pendiente'
                   : conversation.status === 'SPAM' ? 'Spam' : 'Abierto'}
                </Badge>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1">
          {!isResolved && !isClosed && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => resolve(conversationId!)}
            >
              <CheckCheck size={16} />
              Resolver
            </Button>
          )}

          {isResolved && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => reopen(conversationId!)}
            >
              <RotateCcw size={16} />
              Reabrir
            </Button>
          )}

          {!isClosed && !isResolved && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => close(conversationId!)}
            >
              <X size={16} />
              Cerrar
            </Button>
          )}

          <Dropdown
            open={menuOpen}
            onOpenChange={setMenuOpen}
            trigger={
              <Button variant="ghost" size="sm" icon>
                <MoreHorizontal size={18} />
              </Button>
            }
            items={[
              { label: 'Asignar agente', icon: UserPlus, onClick: () => setShowAssign(true) },
              ...(showTags ? [] : [{ label: 'Etiquetas', icon: Tag, onClick: () => setShowTags(true) }]),
              ...(showNotes ? [] : [{ label: 'Notas internas', icon: FileText, onClick: () => setShowNotes(true) }]),
              { label: 'Ver perfil', icon: UserCircle, onClick: () => {} },
            ]}
          />

          <Button
            variant={showInfo ? 'secondary' : 'ghost'}
            size="sm"
            icon
            onClick={onToggleInfo}
          >
            <Info size={18} />
          </Button>
        </div>
      </div>

      {showTags && conversationId && (
        <div className="border-b border-gray-200 px-4 py-2 dark:border-gray-700">
          <TagsManager conversationId={conversationId} />
        </div>
      )}

      {showNotes && conversationId && (
        <div className="border-b border-gray-200 px-4 py-2 dark:border-gray-700">
          <InternalNotes conversationId={conversationId} />
        </div>
      )}

      {showAssign && conversationId && (
        <AssignAgentModal
          open={showAssign}
          conversationId={conversationId}
          currentUserId={currentUserId ?? null}
          onClose={() => setShowAssign(false)}
        />
      )}

      {conversation?.botConversation && conversation?.botHandoffAt && (
        <div className="flex items-center gap-3 border-b border-amber-200 bg-amber-50 px-4 py-2.5 dark:border-amber-800 dark:bg-amber-900/10">
          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
            <AlertTriangle size={14} className="text-amber-600 dark:text-amber-400" />
          </div>
          <div className="flex-1">
            <p className="text-xs font-medium text-amber-800 dark:text-amber-300">
              Conversación transferida del bot a un agente humano
            </p>
            <p className="flex items-center gap-1 text-[11px] text-amber-600 dark:text-amber-400">
              <Clock size={10} />
              Transferida {dayjs(conversation.botHandoffAt).fromNow()}
            </p>
          </div>
        </div>
      )}
    </>
  )
}
