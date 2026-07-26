import { useState } from 'react'
import { FileText, Send } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'
import { Avatar } from '@/shared/atoms/Avatar/Avatar'
import { useAuthStore } from '@/core/auth/auth.store'
import { dayjs } from '@/config/dayjs'

interface InternalNotesProps {
  conversationId: string
}

interface Note {
  id: string
  content: string
  author: string
  authorAvatar?: string
  createdAt: string
}

export function InternalNotes({ conversationId }: InternalNotesProps) {
  const [notes, setNotes] = useState<Note[]>([
    {
      id: '1',
      content: 'Cliente solicita información sobre planes empresariales.',
      author: 'Carlos Pérez',
      createdAt: new Date().toISOString(),
    },
  ])
  const [newNote, setNewNote] = useState('')
  const user = useAuthStore((s) => s.user)

  const handleAddNote = () => {
    if (!newNote.trim() || !user) return
    const note: Note = {
      id: Date.now().toString(),
      content: newNote.trim(),
      author: user.fullName,
      authorAvatar: user.avatarUrl,
      createdAt: new Date().toISOString(),
    }
    setNotes((prev) => [note, ...prev])
    setNewNote('')
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
          <div key={note.id} className="rounded-lg bg-gray-50 p-3 dark:bg-gray-800/50">
            <div className="mb-1 flex items-center gap-2">
              <Avatar name={note.author} src={note.authorAvatar} size="xs" />
              <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{note.author}</span>
              <span className="text-xs text-gray-400">{dayjs(note.createdAt).fromNow()}</span>
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
        <Button variant="ghost" size="sm" icon onClick={handleAddNote} disabled={!newNote.trim()}>
          <Send size={16} />
        </Button>
      </div>
    </div>
  )
}
