import { useRef, useCallback } from 'react'
import { useSocketEvent, useSocketEmit } from '@/core/websocket/socket.hooks'
import { useChatStore } from '../stores/chat.store'
import type { ConversationDto, ConversationMessageDto } from '@/types/chat'
import type { MessageStatus } from '@/types/enums'

export function useChatSocket() {
  const emit = useSocketEmit()
  const addMessage = useChatStore((state) => state.addMessage)
  const addConversation = useChatStore((state) => state.addConversation)
  const updateConversation = useChatStore((state) => state.updateConversation)
  const updateMessageStatus = useChatStore((state) => state.updateMessageStatus)
  const setOnlineUsers = useChatStore((state) => state.setOnlineUsers)
  const addTypingUser = useChatStore((state) => state.addTypingUser)
  const removeTypingUser = useChatStore((state) => state.removeTypingUser)
  const typingTimeouts = useRef<Record<string, ReturnType<typeof setTimeout>>>({})

  useSocketEvent<ConversationMessageDto>('message:new', (message) => {
    addMessage(message.conversationId, message)
    const convs = useChatStore.getState().conversations
    const conv = convs.find((c) => c.id === message.conversationId)
    updateConversation(message.conversationId, {
      lastMessage: message,
      lastMessageAt: message.sentAt,
      messageCount: (conv?.messageCount ?? 0) + 1,
    })
  })

  useSocketEvent<{ messageId: string; status: MessageStatus }>('message:status', (data) => {
    updateMessageStatus(data.messageId, data.status)
  })

  useSocketEvent<ConversationDto>('conversation:updated', (conversation) => {
    updateConversation(conversation.id, conversation)
  })

  useSocketEvent<ConversationDto>('conversation:new', (conversation) => {
    addConversation(conversation)
  })

  useSocketEvent<{ userId: string; online: boolean }>('user:online', (_data) => {
    setOnlineUsers(Array.from(useChatStore.getState().onlineUsers))
  })

  useSocketEvent<{ conversationId: string; userId: string }>('agent:typing', (data) => {
    addTypingUser(data.conversationId, data.userId)

    const key = `${data.conversationId}:${data.userId}`
    if (typingTimeouts.current[key]) {
      clearTimeout(typingTimeouts.current[key])
    }

    typingTimeouts.current[key] = setTimeout(() => {
      removeTypingUser(data.conversationId, data.userId)
      delete typingTimeouts.current[key]
    }, 3000)
  })

  const emitTyping = useCallback(
    (conversationId: string) => {
      emit('message:typing', { conversationId })
    },
    [emit],
  )

  const emitStopTyping = useCallback(
    (conversationId: string) => {
      emit('message:stop-typing', { conversationId })
    },
    [emit],
  )

  return { emitTyping, emitStopTyping }
}
