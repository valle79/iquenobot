import { useState, useRef, useCallback, type ChangeEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Send,
  Paperclip,
  Image,
  FileText,
  X,
  Mic,
} from 'lucide-react'
import { useSendMessage } from '@/modules/chat/hooks/useConversations'
import { Button } from '@/shared/atoms/Button/Button'
import { cn } from '@/shared/utils'
import { AnimatePresence, motion } from 'framer-motion'

interface Attachment {
  file: File
  preview: string
  type: 'image' | 'document' | 'audio'
}

export function MessageInput() {
  const { t } = useTranslation()
  const { id: conversationId } = useParams<{ id: string }>()
  const [message, setMessage] = useState('')
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [showAttachMenu, setShowAttachMenu] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const documentInputRef = useRef<HTMLInputElement>(null)
  const audioInputRef = useRef<HTMLInputElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const sendMutation = useSendMessage()

  const handleFileSelect = useCallback((e: ChangeEvent<HTMLInputElement>, type: 'image' | 'document' | 'audio') => {
    const files = e.target.files
    if (!files?.length) return

    const newAttachments: Attachment[] = Array.from(files).map((file) => ({
      file,
      preview: type === 'image' ? URL.createObjectURL(file) : file.name,
      type,
    }))

    setAttachments((prev) => [...prev, ...newAttachments])
    e.target.value = ''
  }, [])

  const removeAttachment = useCallback((index: number) => {
    setAttachments((prev) => {
      const att = prev[index]
      if (att?.preview && att.type === 'image') {
        URL.revokeObjectURL(att.preview)
      }
      return prev.filter((_, i) => i !== index)
    })
  }, [])

  const handleSend = useCallback(() => {
    if (!conversationId) return
    if (sendMutation.isPending) return
    const trimmed = message.trim()
    if (!trimmed && attachments.length === 0) return

    if (trimmed) {
      sendMutation.mutate({
        conversationId,
        type: 'TEXT',
        content: trimmed,
      })
    }

    attachments.forEach((att) => {
      sendMutation.mutate({
        conversationId,
        type: att.type === 'image' ? 'IMAGE' : att.type === 'audio' ? 'AUDIO' : 'DOCUMENT',
        content: '',
      })
    })

    setMessage('')
    setAttachments([])
    setShowAttachMenu(false)

    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }, [conversationId, message, attachments, sendMutation])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        handleSend()
      }
    },
    [handleSend, sendMutation.isPending],
  )

  const adjustTextarea = useCallback(() => {
    const el = textareaRef.current
    if (el) {
      el.style.height = 'auto'
      el.style.height = `${Math.min(el.scrollHeight, 120)}px`
    }
  }, [])

  if (!conversationId) return null

  return (
    <div className="border-t border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-950">
      <AnimatePresence>
        {attachments.length > 0 && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="mb-3 flex flex-wrap gap-2"
          >
            {attachments.map((att, i) => (
              <div
                key={i}
                className="relative flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 p-2 pr-8 dark:border-gray-700 dark:bg-gray-800"
              >
                {att.type === 'image' ? (
                  <img src={att.preview} alt="" className="h-10 w-10 rounded object-cover" />
                ) : att.type === 'audio' ? (
                  <Mic size={18} className="text-gray-500" />
                ) : (
                  <FileText size={18} className="text-gray-500" />
                )}
                <span className="max-w-[120px] truncate text-sm text-gray-700 dark:text-gray-300">
                  {att.file.name}
                </span>
                <button
                  onClick={() => removeAttachment(i)}
                  className="absolute right-1 top-1 rounded p-0.5 text-gray-400 hover:text-gray-600"
                >
                  <X size={14} />
                </button>
              </div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={(e) => handleFileSelect(e, 'image')}
      />
      <input
        ref={documentInputRef}
        type="file"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.txt"
        className="hidden"
        onChange={(e) => handleFileSelect(e, 'document')}
      />
      <input
        ref={audioInputRef}
        type="file"
        accept="audio/*"
        className="hidden"
        onChange={(e) => handleFileSelect(e, 'audio')}
      />

      <div className="flex items-end gap-2">
        <div className="relative">
          <Button
            variant="ghost"
            size="sm"
            icon
            onClick={() => setShowAttachMenu(!showAttachMenu)}
            className="text-gray-400"
          >
            <Paperclip size={18} />
          </Button>

          <AnimatePresence>
            {showAttachMenu && (
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: -5 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: -5 }}
                className="absolute bottom-full left-0 mb-2 w-44 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-elevated dark:border-gray-700 dark:bg-gray-900"
              >
                <button
                  onClick={() => { fileInputRef.current?.click(); setShowAttachMenu(false) }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
                >
                  <Image size={16} />
                  Imagen
                </button>
                <button
                  onClick={() => { documentInputRef.current?.click(); setShowAttachMenu(false) }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
                >
                  <FileText size={16} />
                  Documento
                </button>
                <button
                  onClick={() => { audioInputRef.current?.click(); setShowAttachMenu(false) }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
                >
                  <Mic size={16} />
                  Audio
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <div className="relative flex-1">
          <textarea
            ref={textareaRef}
            value={message}
            onChange={(e) => {
              setMessage(e.target.value)
              adjustTextarea()
            }}
            onKeyDown={handleKeyDown}
            placeholder={t('chat.typeMessage')}
            rows={1}
            className="max-h-[120px] min-h-[40px] w-full resize-none rounded-xl border border-gray-200 bg-gray-50 px-4 py-2.5 pr-12 text-sm placeholder-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
          />
        </div>

        <Button
          variant="primary"
          size="md"
          icon
          onClick={handleSend}
          disabled={sendMutation.isPending || (!message.trim() && attachments.length === 0)}
          loading={sendMutation.isPending}
          className="shrink-0"
        >
          <Send size={18} />
        </Button>
      </div>
    </div>
  )
}
