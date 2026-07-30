import { create } from 'zustand'
import type { ConversationDto, ConversationMessageDto } from '@/types/chat'

// =====================================================
// CONSTANTS
// =====================================================

const TEMP_ID_PREFIX = 'temp_'

// =====================================================
// PURE HELPERS
// =====================================================

const getMessageTimestamp = (
  message: ConversationMessageDto,
): number => {
  const date = message.sentAt ?? message.createdAt
  if (!date) return 0
  const timestamp = new Date(date).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const sortMessages = (
  messages: ConversationMessageDto[],
): ConversationMessageDto[] =>
  [...messages].sort((a, b) => {
    const tsA = getMessageTimestamp(a)
    const tsB = getMessageTimestamp(b)
    if (tsA !== tsB) return tsA - tsB
    const idA = a.id ?? ''
    const idB = b.id ?? ''
    return idA.localeCompare(idB)
  })

const getConversationTimestamp = (
  conversation: ConversationDto,
): number => {
  const date =
    conversation.lastMessageAt ??
    conversation.updatedAt ??
    conversation.createdAt
  return date ? new Date(date).getTime() : 0
}

const sortConversations = (
  conversations: ConversationDto[],
): ConversationDto[] =>
  [...conversations].sort(
    (a, b) =>
      getConversationTimestamp(b) - getConversationTimestamp(a),
  )

const isTempId = (id: string): boolean =>
  id.startsWith(TEMP_ID_PREFIX)

// =====================================================
// STATE TYPE
// =====================================================

interface ChatState {
  conversations: ConversationDto[]
  activeConversationId: string | null
  messages: Record<string, ConversationMessageDto[]>
  onlineUsers: Set<string>
  typingUsers: Record<string, string[]>
  loading: boolean
  error: string | null

  // Conversations
  setConversations: (conversations: ConversationDto[]) => void
  addConversation: (conversation: ConversationDto) => void
  updateConversation: (
    id: string,
    updates: Partial<ConversationDto>,
  ) => void
  removeConversation: (id: string) => void
  setActiveConversationId: (id: string | null) => void

  // Messages
  setMessages: (
    conversationId: string,
    messages: ConversationMessageDto[],
  ) => void
  addMessage: (
    conversationId: string,
    message: ConversationMessageDto,
  ) => void
  replaceMessage: (
    tempId: string,
    conversationId: string,
    message: ConversationMessageDto,
  ) => void
  updateMessageStatus: (
    messageId: string,
    status: ConversationMessageDto['status'],
  ) => void
  findTempMessage: (
    conversationId: string,
  ) => ConversationMessageDto | undefined

  // Presence
  setOnlineUsers: (users: string[]) => void

  // Typing
  addTypingUser: (conversationId: string, userId: string) => void
  removeTypingUser: (conversationId: string, userId: string) => void

  // UI
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
}

// =====================================================
// STORE
// =====================================================

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},
  onlineUsers: new Set(),
  typingUsers: {},
  loading: false,
  error: null,

  // =====================================================
  // CONVERSATIONS
  // =====================================================

  setConversations: (conversations) =>
    set({
      conversations: sortConversations(conversations),
    }),

  addConversation: (conversation) =>
    set((state) => {
      const exists = state.conversations.some(
        (c) => c.id === conversation.id,
      )
      const updatedConversations = exists
        ? state.conversations.map((c) =>
            c.id === conversation.id ? conversation : c,
          )
        : [conversation, ...state.conversations]
      return {
        conversations: sortConversations(updatedConversations),
      }
    }),

  updateConversation: (id, updates) =>
    set((state) => ({
      conversations: sortConversations(
        state.conversations.map((conversation) =>
          conversation.id === id
            ? { ...conversation, ...updates }
            : conversation,
        ),
      ),
    })),

  removeConversation: (id) =>
    set((state) => ({
      conversations: state.conversations.filter(
        (conversation) => conversation.id !== id,
      ),
      activeConversationId:
        state.activeConversationId === id
          ? null
          : state.activeConversationId,
    })),

  setActiveConversationId: (id) =>
    set({ activeConversationId: id }),

  // =====================================================
  // MESSAGES
  // =====================================================

  setMessages: (conversationId, incomingMessages) =>
    set((state) => {
      const currentMessages =
        state.messages[conversationId] ?? []
      const merged = new Map<
        string,
        ConversationMessageDto
      >()
      // Preserve temp messages first
      currentMessages.forEach((msg) => {
        if (isTempId(msg.id)) {
          merged.set(msg.id, msg)
        }
      })
      // Overwrite with server messages
      incomingMessages.forEach((msg) => {
        merged.set(msg.id, msg)
      })
      // Merge non-temp current messages not in incoming
      currentMessages.forEach((msg) => {
        if (!isTempId(msg.id) && !merged.has(msg.id)) {
          merged.set(msg.id, msg)
        }
      })
      return {
        messages: {
          ...state.messages,
          [conversationId]: sortMessages(
            Array.from(merged.values()),
          ),
        },
      }
    }),

  addMessage: (conversationId, message) =>
    set((state) => {
      const currentMessages =
        state.messages[conversationId] ?? []

      const existingIndex = currentMessages.findIndex(
        (m) => m.id === message.id,
      )

      const updatedMessages =
        existingIndex >= 0
          ? currentMessages.map((m) =>
              m.id === message.id
                ? { ...m, ...message }
                : m,
            )
          : [...currentMessages, message]

      return {
        messages: {
          ...state.messages,
          [conversationId]:
            sortMessages(updatedMessages),
        },
      }
    }),

replaceMessage: (tempId, conversationId, message) =>
  set((state) => {
    const currentMessages =
      state.messages[conversationId] ?? []

    const updatedMessages = currentMessages.map((m) =>
      m.id === tempId ? message : m,
    )

    return {
      messages: {
        ...state.messages,
        [conversationId]: sortMessages(updatedMessages),
      },
    }
  }),

  findTempMessage: (conversationId) => {
    const messages = get().messages[conversationId]
    if (!messages) return undefined
    return messages
      .filter((m) => isTempId(m.id))
      .sort(
        (a, b) =>
          getMessageTimestamp(b) - getMessageTimestamp(a),
      )[0]
  },

  updateMessageStatus: (messageId, status) =>
    set((state) => {
      const updatedMessages: Record<
        string,
        ConversationMessageDto[]
      > = {}

      Object.entries(state.messages).forEach(
        ([conversationId, messages]) => {
          updatedMessages[conversationId] = messages.map(
            (message) =>
              message.id === messageId
                ? { ...message, status }
                : message,
          )
        },
      )

      return { messages: updatedMessages }
    }),

  // =====================================================
  // ONLINE USERS
  // =====================================================

  setOnlineUsers: (users) =>
    set({
      onlineUsers: new Set(users),
    }),

  // =====================================================
  // TYPING
  // =====================================================

  addTypingUser: (conversationId, userId) =>
    set((state) => {
      const currentUsers =
        state.typingUsers[conversationId] ?? []
      if (currentUsers.includes(userId)) return state
      return {
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: [...currentUsers, userId],
        },
      }
    }),

  removeTypingUser: (conversationId, userId) =>
    set((state) => {
      const currentUsers =
        state.typingUsers[conversationId] ?? []
      return {
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: currentUsers.filter(
            (user) => user !== userId,
          ),
        },
      }
    }),

  // =====================================================
  // UI STATE
  // =====================================================

  setLoading: (loading) => set({ loading }),
  setError: (error) => set({ error }),
}))
