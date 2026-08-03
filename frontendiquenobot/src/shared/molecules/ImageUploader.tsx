import { useCallback, useRef, useState } from 'react'
import { ImagePlus, Loader2, RefreshCw, Trash2, UploadCloud } from 'lucide-react'
import { uploadImage } from '@/services/upload.service'
import { cn } from '@/shared/utils'

const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_SIZE = 5 * 1024 * 1024 // 5MB

interface ImageUploaderProps {
  value: string | null
  onChange: (url: string | null) => void
  label?: string
  className?: string
}

export function ImageUploader({ value, onChange, label = 'Imagen', className }: ImageUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dragActive, setDragActive] = useState(false)

  const handleFile = useCallback(
    async (file: File | undefined) => {
      if (!file) return

      if (!ACCEPTED_TYPES.includes(file.type)) {
        setError('Formato no permitido. Usa JPG, PNG, WebP o GIF.')
        return
      }
      if (file.size > MAX_SIZE) {
        setError('La imagen supera el tamaño máximo de 5 MB.')
        return
      }

      setError(null)
      setUploading(true)
      try {
        const url = await uploadImage(file)
        onChange(url)
      } catch {
        setError('No se pudo subir la imagen. Inténtalo de nuevo.')
      } finally {
        setUploading(false)
      }
    },
    [onChange],
  )

  const handleDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault()
      setDragActive(false)
      handleFile(event.dataTransfer.files?.[0])
    },
    [handleFile],
  )

  const handleRemove = useCallback(() => {
    onChange(null)
    if (inputRef.current) inputRef.current.value = ''
  }, [onChange])

  return (
    <div className={cn('space-y-2', className)}>
      {label && <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</span>}

      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        className="hidden"
        onChange={(event) => handleFile(event.target.files?.[0])}
      />

      {value ? (
        <div className="group relative inline-block">
          <img
            src={value}
            alt={label}
            className="h-40 w-40 rounded-xl border border-gray-200 object-cover dark:border-gray-700"
          />
          <div className="absolute inset-0 flex items-center justify-center gap-2 rounded-xl bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={uploading}
              title="Cambiar imagen"
              className="flex h-9 w-9 items-center justify-center rounded-full bg-white/90 text-gray-800 transition-colors hover:bg-white"
            >
              {uploading ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
            </button>
            <button
              type="button"
              onClick={handleRemove}
              title="Quitar imagen"
              className="flex h-9 w-9 items-center justify-center rounded-full bg-red-600/90 text-white transition-colors hover:bg-red-600"
            >
              <Trash2 size={16} />
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          onDragOver={(event) => { event.preventDefault(); setDragActive(true) }}
          onDragLeave={() => setDragActive(false)}
          onDrop={handleDrop}
          disabled={uploading}
          className={cn(
            'flex h-40 w-40 flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-4 text-center transition-colors',
            dragActive
              ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20'
              : 'border-gray-300 hover:border-brand-400 hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-800/50',
            uploading && 'cursor-wait opacity-60',
          )}
        >
          {uploading ? (
            <Loader2 size={28} className="animate-spin text-brand-500" />
          ) : (
            <ImagePlus size={28} className="text-gray-400" />
          )}
          <span className="text-xs leading-relaxed text-gray-500 dark:text-gray-400">
            {uploading ? 'Subiendo imagen…' : (
              <>
                Arrastra una imagen
                <br />
                o haz clic para subir
              </>
            )}
          </span>
          <span className="text-[10px] text-gray-400 dark:text-gray-500">
            JPG, PNG, WebP o GIF · máx. 5 MB
          </span>
        </button>
      )}

      {error && <p className="flex items-center gap-1 text-xs text-red-600"><UploadCloud size={12} /> {error}</p>}
    </div>
  )
}
