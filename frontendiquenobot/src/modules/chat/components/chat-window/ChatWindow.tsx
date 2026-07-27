import { useEffect, useRef, useCallback, useState, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { MessageCircle, ChevronDown } from 'lucide-react'
import { useMessages, useConversation } from '@/modules/chat/hooks/useConversations'
import { useChatSocket } from '@/modules/chat/hooks/useChatSocket'
import { useChatStore } from '@/modules/chat/stores/chat.store'
import { MessageBubble } from '@/modules/chat/components/message-bubble/MessageBubble'
import { ConversationContextPanel } from '@/modules/chat/components/chat-window/ConversationContextPanel'
import { useAuthStore } from '@/core/auth/auth.store'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { AnimatePresence, motion } from 'framer-motion'
import { useUpdateConversation } from '@/modules/chat/hooks/useConversations'

function TypingIndicator({ conversationId }: { conversationId: string }) {
  const typingUsers = useChatStore((state) => state.typingUsers[conversationId])
  const conversations = useChatStore((state) => state.conversations)

  const typingList = typingUsers
  const convList = conversations

  if (!typingList?.length) return null

  const names = typingList
    .map((uid) => {
      const conv = convList.find((c) => c.id === conversationId)
      if (conv?.assignedUser?.id === uid) return conv.assignedUser.fullName
      return 'Alguien'
    })
    .filter(Boolean)

  const text = names.length === 1 ? `${names[0]} está escribiendo...` : 'Varios están escribiendo...'

  return (
    <div className="flex items-center gap-2 px-4 py-1 text-xs text-gray-500">
      <span className="flex gap-0.5">
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400" style={{ animationDelay: '0ms' }} />
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400" style={{ animationDelay: '150ms' }} />
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400" style={{ animationDelay: '300ms' }} />
      </span>
      {text}
    </div>
  )
}

export function ChatWindow() {
  const { t } = useTranslation()
  const { id: conversationId } = useParams<{ id: string }>()
  const currentUserId = useAuthStore((s) => s.user?.id)
  const { emitTyping, emitStopTyping } = useChatSocket()
  const { markAsRead } = useUpdateConversation()
  const [autoScroll, setAutoScroll] = useState(true)
  const [showContext, setShowContext] = useState(false)

  const scrollRef = useRef<HTMLDivElement>(null)
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout>>()

  const { fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useMessages(conversationId)
  const { data: conversation } = useConversation(conversationId)

  const messages = useChatStore((state) => {
    if (!conversationId) return null
    return state.messages[conversationId]
  })

  const messagesList = useMemo(() => messages ?? [], [messages])

  useEffect(() => {
    if (conversationId) {
      markAsRead(conversationId)
    }
  }, [conversationId, markAsRead])

  useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messagesList, autoScroll])

  const handleScroll = useCallback(() => {
    if (!scrollRef.current) return

    const { scrollTop, scrollHeight, clientHeight } = scrollRef.current
    setAutoScroll(scrollHeight - scrollTop - clientHeight < 100)

    if (scrollTop < 100 && hasNextPage && !isFetchingNextPage) {
      void fetchNextPage()
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage])

  const handleTyping = useCallback(() => {
    if (!conversationId) return

    emitTyping(conversationId)

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current)
    }

    typingTimeoutRef.current = setTimeout(() => {
      emitStopTyping(conversationId)
    }, 2000)
  }, [conversationId, emitTyping, emitStopTyping])

  if (!conversationId) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-gray-50 dark:bg-gray-900">
        <MessageCircle className="mb-4 text-gray-300 dark:text-gray-600" size={64} />
        <p className="text-lg font-medium text-gray-500">
          {t('chat.noMessages')}
        </p>
        <p className="text-sm text-gray-400">
          Selecciona una conversación de la lista
        </p>
      </div>
    )
  }

  return (
    <div className="relative flex flex-1 flex-col">
      {conversation?.botConversation && conversationId && (
        <ConversationContextPanel
          conversationId={conversationId}
          open={showContext}
          onToggle={() => setShowContext(!showContext)}
        />
      )}

      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-4 py-4"
      >
        {isLoading ? (
          <div className="space-y-4 px-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className={`flex gap-3 ${i % 2 === 0 ? '' : 'flex-row-reverse'}`}>
                <Skeleton variant="circular" width={32} height={32} />
                <div className={`space-y-2 ${i % 2 === 0 ? '' : 'items-end'}`}>
                  <Skeleton width={200} height={40} className="rounded-2xl" />
                  <Skeleton width={120} height={16} />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            <AnimatePresence initial={false}>
              {messagesList.map((msg) => (
                <motion.div
                  key={msg.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <MessageBubble
                    message={msg}
                    isOwn={msg.userId === currentUserId || msg.direction === 'OUTBOUND'}
                    showAvatar
                  />
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}

        <TypingIndicator conversationId={conversationId} />
      </div>

      <AnimatePresence>
        {!autoScroll && (
          <motion.button
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            onClick={() => {
              setAutoScroll(true)
              scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
            }}
            className="absolute bottom-4 left-1/2 z-10 -translate-x-1/2 rounded-full bg-white p-2 shadow-elevated transition-colors hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-700"
          >
            <ChevronDown size={16} />
          </motion.button>
        )}
      </AnimatePresence>
    </div>
  )
}
