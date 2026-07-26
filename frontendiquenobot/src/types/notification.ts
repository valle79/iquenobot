import type { NotificationType, NotificationPriority, NotificationChannel } from './enums'
import type { UserDto } from './auth'

export interface NotificationDto {
  id: string
  user: UserDto
  type: NotificationType
  channel: NotificationChannel
  priority: NotificationPriority
  title: string
  message: string
  actionUrl: string
  actionLabel: string
  icon: string
  imageUrl: string
  read: boolean
  readAt: string
  sent: boolean
  sentAt: string
  delivered: boolean
  deliveredAt: string
  failed: boolean
  errorMessage: string
  retryCount: number
  scheduledAt: string
  expiresAt: string
  relatedEntityType: string
  relatedEntityId: string
  createdAt: string
}

export interface CreateNotificationRequest {
  userId: string
  type: NotificationType
  channel: NotificationChannel
  priority: NotificationPriority
  title: string
  message: string
  actionUrl?: string
  actionLabel?: string
  icon?: string
  imageUrl?: string
  scheduledAt?: string
  expiresAt?: string
  relatedEntityType?: string
  relatedEntityId?: string
  metadata?: string
}
