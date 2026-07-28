import { useState, useRef, type KeyboardEvent } from 'react'
import { BookOpen, Plus, Search, Trash2, Globe, FileText, Type, X, Upload, Link, ExternalLink, Tag } from 'lucide-react'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { EmptyState } from '@/shared/molecules/EmptyState'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/core/api/client'
import { toast } from 'sonner'
import type { KnowledgeBaseDto, CreateKnowledgeBaseRequest } from '@/types/knowledge'
import type { ApiResponse } from '@/types/api'

const SOURCE_OPTIONS = [
  { value: 'manual', label: 'Texto manual' },
  { value: 'url', label: 'Sitio web' },
  { value: 'pdf', label: 'Documento PDF' },
  { value: 'video', label: 'Video' },
]

function TagInput({ value, onChange, suggestions }: { value: string; onChange: (v: string) => void; suggestions?: string[] }) {
  const tags = value ? value.split(',').map((t) => t.trim()).filter(Boolean) : []
  const tagSet = new Set(tags.map((t) => t.toLowerCase()))
  const [input, setInput] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)

  const addTag = (raw: string) => {
    const tag = raw.replace(/,/g, '').trim()
    if (!tag || tagSet.has(tag.toLowerCase())) return
    const next = [...tags, tag]
    tagSet.add(tag.toLowerCase())
    onChange(next.join(', '))
  }

  const removeTag = (idx: number) => {
    const next = tags.filter((_, i) => i !== idx)
    onChange(next.join(', '))
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      addTag(input)
      setInput('')
      return
    }
    if (e.key === 'Backspace' && !input && tags.length) {
      removeTag(tags.length - 1)
    }
  }

  const handlePaste = (e: React.ClipboardEvent) => {
    const text = e.clipboardData.getData('text')
    if (text.includes(',')) {
      e.preventDefault()
      const parts = text.split(',').map((t) => t.trim())
      parts.forEach((t) => addTag(t))
      setInput('')
    }
  }

  const filteredSuggestions = (suggestions ?? []).filter(
    (s) => s.toLowerCase().includes(input.toLowerCase()) && !tagSet.has(s.toLowerCase()),
  )

  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Tags</label>
      <div
        className="flex min-h-10 flex-wrap items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-1.5 focus-within:border-brand-500 focus-within:ring-1 focus-within:ring-brand-500 dark:border-gray-600 dark:bg-gray-900 dark:focus-within:border-brand-400"
        onClick={() => setShowSuggestions(true)}
      >
        {tags.map((tag, i) => (
          <span
            key={`${tag}-${i}`}
            className="inline-flex items-center gap-1 rounded-md bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-700 dark:bg-brand-900/40 dark:text-brand-400"
          >
            {tag}
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); removeTag(i) }}
              className="inline-flex hover:text-brand-900 dark:hover:text-brand-300"
            >
              <X size={12} />
            </button>
          </span>
        ))}
        <input
          className="min-w-[120px] flex-1 border-0 bg-transparent py-1 text-sm text-gray-900 outline-none placeholder:text-gray-400 dark:text-gray-100"
          placeholder={tags.length ? 'Agregar tag...' : 'Buscar o crear tag...'}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          onPaste={handlePaste}
          onFocus={() => setShowSuggestions(true)}
          onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        />
      </div>
      {showSuggestions && filteredSuggestions.length > 0 && (
        <div className="flex flex-wrap gap-1.5 rounded-lg border border-gray-200 bg-white p-2 dark:border-gray-700 dark:bg-gray-900">
          {filteredSuggestions.map((s) => (
            <button
              key={s}
              type="button"
              onMouseDown={(e) => { e.preventDefault(); addTag(s); setInput('') }}
              className="inline-flex items-center gap-1 rounded-md bg-gray-100 px-2 py-1 text-xs font-medium text-gray-600 transition hover:bg-brand-100 hover:text-brand-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-brand-900/30 dark:hover:text-brand-400"
            >
              <Plus size={10} />
              {s}
            </button>
          ))}
        </div>
      )}
      <p className="text-xs text-gray-500">Selecciona tags existentes o escribe y presiona Enter para crear nuevos</p>
    </div>
  )
}

function KnowledgeBaseForm({ allTags, onClose }: { allTags: string[]; onClose: () => void }) {
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [sourceType, setSourceType] = useState('manual')
  const [sourceUrl, setSourceUrl] = useState('')
  const [tags, setTags] = useState('')
  const [fileUrl, setFileUrl] = useState('')
  const [fileName, setFileName] = useState('')
  const [uploading, setUploading] = useState(false)

  const createMutation = useMutation({
    mutationFn: (dto: CreateKnowledgeBaseRequest) =>
      api.post<ApiResponse<KnowledgeBaseDto>>('/knowledge-base', dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge-base'] })
      toast.success('Entrada creada exitosamente')
      onClose()
    },
    onError: () => toast.error('Error al crear la entrada'),
  })

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.type !== 'application/pdf') {
      toast.error('Solo se permiten archivos PDF')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error('El PDF no puede superar los 10MB')
      return
    }

    setUploading(true)
    setFileName(file.name)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await api.post<ApiResponse<{ url: string }>>('/upload/document', formData, {
        headers: { 'Content-Type': null },
      })
      setFileUrl(res.data.data.url)
      if (!title) setTitle(file.name.replace(/\.pdf$/i, ''))
      toast.success('PDF subido correctamente')
    } catch {
      toast.error('Error al subir el PDF')
      setFileName('')
    } finally {
      setUploading(false)
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !content.trim()) return
    createMutation.mutate({
      title: title.trim(),
      content: content.trim(),
      sourceType,
      sourceUrl: sourceUrl.trim() || undefined,
      fileUrl: fileUrl || undefined,
      tags: tags || undefined,
    })
  }

  const canSubmit = title.trim() && content.trim()

  return (
    <Modal open onClose={onClose} title="Nueva entrada" size="xl">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input label="Título" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Ej: Política de devoluciones" required />

        <div className="space-y-1.5">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Contenido</label>
          {sourceType === 'pdf' ? (
            <div className="space-y-3">
              <div
                className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 py-8 transition hover:border-brand-400 hover:bg-brand-50/50 dark:border-gray-600 dark:bg-gray-900 dark:hover:border-brand-500"
                onClick={() => fileInputRef.current?.click()}
              >
                {uploading ? (
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
                    Subiendo PDF...
                  </div>
                ) : fileUrl ? (
                  <div className="flex items-center gap-2 text-sm text-green-600">
                    <FileText size={20} />
                    <span className="font-medium">{fileName}</span>
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); setFileUrl(''); setFileName('') }}
                      className="ml-2 text-gray-400 hover:text-red-500"
                    >
                      <X size={16} />
                    </button>
                  </div>
                ) : (
                  <>
                    <Upload size={24} className="text-gray-400" />
                    <p className="text-sm font-medium text-gray-600 dark:text-gray-400">
                      Haz clic para seleccionar un PDF
                    </p>
                    <p className="text-xs text-gray-400">PDF hasta 10MB</p>
                  </>
                )}
              </div>
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,application/pdf"
                className="hidden"
                onChange={handleFileSelect}
              />
              <textarea
                className="h-28 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="Describe brevemente qué contiene este PDF..."
              />
            </div>
          ) : (
            <textarea
              className="h-36 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder={
                sourceType === 'manual'
                  ? 'Escribe la información que el bot debe conocer...'
                  : 'Escribe un resumen o la información relevante de la fuente...'
              }
              required
            />
          )}
        </div>

        <Select label="Tipo de fuente" options={SOURCE_OPTIONS} value={sourceType} onChange={(e) => setSourceType(e.target.value)} />

        {(sourceType === 'url' || sourceType === 'video') && (
          <Input
            label={sourceType === 'url' ? 'URL del sitio web' : 'URL del video'}
            value={sourceUrl}
            onChange={(e) => setSourceUrl(e.target.value)}
            placeholder="https://..."
            leftIcon={<Link size={14} />}
          />
        )}

        {fileUrl && sourceType === 'pdf' && (
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <FileText size={14} />
            <span className="truncate">{fileName}</span>
            <a href={fileUrl} target="_blank" rel="noopener noreferrer" className="ml-auto text-brand-600 hover:underline dark:text-brand-400">
              <ExternalLink size={14} className="inline" /> Ver PDF
            </a>
          </div>
        )}

        <TagInput value={tags} onChange={setTags} suggestions={allTags} />

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancelar</Button>
          <Button type="submit" loading={createMutation.isPending} disabled={!canSubmit}>
            <Plus size={16} className="mr-1" />Crear
          </Button>
        </div>
      </form>
    </Modal>
  )
}

const sourceConfig: Record<string, { icon: React.ReactNode; label: string }> = {
  manual: { icon: <Type size={14} />, label: 'Texto' },
  url: { icon: <Globe size={14} />, label: 'Web' },
  pdf: { icon: <FileText size={14} />, label: 'PDF' },
  video: { icon: <FileText size={14} />, label: 'Video' },
}

export default function KnowledgeBasePage() {
  const queryClient = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [search, setSearch] = useState('')

  const { data: entries, isLoading } = useQuery({
    queryKey: ['knowledge-base', search],
    queryFn: async () => {
      const url = search ? `/knowledge-base/search?q=${encodeURIComponent(search)}` : '/knowledge-base'
      const res = await api.get<ApiResponse<KnowledgeBaseDto[]>>(url)
      return res.data.data
    },
  })

  const { data: allEntries } = useQuery({
    queryKey: ['knowledge-base', ''],
    queryFn: async () => {
      const res = await api.get<ApiResponse<KnowledgeBaseDto[]>>('/knowledge-base')
      return res.data.data
    },
    staleTime: 30000,
  })

  const allTags = Array.from(
    new Set(
      (allEntries ?? [])
        .flatMap((e) => (e.tags ? e.tags.split(',').map((t) => t.trim()).filter(Boolean) : []))
        .map((t) => t.toLowerCase()),
    ),
  ).sort()

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/knowledge-base/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['knowledge-base'] })
      toast.success('Entrada eliminada')
    },
  })

  const sourceIcon = (type: string) => sourceConfig[type]?.icon ?? <Type size={14} />
  const sourceLabel = (type: string) => sourceConfig[type]?.label ?? type

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Base de Conocimiento</h3>
            <p className="mt-1 text-sm text-gray-500">Información que el bot usará para responder a los clientes</p>
          </div>
          <Button onClick={() => setShowForm(true)}><Plus size={16} className="mr-1" />Agregar</Button>
        </div>

        <div className="relative mt-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            className="h-10 w-full rounded-lg border border-gray-300 bg-white pl-10 pr-4 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
            placeholder="Buscar en la base de conocimiento..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {isLoading ? (
          <div className="mt-8 flex justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
          </div>
        ) : !entries || entries.length === 0 ? (
          <EmptyState
            icon={<BookOpen size={48} />}
            title={search ? 'Sin resultados' : 'No hay entradas en la base de conocimiento'}
            description={
              search
                ? 'No se encontraron entradas con ese texto. Intenta con otros términos.'
                : 'Agrega las políticas, productos e información de tu empresa para que el bot responda con datos reales.'
            }
            action={search ? undefined : { label: 'Agregar primera entrada', onClick: () => setShowForm(true) }}
            className="mt-8"
          />
        ) : (
          <div className="mt-4 space-y-2">
            {entries.map((entry) => (
              <div key={entry.id} className="flex items-start gap-4 rounded-lg border border-gray-100 bg-gray-50 px-4 py-3 transition hover:border-gray-200 dark:border-gray-800 dark:bg-gray-900 dark:hover:border-gray-700">
                <div className="mt-0.5 shrink-0 text-gray-400">{sourceIcon(entry.sourceType)}</div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{entry.title}</p>
                    <span className="rounded bg-gray-200 px-1.5 py-0.5 text-[10px] font-medium text-gray-600 dark:bg-gray-700 dark:text-gray-400">
                      {sourceLabel(entry.sourceType)}
                    </span>
                  </div>
                  <p className="mt-0.5 line-clamp-2 text-xs text-gray-500">{entry.content}</p>
                  {entry.sourceType === 'pdf' && entry.fileUrl && (
                    <a
                      href={entry.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mt-1 inline-flex items-center gap-1 text-xs text-brand-600 hover:underline dark:text-brand-400"
                    >
                      <FileText size={12} /> Ver PDF <ExternalLink size={10} />
                    </a>
                  )}
                  {entry.tags && (
                    <div className="mt-1 flex flex-wrap gap-1">
                      {entry.tags.split(',').map((tag) => (
                        <span key={tag.trim()} className="inline-flex items-center gap-1 rounded bg-brand-100 px-1.5 py-0.5 text-[10px] font-medium text-brand-700 dark:bg-brand-900/30 dark:text-brand-400">
                          <Tag size={10} />
                          {tag.trim()}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
                <button
                  onClick={() => {
                    if (confirm('¿Eliminar esta entrada?')) deleteMutation.mutate(entry.id)
                  }}
                  className="shrink-0 self-center rounded-lg p-1.5 text-gray-400 transition hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-900/20"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {showForm && <KnowledgeBaseForm allTags={allTags} onClose={() => setShowForm(false)} />}
    </div>
  )
}
