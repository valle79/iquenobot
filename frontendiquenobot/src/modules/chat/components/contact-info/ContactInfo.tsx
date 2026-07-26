import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Mail,
  Phone,
  Building,
  Globe,
  Calendar,
  Tag,
  MessageCircle,
  Clock,
} from 'lucide-react'
import { useConversation } from '@/modules/chat/hooks/useConversations'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { dayjs } from '@/config/dayjs'
import { cn } from '@/shared/utils'

interface ContactInfoProps {
  conversationId: string
  open: boolean
  onClose: () => void
}

function InfoRow({ icon: Icon, label, value }: { icon: typeof Mail; label: string; value: string }) {
  if (!value) return null

  return (
    <div className="flex items-start gap-3">
      <div className="mt-0.5 flex h-8 w-8 items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800">
        <Icon size={14} className="text-gray-500" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs text-gray-500">{label}</p>
        <p className="truncate text-sm text-gray-900 dark:text-gray-100">{value}</p>
      </div>
    </div>
  )
}

export function ContactInfo({ conversationId, open, onClose }: ContactInfoProps) {
  const { t } = useTranslation()
  const { data: conversation, isLoading } = useConversation(conversationId)

  const contact = conversation?.contact
  const tags = useMemo(() => {
    if (!contact?.tags) return []
    return contact.tags.split(',').map((t) => t.trim()).filter(Boolean)
  }, [contact?.tags])

  if (!open) return null

  return (
    <div className="flex h-full w-80 flex-col border-l border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-950">
      <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3 dark:border-gray-700">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
          {t('chat.customerInfo')}
        </h3>
        <button
          onClick={onClose}
          className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800"
        >
          ✕
        </button>
      </div>

      {isLoading ? (
        <div className="space-y-4 p-4">
          <div className="flex items-center gap-3">
            <Skeleton variant="circular" width={48} height={48} />
            <div className="space-y-2">
              <Skeleton width={120} />
              <Skeleton width={80} />
            </div>
          </div>
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} width="100%" height={40} />
          ))}
        </div>
      ) : contact ? (
        <div className="flex-1 overflow-y-auto">
          <div className="border-b border-gray-100 p-4 text-center dark:border-gray-800">
            <Avatar
              name={contact.displayName || contact.fullName}
              src={contact.avatarUrl}
              size="xl"
              className="mx-auto"
            />
            <h2 className="mt-3 text-base font-semibold text-gray-900 dark:text-gray-100">
              {contact.displayName || contact.fullName}
            </h2>
            <p className="text-sm text-gray-500">{contact.jobTitle || 'Sin cargo'}</p>

            <div className="mt-3 flex justify-center gap-2">
              <Badge variant={contact.status === 'ACTIVE' ? 'success' : contact.status === 'BLOCKED' ? 'error' : 'neutral'} size="sm">
                {contact.status === 'ACTIVE' ? 'Activo' : contact.status === 'BLOCKED' ? 'Bloqueado' : 'Inactivo'}
              </Badge>
              {conversation?.channel && (
                <Badge variant="info" size="sm">{conversation.channel}</Badge>
              )}
            </div>
          </div>

          <div className="space-y-4 p-4">
            <InfoRow icon={Mail} label="Email" value={contact.email} />
            <InfoRow icon={Phone} label="Teléfono" value={contact.phone} />
            <InfoRow icon={Building} label="Empresa" value={contact.company} />
            <InfoRow icon={Globe} label="Idioma" value={contact.language} />
            <InfoRow icon={Calendar} label="Creado" value={dayjs(contact.createdAt).format('DD/MM/YYYY')} />

            <InfoRow
              icon={MessageCircle}
              label="Conversaciones"
              value={String(contact.conversationCount ?? 0)}
            />

            <InfoRow
              icon={Clock}
              label="Último contacto"
              value={contact.lastContactedAt ? dayjs(contact.lastContactedAt).fromNow() : 'Nunca'}
            />
          </div>

          {tags.length > 0 && (
            <div className="border-t border-gray-100 px-4 py-3 dark:border-gray-800">
              <div className="flex items-center gap-2 mb-2">
                <Tag size={14} className="text-gray-400" />
                <span className="text-xs font-medium text-gray-500">Etiquetas</span>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {tags.map((tag) => (
                  <Badge key={tag} variant="neutral" size="sm">{tag}</Badge>
                ))}
              </div>
            </div>
          )}

          {contact.notes && (
            <div className="border-t border-gray-100 px-4 py-3 dark:border-gray-800">
              <p className="mb-1 text-xs font-medium text-gray-500">Notas</p>
              <p className="text-sm text-gray-700 dark:text-gray-300">{contact.notes}</p>
            </div>
          )}
        </div>
      ) : null}
    </div>
  )
}
