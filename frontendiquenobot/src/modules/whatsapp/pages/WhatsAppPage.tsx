import { useState } from 'react'
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
import { Save, Smartphone, MessageCircle, Wifi, WifiOff, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { type WhatsAppProvider } from '@/types/enums'
import { StaggerContainer, StaggerItem } from '@/shared/molecules/StaggerContainer'

const schema = z.object({
  provider: z.string().min(1, 'Selecciona un proveedor'),
  apiKey: z.string().min(1, 'La API Key es obligatoria'),
  phoneNumber: z.string().min(1, 'El número de teléfono es obligatorio'),
  phoneNumberId: z.string().optional(),
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

export default function WhatsAppPage() {
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)

  const { data: settings, isLoading } = useQuery({
    queryKey: ['settings', 'whatsapp'],
    queryFn: () => settingService.getByCategory('whatsapp'),
  })

  const getVal = (key: string) => settings?.find((s) => s.key === key)?.value ?? ''

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    values: {
      provider: getVal('provider') || 'EVOLUTION_API',
      apiKey: getVal('api_key'),
      phoneNumber: getVal('phone_number'),
      phoneNumberId: getVal('phone_number_id'),
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
          { key: 'phone_number', value: data.phoneNumber },
          { key: 'phone_number_id', value: data.phoneNumberId ?? '' },
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
            <StatusBadge connected={false} />
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
                  label="Número de teléfono"
                  placeholder="+521234567890"
                  error={errors.phoneNumber?.message}
                  {...register('phoneNumber')}
                />
                <Input
                  label="Phone Number ID"
                  placeholder="ID del número (Cloud API)"
                  error={errors.phoneNumberId?.message}
                  {...register('phoneNumberId')}
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
            <Button type="button" variant="outline" onClick={() => toast.info('Probando conexión...')}>
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
