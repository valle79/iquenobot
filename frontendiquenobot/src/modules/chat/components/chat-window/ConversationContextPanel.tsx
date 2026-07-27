import { useMemo } from 'react'
import { Bot, Brain, BarChart3, ChevronRight } from 'lucide-react'
import { useChatStore } from '@/modules/chat/stores/chat.store'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { cn } from '@/shared/utils'
import type { ConversationMessageDto } from '@/types/chat'

interface ConversationContextPanelProps {
  conversationId: string
  open: boolean
  onToggle: () => void
}

export function ConversationContextPanel({ conversationId, open, onToggle }: ConversationContextPanelProps) {
  const messages = useChatStore((state) => state.messages[conversationId])

  const context = useMemo(() => {
    if (!messages || messages.length === 0) return null

    const botMessages = messages.filter((m) => m.fromBot)
    const messagesFromBot = botMessages.length

    let consecutiveBotResponses = 0
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].fromBot) {
        consecutiveBotResponses++
      } else {
        break
      }
    }

    const intents = new Map<string, number>()
    botMessages.forEach((m) => {
      if (m.botIntent) {
        intents.set(m.botIntent, (intents.get(m.botIntent) ?? 0) + 1)
      }
    })
    const recentIntents = Array.from(intents.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([intent, count]) => ({ intent, count }))

    const avgConfidence = botMessages.length > 0
      ? botMessages.reduce((sum, m) => sum + (m.botConfidence ?? 0), 0) / botMessages.length
      : 0

    return { messagesFromBot, consecutiveBotResponses, recentIntents, avgConfidence }
  }, [messages])

  if (!context) return null

  return (
    <div className="border-b border-gray-200 dark:border-gray-700">
      <button
        onClick={onToggle}
        className="flex w-full items-center gap-2 px-4 py-2 text-xs font-medium text-gray-500 hover:bg-gray-50 dark:hover:bg-gray-800/50"
      >
        <Bot size={14} className="text-brand-500" />
        <span>Contexto del bot</span>
        <Badge variant="info" size="sm">{context.messagesFromBot} msgs</Badge>
        <ChevronRight
          size={14}
          className={cn(
            'ml-auto transition-transform',
            open && 'rotate-90',
          )}
        />
      </button>

      {open && (
        <div className="grid grid-cols-3 gap-3 border-t border-gray-100 px-4 py-3 dark:border-gray-800">
          <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-800/50">
            <div className="flex items-center gap-1.5 text-gray-500">
              <Bot size={12} />
              <span className="text-[10px] font-medium uppercase">Respuestas bot</span>
            </div>
            <p className="mt-1 text-lg font-bold text-gray-900 dark:text-gray-100">{context.messagesFromBot}</p>
          </div>

          <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-800/50">
            <div className="flex items-center gap-1.5 text-gray-500">
              <BarChart3 size={12} />
              <span className="text-[10px] font-medium uppercase">Consecutivas</span>
            </div>
            <p className={cn(
              'mt-1 text-lg font-bold',
              context.consecutiveBotResponses >= 5 ? 'text-red-500' : context.consecutiveBotResponses >= 3 ? 'text-amber-500' : 'text-gray-900 dark:text-gray-100',
            )}>
              {context.consecutiveBotResponses}
            </p>
          </div>

          <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-800/50">
            <div className="flex items-center gap-1.5 text-gray-500">
              <Brain size={12} />
              <span className="text-[10px] font-medium uppercase">Confianza</span>
            </div>
            <p className="mt-1 text-lg font-bold text-gray-900 dark:text-gray-100">
              {(context.avgConfidence * 100).toFixed(0)}%
            </p>
          </div>

          {context.recentIntents.length > 0 && (
            <div className="col-span-3">
              <p className="mb-1.5 text-[10px] font-medium uppercase text-gray-500">Intenciones detectadas</p>
              <div className="flex flex-wrap gap-1.5">
                {context.recentIntents.map(({ intent, count }) => (
                  <Badge key={intent} variant="neutral" size="sm">
                    {intent} ({count})
                  </Badge>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
