import { useState, useRef, useEffect } from 'react'
import {
  Upload, Download, FileText, CheckCircle, XCircle, AlertTriangle, ArrowRight,
  ChevronDown, Table, Loader2,
} from 'lucide-react'
import { Modal } from '@/shared/atoms/Modal/Modal'
import { Button } from '@/shared/atoms/Button/Button'
import { api } from '@/core/api/client'
import { toast } from 'sonner'
import type { ApiResponse } from '@/types/api'

interface FieldOption {
  value: string
  label: string
  required: boolean
}

interface PreviewData {
  fileName: string
  totalRows: number
  detectedColumns: string[]
  sampleRows: Record<string, string>[]
  productFields: FieldOption[]
}

interface RowError {
  row: number
  productName: string
  reason: string
}

interface ImportResult {
  totalRows: number
  created: number
  skipped: number
  errors: RowError[]
}

interface ColumnMapping {
  fileColumn: string
  productField: string
}

const CSV_TEMPLATE = `name,sku,price,stock_quantity,description,short_description,category_name,status,tags,weight,width,height,length,image_url
"Desemplastificador Agrícola",DES-001,0,10,Descripción del producto,Resumen del producto,Categoría,ACTIVE,"tag1, tag2",300,150,120,280,https://ejemplo.com/img.jpg`

type Step = 'upload' | 'mapping' | 'result'

export function ImportProductsModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [step, setStep] = useState<Step>('upload')
  const [preview, setPreview] = useState<PreviewData | null>(null)
  const [mapping, setMapping] = useState<ColumnMapping[]>([])
  const [result, setResult] = useState<ImportResult | null>(null)

  useEffect(() => {
    if (!open) {
      setFile(null)
      setPreview(null)
      setMapping([])
      setResult(null)
      setStep('upload')
      setLoading(false)
    }
  }, [open])

  const handleDownloadTemplate = () => {
    const blob = new Blob(['\uFEFF' + CSV_TEMPLATE], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'plantilla_productos.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0]
    if (!f) return

    const ext = f.name.split('.').pop()?.toLowerCase()
    if (!ext || !['csv', 'xlsx', 'xls'].includes(ext)) {
      toast.error('Formato no soportado. Usa CSV o Excel (.xlsx, .xls)')
      return
    }

    setFile(f)
    setResult(null)
    await loadPreview(f)
  }

  const loadPreview = async (f: File) => {
    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('file', f)
      const res = await api.post<ApiResponse<PreviewData>>('/products/import/preview', formData, {
        headers: { 'Content-Type': null },
      })
      const data = res.data.data
      setPreview(data)

      // Auto-detect mapping: try to match detected columns to product fields
      const fields = data.productFields.map((pf) => pf.value)
      const auto: ColumnMapping[] = data.detectedColumns.map((col) => {
        const normalized = col.toLowerCase().replace(/[^a-z0-9]/g, '')
        const match = fields.find((f) => f === normalized || f.replace('_', '') === normalized)
        return { fileColumn: col, productField: match || '' }
      })
      setMapping(auto)
      setStep('mapping')
    } catch {
      toast.error('Error al leer el archivo. Verifica que tenga una fila de cabecera.')
    } finally {
      setLoading(false)
    }
  }

  const updateMapping = (fileColumn: string, productField: string) => {
    setMapping((prev) => prev.map((m) => m.fileColumn === fileColumn ? { ...m, productField } : m))
  }

  const getFieldLabel = (value: string) => {
    return preview?.productFields.find((f) => f.value === value)?.label || value
  }

  const handleExecute = async () => {
    if (!file || !preview) return

    // Validate required fields are mapped
    const mappedFields = mapping.map((m) => m.productField).filter(Boolean)
    const missingRequired = preview.productFields
      .filter((f) => f.required)
      .filter((f) => !mappedFields.includes(f.value))

    if (missingRequired.length > 0) {
      const names = missingRequired.map((f) => f.label).join(', ')
      toast.error(`Campos obligatorios sin mapear: ${names}`)
      return
    }

    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('mapping', JSON.stringify({ columnMapping: mapping.filter((m) => m.productField) }))

      const res = await api.post<ApiResponse<ImportResult>>('/products/import/execute', formData, {
        headers: { 'Content-Type': null },
      })
      setResult(res.data.data)
      setStep('result')
      if (res.data.data.created > 0) {
        toast.success(`${res.data.data.created} productos importados`)
      }
    } catch {
      toast.error('Error al importar productos')
    } finally {
      setLoading(false)
    }
  }

  const handleRetry = () => {
    setFile(null)
    setPreview(null)
    setMapping([])
    setResult(null)
    setStep('upload')
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  return (
    <Modal open={open} onClose={step === 'result' ? onClose : () => {}} size="xl">
      <div className="space-y-5">
        {/* Header */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Importar productos</h2>
          <p className="mt-1 text-sm text-gray-500">
            {step === 'upload' && 'Sube un archivo CSV o Excel con tus productos'}
            {step === 'mapping' && 'Indica qué columna del archivo corresponde a cada campo del producto'}
            {step === 'result' && 'Resultado de la importación'}
          </p>
        </div>

        {/* Steps indicator */}
        <div className="flex items-center gap-2 text-xs font-medium">
          {(['upload', 'mapping', 'result'] as Step[]).map((s, i) => {
            const isActive = step === s
            const isDone = ['upload', 'mapping', 'result'].indexOf(step) > i
            return (
              <div key={s} className="flex items-center gap-2">
                <span className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold
                  ${isActive ? 'bg-brand-600 text-white' : isDone ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-500 dark:bg-gray-700'}`}>
                  {isDone ? <CheckCircle size={14} /> : i + 1}
                </span>
                <span className={isActive ? 'text-brand-700 dark:text-brand-400' : 'text-gray-500'}>
                  {s === 'upload' ? 'Subir archivo' : s === 'mapping' ? 'Mapear columnas' : 'Resultado'}
                </span>
                {i < 2 && <ArrowRight size={14} className="text-gray-300" />}
              </div>
            )
          })}
        </div>

        {/* STEP 1: Upload */}
        {step === 'upload' && (
          <div className="space-y-4">
            <div className="rounded-lg border border-brand-200 bg-brand-50 p-3 dark:border-brand-900/50 dark:bg-brand-900/20">
              <div className="flex items-start gap-2 text-sm text-brand-800 dark:text-brand-200">
                <AlertTriangle size={16} className="mt-0.5 shrink-0" />
                <span>Tu archivo debe tener una fila de cabecera con los nombres de las columnas. Luego podrás indicar qué columna corresponde a cada campo.</span>
              </div>
            </div>

            <Button variant="outline" size="sm" onClick={handleDownloadTemplate}>
              <Download size={14} className="mr-1" />Descargar plantilla
            </Button>

            <div
              className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 py-12 transition hover:border-brand-400 hover:bg-brand-50/50 dark:border-gray-600 dark:bg-gray-900 dark:hover:border-brand-500"
              onClick={() => fileInputRef.current?.click()}
            >
              <Upload size={36} className="text-gray-400" />
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Haz clic para seleccionar archivo</p>
              <p className="text-xs text-gray-400">CSV o Excel (.xlsx, .xls)</p>
            </div>
            <input ref={fileInputRef} type="file" accept=".csv,.xlsx,.xls" className="hidden" onChange={handleFileSelect} />
          </div>
        )}

        {/* STEP 2: Mapping */}
        {step === 'mapping' && preview && (
          <div className="space-y-4">
            {/* File info */}
            <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
              <FileText size={16} />
              <span className="font-medium">{preview.fileName}</span>
              <span className="text-gray-300">|</span>
              <span>{preview.totalRows} productos detectados</span>
            </div>

            {/* Sample preview table */}
            <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
              <table className="w-full text-xs">
                <thead>
                  <tr className="bg-gray-50 dark:bg-gray-800">
                    {preview.detectedColumns.map((col) => (
                      <th key={col} className="whitespace-nowrap px-3 py-2 text-left font-medium text-gray-600 dark:text-gray-400">
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {preview.sampleRows.map((row, i) => (
                    <tr key={i} className="border-t border-gray-100 dark:border-gray-800">
                      {preview.detectedColumns.map((col) => (
                        <td key={col} className="max-w-[200px] truncate px-3 py-2 text-gray-700 dark:text-gray-300">
                          {row[col] || ''}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Column mapping */}
            <div className="space-y-2">
              <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Mapeo de columnas</h4>
              <p className="text-xs text-gray-500">Selecciona a qué campo del producto corresponde cada columna de tu archivo</p>

              <div className="space-y-1.5">
                {mapping.map((m) => (
                  <div key={m.fileColumn} className="flex items-center gap-3 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 dark:border-gray-700 dark:bg-gray-900">
                    <span className="min-w-[140px] text-sm font-medium text-gray-700 dark:text-gray-300">{m.fileColumn}</span>
                    <ArrowRight size={14} className="shrink-0 text-gray-400" />
                    <select
                      value={m.productField}
                      onChange={(e) => updateMapping(m.fileColumn, e.target.value)}
                      className="h-8 flex-1 rounded-md border border-gray-300 bg-white px-2 text-sm dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                    >
                      <option value="">— No importar —</option>
                      {preview.productFields.map((f) => (
                        <option key={f.value} value={f.value}>
                          {f.label} {f.required ? '*' : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                ))}
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-between">
              <Button variant="ghost" size="sm" onClick={handleRetry}>
                <XCircle size={14} className="mr-1" />Otro archivo
              </Button>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={onClose}>Cancelar</Button>
                <Button onClick={handleExecute} loading={loading}>
                  <Upload size={16} className="mr-1" />Importar {preview.totalRows} productos
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* STEP 3: Result */}
        {step === 'result' && result && (
          <div className="space-y-4">
            <div className="flex items-center gap-4 rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900">
              <div className="flex items-center gap-2 text-sm">
                <FileText size={16} className="text-gray-500" />
                <span className="text-gray-700 dark:text-gray-300">{result.totalRows} filas procesadas</span>
              </div>
              <span className="text-gray-300">|</span>
              <div className="flex items-center gap-2 text-sm">
                <CheckCircle size={16} className="text-green-500" />
                <span className="font-medium text-green-700 dark:text-green-400">{result.created} creados</span>
              </div>
              {result.errors.length > 0 && (
                <>
                  <span className="text-gray-300">|</span>
                  <div className="flex items-center gap-2 text-sm">
                    <XCircle size={16} className="text-red-500" />
                    <span className="font-medium text-red-700 dark:text-red-400">{result.errors.length} errores</span>
                  </div>
                </>
              )}
            </div>

            {result.errors.length > 0 && (
              <div className="max-h-48 overflow-y-auto rounded-lg border border-red-200 bg-red-50 dark:border-red-900/50 dark:bg-red-900/20">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-red-200 text-left dark:border-red-900/50">
                      <th className="px-3 py-2 font-medium text-red-700 dark:text-red-400">Fila</th>
                      <th className="px-3 py-2 font-medium text-red-700 dark:text-red-400">Producto</th>
                      <th className="px-3 py-2 font-medium text-red-700 dark:text-red-400">Error</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.errors.map((err, i) => (
                      <tr key={i} className="border-t border-red-100 dark:border-red-900/30">
                        <td className="px-3 py-1.5 text-red-600">{err.row}</td>
                        <td className="px-3 py-1.5 text-red-600">{err.productName}</td>
                        <td className="px-3 py-1.5 text-red-500">{err.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={handleRetry}>
                <Upload size={14} className="mr-1" />Importar otro archivo
              </Button>
              <Button onClick={onClose}>Finalizar</Button>
            </div>
          </div>
        )}

        {/* Global loader */}
        {loading && step === 'upload' && (
          <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500">
            <Loader2 size={20} className="animate-spin" />
            Leyendo archivo...
          </div>
        )}
      </div>
    </Modal>
  )
}
