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

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},
  onlineUsers: new Set(),
  typingUsers: {},
  loading: false,
  error: null,

  setConversations: (conversations) => set({ conversations }),

  addConversation: (conversation) =>
    set((state) => {
      const exists = state.conversations.find((c) => c.id === conversation.id)
      if (exists) {
        return {
          conversations: state.conversations.map((c) =>
            c.id === conversation.id ? conversation : c,
          ),
        }
      }
      return { conversations: [conversation, ...state.conversations] }
    }),

  updateConversation: (id, updates) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.id === id ? { ...c, ...updates } : c,
      ),
    })),

  removeConversation: (id) =>
    set((state) => ({
      conversations: state.conversations.filter((c) => c.id !== id),
      activeConversationId:
        state.activeConversationId === id ? null : state.activeConversationId,
    })),

  setActiveConversationId: (id) => set({ activeConversationId: id }),

  setMessages: (conversationId, messages) =>
    set((state) => ({
      messages: { ...state.messages, [conversationId]: messages },
    })),

  addMessage: (conversationId, message) =>
    set((state) => {
      const existing = state.messages[conversationId] ?? []
      const exists = existing.find((m) => m.id === message.id)
      if (exists) {
        return {
          messages: {
            ...state.messages,
            [conversationId]: existing.map((m) =>
              m.id === message.id ? message : m,
            ),
          },
        }
      }
      return {
        messages: {
          ...state.messages,
          [conversationId]: [...existing, message],
        },
      }
    }),

  updateMessageStatus: (messageId, status) =>
    set((state) => {
      const newMessages = { ...state.messages }
      for (const convId of Object.keys(newMessages)) {
        newMessages[convId] = newMessages[convId].map((m) =>
          m.id === messageId ? { ...m, status } : m,
        )
      }
      return { messages: newMessages }
    }),

  setOnlineUsers: (users) => set({ onlineUsers: new Set(users) }),

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
    set((state) => {
      const current = state.typingUsers[conversationId] ?? []
      return {
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: current.filter((u) => u !== userId),
        },
      }
    }),

  setLoading: (loading) => set({ loading }),
  setError: (error) => set({ error }),
}))
