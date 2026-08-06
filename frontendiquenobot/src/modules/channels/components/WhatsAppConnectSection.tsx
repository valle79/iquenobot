import { useState, useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/shared/atoms/Button/Button'
import { Select } from '@/shared/atoms/Select/Select'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { whatsappService, type WhatsAppQRCodeResult } from '@/services/whatsapp.service'
import { toast } from 'sonner'
import { cn } from '@/shared/utils'
import {
  MessageCircle,
  Wifi,
  WifiOff,
  RefreshCw,
  QrCode,
  LogOut,
  AlertCircle,
  Zap,
  ExternalLink,
} from 'lucide-react'

const PROVIDER_OPTIONS = [{ value: 'EVOLUTION_API', label: 'Evolution API' }]

interface WhatsAppConnectSectionProps {
  compact?: boolean
}

/**
 * Configuración simplificada de WhatsApp.
 * El usuario solo elige el proveedor y escanea el QR; la instancia,
 * el webhook y las credenciales se crean automáticamente por el backend.
 */
export function WhatsAppConnectSection({ compact = false }: WhatsAppConnectSectionProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [provider, setProvider] = useState('EVOLUTION_API')
  const [settingUp, setSettingUp] = useState(false)
  const [qrError, setQrError] = useState(false)

  const { data: status, isLoading: statusLoading, refetch: refetchStatus } = useQuery({
    queryKey: ['whatsapp-status'],
    queryFn: () => whatsappService.getStatus(),
    refetchInterval: 30000,
  })

  const configured = status?.configured ?? false
  const connected = status?.connected ?? false

  const { data: qrCode, isLoading: qrLoading, refetch: refetchQR } = useQuery<WhatsAppQRCodeResult>({
    queryKey: ['whatsapp-qr'],
    queryFn: () => whatsappService.getQRCode(),
    enabled: configured && !connected,
    refetchInterval: 30000,
  })

  const qrBase64 = qrCode?.base64 ?? null
  const hasQR = qrCode?.hasQR ?? false

  useEffect(() => {
    setQrError(false)
  }, [qrCode?.base64])

  const handleSetup = async () => {
    setSettingUp(true)
    try {
      const result = await whatsappService.setup({ provider })
      toast.success(result.message)
      await refetchStatus()
      await refetchQR()
    } catch (error) {
      const detail = error as { response?: { data?: { message?: string } } }
      toast.error(detail.response?.data?.message || 'Error al configurar WhatsApp')
    } finally {
      setSettingUp(false)
    }
  }

  const handleDisconnect = async () => {
    try {
      await whatsappService.disconnect()
      await queryClient.invalidateQueries({ queryKey: ['whatsapp-status'] })
      toast.success('WhatsApp desconectado exitosamente')
    } catch {
      toast.error('Error al desconectar WhatsApp')
    }
  }

  const handleRefreshQR = async () => {
    setQrError(false)
    await refetchQR()
    toast.success('Código QR actualizado')
  }

  return (
    <div className={cn('space-y-6', compact && 'space-y-4')}>
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600 dark:bg-green-900/20 dark:text-green-400">
              <MessageCircle size={24} />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">WhatsApp</h2>
              <p className="text-sm text-gray-500">Configuración de la integración</p>
            </div>
          </div>
          {statusLoading ? (
            <div className="h-6 w-24 animate-pulse rounded bg-gray-200 dark:bg-gray-700" />
          ) : (
            <Badge variant={connected ? 'success' : 'error'} size="sm" className="gap-1">
              {connected ? <><Wifi size={12} /> Conectado</> : <><WifiOff size={12} /> Desconectado</>}
            </Badge>
          )}
        </div>
      </div>

      {!configured && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <div className="flex items-center gap-2">
            <Zap size={16} className="text-green-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
              Conectar WhatsApp
            </h3>
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Selecciona el proveedor y conéctate escaneando el código QR con tu teléfono.
            No necesitas API Key ni ninguna otra credencial.
          </p>
          <div className="mt-4 grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <Select
              label="Proveedor"
              options={PROVIDER_OPTIONS}
              value={provider}
              onChange={(e) => { setProvider(e.target.value) }}
            />
            <Button onClick={() => void handleSetup()} loading={settingUp}>
              <QrCode size={16} className="mr-2" />
              Conectar WhatsApp
            </Button>
          </div>
        </div>
      )}

      {configured && !connected && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <QrCode size={16} className="text-gray-400" />
              <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                Escanear código QR
              </span>
            </div>
            <Button type="button" variant="ghost" size="sm" onClick={() => void handleRefreshQR()}>
              <RefreshCw size={14} className="mr-1" />
              Actualizar QR
            </Button>
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Escanea este código QR con WhatsApp en tu teléfono para conectar la instancia.
          </p>
          <div className="mt-4 flex flex-col items-center gap-4">
            {qrLoading && !qrBase64 ? (
              <div className="flex h-64 w-64 items-center justify-center">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-600 border-t-transparent" />
              </div>
            ) : hasQR && qrBase64 ? (
              <div className="relative">
                <img
                  src={qrBase64}
                  alt="QR Code"
                  className="h-64 w-64 rounded-lg border border-gray-200 object-contain dark:border-gray-700"
                  onError={() => { setQrError(true) }}
                />
                {qrError && (
                  <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-white/80 dark:bg-gray-950/80">
                    <p className="text-sm text-red-500">Error al cargar QR</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex h-48 w-full flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-gray-300 dark:border-gray-600">
                <AlertCircle size={24} className="text-gray-400" />
                <p className="text-sm text-gray-500">
                  {qrCode?.error || 'No se pudo generar el código QR'}
                </p>
                <Button type="button" variant="outline" size="sm" onClick={() => void handleRefreshQR()}>
                  <RefreshCw size={14} className="mr-1" />
                  Reintentar
                </Button>
              </div>
            )}
            <ol className="space-y-1.5 text-xs text-gray-500">
              <li className="flex items-start gap-2">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-100 text-[10px] font-bold text-brand-700 dark:bg-brand-900/30 dark:text-brand-400">1</span>
                Abre WhatsApp en tu teléfono
              </li>
              <li className="flex items-start gap-2">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-100 text-[10px] font-bold text-brand-700 dark:bg-brand-900/30 dark:text-brand-400">2</span>
                Ve a <span className="font-medium text-gray-700 dark:text-gray-300">Menú &gt; Dispositivos vinculados</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-100 text-[10px] font-bold text-brand-700 dark:bg-brand-900/30 dark:text-brand-400">3</span>
                Toca <span className="font-medium text-gray-700 dark:text-gray-300">Vincular un dispositivo</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-100 text-[10px] font-bold text-brand-700 dark:bg-brand-900/30 dark:text-brand-400">4</span>
                Escanea el código QR con tu teléfono
              </li>
            </ol>
          </div>
        </div>
      )}

      {connected && (
        <div className="rounded-xl border border-green-200 bg-white p-6 dark:border-green-800 dark:bg-gray-950">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Wifi size={16} className="text-green-500" />
              <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                WhatsApp conectado
              </span>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="text-red-500 hover:text-red-600"
              onClick={() => void handleDisconnect()}
            >
              <LogOut size={14} className="mr-1.5" />
              Desconectar
            </Button>
          </div>
        </div>
      )}

      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex items-center gap-2">
          <MessageCircle size={16} className="text-gray-400" />
          <span className="text-sm font-medium text-gray-900 dark:text-gray-100">Estado de conexión</span>
        </div>
        <div className="mt-3 space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-500">Proveedor:</span>
            <span className="font-medium text-gray-900 dark:text-gray-100">{status?.provider ?? '—'}</span>
          </div>
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-500">Estado:</span>
            <Badge variant={connected ? 'success' : 'error'} size="sm">
              {connected ? 'Conectado' : 'Desconectado'}
            </Badge>
          </div>
          {status?.error && (
            <p className="mt-2 text-xs text-red-500">{status.error}</p>
          )}
        </div>
        <p className="mt-3 text-xs text-gray-500">
          Las conversaciones de WhatsApp aparecen en la sección de Conversaciones, filtradas por canal WhatsApp.
        </p>
        <Button variant="outline" size="sm" className="mt-3" onClick={() => void navigate('/conversations')}>
          <ExternalLink size={14} className="mr-1.5" />
          Ir a conversaciones
        </Button>
      </div>
    </div>
  )
}
