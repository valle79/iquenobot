import type {
  ChannelType,
  ConversationStatus,
  ConversationPriority,
  MessageType,
  MessageDirection,
  MessageStatus,
  AttachmentType,
} from './enums'
import type { UserDto } from './auth'
import type { ContactDto } from './contact'

export interface ConversationDto {
  id: string
  contact: ContactDto
  assignedUser: UserDto | null
  channel: ChannelType
  status: ConversationStatus
  priority: ConversationPriority
  subject: string
  channelConversationId: string
  lastMessageAt: string
  firstResponseAt: string
  resolvedAt: string
  closedAt: string
  responseTimeSeconds: number
  resolutionTimeSeconds: number
  messageCount: number
  unreadCount: number
  satisfactionRating: number
  satisfactionFeedback: string
  tags: string
  botConversation: boolean
  botHandoffAt: string
  createdAt: string
  updatedAt: string
  lastMessage: ConversationMessageDto | null
}

export interface ConversationMessageDto {
  id: string
  conversationId: string
  userId: string
  userName: string
  direction: MessageDirection
  type: MessageType
  status: MessageStatus
  content: string
  channelMessageId: string
  replyToMessageId: string
  senderName: string
  senderPhone: string
  senderEmail: string
  fromBot: boolean
  botIntent: string
  botConfidence: number
  sentAt: string
  deliveredAt: string
  readAt: string
  failedAt: string
  failureReason: string
  attachments: MessageAttachmentDto[]
  createdAt: string
}

export interface MessageAttachmentDto {
  id: string
  type: AttachmentType
  fileName: string
  fileUrl: string
  fileSize: number
  formattedFileSize: string
  mimeType: string
  thumbnailUrl: string
  durationSeconds: number | null
  width: number | null
  height: number | null
  caption: string
}

export interface CreateConversationRequest {
  contactId: string
  channel: ChannelType
  subject?: string
  priority?: ConversationPriority
  assignedUserId?: string
  channelConversationId?: string
  tags?: string
  initialMessage?: string
}

export interface SendMessageRequest {
  conversationId: string
  type: MessageType
  content: string
  replyToMessageId?: string
  attachmentUrls?: string[]
}
