import { memo } from 'react'
import {
  Check,
  CheckCheck,
  Clock,
  AlertCircle,
  FileText,
  Image,
  Headphones,
  Film,
  Download,
} from 'lucide-react'
import { cn } from '@/shared/utils'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Tooltip } from '@/shared/atoms/Tooltip/Tooltip'
import { dayjs } from '@/config/dayjs'
import type { ConversationMessageDto } from '@/types/chat'

const DEFAULT_TIMEZONE = 'America/Lima'

const formatMessageTime = (
  message: ConversationMessageDto,
  timezone = DEFAULT_TIMEZONE,
): string => {
  const date = message.sentAt ?? message.createdAt
  if (!date) return ''
  return dayjs.utc(date).tz(timezone).format('HH:mm')
}

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

function AttachmentPreview({
  attachment,
}: {
  attachment: ConversationMessageDto['attachments'][0]
}) {
  const Icon =
    attachment.type === 'IMAGE'
      ? Image
      : attachment.type === 'AUDIO' || attachment.type === 'VOICE'
        ? Headphones
        : attachment.type === 'VIDEO'
          ? Film
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

function MediaPlaceholder() {
  return (
    <div className="flex items-center gap-2 rounded-lg bg-black/5 p-3 dark:bg-black/20">
      <Headphones size={18} className="text-gray-400" />
      <span className="text-sm text-gray-400">Enviando...</span>
    </div>
  )
}

export const MessageBubble = memo(function MessageBubble({
  message,
  isOwn,
  showAvatar,
}: MessageBubbleProps) {
  const isText = message.type === 'TEXT'
  const isImage = message.type === 'IMAGE'
  const isDocument = message.type === 'DOCUMENT'
  const isVideo = message.type === 'VIDEO'
  const isAudio = message.type === 'AUDIO'
  const isSticker = message.type === 'STICKER'

  const firstAttachment = message.attachments?.[0]

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
            {message.senderName || message.userName}
          </p>
        )}

        {isSticker && firstAttachment ? (
          <img
            src={firstAttachment.fileUrl}
            alt={firstAttachment.caption ?? 'Sticker'}
            className="h-28 w-28 object-contain"
          />
        ) : (
          <div
            className={cn(
              'rounded-2xl',
              isImage || isVideo ? 'p-1.5' : 'px-4 py-2.5',
              isOwn
                ? 'bg-brand-600 rounded-br-sm text-white'
                : 'rounded-bl-sm bg-gray-100 text-gray-900 dark:bg-gray-800 dark:text-gray-100',
              message.status === 'FAILED' && 'opacity-70',
            )}
          >
            {isImage &&
              (firstAttachment ? (
                <div>
                  <img
                    src={firstAttachment.fileUrl}
                    alt={firstAttachment.caption ?? ''}
                    className="block max-h-64 max-w-[240px] rounded-xl object-cover"
                  />
                  {firstAttachment.caption && (
                    <p className="px-2 pb-1 pt-1.5 text-sm break-words whitespace-pre-wrap">
                      {firstAttachment.caption}
                    </p>
                  )}
                </div>
              ) : (
                <MediaPlaceholder />
              ))}

            {isText && <p className="text-sm break-words whitespace-pre-wrap">{message.content}</p>}

            {isDocument &&
              (message.attachments?.length
                ? message.attachments.map((att) => (
                    <AttachmentPreview key={att.id} attachment={att} />
                  ))
                : <MediaPlaceholder />)}

            {isVideo &&
              (firstAttachment ? (
                <video
                  src={firstAttachment.fileUrl}
                  controls
                  className="mb-1 block max-h-64 max-w-[280px] rounded-xl"
                />
              ) : (
                <MediaPlaceholder />
              ))}

            {isAudio &&
              (firstAttachment ? (
                <div>
                  <audio src={firstAttachment.fileUrl} controls className="w-56 max-w-full" />
                  {firstAttachment.caption && (
                    <p className="mt-1 text-sm break-words whitespace-pre-wrap">
                      {firstAttachment.caption}
                    </p>
                  )}
                </div>
              ) : (
                <MediaPlaceholder />
              ))}
          </div>
        )}

        <div
          className={cn('mt-1 flex items-center gap-1', isOwn ? 'flex-row-reverse' : 'flex-row')}
        >
          <span className="text-[10px] text-gray-400">
            {formatMessageTime(message)}
          </span>
          {isOwn && (
            <Tooltip
              content={
                message.status === 'READ'
                  ? 'Leído'
                  : message.status === 'DELIVERED'
                    ? 'Entregado'
                    : message.status === 'SENT'
                      ? 'Enviado'
                      : message.status === 'FAILED'
                        ? 'Error'
                        : 'Pendiente'
              }
            >
              <MessageStatusIcon status={message.status} />
            </Tooltip>
          )}
        </div>
      </div>
    </div>
  )
})
