export interface KnowledgeBaseDto {
  id: string
  title: string
  content: string
  sourceType: string
  sourceUrl?: string
  fileUrl?: string
  tags?: string
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBaseRequest {
  title: string
  content: string
  sourceType?: string
  sourceUrl?: string
  fileUrl?: string
  extractedText?: string
  tags?: string
}

export type UpdateKnowledgeBaseRequest = CreateKnowledgeBaseRequest

export interface DocumentUploadResult {
  url: string
  pageCount?: number
  extractedText?: string
}
