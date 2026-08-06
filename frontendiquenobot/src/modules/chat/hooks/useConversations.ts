import { useEffect, useRef } from 'react'
import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { conversationService } from '@/services/conversation.service'
import { useChatStore } from '../stores/chat.store'
import { useAuthStore } from '@/core/auth/auth.store'
import { toast } from 'sonner'
import type {
  ConversationDto,
  CreateConversationRequest,
  SendMessageRequest,
  ConversationMessageDto,
  MessageAttachmentDto,
} from '@/types/chat'
import type { PagedResponse } from '@/types/api'

const TEMP_ID_PREFIX = 'temp_'

function generateTempId(): string {
  return `${TEMP_ID_PREFIX}${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
}

function buildOptimisticAttachments(
  tempId: string,
  dto: SendMessageRequest,
): MessageAttachmentDto[] {
  return (dto.attachmentUrls ?? []).map((url, i) => ({
    id: `${tempId}_att_${i}`,
    type:
      dto.type === 'IMAGE'
        ? 'IMAGE'
        : dto.type === 'AUDIO'
          ? 'AUDIO'
          : dto.type === 'VIDEO'
            ? 'VIDEO'
            : dto.type === 'STICKER'
              ? 'STICKER'
              : 'DOCUMENT',
    fileName: url.split('/').pop() ?? '',
    fileUrl: url,
    fileSize: 0,
    formattedFileSize: '',
    mimeType: '',
    thumbnailUrl: '',
    durationSeconds: null,
    width: null,
    height: null,
    caption: '',
  }))
}

export function useConversations(filter?: {
  assigned?: 'mine' | 'unassigned'
}) {
  const { setConversations, setLoading } = useChatStore()

  const query = useInfiniteQuery<
    PagedResponse<ConversationDto>,
    Error
  >({
    queryKey: ['conversations', filter],
    queryFn: async ({ pageParam = 0 }) => {
      const params = { page: pageParam as number, size: 20 }
      if (filter?.assigned === 'mine')
        return conversationService.getMyConversations(params)
      if (filter?.assigned === 'unassigned')
        return conversationService.getUnassigned(params)
      return conversationService.getActive(params)
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined
      return lastPage.page + 1
    },
    initialPageParam: 0,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    refetchInterval: 30_000,
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
    retry: false,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  })
}

export function useSafeConversation(id: string | undefined) {
  const query = useConversation(id)
  const activeIdRef = useRef<string | undefined>(undefined)

  useEffect(() => {
    activeIdRef.current = id
  }, [id])

  const isCurrent = id === activeIdRef.current

  return {
    data: isCurrent ? query.data : undefined,
    isLoading: query.isLoading,
    isError: query.isError,
    isNotFound: query.isError && !query.isLoading && !query.data,
    error: query.error,
  }
}

export function useCreateConversation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (dto: CreateConversationRequest) => {
      return conversationService.create(dto)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['conversations'],
      })
    },
    onError: (error: Error) => {
      toast.error(
        error.message || 'Error al crear la conversación',
      )
    },
  })
}

export function useMessages(conversationId: string | undefined) {
  const { setMessages } = useChatStore()
  const fetchKeyRef = useRef(0)

  const query = useInfiniteQuery<
    PagedResponse<ConversationMessageDto>,
    Error
  >({
    queryKey: ['messages', conversationId],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await conversationService.getMessages(
        conversationId!,
        { page: pageParam as number, size: 50 },
      )
      return response
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined
      return lastPage.page + 1
    },
    initialPageParam: 0,
    enabled: !!conversationId,
    retry: false,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    refetchInterval: 30_000,
  })

  useEffect(() => {
    if (!conversationId || !query.data) return

    const currentKey = ++fetchKeyRef.current
    const allMessages = query.data.pages.flatMap(
      (page) => page.content,
    )

    requestAnimationFrame(() => {
      if (fetchKeyRef.current === currentKey) {
        setMessages(conversationId, allMessages)
      }
    })
  }, [conversationId, query.data, setMessages])

  return {
    fetchNextPage: query.fetchNextPage,
    hasNextPage: query.hasNextPage,
    isFetchingNextPage: query.isFetchingNextPage,
    isLoading: query.isLoading,
    isError: query.isError,
  }
}

export function useSendMessage() {
  const addMessage = useChatStore((state) => state.addMessage)
  const replaceMessage = useChatStore(
    (state) => state.replaceMessage,
  )
  const currentUser = useAuthStore((state) => state.user)

  return useMutation({
    mutationFn: async (
      dto: SendMessageRequest,
    ): Promise<ConversationMessageDto> => {
      return conversationService.sendMessage(dto)
    },
    onMutate: async (dto) => {
      const tempId = generateTempId()
      const now = new Date().toISOString()

      const optimistic: ConversationMessageDto = {
        id: tempId,
        conversationId: dto.conversationId,
        userId: currentUser?.id ?? '',
        userName: currentUser?.fullName ?? '',
        direction: 'OUTBOUND',
        type: dto.type,
        status: 'PENDING',
        content: dto.content,
        channelMessageId: '',
        replyToMessageId: '',
        senderName: currentUser?.fullName ?? '',
        senderPhone: '',
        senderEmail: '',
        fromBot: false,
        botIntent: '',
        botConfidence: 0,
        sentAt: now,
        deliveredAt: '',
        readAt: '',
        failedAt: '',
        failureReason: '',
        attachments: buildOptimisticAttachments(tempId, dto),
        createdAt: now,
      }

      addMessage(dto.conversationId, optimistic)
      return { tempId }
    },
onSuccess: (realMessage, dto, context) => {
  if (context?.tempId) {
    replaceMessage(
      context.tempId,
      dto.conversationId,
      realMessage,
    )
  } else {
    addMessage(dto.conversationId, realMessage)
  }

  // No invalidar queries — el socket message:new actualiza la conversación
  // y evita el refetch completo que causa saltos de posición
},
    onError: (error: Error, dto, context) => {
      if (context?.tempId) {
        const failed: ConversationMessageDto = {
          id: context.tempId,
          conversationId: dto.conversationId,
          userId: currentUser?.id ?? '',
          userName: currentUser?.fullName ?? '',
          direction: 'OUTBOUND',
          type: dto.type,
          status: 'FAILED',
          content: dto.content,
          channelMessageId: '',
          replyToMessageId: '',
          senderName: currentUser?.fullName ?? '',
          senderPhone: '',
          senderEmail: '',
          fromBot: false,
          botIntent: '',
          botConfidence: 0,
          sentAt: new Date().toISOString(),
          deliveredAt: '',
          readAt: '',
          failedAt: new Date().toISOString(),
          failureReason: error.message,
          attachments: buildOptimisticAttachments(context.tempId, dto),
          createdAt: new Date().toISOString(),
        }
        replaceMessage(
          context.tempId,
          dto.conversationId,
          failed,
        )
      }
      toast.error(
        error.message || 'Error al enviar el mensaje',
      )
    },
  })
}

export function useUpdateConversation() {
  const queryClient = useQueryClient()
  const updateConversation = useChatStore(
    (state) => state.updateConversation,
  )

  const { mutate: actionMutate } = useMutation({
    mutationFn: async ({
      id,
      action,
    }: {
      id: string
      action: string
    }) => {
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
      queryClient.invalidateQueries({
        queryKey: ['conversations'],
      })
      queryClient.invalidateQueries({
        queryKey: ['conversation', variables.id],
      })
    },
  })

  const { mutate: markAsReadMutate } = useMutation({
    mutationFn: async (id: string) => {
      await conversationService.markAsRead(id)
    },
    onError: () => {
      // Silently ignore markAsRead errors (expected for deleted/expired conversations)
    },
  })

  return {
    resolve: (id: string) =>
      actionMutate({ id, action: 'resolve' }),
    close: (id: string) =>
      actionMutate({ id, action: 'close' }),
    reopen: (id: string) =>
      actionMutate({ id, action: 'reopen' }),
    markAsRead: markAsReadMutate,
  }
}

export function useAssignConversation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      id,
      userId,
    }: {
      id: string
      userId: string
    }) => {
      await conversationService.assign(id, userId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['conversations'],
      })
      toast.success('Conversación asignada')
    },
  })
}
