import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { chatbotService } from '@/services/chatbot.service'
import type { CreateChatbotIntentRequest } from '@/types/chatbot'
import { toast } from 'sonner'
import { useState } from 'react'
import { STALE_TIMES } from '@/config/constants'

export function useChatbotIntents() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['chatbot-intents', page, search],
    queryFn: () => chatbotService.getIntents({ page, size: 20 }),
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    intents: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page, setPage, search, setSearch, isLoading,
  }
}

export function useChatbotIntentMutations() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['chatbot-intents'] })

  const create = useMutation({
    mutationFn: (dto: CreateChatbotIntentRequest) => chatbotService.createIntent(dto),
    onSuccess: () => { invalidate(); toast.success('Intención creada') },
    onError: () => toast.error('Error al crear intención'),
  })

  const update = useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: CreateChatbotIntentRequest }) =>
      chatbotService.updateIntent(id, dto),
    onSuccess: () => { invalidate(); toast.success('Intención actualizada') },
    onError: () => toast.error('Error al actualizar intención'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => chatbotService.deleteIntent(id),
    onSuccess: () => { invalidate(); toast.success('Intención eliminada') },
    onError: () => toast.error('Error al eliminar intención'),
  })

  return { createMutation: create, updateMutation: update, deleteMutation: remove }
}
