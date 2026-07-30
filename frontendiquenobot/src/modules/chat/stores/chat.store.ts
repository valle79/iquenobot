import { create } from 'zustand'
import type { ConversationDto, ConversationMessageDto } from '@/types/chat'

interface ChatState {
  conversations: ConversationDto[]
  activeConversationId: string | null
  messages: Record<string, ConversationMessageDto[]>
  onlineUsers: Set<string>
  typingUsers: Record<string, string[]>
  loading: boolean
  error: string | null

  setConversations: (conversations: ConversationDto[]) => void
  addConversation: (conversation: ConversationDto) => void
  updateConversation: (id: string, updates: Partial<ConversationDto>) => void
  removeConversation: (id: string) => void
  setActiveConversationId: (id: string | null) => void
  setMessages: (conversationId: string, messages: ConversationMessageDto[]) => void
  addMessage: (conversationId: string, message: ConversationMessageDto) => void
  updateMessageStatus: (messageId: string, status: ConversationMessageDto['status']) => void
  setOnlineUsers: (users: string[]) => void
  addTypingUser: (conversationId: string, userId: string) => void
  removeTypingUser: (conversationId: string, userId: string) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
}

// ==================== HELPERS ====================

const getMessageDate = (message: ConversationMessageDto) =>
  new Date(message.sentAt || message.createdAt).getTime()

const sortMessages = (messages: ConversationMessageDto[]) =>
  [...messages].sort((a, b) => getMessageDate(a) - getMessageDate(b))

const getConversationDate = (conversation: ConversationDto) =>
  new Date(
    conversation.lastMessageAt || conversation.updatedAt || conversation.createdAt,
  ).getTime()

const sortConversations = (conversations: ConversationDto[]) =>
  [...conversations].sort((a, b) => getConversationDate(b) - getConversationDate(a))

// ==================== STORE ====================

export const useChatStore = create<ChatState>((set) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},
  onlineUsers: new Set(),
  typingUsers: {},
  loading: false,
  error: null,

  // ==================== CONVERSATIONS ====================

  setConversations: (conversations) =>
    set({
      conversations: sortConversations(conversations),
    }),

  addConversation: (conversation) =>
    set((state) => {
      const exists = state.conversations.find((c) => c.id === conversation.id)

      if (exists) {
        return {
          conversations: sortConversations(
            state.conversations.map((c) =>
              c.id === conversation.id ? conversation : c,
            ),
          ),
        }
      }

      return {
        conversations: sortConversations([
          conversation,
          ...state.conversations,
        ]),
      }
    }),

  updateConversation: (id, updates) =>
    set((state) => ({
      conversations: sortConversations(
        state.conversations.map((c) =>
          c.id === id ? { ...c, ...updates } : c,
        ),
      ),
    })),

  removeConversation: (id) =>
    set((state) => ({
      conversations: state.conversations.filter((c) => c.id !== id),
      activeConversationId:
        state.activeConversationId === id ? null : state.activeConversationId,
    })),

  setActiveConversationId: (id) => set({ activeConversationId: id }),

  // ==================== MESSAGES ====================

  setMessages: (conversationId, messages) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [conversationId]: sortMessages(messages),
      },
    })),

  addMessage: (conversationId, message) =>
    set((state) => {
      const existing = state.messages[conversationId] ?? []

      // evitar duplicados
      const exists = existing.some((m) => m.id === message.id)

      let updated: ConversationMessageDto[]

      if (exists) {
        updated = existing.map((m) =>
          m.id === message.id ? { ...m, ...message } : m,
        )
      } else {
        updated = [...existing, message]
      }

      return {
        messages: {
          ...state.messages,
          [conversationId]: sortMessages(updated),
        },
      }
    }),

updateMessageStatus: (messageId, status) =>
  set((state) => {
    const newMessages: Record<string, ConversationMessageDto[]> = {
      ...state.messages,
    }

    for (const convId of Object.keys(newMessages)) {
      const messages = newMessages[convId] ?? []

      newMessages[convId] = messages.map((m) =>
        m.id === messageId ? { ...m, status } : m,
      )
    }

    return { messages: newMessages }
  }),

  // ==================== ONLINE USERS ====================

  setOnlineUsers: (users) =>
    set({
      onlineUsers: new Set(users),
    }),

  // ==================== TYPING ====================

  addTypingUser: (conversationId, userId) =>
    set((state) => {
      const current = state.typingUsers[conversationId] ?? []

      if (current.includes(userId)) return state

      return {
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: [...current, userId],
        },
      }
    }),

  removeTypingUser: (conversationId, userId) =>
    set((state) => ({
      typingUsers: {
        ...state.typingUsers,
        [conversationId]: (state.typingUsers[conversationId] ?? []).filter(
          (u) => u !== userId,
        ),
      },
    })),

  // ==================== UI STATE ====================

  setLoading: (loading) => set({ loading }),
  setError: (error) => set({ error }),
}))