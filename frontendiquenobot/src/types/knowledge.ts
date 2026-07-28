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
  tags?: string
}
