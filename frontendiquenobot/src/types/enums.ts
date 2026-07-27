export type RoleType = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'SUPERVISOR' | 'AGENT' | 'BOT'

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' | 'LOCKED'

export type TenantStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'TRIAL' | 'EXPIRED'

export type ChannelType =
  | 'WHATSAPP'
  | 'TELEGRAM'
  | 'MESSENGER'
  | 'INSTAGRAM'
  | 'EMAIL'
  | 'WEBCHAT'
  | 'SMS'
  | 'TWITTER'
  | 'API'

export type ConversationStatus = 'OPEN' | 'IN_PROGRESS' | 'PENDING' | 'RESOLVED' | 'CLOSED' | 'SPAM' | 'ARCHIVED'

export type ConversationPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type MessageType = 'TEXT' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'LOCATION' | 'CONTACT' | 'STICKER' | 'TEMPLATE' | 'INTERACTIVE' | 'SYSTEM'

export type MessageDirection = 'INBOUND' | 'OUTBOUND'

export type MessageStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'DELETED'

export type LeadStatus = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'CONVERTED' | 'LOST' | 'DISQUALIFIED'

export type LeadSource = 'WHATSAPP' | 'WEB_FORM' | 'PHONE' | 'EMAIL' | 'SOCIAL_MEDIA' | 'REFERRAL' | 'ADVERTISING' | 'EVENT' | 'DIRECT' | 'OTHER'

export type ContactStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'ARCHIVED'

export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK' | 'DISCONTINUED'

export type NotificationType =
  | 'NEW_MESSAGE'
  | 'NEW_CONVERSATION'
  | 'CONVERSATION_ASSIGNED'
  | 'CONVERSATION_ESCALATED'
  | 'LEAD_ASSIGNED'
  | 'LEAD_CREATED'
  | 'LEAD_STATUS_CHANGED'
  | 'TASK_REMINDER'
  | 'SYSTEM_ALERT'
  | 'MENTION'
  | 'LOW_STOCK_ALERT'
  | 'NEW_ORDER'
  | 'ORDER_STATUS_CHANGED'
  | 'WELCOME'
  | 'ANNOUNCEMENT'
  | 'CUSTOM'

export type NotificationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SMS' | 'PUSH' | 'WHATSAPP' | 'WEBHOOK'

export type AttachmentType = 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'VOICE' | 'STICKER' | 'LOCATION' | 'CONTACT'

export type AIProvider = 'OPENAI' | 'GROQ' | 'GEMINI' | 'CLAUDE' | 'NONE'

export type WhatsAppProvider = 'EVOLUTION_API' | 'WHATSAPP_CLOUD_API' | 'BAILEYS' | 'TWILIO'
