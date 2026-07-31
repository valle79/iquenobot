export type QuoteStatus =
  | 'GENERADA'
  | 'DRAFT'
  | 'SENT'
  | 'REENVIADA'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'ANULADA'
  | 'EXPIRED'

export interface QuoteSummaryDto {
  id: string
  quoteNumber: string
  customerName: string | null
  customerPhone: string | null
  whatsappPhone: string | null
  createdAt: string
  total: number
  currency: string
  status: QuoteStatus
  resendCount: number
  lastResentAt: string | null
  generatedBy: string | null
  generatedByName: string
  pdfUrl: string | null
  fileName: string | null
  fileSize: number | null
}

export interface QuoteItem {
  name: string
  sku: string
  quantity: number
  unitPrice: number
}

export interface QuoteHistoryItem {
  id: string
  action: string
  actorName: string
  channel: string | null
  channelMessageId: string | null
  details: string | null
  createdAt: string
}

export interface QuoteDetailDto extends QuoteSummaryDto {
  contactId: string | null
  conversationId: string | null
  subtotal: number
  discount: number
  igv: number
  observations: string | null
  fileHash: string | null
  items: QuoteItem[]
  history: QuoteHistoryItem[]
}

export interface QuoteFilters {
  query?: string
  customer?: string
  quoteNumber?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
  status?: string
  generatedBy?: string
  minTotal?: number
  maxTotal?: number
}

export interface QuoteResendResult {
  quoteId: string
  quoteNumber: string
  success: boolean
  message: string
  channel: string
  channelMessageId: string | null
  resentAt: string
}
