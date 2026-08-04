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

export interface SimulatedActionDto {
  actionType: string
  label: string
  description: string
}

export interface ChatbotPreviewResponse {
  message: string
  intentDetected: string | null
  confidence: number | null
  requiresHumanAgent: boolean
  requiresClarification: boolean
  flowExecuted: string | null
  simulatedActions: SimulatedActionDto[]
  botAvailable: boolean
}

export interface ChatbotHistoryItem {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatbotPreviewRequest {
  message: string
  context?: {
    history?: ChatbotHistoryItem[]
    isFirstMessage?: boolean
    fallbackCount?: number
  }
}
