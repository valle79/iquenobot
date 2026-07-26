import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAssignConversation } from '@/modules/chat/hooks/useConversations'
import { userService } from '@/services/user.service'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { Button } from '@/shared/atoms/Button/Button'
import { Search, UserCheck, Loader2 } from 'lucide-react'

interface AssignAgentModalProps {
  open: boolean
  conversationId: string
  currentUserId: string | null
  onClose: () => void
}

export function AssignAgentModal({ open, conversationId, currentUserId, onClose }: AssignAgentModalProps) {
  const [search, setSearch] = useState('')
  const assign = useAssignConversation()

  const { data: users, isLoading } = useQuery({
    queryKey: ['users', 'assignment'],
    queryFn: async () => {
      const res = await userService.getAll({ page: 0, size: 50 })
      return res.content
    },
    enabled: open,
  })

  const filtered = users?.filter(
    (u) =>
      u.id !== currentUserId &&
      (u.fullName.toLowerCase().includes(search.toLowerCase()) ||
        u.email.toLowerCase().includes(search.toLowerCase())),
  )

  const handleAssign = (userId: string) => {
    assign.mutate({ id: conversationId, userId })
    onClose()
  }

  return (
    <Modal open={open} onClose={onClose} title="Asignar agente" size="md">
      <div className="space-y-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            className="h-10 w-full rounded-lg border border-gray-300 bg-white pl-10 pr-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Buscar agente..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            autoFocus
          />
        </div>

        <div className="max-h-64 space-y-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
            </div>
          ) : filtered?.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">No se encontraron agentes</p>
          ) : (
            filtered?.map((user) => (
              <button
                key={user.id}
                onClick={() => handleAssign(user.id)}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-gray-100 dark:hover:bg-gray-800"
              >
                <Avatar name={user.fullName} src={user.avatarUrl} size="sm" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                    {user.fullName}
                  </p>
                  <p className="text-xs text-gray-500 truncate">{user.email}</p>
                </div>
                <UserCheck size={16} className="text-gray-400 shrink-0" />
              </button>
            ))
          )}
        </div>
      </div>

      <div className="flex justify-end pt-4">
        <Button variant="ghost" onClick={onClose}>Cancelar</Button>
      </div>
    </Modal>
  )
}
