import type { AIProvider, ChannelType } from './enums'

export interface BotConfiguration {
  enabled: boolean
  autoReply: boolean
  aiProvider: AIProvider
  systemPrompt: string
  temperature: number
  humanHandoffEnabled: boolean
  fallbackMessage: string
}

export interface WorkingHoursDay {
  start: string
  end: string
  active: boolean
}

export type WorkingHoursSchedule = Record<string, WorkingHoursDay>

export interface WorkingHoursConfig {
  enabled: boolean
  schedule: WorkingHoursSchedule
  closedMessage: string
  timezone: string
}

export interface ConversationContext {
  consecutiveBotResponses: number
  messagesFromBot: number
  recentIntents: string[]
  conversationId: string
}

export interface LeadCreatedEvent {
  leadId: string
  contactId: string
  conversationId: string
  channel: ChannelType
  source: string
  createdAt: string
}

export interface ConversationEscalatedEvent {
  conversationId: string
  reason: string
  escalatedAt: string
  channel: ChannelType
}

export const DEFAULT_WORKING_HOURS: WorkingHoursSchedule = {
  monday:    { start: '09:00', end: '18:00', active: true },
  tuesday:   { start: '09:00', end: '18:00', active: true },
  wednesday: { start: '09:00', end: '18:00', active: true },
  thursday:  { start: '09:00', end: '18:00', active: true },
  friday:    { start: '09:00', end: '18:00', active: true },
  saturday:  { start: '09:00', end: '18:00', active: false },
  sunday:    { start: '09:00', end: '18:00', active: false },
}

export const DAY_LABELS: Record<string, string> = {
  monday: 'Lunes',
  tuesday: 'Martes',
  wednesday: 'Miércoles',
  thursday: 'Jueves',
  friday: 'Viernes',
  saturday: 'Sábado',
  sunday: 'Domingo',
}

export const DAY_ORDER = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday']
