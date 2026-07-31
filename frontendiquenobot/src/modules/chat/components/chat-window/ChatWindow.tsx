import { useEffect, useRef, useCallback, useState, useMemo, type RefObject } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { MessageCircle, ChevronDown } from 'lucide-react'
import { AnimatePresence, motion } from 'framer-motion'

import {
  useMessages,
  useSafeConversation,
  useUpdateConversation,
} from '@/modules/chat/hooks/useConversations'

import { useChatSocket } from '@/modules/chat/hooks/useChatSocket'
import { useSocketJoinConversation } from '@/core/websocket/socket.hooks'
import { useChatStore } from '@/modules/chat/stores/chat.store'
import { MessageBubble } from '@/modules/chat/components/message-bubble/MessageBubble'
import { ConversationContextPanel } from '@/modules/chat/components/chat-window/ConversationContextPanel'

import { useAuthStore } from '@/core/auth/auth.store'
import { Skeleton } from '@/shared/atoms/Skeleton/Skeleton'
import { dayjs } from '@/config/dayjs'
import type { ConversationMessageDto } from '@/types/chat'

// =====================================================
// HELPERS
// =====================================================

const SCROLL_THRESHOLD = 100

const getMessageTimestamp = (message: {
  sentAt?: string | null
  createdAt?: string | null
}): number => {
  const date = message.sentAt ?? message.createdAt
  if (!date) return 0
  const value = dayjs.utc(date).valueOf()
  return Number.isNaN(value) ? 0 : value
}

// =====================================================
// SUB-COMPONENTS
// =====================================================

interface TypingIndicatorProps {
  conversationId: string
}

function TypingIndicator({ conversationId }: TypingIndicatorProps) {
  const typingUsers = useChatStore(
    (state) => state.typingUsers[conversationId],
  )
  const conversations = useChatStore((state) => state.conversations)

  if (!typingUsers?.length) return null

  const names = typingUsers
    .map((userId) => {
      const conversation = conversations.find(
        (c) => c.id === conversationId,
      )
      return conversation?.assignedUser?.id === userId
        ? conversation.assignedUser.fullName
        : 'Alguien'
    })
    .filter(Boolean)

  const text =
    names.length === 1
      ? `${names[0]} está escribiendo...`
      : 'Varios usuarios están escribiendo...'

  return (
    <div className="flex items-center gap-2 px-4 py-1 text-xs text-gray-500">
      <span className="flex gap-0.5">
        <span
          className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400"
          style={{ animationDelay: '0ms' }}
        />
        <span
          className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400"
          style={{ animationDelay: '150ms' }}
        />
        <span
          className="h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400"
          style={{ animationDelay: '300ms' }}
        />
      </span>
      {text}
    </div>
  )
}

// =====================================================
// SCROLL HOOK
// =====================================================

interface UseAutoScrollOptions {
  scrollRef: RefObject<HTMLDivElement | null>
  hasNextPage: boolean
  isFetchingNextPage: boolean
  fetchNextPage: () => void
  messages: ConversationMessageDto[]
}

function useAutoScroll({
  scrollRef,
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
}: UseAutoScrollOptions) {
  const autoScrollRef = useRef(true)
  const [showScrollButton, setShowScrollButton] = useState(false)
  const prevScrollHeightRef = useRef(0)

  const handleScroll = useCallback(() => {
    const container = scrollRef.current
    if (!container) return

    const { scrollTop, scrollHeight, clientHeight } = container
    const distanceFromBottom = scrollHeight - scrollTop - clientHeight

    const isAtBottom = distanceFromBottom < SCROLL_THRESHOLD
    autoScrollRef.current = isAtBottom
    setShowScrollButton(!isAtBottom)

    if (scrollTop < SCROLL_THRESHOLD && hasNextPage && !isFetchingNextPage) {
      prevScrollHeightRef.current = scrollHeight
      void fetchNextPage()
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage, scrollRef])

  const scrollToBottom = useCallback(
    (smooth = true) => {
      const container = scrollRef.current
      if (!container) return
      autoScrollRef.current = true
      setShowScrollButton(false)
      container.scrollTo({
        top: container.scrollHeight,
        behavior: smooth ? 'smooth' : 'instant',
      })
    },
    [scrollRef],
  )

  const preserveScrollPosition = useCallback(() => {
    const container = scrollRef.current
    if (!container) return
    const newScrollHeight = container.scrollHeight
    const diff = newScrollHeight - prevScrollHeightRef.current
    if (diff > 0 && !autoScrollRef.current) {
      container.scrollTop = diff
    }
  }, [scrollRef])

  return {
    autoScrollRef,
    showScrollButton,
    handleScroll,
    scrollToBottom,
    preserveScrollPosition,
  }
}

// =====================================================
// MAIN COMPONENT
// =====================================================

export function ChatWindow() {
  const { t } = useTranslation()
  const { id: conversationId } = useParams<{ id: string }>()
  const currentUserId = useAuthStore((state) => state.user?.id)
  const { emitTyping, emitStopTyping } = useChatSocket()
  const { markAsRead } = useUpdateConversation()

  const [showContext, setShowContext] = useState(false)
  const scrollRef = useRef<HTMLDivElement | null>(null)
  useSocketJoinConversation(conversationId ?? '')
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const prevMessagesLengthRef = useRef(0)
  const markAsReadDoneRef = useRef<string | null>(null)

  const {
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
  } = useMessages(conversationId)

  const { data: conversation, isLoading: isConvLoading } =
    useSafeConversation(conversationId)

  const messages = useChatStore((state) => {
    if (!conversationId) return undefined
    return state.messages[conversationId]
  })

  const messagesList = useMemo(() => {
    if (!messages) return []
    return [...messages].sort((a, b) => {
      const tsA = getMessageTimestamp(a)
      const tsB = getMessageTimestamp(b)
      if (tsA !== tsB) return tsA - tsB
      return a.id.localeCompare(b.id)
    })
  }, [messages])

  const {
    autoScrollRef,
    showScrollButton,
    handleScroll,
    scrollToBottom,
    preserveScrollPosition,
  } = useAutoScroll({
    scrollRef,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    messages: messagesList,
  })

  // Mark as read only after the conversation has been fully loaded and confirmed to exist
  useEffect(() => {
    if (!conversationId) {
      markAsReadDoneRef.current = null
      return
    }
    if (isConvLoading || !conversation) return
    if (markAsReadDoneRef.current === conversationId) return

    markAsReadDoneRef.current = conversationId
    markAsRead(conversationId)
  }, [conversationId, conversation, isConvLoading, markAsRead])

  // Preserve scroll position after loading older pages
  useEffect(() => {
    if (
      prevMessagesLengthRef.current > 0 &&
      messagesList.length > prevMessagesLengthRef.current
    ) {
      preserveScrollPosition()
    }
    prevMessagesLengthRef.current = messagesList.length
  }, [messagesList.length, preserveScrollPosition])

  // Auto-scroll when new messages arrive at the bottom
  useEffect(() => {
    if (!scrollRef.current || !autoScrollRef.current) return
    if (messagesList.length > 0) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messagesList, autoScrollRef])

  // Cleanup timeouts on unmount
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current)
      }
    }
  }, [])

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
        <MessageCircle
          className="mb-4 text-gray-300 dark:text-gray-600"
          size={64}
        />
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
    <div className="relative flex min-h-0 flex-1 flex-col">
      {conversation?.botConversation && (
        <ConversationContextPanel
          conversationId={conversationId}
          open={showContext}
          onToggle={() => setShowContext((prev) => !prev)}
        />
      )}

      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto px-4 py-4"
      >
        {isLoading ? (
          <div className="space-y-4 px-4">
            {Array.from({ length: 5 }).map((_, index) => (
              <div
                key={index}
                className={`flex gap-3 ${
                  index % 2 === 0 ? '' : 'flex-row-reverse'
                }`}
              >
                <Skeleton
                  variant="circular"
                  width={32}
                  height={32}
                />
                <div
                  className={`space-y-2 ${
                    index % 2 === 0 ? '' : 'items-end'
                  }`}
                >
                  <Skeleton
                    width={200}
                    height={40}
                    className="rounded-2xl"
                  />
                  <Skeleton width={120} height={16} />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            <AnimatePresence initial={false}>
              {messagesList.map((message) => (
                <motion.div
                  key={message.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <MessageBubble
                    message={message}
                    isOwn={
                      message.userId === currentUserId ||
                      message.direction === 'OUTBOUND'
                    }
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
        {showScrollButton && (
          <motion.button
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            onClick={() => scrollToBottom(true)}
            className="absolute bottom-4 left-1/2 z-10 -translate-x-1/2 rounded-full bg-white p-2 shadow-elevated transition-colors hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-700"
            aria-label="Ir al último mensaje"
          >
            <ChevronDown size={16} />
          </motion.button>
        )}
      </AnimatePresence>
    </div>
  )
}
