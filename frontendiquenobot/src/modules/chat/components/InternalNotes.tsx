import { useState, useEffect } from 'react'
import { FileText, Send, Trash2 } from 'lucide-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/shared/atoms/Button/Button'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { useAuthStore } from '@/core/auth/auth.store'
import { conversationService } from '@/services/conversation.service'
import { useConversation } from '@/modules/chat/hooks/useConversations'
import { dayjs } from '@/config/dayjs'
import { toast } from 'sonner'

interface InternalNotesProps {
  conversationId: string
}

interface Note {
  id: string
  content: string
  authorId: string
  author: string
  authorAvatar?: string
  createdAt: string
}

function parseNotes(metadata: string | null | undefined): Note[] {
  if (!metadata) return []
  try {
    const parsed = JSON.parse(metadata)
    return Array.isArray(parsed.notes) ? parsed.notes : []
  } catch {
    return []
  }
}

function buildMetadata(notes: Note[]): string {
  return JSON.stringify({ notes })
}

export function InternalNotes({ conversationId }: InternalNotesProps) {
  const [newNote, setNewNote] = useState('')
  const user = useAuthStore((s) => s.user)
  const queryClient = useQueryClient()

  const { data: conversation } = useConversation(conversationId)
  const notes = parseNotes(conversation?.metadata)

  const updateMetadataMutation = useMutation({
    mutationFn: (updatedNotes: Note[]) =>
      conversationService.updateMetadata(conversationId, buildMetadata(updatedNotes)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversation', conversationId] })
    },
  })

  const handleAddNote = () => {
    if (!newNote.trim() || !user) return
    const note: Note = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      content: newNote.trim(),
      authorId: user.id,
      author: user.fullName,
      authorAvatar: user.avatarUrl,
      createdAt: new Date().toISOString(),
    }
    const updated = [note, ...notes]
    updateMetadataMutation.mutate(updated, {
      onSuccess: () => setNewNote(''),
      onError: () => toast.error('Error al guardar nota'),
    })
  }

  const handleDeleteNote = (noteId: string) => {
    const updated = notes.filter((n) => n.id !== noteId)
    updateMetadataMutation.mutate(updated, {
      onError: () => toast.error('Error al eliminar nota'),
    })
  }

  return (
    <div className="rounded-lg border border-gray-200 dark:border-gray-700">
      <div className="flex items-center gap-2 border-b border-gray-200 px-3 py-2.5 dark:border-gray-700">
        <FileText size={14} className="text-gray-400" />
        <span className="text-xs font-medium text-gray-500">Notas internas</span>
      </div>

      <div className="space-y-2 p-3">
        {notes.length === 0 && (
          <p className="py-4 text-center text-sm text-gray-400">Sin notas internas</p>
        )}
        {notes.map((note) => (
          <div key={note.id} className="group rounded-lg bg-gray-50 p-3 dark:bg-gray-800/50">
            <div className="mb-1 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Avatar name={note.author} src={note.authorAvatar} size="xs" />
                <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{note.author}</span>
                <span className="text-xs text-gray-400">{dayjs(note.createdAt).fromNow()}</span>
              </div>
              {note.authorId === user?.id && (
                <button
                  onClick={() => handleDeleteNote(note.id)}
                  className="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 transition-opacity"
                >
                  <Trash2 size={12} />
                </button>
              )}
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400">{note.content}</p>
          </div>
        ))}
      </div>

      <div className="flex items-center gap-2 border-t border-gray-200 p-3 dark:border-gray-700">
        <input
          className="h-9 flex-1 rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
          placeholder="Agregar nota..."
          value={newNote}
          onChange={(e) => setNewNote(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAddNote() } }}
        />
        <Button
          variant="ghost"
          size="sm"
          icon
          onClick={handleAddNote}
          disabled={!newNote.trim()}
          loading={updateMetadataMutation.isPending}
        >
          <Send size={16} />
        </Button>
      </div>
    </div>
  )
}
