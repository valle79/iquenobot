import { useRef, useCallback } from 'react'
import { useSocketEvent, useSocketEmit } from '@/core/websocket/socket.hooks'
import { useChatStore } from '../stores/chat.store'
import type { ConversationDto, ConversationMessageDto } from '@/types/chat'
import type { MessageStatus } from '@/types/enums'

export function useChatSocket() {
  const emit = useSocketEmit()
  const typingTimeouts = useRef<Record<string, ReturnType<typeof setTimeout>>>({})

  useSocketEvent<ConversationMessageDto>('message:new', (message) => {
    const store = useChatStore.getState()
    const tempMessage = store.findTempMessage(message.conversationId)

    if (tempMessage) {
      store.replaceMessage(tempMessage.id, message.conversationId, message)
    } else {
      store.addMessage(message.conversationId, message)
    }

    const convs = store.conversations
    const conv = convs.find((c) => c.id === message.conversationId)
    store.updateConversation(message.conversationId, {
      lastMessage: message,
      lastMessageAt: message.sentAt,
      messageCount: (conv?.messageCount ?? 0) + 1,
    })
  })

  useSocketEvent<{ messageId: string; status: MessageStatus }>('message:status', (data) => {
    useChatStore.getState().updateMessageStatus(data.messageId, data.status)
  })

  useSocketEvent<ConversationDto>('conversation:updated', (conversation) => {
    useChatStore.getState().updateConversation(conversation.id, conversation)
  })

  useSocketEvent<ConversationDto>('conversation:new', (conversation) => {
    useChatStore.getState().addConversation(conversation)
  })

  useSocketEvent<{ userId: string; online: boolean }>('user:online', (_data) => {
    useChatStore.getState().setOnlineUsers(
      Array.from(useChatStore.getState().onlineUsers),
    )
  })

  useSocketEvent<{ conversationId: string; userId: string }>('agent:typing', (data) => {
    const store = useChatStore.getState()
    store.addTypingUser(data.conversationId, data.userId)

    const key = `${data.conversationId}:${data.userId}`
    if (typingTimeouts.current[key]) {
      clearTimeout(typingTimeouts.current[key])
    }

    typingTimeouts.current[key] = setTimeout(() => {
      store.removeTypingUser(data.conversationId, data.userId)
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
