import { memo } from 'react'
import { Check, CheckCheck, Clock, AlertCircle, FileText, Image, Headphones, Film, Download } from 'lucide-react'
import { cn } from '@/shared/utils'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Tooltip } from '@/shared/atoms/Tooltip/Tooltip'
import { dayjs } from '@/config/dayjs'
import type { ConversationMessageDto } from '@/types/chat'

interface MessageBubbleProps {
  message: ConversationMessageDto
  isOwn: boolean
  showAvatar?: boolean
}

function MessageStatusIcon({ status }: { status: string }) {
  switch (status) {
    case 'SENDING':
    case 'PENDING':
      return <Clock size={12} className="text-gray-400" />
    case 'SENT':
      return <Check size={12} className="text-gray-400" />
    case 'DELIVERED':
      return <CheckCheck size={12} className="text-gray-400" />
    case 'READ':
      return <CheckCheck size={12} className="text-brand-500" />
    case 'FAILED':
      return <AlertCircle size={12} className="text-red-500" />
    default:
      return null
  }
}

function AttachmentPreview({ attachment }: { attachment: ConversationMessageDto['attachments'][0] }) {
  const Icon = attachment.type === 'IMAGE' ? Image
    : attachment.type === 'AUDIO' || attachment.type === 'VOICE' ? Headphones
    : attachment.type === 'VIDEO' ? Film
    : FileText

  return (
    <a
      href={attachment.fileUrl}
      target="_blank"
      rel="noopener noreferrer"
      className="mt-2 flex items-center gap-3 rounded-lg border border-gray-200 bg-gray-50 p-3 transition-colors hover:bg-gray-100 dark:border-gray-700 dark:bg-gray-800 dark:hover:bg-gray-700"
    >
      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gray-200 dark:bg-gray-700">
        {attachment.type === 'IMAGE' && attachment.thumbnailUrl ? (
          <img
            src={attachment.thumbnailUrl}
            alt={attachment.fileName}
            className="h-10 w-10 rounded-lg object-cover"
          />
        ) : (
          <Icon size={18} className="text-gray-500" />
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-gray-700 dark:text-gray-300">
          {attachment.fileName}
        </p>
        <p className="text-xs text-gray-500">{attachment.formattedFileSize}</p>
      </div>
      <Download size={16} className="shrink-0 text-gray-400" />
    </a>
  )
}

export const MessageBubble = memo(function MessageBubble({ message, isOwn, showAvatar }: MessageBubbleProps) {
  const isText = message.type === 'TEXT'
  const isImage = message.type === 'IMAGE'
  const isDocument = message.type === 'DOCUMENT'

  return (
    <div className={cn('flex gap-2', isOwn ? 'flex-row-reverse' : 'flex-row')}>
      {showAvatar && !isOwn ? (
        <Avatar name={message.senderName || message.userName} size="sm" className="mt-1" />
      ) : (
        <div className="w-8" />
      )}

      <div className={cn('max-w-[70%]', isOwn ? 'items-end' : 'items-start')}>
        {showAvatar && (
          <p className={cn('mb-1 text-xs text-gray-500', isOwn && 'text-right')}>
            {message.userName}
          </p>
        )}

        <div
          className={cn(
            'rounded-2xl px-4 py-2.5',
            isOwn
              ? 'rounded-br-sm bg-brand-600 text-white'
              : 'rounded-bl-sm bg-gray-100 text-gray-900 dark:bg-gray-800 dark:text-gray-100',
            message.status === 'FAILED' && 'opacity-70',
          )}
        >
          {isImage && message.attachments[0] && (
            <img
              src={message.attachments[0].fileUrl}
              alt={message.attachments[0].caption ?? ''}
              className="mb-2 max-w-full rounded-lg"
            />
          )}

          {isText && <p className="whitespace-pre-wrap break-words text-sm">{message.content}</p>}

          {isDocument && (message.attachments ?? []).map((att) => (
            <AttachmentPreview key={att.id} attachment={att} />
          ))}

          {(message.attachments ?? []).filter((a) => a.type !== 'IMAGE' && a.type !== 'DOCUMENT').map((att) => (
            <AttachmentPreview key={att.id} attachment={att} />
          ))}
        </div>

        <div className={cn('mt-1 flex items-center gap-1', isOwn ? 'flex-row-reverse' : 'flex-row')}>
          <span className="text-[10px] text-gray-400">
            {dayjs(message.sentAt || message.createdAt).format('HH:mm')}
          </span>
          {isOwn && (
            <Tooltip content={message.status === 'READ' ? 'Leído' : message.status === 'DELIVERED' ? 'Entregado' : message.status === 'SENT' ? 'Enviado' : message.status === 'FAILED' ? 'Error' : 'Pendiente'}>
              <MessageStatusIcon status={message.status} />
            </Tooltip>
          )}
        </div>
      </div>
    </div>
  )
})
