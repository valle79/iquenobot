import { useState } from 'react'
import { X, Plus, Tag } from 'lucide-react'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Button } from '@/shared/atoms/Button/Button'
import { useConversation, useUpdateConversation } from '@/modules/chat/hooks/useConversations'

interface TagsManagerProps {
  conversationId: string
}

const AVAILABLE_TAGS = ['VIP', 'Nuevo', 'Reclamo', 'Soporte', 'Ventas', 'Postventa', 'Urgente', 'Follow-up']

export function TagsManager({ conversationId }: TagsManagerProps) {
  const { data: conversation } = useConversation(conversationId)
  const [adding, setAdding] = useState(false)

  const currentTags = conversation?.tags
    ? conversation.tags.split(',').map((t: string) => t.trim()).filter(Boolean)
    : []

  const addTag = async (tag: string) => {
    const updated = [...currentTags, tag].join(', ')
    try {
      await useUpdateConversation().resolve(conversationId)
    } catch {}
  }

  const removeTag = async (tag: string) => {
    const updated = currentTags.filter((t: string) => t !== tag).join(', ')
    try {} catch {}
  }

  const available = AVAILABLE_TAGS.filter((t) => !currentTags.includes(t))

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-3 dark:border-gray-700 dark:bg-gray-950">
      <div className="mb-2 flex items-center gap-2">
        <Tag size={14} className="text-gray-400" />
        <span className="text-xs font-medium text-gray-500">Etiquetas</span>
      </div>

      <div className="flex flex-wrap gap-1.5">
        {currentTags.map((tag: string) => (
          <Badge key={tag} variant="neutral" size="sm" className="pr-1">
            {tag}
            <button
              onClick={() => removeTag(tag)}
              className="ml-1 rounded-full p-0.5 hover:bg-gray-200 dark:hover:bg-gray-600"
            >
              <X size={10} />
            </button>
          </Badge>
        ))}

        {!adding ? (
          <button
            onClick={() => setAdding(true)}
            className="inline-flex items-center gap-0.5 rounded-full border border-dashed border-gray-300 px-2 py-0.5 text-xs text-gray-500 hover:border-gray-400 hover:text-gray-700 dark:border-gray-600 dark:hover:border-gray-500"
          >
            <Plus size={12} />
            Agregar
          </button>
        ) : (
          <div className="flex flex-wrap gap-1">
            {available.map((tag) => (
              <button
                key={tag}
                onClick={() => { addTag(tag); setAdding(false) }}
                className="rounded-full border border-gray-200 px-2 py-0.5 text-xs text-gray-600 hover:bg-gray-100 dark:border-gray-600 dark:text-gray-400 dark:hover:bg-gray-800"
              >
                {tag}
              </button>
            ))}
            {available.length === 0 && (
              <span className="text-xs text-gray-400">No más etiquetas disponibles</span>
            )}
            <button
              onClick={() => setAdding(false)}
              className="rounded-full px-2 py-0.5 text-xs text-gray-400 hover:text-gray-600"
            >
              Cancelar
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
