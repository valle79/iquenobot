import { useRef, useCallback } from 'react'
import { useSocketEvent, useSocketEmit } from '@/core/websocket/socket.hooks'
import { useChatStore } from '../stores/chat.store'
import type { ContactDto } from '@/types/contact'
import type { ConversationDto, ConversationMessageDto } from '@/types/chat'
import type { MessageStatus } from '@/types/enums'

export function useChatSocket() {
  const emit = useSocketEmit()

  const typingTimeouts = useRef<
    Record<string, ReturnType<typeof setTimeout>>
  >({})

  // =====================================================
  // NUEVO MENSAJE
  // =====================================================

  useSocketEvent<ConversationMessageDto>(
    'message:new',
    (message) => {
      const store = useChatStore.getState()

      // Buscar mensaje optimista (SENDING)
      const tempMessage = store.findTempMessage(
        message.conversationId,
      )

      if (tempMessage) {
        store.replaceMessage(
          tempMessage.id,
          message.conversationId,
          message,
        )
      } else {
        store.addMessage(message.conversationId, message)
      }

      // Actualizar conversación en memoria
      const conversation = store.conversations.find(
        (c) => c.id === message.conversationId,
      )

      if (conversation) {
        const isInbound =
          message.direction === 'INBOUND'

        const update: Partial<ConversationDto> = {
          lastMessage: message,

          lastMessageAt:
            message.sentAt ?? message.createdAt,

          messageCount:
            (conversation.messageCount ?? 0) + 1,
        }

        if (isInbound && message.senderName?.trim()) {
          const name = message.senderName.trim()
          update.contact = {
            ...(conversation.contact || { id: '' }),
            fullName: name,
            displayName: name,
          } as ContactDto
        }

        store.updateConversation(
          message.conversationId,
          update,
        )
      }
    },
  )

  // =====================================================
  // ESTADO DEL MENSAJE
  // =====================================================

  useSocketEvent<{
    messageId: string
    status: MessageStatus
  }>('message:status', (data) => {
    useChatStore
      .getState()
      .updateMessageStatus(data.messageId, data.status)
  })

  // =====================================================
  // CONVERSACIÓN ACTUALIZADA
  // =====================================================

  useSocketEvent<ConversationDto>(
    'conversation:updated',
    (conversation) => {
      useChatStore
        .getState()
        .updateConversation(conversation.id, conversation)
    },
  )

  // =====================================================
  // NUEVA CONVERSACIÓN
  // =====================================================

  useSocketEvent<ConversationDto>(
    'conversation:new',
    (conversation) => {
      useChatStore.getState().addConversation(conversation)
    },
  )

  // =====================================================
  // PRESENCIA ONLINE
  // =====================================================

  useSocketEvent<{
    userId: string
    online: boolean
  }>('user:online', (_data) => {
    const store = useChatStore.getState()

    store.setOnlineUsers(Array.from(store.onlineUsers))
  })

  // =====================================================
  // TYPING INDICATOR
  // =====================================================

  useSocketEvent<{
    conversationId: string
    userId: string
  }>('agent:typing', (data) => {
    const store = useChatStore.getState()

    store.addTypingUser(
      data.conversationId,
      data.userId,
    )

    const key = `${data.conversationId}:${data.userId}`

    if (typingTimeouts.current[key]) {
      clearTimeout(typingTimeouts.current[key])
    }

    typingTimeouts.current[key] = setTimeout(() => {
      store.removeTypingUser(
        data.conversationId,
        data.userId,
      )

      delete typingTimeouts.current[key]
    }, 3000)
  })

  // =====================================================
  // EMIT TYPING
  // =====================================================

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

  return {
    emitTyping,
    emitStopTyping,
  }
}