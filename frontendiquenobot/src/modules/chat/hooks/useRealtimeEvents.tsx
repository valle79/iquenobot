import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSocketEvent } from '@/core/websocket/socket.hooks'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { UserPlus, AlertTriangle } from 'lucide-react'
import type { LeadCreatedEvent, ConversationEscalatedEvent } from '@/types/orchestrator'

export function useRealtimeEvents() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  useSocketEvent<LeadCreatedEvent>('lead:created', (event) => {
    toast.success('Nuevo lead creado', {
      description: `Lead #${event.leadId.slice(0, 8)} desde ${event.channel}`,
      icon: <UserPlus size={16} />,
      action: {
        label: 'Ver',
        onClick: () => navigate(`/leads/${event.leadId}`),
      },
    })
    queryClient.invalidateQueries({ queryKey: ['leads'] })
  })

  useSocketEvent<ConversationEscalatedEvent>('conversation:escalated', (event) => {
    toast.warning('Conversación escalada', {
      description: `Razón: ${event.reason}`,
      icon: <AlertTriangle size={16} />,
      action: {
        label: 'Ver',
        onClick: () => navigate(`/conversations/${event.conversationId}`),
      },
    })
    queryClient.invalidateQueries({ queryKey: ['conversations'] })
  })
}
