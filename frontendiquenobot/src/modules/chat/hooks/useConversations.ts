import { useEffect } from 'react'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { conversationService } from '@/services/conversation.service'
import { useChatStore } from '../stores/chat.store'
import { toast } from 'sonner'
import type { CreateConversationRequest } from '@/types/chat'

export function useConversations(filter?: { assigned?: 'mine' | 'unassigned' }) {
  const { setConversations, setLoading } = useChatStore()

  const query = useInfiniteQuery({
    queryKey: ['conversations', filter],
    queryFn: async ({ pageParam = 0 }) => {
      const params = { page: pageParam, size: 20 }
      if (filter?.assigned === 'mine') return conversationService.getMyConversations(params)
      if (filter?.assigned === 'unassigned') return conversationService.getUnassigned(params)
      return conversationService.getActive(params)
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined
      return lastPage.number + 1
    },
    initialPageParam: 0,
  })

  useEffect(() => {
    setLoading(query.isLoading)
    if (query.data) {
      const all = query.data.pages.flatMap((p) => p.content)
      setConversations(all)
    }
  }, [query.data, query.isLoading, setConversations, setLoading])

  return query
}

export function useConversation(id: string | undefined) {
  return useQuery({
    queryKey: ['conversation', id],
    queryFn: () => conversationService.getById(id!),
    enabled: !!id,
  })
}

export function useCreateConversation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (dto: CreateConversationRequest) => {
      return conversationService.create(dto)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    },
    onError: (error: Error) => {
      toast.error(error.message || 'Error al crear la conversación')
    },
  })
}

export function useMessages(conversationId: string | undefined) {
  const { setMessages, addMessage } = useChatStore()

  const query = useInfiniteQuery({
    queryKey: ['messages', conversationId],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await conversationService.getMessages(conversationId!, {
        page: pageParam,
        size: 50,
      })
      return response
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined
      return lastPage.number + 1
    },
    initialPageParam: 0,
    enabled: !!conversationId,
  })

  useEffect(() => {
    if (query.data && conversationId) {
      const all = query.data.pages.flatMap((p) => p.content)
      setMessages(conversationId, all)
    }
  }, [query.data, conversationId, setMessages])

  return {
    fetchNextPage: query.fetchNextPage,
    hasNextPage: query.hasNextPage,
    isFetchingNextPage: query.isFetchingNextPage,
    isLoading: query.isLoading,
  }
}

export function useSendMessage() {
  const addMessage = useChatStore((state) => state.addMessage)

  return useMutation({
    mutationFn: async (dto: { conversationId: string; type: string; content: string }) => {
      return conversationService.sendMessage({
        conversationId: dto.conversationId,
        type: dto.type as 'TEXT' | 'IMAGE' | 'DOCUMENT' | 'AUDIO',
        content: dto.content,
      })
    },
    onSuccess: (message) => {
      addMessage(message.conversationId, message)
    },
  })
}

export function useUpdateConversation() {
  const queryClient = useQueryClient()
  const updateConversation = useChatStore((state) => state.updateConversation)

  const { mutate: actionMutate } = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: string }) => {
      switch (action) {
        case 'resolve':
          await conversationService.resolve(id)
          break
        case 'close':
          await conversationService.close(id)
          break
        case 'reopen':
          await conversationService.reopen(id)
          break
      }
    },
    onSuccess: (_data, variables) => {
      updateConversation(variables.id, {})
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
      queryClient.invalidateQueries({ queryKey: ['conversation', variables.id] })
    },
  })

  const { mutate: markAsReadMutate } = useMutation({
    mutationFn: async (id: string) => {
      await conversationService.markAsRead(id)
    },
  })

  return {
    resolve: (id: string) => actionMutate({ id, action: 'resolve' }),
    close: (id: string) => actionMutate({ id, action: 'close' }),
    reopen: (id: string) => actionMutate({ id, action: 'reopen' }),
    markAsRead: markAsReadMutate,
  }
}

export function useAssignConversation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ id, userId }: { id: string; userId: string }) => {
      await conversationService.assign(id, userId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
      toast.success('Conversación asignada')
    },
  })
}
