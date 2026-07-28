import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { settingService } from '@/services/setting.service'
import { whatsappService } from '@/services/whatsapp.service'
import { Save, Smartphone, MessageCircle, Wifi, WifiOff, RefreshCw, QrCode, LogOut, AlertCircle } from 'lucide-react'
import { toast } from 'sonner'
import { type WhatsAppProvider } from '@/types/enums'
import { StaggerContainer, StaggerItem } from '@/shared/molecules/StaggerContainer'

const schema = z.object({
  provider: z.string().min(1, 'Selecciona un proveedor'),
  apiKey: z.string().min(1, 'La API Key es obligatoria'),
  instanceId: z.string().min(1, 'El ID de instancia es obligatorio'),
  phoneNumber: z.string().min(1, 'El número de teléfono es obligatorio'),
  webhookUrl: z.string().optional(),
})

type FormData = z.infer<typeof schema>

const providerOptions = [
  { value: 'EVOLUTION_API', label: 'Evolution API' },
  { value: 'WHATSAPP_CLOUD_API', label: 'WhatsApp Cloud API' },
  { value: 'BAILEYS', label: 'Baileys' },
  { value: 'TWILIO', label: 'Twilio' },
]

function StatusBadge({ connected }: { connected: boolean }) {
  return (
    <Badge variant={connected ? 'success' : 'error'} size="sm" className="gap-1">
      {connected ? <><Wifi size={12} /> Conectado</> : <><WifiOff size={12} /> Desconectado</>}
    </Badge>
  )
}

function WhatsAppQRCodeSection() {
  const [qrError, setQrError] = useState(false)

  const { data: qrCode, isLoading: qrLoading, refetch: refetchQR } = useQuery({
    queryKey: ['whatsapp-qr'],
    queryFn: () => whatsappService.getQRCode(),
    refetchInterval: 30000,
  })

  useEffect(() => {
    setQrError(false)
  }, [qrCode?.base64])

  const handleRefreshQR = async () => {
    setQrError(false)
    await refetchQR()
    toast.success('Código QR actualizado')
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <QrCode size={16} className="text-gray-400" />
          <span className="text-sm font-medium text-gray-900 dark:text-gray-100">Escanear código QR</span>
        </div>
        <Button type="button" variant="ghost" size="sm" onClick={handleRefreshQR}>
          <RefreshCw size={14} className="mr-1" />
          Actualizar QR
        </Button>
      </div>
      <p className="mt-1 text-xs text-gray-500">
        Escanea este código QR con WhatsApp en tu teléfono para conectar la instancia.
      </p>
      <div className="mt-4 flex flex-col items-center gap-4">
        {qrLoading && !qrCode?.base64 ? (
          <div className="flex h-64 w-64 items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-600 border-t-transparent" />
          </div>
        ) : qrCode?.hasQR && qrCode?.base64 ? (
          <div className="relative">
            <img
              src={qrCode.base64}
              alt="QR Code"
              className="h-64 w-64 rounded-lg border border-gray-200 object-contain dark:border-gray-700"
              onError={() => setQrError(true)}
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
            <Button type="button" variant="outline" size="sm" onClick={handleRefreshQR}>
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
  )
}

export default function WhatsAppPage() {
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)

  const { data: settings, isLoading } = useQuery({
    queryKey: ['settings', 'whatsapp'],
    queryFn: () => settingService.getByCategory('whatsapp'),
  })

  const { data: connectionStatus, isLoading: statusLoading, refetch: refetchStatus } = useQuery({
    queryKey: ['whatsapp-status'],
    queryFn: () => whatsappService.getStatus(),
    refetchInterval: 30000,
  })

  const getVal = (key: string) => settings?.find((s) => s.key === key)?.value ?? ''

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    values: {
      provider: getVal('provider') || 'EVOLUTION_API',
      apiKey: getVal('api_key'),
      instanceId: getVal('instance_id'),
      phoneNumber: getVal('phone_number'),
      webhookUrl: getVal('webhook_url'),
    },
  })

  const onSubmit = async (data: FormData) => {
    setSaving(true)
    try {
      await settingService.update({
        category: 'whatsapp',
        settings: [
          { key: 'provider', value: data.provider },
          { key: 'api_key', value: data.apiKey },
          { key: 'instance_id', value: data.instanceId },
          { key: 'phone_number', value: data.phoneNumber },
          { key: 'webhook_url', value: data.webhookUrl ?? '' },
        ],
      })
      toast.success('Configuración de WhatsApp guardada')
    } catch {
      toast.error('Error al guardar la configuración')
    } finally {
      setSaving(false)
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-14 animate-pulse rounded-lg bg-gray-200 dark:bg-gray-700" />
        ))}
      </div>
    )
  }

  return (
    <StaggerContainer className="mx-auto max-w-2xl space-y-6">
      <StaggerItem>
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600 dark:bg-green-900/20 dark:text-green-400">
                <MessageCircle size={24} />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                  WhatsApp Business
                </h2>
                <p className="text-sm text-gray-500">Configuración de la integración</p>
              </div>
            </div>
            {statusLoading ? (
              <div className="h-6 w-24 animate-pulse rounded bg-gray-200 dark:bg-gray-700" />
            ) : (
              <StatusBadge connected={connectionStatus?.connected ?? false} />
            )}
          </div>
        </div>
      </StaggerItem>

      <StaggerItem>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-1 text-sm font-semibold text-gray-900 dark:text-gray-100">Proveedor</h3>
            <p className="mb-4 text-xs text-gray-500">Selecciona el proveedor de WhatsApp que deseas utilizar</p>
            <Select
              label="Proveedor"
              options={providerOptions}
              error={errors.provider?.message}
              {...register('provider')}
            />
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <h3 className="mb-1 text-sm font-semibold text-gray-900 dark:text-gray-100">Credenciales</h3>
            <p className="mb-4 text-xs text-gray-500">Configura las credenciales de la API de WhatsApp</p>
            <div className="space-y-4">
              <Input
                label="API Key"
                type="password"
                placeholder="Ingresa tu API Key"
                error={errors.apiKey?.message}
                {...register('apiKey')}
              />
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="Instance ID"
                  placeholder="ID de la instancia de Evolution API"
                  error={errors.instanceId?.message}
                  {...register('instanceId')}
                />
                <Input
                  label="Número de teléfono"
                  placeholder="+521234567890"
                  error={errors.phoneNumber?.message}
                  {...register('phoneNumber')}
                />
              </div>
              <Input
                label="Webhook URL"
                placeholder="https://tudominio.com/webhook/whatsapp"
                error={errors.webhookUrl?.message}
                {...register('webhookUrl')}
              />
            </div>
          </div>

          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={async () => {
              const toastId = toast.loading('Guardando y probando conexión...')
              try {
                const data = getValues()
                await settingService.update({
                  category: 'whatsapp',
                  settings: [
                    { key: 'provider', value: data.provider },
                    { key: 'api_key', value: data.apiKey },
                    { key: 'instance_id', value: data.instanceId },
                    { key: 'phone_number', value: data.phoneNumber },
                    { key: 'webhook_url', value: data.webhookUrl ?? '' },
                  ],
                })
                const result = await whatsappService.testConnection()
                refetchStatus()
                if (result.success && result.connected) {
                  toast.success(result.message, { id: toastId })
                } else {
                  toast.error(result.message, { id: toastId })
                }
              } catch {
                toast.error('Error al probar la conexión', { id: toastId })
              }
            }}>
              <RefreshCw size={16} className="mr-2" />
              Probar conexión
            </Button>
            <Button type="submit" loading={saving}>
              <Save size={16} className="mr-2" />
              Guardar configuración
            </Button>
          </div>
        </form>
      </StaggerItem>

      {connectionStatus?.configured && !connectionStatus?.connected && (
        <StaggerItem>
          <WhatsAppQRCodeSection />
        </StaggerItem>
      )}

      {connectionStatus?.connected && (
        <StaggerItem>
          <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Wifi size={16} className="text-green-500" />
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">WhatsApp conectado</span>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="text-red-500 hover:text-red-600"
                onClick={async () => {
                  try {
                    await whatsappService.disconnect()
                    refetchStatus()
                    toast.success('WhatsApp desconectado exitosamente')
                  } catch {
                    toast.error('Error al desconectar WhatsApp')
                  }
                }}
              >
                <LogOut size={14} className="mr-1.5" />
                Desconectar
              </Button>
            </div>
          </div>
        </StaggerItem>
      )}

      <StaggerItem>
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <div className="flex items-center gap-2">
            <Smartphone size={16} className="text-gray-400" />
            <span className="text-sm font-medium text-gray-900 dark:text-gray-100">Conversaciones WhatsApp</span>
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Las conversaciones de WhatsApp aparecen en la sección de Conversaciones, filtradas por canal WhatsApp.
          </p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate('/conversations')}>
            <MessageCircle size={14} className="mr-1.5" />
            Ir a conversaciones
          </Button>
        </div>
      </StaggerItem>
    </StaggerContainer>
  )
}
