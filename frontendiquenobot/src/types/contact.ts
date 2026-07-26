import type { ContactStatus, LeadStatus, LeadSource } from './enums'
import type { UserDto } from './auth'

export interface ContactDto {
  id: string
  firstName: string
  lastName: string
  fullName: string
  displayName: string
  email: string
  phone: string
  whatsappPhone: string
  company: string
  jobTitle: string
  avatarUrl: string
  status: ContactStatus
  language: string
  timezone: string
  tags: string
  notes: string
  lastContactedAt: string
  conversationCount: number
  messageCount: number
  subscribed: boolean
  unsubscribedAt: string
  blockedAt: string
  blockedReason: string
  createdAt: string
  updatedAt: string
}

export interface CreateContactRequest {
  firstName?: string
  lastName?: string
  email?: string
  phone?: string
  whatsappPhone?: string
  company?: string
  jobTitle?: string
  avatarUrl?: string
  language?: string
  timezone?: string
  tags?: string
  notes?: string
}

export interface LeadDto {
  id: string
  contact: ContactDto
  title: string
  description: string
  status: LeadStatus
  source: LeadSource
  sourceDetails: string
  estimatedValue: number
  probability: number
  score: number
  assignedTo: UserDto | null
  assignedAt: string
  firstContactAt: string
  lastContactAt: string
  expectedCloseDate: string
  closedAt: string
  lostReason: string
  convertedToContactId: string
  tags: string
  notes: string
  open: boolean
  closed: boolean
  daysSinceCreated: number
  daysSinceLastContact: number
  createdAt: string
  updatedAt: string
}

export interface CreateLeadRequest {
  contactId: string
  title: string
  description?: string
  status: LeadStatus
  source: LeadSource
  sourceDetails?: string
  estimatedValue?: number
  probability?: number
  score?: number
  assignedToUserId?: string
  expectedCloseDate?: string
  tags?: string
  notes?: string
}
