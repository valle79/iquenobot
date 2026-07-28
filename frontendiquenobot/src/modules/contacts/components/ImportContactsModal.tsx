import { useState, useRef, useCallback } from 'react'
import { Upload, FileText, CheckCircle2, AlertTriangle, ArrowLeft, ChevronDown, ChevronRight, Eye, EyeOff } from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Button } from '@/shared/atoms/Button/Button'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { useImportContacts } from '../hooks/useContacts'
import { toast } from 'sonner'

type Step = 'upload' | 'preview' | 'result'

interface ParsedContact {
  firstName: string
  lastName: string
  phone: string
  email: string
  company: string
  notes: string
  [key: string]: string
}

interface ImportContactsModalProps {
  open: boolean
  onClose: () => void
}

function detectColumns(headers: string[]): string[] {
  const fieldMap: Record<string, string> = {
    nombre: 'firstName', name: 'firstName', 'first name': 'firstName',
    apellido: 'lastName', 'last name': 'lastName', surname: 'lastName',
    telefono: 'phone', teléfono: 'phone', phone: 'phone',
    celular: 'phone', movil: 'phone', móvil: 'phone', mobile: 'phone',
    email: 'email', 'e-mail': 'email', correo: 'email', mail: 'email',
    empresa: 'company', company: 'company',
    organización: 'company', organization: 'company',
    'organization name': 'company',
    notas: 'notes', note: 'notes', notes: 'notes', observaciones: 'notes',
    'e-mail 1 - value': 'email',
    'e-mail 2 - value': 'email',
    'e-mail 3 - value': 'email',
    'phone 1 - value': 'phone',
    'phone 2 - value': 'phone',
    'phone 3 - value': 'phone',
  }

  const ignoredExact = new Set([
    'middle name', 'name prefix', 'name suffix',
    'phonetic first name', 'phonetic middle name', 'phonetic last name',
    'nickname', 'file as', 'photo', 'birthday',
    'organization title', 'organization department',
    'labels',
  ])

  return headers.map((h) => {
    const lowered = h.toLowerCase().trim()
    if (ignoredExact.has(lowered)) return ''
    if (lowered.includes(' label') || lowered.startsWith('address') || lowered.includes(' - label')) return ''
    if (lowered.includes('address ')) return ''
    return fieldMap[lowered] || ''
  })
}

export function ImportContactsModal({ open, onClose }: ImportContactsModalProps) {
  const [step, setStep] = useState<Step>('upload')
  const [csvText, setCsvText] = useState('')
  const [contacts, setContacts] = useState<ParsedContact[]>([])
  const [headers, setHeaders] = useState<string[]>([])
  const [columnMapping, setColumnMapping] = useState<string[]>([])
  const [dragging, setDragging] = useState(false)
  const [result, setResult] = useState<{ total: number; created: number; skipped: number; errors: number; messages: string[] } | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const importMutation = useImportContacts()

  const parseCSV = useCallback((text: string) => {
    const lines = text.split(/\r?\n/).filter((l) => l.trim())
    if (lines.length < 2) {
      toast.error('El CSV debe tener al menos un encabezado y una fila de datos')
      return
    }

    const parsedHeaders = parseCSVLine(lines[0])
    const detected = detectColumns(parsedHeaders)
    setHeaders(parsedHeaders)
    setColumnMapping(detected)

    const data: ParsedContact[] = []
    for (let i = 1; i < lines.length; i++) {
      const values = parseCSVLine(lines[i])
      if (values.length === 0 || values.every((v) => !v.trim())) continue
      const contact: ParsedContact = { firstName: '', lastName: '', phone: '', email: '', company: '', notes: '' }
      for (let j = 0; j < parsedHeaders.length; j++) {
        const mappedField = detected[j] || ''
        if (mappedField && values[j] !== undefined) {
          contact[mappedField] = values[j].trim()
          contact[parsedHeaders[j]] = values[j].trim()
        }
      }
      data.push(contact)
    }

    setContacts(data)
    setStep('preview')
  }, [])

  const handleFile = useCallback((file: File) => {
    if (!file.name.endsWith('.csv')) {
      toast.error('Solo se aceptan archivos CSV')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      const text = e.target?.result as string
      setCsvText(text)
      parseCSV(text)
    }
    reader.readAsText(file)
  }, [parseCSV])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }, [handleFile])

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(true)
  }

  const handleDragLeave = () => setDragging(false)

  const handleImport = async () => {
    const mappedContacts = contacts.map((c) => {
      const result: Record<string, string> = {}
      columnMapping.forEach((field, idx) => {
        if (field) {
          const header = headers[idx]
          result[field] = c[header] || ''
        }
      })
      return result
    })

    console.log('[ImportModal] mappedContacts count:', mappedContacts.length)
    console.log('[ImportModal] sample:', JSON.stringify(mappedContacts[0]))

    try {
      console.log('[ImportModal] calling mutateAsync...')
      const importResult = await importMutation.mutateAsync(mappedContacts)
      console.log('[ImportModal] result:', importResult)
      setResult(importResult)
      setStep('result')
    } catch (err: any) {
      console.error('[ImportModal] full error:', err)
      console.error('[ImportModal] response:', err?.response?.data)
      console.error('[ImportModal] stack:', err?.stack)
      const msg = err?.response?.data?.message || err?.message || 'Error al importar contactos'
      toast.error(msg)
    }
  }

  const updateMapping = (headerIdx: number, value: string) => {
    const newMapping = [...columnMapping]
    newMapping[headerIdx] = value
    setColumnMapping(newMapping)
  }

  const reset = () => {
    setStep('upload')
    setCsvText('')
    setContacts([])
    setHeaders([])
    setColumnMapping([])
    setResult(null)
    setDragging(false)
    onClose()
  }

  const mappedFieldOptions = [
    { value: '', label: 'Ignorar' },
    { value: 'firstName', label: 'Nombre' },
    { value: 'lastName', label: 'Apellido' },
    { value: 'phone', label: 'Teléfono' },
    { value: 'email', label: 'Email' },
    { value: 'company', label: 'Empresa' },
    { value: 'notes', label: 'Notas' },
  ]

  return (
    <Modal open={open} onClose={reset} title="Importar contactos desde CSV" size="full">
      {step === 'upload' && (
        <div className="space-y-6">
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed p-12 transition-colors ${
              dragging
                ? 'border-brand-500 bg-brand-50 dark:border-brand-400 dark:bg-brand-950/20'
                : 'border-gray-300 bg-gray-50 hover:border-brand-400 hover:bg-brand-50/50 dark:border-gray-600 dark:bg-gray-900 dark:hover:border-brand-500 dark:hover:bg-brand-950/10'
            }`}
            onClick={() => fileInputRef.current?.click()}
          >
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-brand-100 dark:bg-brand-900/30">
              <Upload size={28} className="text-brand-600 dark:text-brand-400" />
            </div>
            <p className="text-base font-medium text-gray-700 dark:text-gray-300">
              Arrastra tu archivo CSV aquí
            </p>
            <p className="mt-1 text-sm text-gray-500">o haz clic para seleccionar un archivo</p>
            <p className="mt-4 text-xs text-gray-400">Solo archivos .csv con extensión</p>
            <Button type="button" variant="outline" size="sm" className="mt-4" onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click() }}>
              <Upload size={14} className="mr-1.5" />
              Seleccionar archivo
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              className="hidden"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f) }}
            />
          </div>
          <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900">
            <div className="flex items-start gap-3">
              <FileText size={18} className="mt-0.5 shrink-0 text-gray-400" />
              <div>
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Formato esperado</p>
                <p className="mt-1 text-xs text-gray-500">
                  El CSV debe tener una fila de encabezados. Las columnas se detectan automáticamente. Compatible con exportación de Google Contacts:
                </p>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {['First Name', 'Last Name', 'Phone 1 - Value', 'E-mail 1 - Value', 'Organization Name', 'Notes'].map((col) => (
                    <Badge key={col} variant="neutral" size="sm">{col}</Badge>
                  ))}
                </div>
                <p className="mt-2 text-xs text-gray-500">
                  Las columnas sueltas (Label, Address, Birthday, etc.) se ignoran automáticamente.
                </p>
                <p className="mt-2 text-xs text-gray-500">
                  Puedes ajustar la correspondencia de columnas en el siguiente paso.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {step === 'preview' && (
        <div className="flex max-h-[70vh] flex-col space-y-4">
          <div className="flex shrink-0 items-center justify-between rounded-lg border border-gray-200 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-900">
            <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
              <FileText size={16} />
              <span>{contacts.length} contactos detectados</span>
            </div>
            <Button type="button" variant="ghost" size="sm" onClick={() => setStep('upload')}>
              <ArrowLeft size={14} className="mr-1" />
              Cambiar archivo
            </Button>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto space-y-4 pr-1">
            <div>
              <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                Correspondencia de columnas
              </p>
              <div className="space-y-1.5">
                {(() => {
                  const mappedCols = headers.filter((_, idx) => columnMapping[idx])
                  const ignoredCols = headers.filter((_, idx) => !columnMapping[idx])
                  return (
                    <>
                      {mappedCols.map((header, displayIdx) => {
                        const idx = headers.indexOf(header)
                        return (
                          <div key={idx} className="flex items-center gap-2 rounded-lg border border-brand-200 bg-brand-50/30 px-3 py-2 dark:border-brand-800 dark:bg-brand-950/20">
                            <span className="min-w-0 flex-1 truncate text-sm font-medium text-gray-700 dark:text-gray-300" title={header}>{header}</span>
                            <span className="shrink-0 text-xs text-brand-500">&rarr;</span>
                            <select
                              value={columnMapping[idx] || ''}
                              onChange={(e) => updateMapping(idx, e.target.value)}
                              className="w-36 shrink-0 rounded-md border border-gray-200 bg-white px-2 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
                            >
                              {mappedFieldOptions.map((opt) => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                              ))}
                            </select>
                          </div>
                        )
                      })}
                      {ignoredCols.length > 0 && (
                        <details className="group">
                          <summary className="flex cursor-pointer items-center gap-1.5 rounded-lg px-3 py-2 text-xs text-gray-400 hover:text-gray-600 hover:bg-gray-50 dark:hover:text-gray-300 dark:hover:bg-gray-900">
                            <ChevronRight size={14} className="transition-transform group-open:rotate-90" />
                            {ignoredCols.length} columnas ignoradas
                          </summary>
                          <div className="mt-1 space-y-1 pl-5">
                            {ignoredCols.map((header) => {
                              const idx = headers.indexOf(header)
                              return (
                                <div key={idx} className="flex items-center gap-2 rounded-lg border border-gray-100 px-3 py-1.5 dark:border-gray-800">
                                  <span className="min-w-0 flex-1 truncate text-sm text-gray-400" title={header}>{header}</span>
                                  <span className="shrink-0 text-xs text-gray-300">&rarr;</span>
                                  <select
                                    value={columnMapping[idx] || ''}
                                    onChange={(e) => updateMapping(idx, e.target.value)}
                                    className="w-36 shrink-0 rounded-md border border-gray-100 bg-gray-50 px-2 py-1 text-xs text-gray-400 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-500"
                                  >
                                    {mappedFieldOptions.map((opt) => (
                                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                  </select>
                                </div>
                              )
                            })}
                          </div>
                        </details>
                      )}
                    </>
                  )
                })()}
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                Vista previa ({Math.min(contacts.length, 5)} de {contacts.length} filas)
              </p>
              <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-50 dark:bg-gray-900">
                    <tr>
                      {headers.map((h, idx) => (
                        columnMapping[idx] && (
                          <th key={idx} className="whitespace-nowrap px-4 py-2 text-left text-xs font-medium uppercase text-gray-500">
                            {mappedFieldOptions.find((o) => o.value === columnMapping[idx])?.label || h}
                          </th>
                        )
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                    {contacts.slice(0, 5).map((contact, rowIdx) => (
                      <tr key={rowIdx}>
                        {headers.map((h, colIdx) => (
                          columnMapping[colIdx] && (
                            <td key={colIdx} className="whitespace-nowrap px-4 py-2 text-sm text-gray-700 dark:text-gray-300">
                              {contact[h] || '—'}
                            </td>
                          )
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div className="flex shrink-0 justify-end gap-3 border-t border-gray-200 pt-4 dark:border-gray-700">
            <Button type="button" variant="outline" onClick={reset}>Cancelar</Button>
            <Button type="button" onClick={handleImport} loading={importMutation.isPending}>
              <Upload size={16} className="mr-1.5" />
              Importar {contacts.length} contactos
            </Button>
          </div>
        </div>
      )}

      {step === 'result' && result && (
        <div className="space-y-6">
          <div className="flex items-center justify-center py-6">
            {result.errors > 0 ? (
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30">
                <AlertTriangle size={36} className="text-amber-600 dark:text-amber-400" />
              </div>
            ) : result.skipped > 0 ? (
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-yellow-100 dark:bg-yellow-900/30">
                <CheckCircle2 size={36} className="text-yellow-600 dark:text-yellow-400" />
              </div>
            ) : (
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
                <CheckCircle2 size={36} className="text-green-600 dark:text-green-400" />
              </div>
            )}
          </div>

          <div className="text-center">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Importación completada
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              {result.created} de {result.total} contactos importados correctamente
            </p>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="rounded-lg border border-green-200 bg-green-50 p-4 text-center dark:border-green-900 dark:bg-green-950/30">
              <p className="text-2xl font-bold text-green-600 dark:text-green-400">{result.created}</p>
              <p className="text-xs text-green-600/70 dark:text-green-400/70">Importados</p>
            </div>
            <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-center dark:border-yellow-900 dark:bg-yellow-950/30">
              <p className="text-2xl font-bold text-yellow-600 dark:text-yellow-400">{result.skipped}</p>
              <p className="text-xs text-yellow-600/70 dark:text-yellow-400/70">Omitidos</p>
            </div>
            <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-center dark:border-red-900 dark:bg-red-950/30">
              <p className="text-2xl font-bold text-red-600 dark:text-red-400">{result.errors}</p>
              <p className="text-xs text-red-600/70 dark:text-red-400/70">Errores</p>
            </div>
          </div>

          {result.messages.length > 0 && (
            <div className="max-h-40 overflow-y-auto rounded-lg border border-gray-200 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-900">
              <p className="mb-2 text-xs font-medium text-gray-500 uppercase">Detalle</p>
              <div className="space-y-1">
                {result.messages.map((msg, idx) => (
                  <p key={idx} className="text-xs text-gray-600 dark:text-gray-400">{msg}</p>
                ))}
              </div>
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-gray-200 pt-4 dark:border-gray-700">
            <Button type="button" variant="outline" onClick={reset}>
              Cerrar
            </Button>
            <Button type="button" onClick={() => { reset(); }}>
              Importar otro archivo
            </Button>
          </div>
        </div>
      )}
    </Modal>
  )
}

function parseCSVLine(line: string): string[] {
  const result: string[] = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"'
        i++
      } else {
        inQuotes = !inQuotes
      }
    } else if (char === ',' && !inQuotes) {
      result.push(current)
      current = ''
    } else {
      current += char
    }
  }
  result.push(current)

  return result.map((v) => v.trim())
}
