import { useCallback, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { toast } from 'sonner'
import { chatbotService } from '@/services/chatbot.service'
import type { ChatbotHistoryItem, ChatbotPreviewRequest, ChatbotPreviewResponse, SimulatedActionDto } from '@/types/chatbot'

export interface TestMessage {
  id: string
  role: 'user' | 'bot'
  content: string
  createdAt: string
  intentDetected?: string | null
  flowExecuted?: string | null
  requiresHumanAgent?: boolean
  requiresClarification?: boolean
  simulatedActions?: SimulatedActionDto[]
  error?: boolean
}

/** Igual que HISTORY_LIMIT del orquestador en producción. */
const MAX_HISTORY = 30

/** Tiempo mínimo de "escribiendo" para simular el debounce del scheduler de producción. */
const MIN_TYPING_MS = 1800

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

export function useChatbotTest() {
  const [messages, setMessages] = useState<TestMessage[]>([])
  const [isTyping, setIsTyping] = useState(false)

  // Estado de la conversación de prueba en memoria (nunca se persiste).
  const historyRef = useRef<ChatbotHistoryItem[]>([])
  const fallbackRef = useRef(0)
  const isFirstRef = useRef(true)

  const mutation = useMutation({
    mutationFn: (payload: ChatbotPreviewRequest) => chatbotService.sendPreviewMessage(payload),
  })

  const sendMessage = useCallback(
    (text: string) => {
      const trimmed = text.trim()
      if (!trimmed || mutation.isPending) return

      const userMsg: TestMessage = {
        id: crypto.randomUUID(),
        role: 'user',
        content: trimmed,
        createdAt: new Date().toISOString(),
      }
      setMessages((prev) => [...prev, userMsg])
      setIsTyping(true)

      const payload: ChatbotPreviewRequest = {
        message: trimmed,
        context: {
          history: historyRef.current,
          isFirstMessage: isFirstRef.current,
          fallbackCount: fallbackRef.current,
        },
      }

      Promise.all([mutation.mutateAsync(payload), delay(MIN_TYPING_MS)])
        .then(([resp]: [ChatbotPreviewResponse, unknown]) => {
          const botMsg: TestMessage = {
            id: crypto.randomUUID(),
            role: 'bot',
            content: resp.message,
            createdAt: new Date().toISOString(),
            intentDetected: resp.intentDetected,
            flowExecuted: resp.flowExecuted,
            requiresHumanAgent: resp.requiresHumanAgent,
            requiresClarification: resp.requiresClarification,
            simulatedActions: resp.simulatedActions,
          }
          setMessages((prev) => [...prev, botMsg])

          historyRef.current = [
            ...historyRef.current,
            { role: 'user', content: trimmed },
            { role: 'assistant', content: resp.message },
          ]
          if (historyRef.current.length > MAX_HISTORY) {
            historyRef.current = historyRef.current.slice(-MAX_HISTORY)
          }
          fallbackRef.current = resp.requiresClarification ? fallbackRef.current + 1 : 0
          isFirstRef.current = false
        })
        .catch((err: unknown) => {
          const detail = isAxiosError(err)
            ? (err.response?.data as { message?: string } | undefined)?.message
            : undefined
          if (detail) {
            toast.error(`Error en el modo prueba: ${detail}`)
          } else {
            toast.error('No se pudo conectar con el servidor. Intenta nuevamente.')
          }
          setMessages((prev) => [
            ...prev,
            {
              id: crypto.randomUUID(),
              role: 'bot',
              content: 'No se pudo procesar tu mensaje (error de conexión o del servidor). Esta conversación no se envió a producción.',
              createdAt: new Date().toISOString(),
              error: true,
            },
          ])
        })
        .finally(() => {
          setIsTyping(false)
        })
    },
    // mutation es estable en React Query v5; isPending se usa para evitar envíos simultáneos.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [mutation.isPending],
  )

  const reset = useCallback(() => {
    historyRef.current = []
    fallbackRef.current = 0
    isFirstRef.current = true
    setMessages([])
    setIsTyping(false)
  }, [])

  return { messages, isTyping, isPending: mutation.isPending, sendMessage, reset }
}