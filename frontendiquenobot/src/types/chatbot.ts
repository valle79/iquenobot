export type ChatbotFlowTrigger = 'KEYWORD' | 'PATTERN' | 'AI' | 'SCHEDULED' | 'EVENT'

export interface ChatbotIntentDto {
  id: string
  intentName: string
  description: string
  trainingPhrases: string
  responses: string
  entities: string
  contextRequired: string
  contextOutput: string
  actions: string
  confidenceThreshold: number
  active: boolean
  priority: number
  matchedCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateChatbotIntentRequest {
  intentName: string
  description?: string
  trainingPhrases: string
  responses: string
  entities?: string
  contextRequired?: string
  contextOutput?: string
  actions?: string
  confidenceThreshold: number
  active?: boolean
  priority?: number
}

export interface ChatbotFlowDto {
  id: string
  name: string
  description: string
  triggerType: ChatbotFlowTrigger
  triggerKeywords: string
  triggerPattern: string
  flowConfig: string
  priority: number
  active: boolean
  useAI: boolean
  aiPrompt: string
  fallbackMessage: string
  successCount: number
  failureCount: number
  executionCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateChatbotFlowRequest {
  name: string
  description?: string
  triggerType: ChatbotFlowTrigger
  triggerKeywords?: string
  triggerPattern?: string
  flowConfig: string
  priority?: number
  active?: boolean
  useAI?: boolean
  aiPrompt?: string
  fallbackMessage?: string
}
