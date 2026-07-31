import { useState, useRef, useCallback, useEffect, type ChangeEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Send,
  Paperclip,
  Image,
  FileText,
  X,
  Mic,
  Square,
  Smile,
} from 'lucide-react'
import { toast } from 'sonner'
import { useSendMessage } from '@/modules/chat/hooks/useConversations'
import { uploadAttachment } from '@/services/upload.service'
import { Button } from '@/shared/atoms/Button/Button'
import { cn } from '@/shared/utils'
import { AnimatePresence, motion } from 'framer-motion'

interface Attachment {
  file: File
  preview: string
  type: 'image' | 'document' | 'audio'
}

function formatRecordTime(seconds: number): string {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const EMOJIS = [
  '😀', '😄', '😁', '😂', '🤣', '😊', '😍', '🥰',
  '😉', '😎', '🤗', '🤔', '😅', '🙃', '😢', '😭',
  '😡', '😴', '👍', '👎', '👏', '🙏', '🤝', '💪',
  '❤️', '💖', '🔥', '✨', '🎉', '🎂', '🌹', '🌞',
  '✅', '❌', '❗', '❓', '💯', '🚀', '🆗', '🙌',
]

export function MessageInput() {
  const { t } = useTranslation()
  const { id: conversationId } = useParams<{ id: string }>()
  const [message, setMessage] = useState('')
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [showAttachMenu, setShowAttachMenu] = useState(false)
  const [showEmojiPicker, setShowEmojiPicker] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [isRecording, setIsRecording] = useState(false)
  const [recordTime, setRecordTime] = useState(0)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const documentInputRef = useRef<HTMLInputElement>(null)
  const audioInputRef = useRef<HTMLInputElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const streamRef = useRef<MediaStream | null>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const sendMutation = useSendMessage()

  const stopRecordingCleanup = useCallback(() => {
    setIsRecording(false)
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }
  }, [])

  useEffect(() => {
    return () => {
      if (recorderRef.current && recorderRef.current.state !== 'inactive') {
        recorderRef.current.stop()
      }
      stopRecordingCleanup()
    }
  }, [stopRecordingCleanup])

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

  const startRecording = useCallback(async () => {
    if (isRecording) return
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      const recorder = new MediaRecorder(stream)
      chunksRef.current = []

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data)
      }
      recorder.onstop = () => {
        const mimeType = recorder.mimeType || 'audio/webm'
        const blob = new Blob(chunksRef.current, { type: mimeType })
        const file = new File([blob], `audio-${Date.now()}.${mimeType.includes('mp4') ? 'm4a' : 'webm'}`, {
          type: mimeType,
        })
        setAttachments((prev) => [...prev, { file, preview: file.name, type: 'audio' }])
      }

      recorder.start()
      recorderRef.current = recorder
      setRecordTime(0)
      setIsRecording(true)
      timerRef.current = setInterval(() => setRecordTime((sec) => sec + 1), 1000)
      setShowAttachMenu(false)
    } catch {
      toast.error('No se pudo acceder al micrófono')
    }
  }, [isRecording])

  const stopRecording = useCallback(() => {
    if (recorderRef.current && recorderRef.current.state !== 'inactive') {
      recorderRef.current.stop()
    }
    recorderRef.current = null
    stopRecordingCleanup()
  }, [stopRecordingCleanup])

  const handleSend = useCallback(async () => {
    if (!conversationId) return
    if (sendMutation.isPending || isUploading || isRecording) return
    const trimmed = message.trim()
    if (!trimmed && attachments.length === 0) return

    if (trimmed) {
      sendMutation.mutate({
        conversationId,
        type: 'TEXT',
        content: trimmed,
      })
    }

    if (attachments.length > 0) {
      setIsUploading(true)
      try {
        for (const att of attachments) {
          const url = await uploadAttachment(att.file)
          const type = att.type === 'image' ? 'IMAGE' : att.type === 'audio' ? 'AUDIO' : 'DOCUMENT'
          sendMutation.mutate({
            conversationId,
            type,
            content: '',
            attachmentUrls: [url],
          })
        }
      } catch {
        toast.error('Error al subir el archivo')
      } finally {
        setIsUploading(false)
      }
    }

    setMessage('')
    setAttachments([])
    setShowAttachMenu(false)

    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
    }
  }, [conversationId, message, attachments, sendMutation, isUploading, isRecording])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        void handleSend()
      }
    },
    [handleSend],
  )

  const adjustTextarea = useCallback(() => {
    const el = textareaRef.current
    if (el) {
      el.style.height = 'auto'
      el.style.height = `${Math.min(el.scrollHeight, 120)}px`
    }
  }, [])

  const insertEmoji = useCallback((emoji: string) => {
    setMessage((prev) => prev + emoji)
    adjustTextarea()
    if (textareaRef.current) {
      textareaRef.current.focus()
    }
  }, [adjustTextarea])

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
            onClick={() => {
              setShowEmojiPicker((prev) => !prev)
              setShowAttachMenu(false)
            }}
            className="text-gray-400"
          >
            <Smile size={18} />
          </Button>

          <AnimatePresence>
            {showEmojiPicker && (
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: -5 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: -5 }}
                className="absolute bottom-full left-0 mb-2 w-64 overflow-hidden rounded-lg border border-gray-200 bg-white p-2 shadow-elevated dark:border-gray-700 dark:bg-gray-900"
              >
                <div className="grid grid-cols-8 gap-0.5">
                  {EMOJIS.map((emoji) => (
                    <button
                      key={emoji}
                      onClick={() => insertEmoji(emoji)}
                      className="rounded p-1 text-lg transition-colors hover:bg-gray-100 dark:hover:bg-gray-800"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <div className="relative">
          <Button
            variant="ghost"
            size="sm"
            icon
            onClick={() => {
              setShowAttachMenu(!showAttachMenu)
              setShowEmojiPicker(false)
            }}
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
                className="absolute bottom-full left-0 mb-2 w-48 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-elevated dark:border-gray-700 dark:bg-gray-900"
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
                <button
                  onClick={() => { void startRecording() }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800"
                >
                  <Square size={16} className="text-red-500" />
                  Grabar audio
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <div className="relative flex-1">
          {isRecording ? (
            <div className="flex h-10 w-full items-center justify-between rounded-xl border border-red-200 bg-red-50 px-4 dark:border-red-900 dark:bg-red-950">
              <div className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 animate-pulse rounded-full bg-red-500" />
                <span className="text-sm font-medium text-red-600 dark:text-red-400">
                  Grabando... {formatRecordTime(recordTime)}
                </span>
              </div>
              <Button
                variant="ghost"
                size="sm"
                icon
                onClick={stopRecording}
                className="text-red-500 hover:text-red-600"
              >
                <Square size={16} />
              </Button>
            </div>
          ) : (
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
          )}
        </div>

        <Button
          variant="primary"
          size="md"
          icon
          onClick={() => void handleSend()}
          disabled={sendMutation.isPending || isUploading || isRecording || (!message.trim() && attachments.length === 0)}
          loading={sendMutation.isPending || isUploading}
          className={cn('shrink-0', isRecording && 'hidden')}
        >
          <Send size={18} />
        </Button>
      </div>
    </div>
  )
}