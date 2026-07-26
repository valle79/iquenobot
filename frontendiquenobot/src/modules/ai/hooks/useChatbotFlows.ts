import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { chatbotService } from '@/services/chatbot.service'
import type { CreateChatbotFlowRequest } from '@/types/chatbot'
import { toast } from 'sonner'
import { useState } from 'react'
import { STALE_TIMES } from '@/config/constants'

export function useChatbotFlows() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['chatbot-flows', page, search],
    queryFn: () => chatbotService.getFlows({ page, size: 20 }),
    staleTime: STALE_TIMES.MEDIUM,
  })

  return {
    flows: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    page, setPage, search, setSearch, isLoading,
  }
}

export function useChatbotFlowMutations() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['chatbot-flows'] })

  const create = useMutation({
    mutationFn: (dto: CreateChatbotFlowRequest) => chatbotService.createFlow(dto),
    onSuccess: () => { invalidate(); toast.success('Flujo creado') },
    onError: () => toast.error('Error al crear flujo'),
  })

  const update = useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: CreateChatbotFlowRequest }) =>
      chatbotService.updateFlow(id, dto),
    onSuccess: () => { invalidate(); toast.success('Flujo actualizado') },
    onError: () => toast.error('Error al actualizar flujo'),
  })

  const remove = useMutation({
    mutationFn: (id: string) => chatbotService.deleteFlow(id),
    onSuccess: () => { invalidate(); toast.success('Flujo eliminado') },
    onError: () => toast.error('Error al eliminar flujo'),
  })

  return { createMutation: create, updateMutation: update, deleteMutation: remove }
}
