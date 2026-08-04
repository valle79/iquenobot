import { useEffect, useRef } from 'react'
import { Brain } from 'lucide-react'
import { dayjs } from '@/config/dayjs'
import type { TestMessage } from '@/modules/ai/hooks/useChatbotTest'
import { TestActionBadges } from './TestActionBadges'

function MessageBubble({ message }: { message: TestMessage }) {
  const isUser = message.role === 'user'

  return (
    <div className={`flex w-full ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`flex max-w-[85%] items-end gap-1.5 ${isUser ? 'flex-row-reverse' : ''}`}>
        {!isUser && (
          <span className="mb-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-sm">
            <Brain size={14} />
          </span>
        )}
        <div className="min-w-0">
          <div
            className={`rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed shadow-sm ${
              isUser
                ? 'rounded-br-sm bg-[#d9fdd3] text-gray-800 dark:bg-[#1c5c46] dark:text-gray-50'
                : message.error
                  ? 'rounded-bl-sm border border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-900 dark:bg-rose-950/50 dark:text-rose-300'
                  : 'rounded-bl-sm border border-gray-200 bg-white text-gray-800 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100'
            }`}
          >
            {message.content}
          </div>

          {!isUser && !message.error && message.intentDetected && (
            <p className="mt-1 pl-1 text-[11px] font-medium text-brand-600 dark:text-brand-400">
              Intención detectada: {message.intentDetected}
              {message.flowExecuted ? ` · Flujo: ${message.flowExecuted}` : ''}
            </p>
          )}

          {!isUser && !message.error && <TestActionBadges actions={message.simulatedActions} />}

          <p className={`mt-0.5 text-[10px] text-gray-400 dark:text-gray-500 ${isUser ? 'pr-1 text-right' : 'pl-1'}`}>
            {dayjs(message.createdAt).format('HH:mm')}
          </p>
        </div>
      </div>
    </div>
  )
}

function TypingIndicator() {
  return (
    <div className="flex w-full justify-start">
      <div className="flex max-w-[85%] items-end gap-1.5">
        <span className="mb-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-sm">
          <Brain size={14} />
        </span>
        <div className="flex items-center gap-1 rounded-2xl rounded-bl-sm border border-gray-200 bg-white px-4 py-3 shadow-sm dark:border-gray-700 dark:bg-gray-800">
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400 [animation-delay:-0.3s]" />
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400 [animation-delay:-0.15s]" />
          <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400" />
        </div>
      </div>
    </div>
  )
}

export function TestChatMessages({
  messages,
  isTyping,
  botName,
}: {
  messages: TestMessage[]
  isTyping: boolean
  botName: string
}) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  return (
    <div className="relative flex-1 space-y-3 overflow-y-auto bg-gray-50 px-4 py-4 dark:bg-gray-900">
      <div className="mx-auto mb-4 flex w-fit items-center gap-2 rounded-full border border-amber-300 bg-amber-50 px-3 py-1 text-[11px] font-medium text-amber-700 dark:border-amber-800 dark:bg-amber-950/60 dark:text-amber-400">
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-500 opacity-75" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-amber-500" />
        </span>
        Simulación de {botName.replace(/—.*/, '').trim()} · nada se guarda en tu CRM
      </div>

      {messages.length === 0 && (
        <div className="flex h-40 items-center justify-center">
          <p className="max-w-xs text-center text-sm text-gray-400 dark:text-gray-500">
            Escribe un mensaje como lo haría tu cliente para ver cómo respondería el bot de forma real.
          </p>
        </div>
      )}

      {messages.map((message) => (
        <MessageBubble key={message.id} message={message} />
      ))}

      {isTyping && <TypingIndicator />}

      <div ref={bottomRef} />
    </div>
  )
}