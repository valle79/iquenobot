import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  MessageCircle,
  MessageSquare,
  Instagram,
  Mail,
  Globe,
  Send,
  Smartphone,
  Wifi,
  WifiOff,
  Save,
  ExternalLink,
  Plug,
} from 'lucide-react'
import { Tabs } from '@/shared/molecules/Tabs'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { settingService } from '@/services/setting.service'
import { whatsappService } from '@/services/whatsapp.service'
import { toast } from 'sonner'
import { cn } from '@/shared/utils'

interface ChannelConfig {
  id: string
  label: string
  icon: typeof MessageCircle
  color: string
  bgColor: string
  fields: { key: string; label: string; placeholder: string; type?: string }[]
  category: string
}

const CHANNELS: ChannelConfig[] = [
  {
    id: 'whatsapp',
    label: 'WhatsApp',
    icon: MessageCircle,
    color: 'text-green-600',
    bgColor: 'bg-green-100 dark:bg-green-900/20',
    category: 'whatsapp',
    fields: [
      { key: 'provider', label: 'Proveedor', placeholder: '', type: 'select' },
      { key: 'api_key', label: 'API Key', placeholder: 'Tu API Key', type: 'password' },
      { key: 'instance_id', label: 'Instance ID', placeholder: 'ID de la instancia de Evolution API' },
      { key: 'phone_number', label: 'Número de teléfono', placeholder: '+521234567890' },
      { key: 'webhook_url', label: 'Webhook URL', placeholder: 'https://tudominio.com/webhook/whatsapp' },
    ],
  },
  {
    id: 'messenger',
    label: 'Messenger',
    icon: MessageSquare,
    color: 'text-blue-500',
    bgColor: 'bg-blue-100 dark:bg-blue-900/20',
    category: 'messenger',
    fields: [
      { key: 'page_id', label: 'Page ID', placeholder: 'ID de la página de Facebook' },
      { key: 'access_token', label: 'Access Token', placeholder: 'Token de acceso de la página', type: 'password' },
      { key: 'verify_token', label: 'Verify Token', placeholder: 'Token de verificación del webhook', type: 'password' },
      { key: 'app_secret', label: 'App Secret', placeholder: 'Secreto de la aplicación', type: 'password' },
      { key: 'webhook_url', label: 'Webhook URL', placeholder: 'https://tudominio.com/webhook/messenger' },
    ],
  },
  {
    id: 'instagram',
    label: 'Instagram',
    icon: Instagram,
    color: 'text-pink-500',
    bgColor: 'bg-pink-100 dark:bg-pink-900/20',
    category: 'instagram',
    fields: [
      { key: 'page_id', label: 'Page ID', placeholder: 'ID de la página de Instagram' },
      { key: 'access_token', label: 'Access Token', placeholder: 'Token de acceso', type: 'password' },
      { key: 'verify_token', label: 'Verify Token', placeholder: 'Token de verificación', type: 'password' },
      { key: 'webhook_url', label: 'Webhook URL', placeholder: 'https://tudominio.com/webhook/instagram' },
    ],
  },
  {
    id: 'email',
    label: 'Email',
    icon: Mail,
    color: 'text-blue-600',
    bgColor: 'bg-blue-100 dark:bg-blue-900/20',
    category: 'email',
    fields: [
      { key: 'imap_host', label: 'Servidor IMAP', placeholder: 'imap.gmail.com' },
      { key: 'imap_port', label: 'Puerto IMAP', placeholder: '993' },
      { key: 'smtp_host', label: 'Servidor SMTP', placeholder: 'smtp.gmail.com' },
      { key: 'smtp_port', label: 'Puerto SMTP', placeholder: '587' },
      { key: 'email_address', label: 'Email', placeholder: 'soporte@tudominio.com' },
      { key: 'email_password', label: 'Contraseña', placeholder: 'Contraseña o contraseña de aplicación', type: 'password' },
    ],
  },
  {
    id: 'webchat',
    label: 'Web Chat',
    icon: Globe,
    color: 'text-indigo-500',
    bgColor: 'bg-indigo-100 dark:bg-indigo-900/20',
    category: 'webchat',
    fields: [
      { key: 'widget_color', label: 'Color del widget', placeholder: '#6366f1' },
      { key: 'welcome_message', label: 'Mensaje de bienvenida', placeholder: '¡Hola! ¿En qué podemos ayudarte?' },
      { key: 'offline_message', label: 'Mensaje fuera de línea', placeholder: 'Estamos fuera de línea. Te responderemos pronto.' },
      { key: 'position', label: 'Posición', placeholder: '', type: 'select' },
    ],
  },
  {
    id: 'sms',
    label: 'SMS',
    icon: Send,
    color: 'text-amber-500',
    bgColor: 'bg-amber-100 dark:bg-amber-900/20',
    category: 'sms',
    fields: [
      { key: 'provider', label: 'Proveedor', placeholder: '', type: 'select' },
      { key: 'api_key', label: 'API Key', placeholder: 'Tu API Key', type: 'password' },
      { key: 'api_secret', label: 'API Secret', placeholder: 'Tu API Secret', type: 'password' },
      { key: 'phone_number', label: 'Número de teléfono', placeholder: '+521234567890' },
      { key: 'webhook_url', label: 'Webhook URL', placeholder: 'https://tudominio.com/webhook/sms' },
    ],
  },
]

interface ChannelConfigFormProps {
  channel: ChannelConfig
  whatsappStatus?: { connected: boolean; provider: string | null; instanceId: string | null; error: string | null }
  statusLoading?: boolean
  onTestConnection?: () => void
  testingConnection?: boolean
}

function ChannelConfigForm({ channel, whatsappStatus, statusLoading, onTestConnection, testingConnection }: ChannelConfigFormProps) {
  const [saving, setSaving] = useState(false)
  const { data: settings } = useQuery({
    queryKey: ['settings', channel.category],
    queryFn: () => settingService.getByCategory(channel.category),
  })

  const isWhatsApp = channel.id === 'whatsapp'

  const getVal = (key: string) => settings?.find((s) => s.key === key)?.value ?? ''
  const connected = isWhatsApp ? (whatsappStatus?.connected ?? false) : getVal('connected') === 'true'

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(z.object(
      Object.fromEntries(channel.fields.map((f) => [f.key, z.string()]))
    )),
    values: Object.fromEntries(channel.fields.map((f) => [f.key, getVal(f.key)])) as Record<string, string>,
  })

  const onSubmit = async (data: Record<string, string>) => {
    setSaving(true)
    try {
      await settingService.update({
        category: channel.category,
        settings: Object.entries(data).map(([key, value]) => ({ key, value, type: 'text' })),
      })
      toast.success(`Configuración de ${channel.label} guardada`)
    } catch {
      toast.error(`Error al guardar configuración de ${channel.label}`)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={cn('flex h-12 w-12 items-center justify-center rounded-xl', channel.bgColor)}>
              <channel.icon size={24} className={channel.color} />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{channel.label}</h2>
              <p className="text-sm text-gray-500">Configuración de la integración</p>
            </div>
          </div>
          <Badge variant={connected ? 'success' : 'error'} size="sm" className="gap-1">
            {(isWhatsApp && statusLoading) ? (
              'Verificando...'
            ) : connected ? (
              <><Wifi size={12} /> Conectado</>
            ) : (
              <><WifiOff size={12} /> Desconectado</>
            )}
          </Badge>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <h3 className="mb-1 text-sm font-semibold text-gray-900 dark:text-gray-100">Credenciales</h3>
          <p className="mb-4 text-xs text-gray-500">Configura las credenciales de la API de {channel.label}</p>
          <div className="space-y-4">
            {channel.fields.map((field) => (
              field.type === 'select' ? (
                <Select
                  key={field.key}
                  label={field.label}
                  options={
                    field.key === 'provider'
                      ? channel.id === 'whatsapp'
                        ? [
                            { value: 'EVOLUTION_API', label: 'Evolution API' },
                            { value: 'WHATSAPP_CLOUD', label: 'WhatsApp Cloud API' },
                            { value: 'TWILIO', label: 'Twilio' },
                          ]
                        : [
                            { value: 'TWILIO', label: 'Twilio' },
                            { value: 'VONAGE', label: 'Vonage' },
                            { value: 'MESSAGEBIRD', label: 'MessageBird' },
                          ]
                      : field.key === 'position'
                        ? [
                            { value: 'bottom-right', label: 'Esquina inferior derecha' },
                            { value: 'bottom-left', label: 'Esquina inferior izquierda' },
                          ]
                        : [{ value: '', label: 'Seleccionar...' }]
                  }
                  error={errors[field.key]?.message}
                  {...register(field.key)}
                />
              ) : (
                <Input
                  key={field.key}
                  label={field.label}
                  type={field.type ?? 'text'}
                  placeholder={field.placeholder}
                  error={errors[field.key]?.message}
                  {...register(field.key)}
                />
              )
            ))}
          </div>
        </div>

        <div className="flex justify-end gap-3">
          {isWhatsApp && (
            <Button
              type="button"
              variant="outline"
              loading={testingConnection}
              onClick={onTestConnection}
            >
              <Plug size={16} className="mr-2" />
              Probar conexión
            </Button>
          )}
          <Button type="submit" loading={saving}>
            <Save size={16} className="mr-2" />
            Guardar configuración
          </Button>
        </div>
      </form>
    </div>
  )
}

export default function ChannelsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [activeChannel, setActiveChannel] = useState('whatsapp')

  const { data: whatsappStatus, isLoading: statusLoading } = useQuery({
    queryKey: ['whatsapp-status'],
    queryFn: () => whatsappService.getStatus(),
    refetchInterval: 30000,
  })

  const testConnectionMutation = useMutation({
    mutationFn: () => whatsappService.testConnection(),
    onSuccess: (result) => {
      if (result.connected) {
        toast.success(result.message)
      } else {
        toast.warning(result.message)
      }
      queryClient.invalidateQueries({ queryKey: ['whatsapp-status'] })
    },
    onError: () => {
      toast.error('Error al probar la conexión de WhatsApp')
    },
  })

  const connected = whatsappStatus?.connected ?? false

  const tabs = CHANNELS.map((ch) => ({
    id: ch.id,
    label: ch.label,
    icon: <ch.icon size={16} />,
  }))

  const currentChannel = CHANNELS.find((ch) => ch.id === activeChannel)!

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Canales</h1>
          <p className="mt-1 text-sm text-gray-500">Configura los canales de comunicación</p>
        </div>
      </div>

      <Tabs
        tabs={tabs}
        activeTab={activeChannel}
        onChange={setActiveChannel}
        variant="pills"
      />

      <ChannelConfigForm
        channel={currentChannel}
        whatsappStatus={whatsappStatus}
        statusLoading={statusLoading}
        onTestConnection={() => testConnectionMutation.mutate()}
        testingConnection={testConnectionMutation.isPending}
      />

      {activeChannel === 'whatsapp' && (
        <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
          <div className="flex items-center gap-2">
            <MessageCircle size={16} className="text-gray-400" />
            <span className="text-sm font-medium text-gray-900 dark:text-gray-100">Estado de conexión</span>
          </div>
          <div className="mt-3 space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">Proveedor:</span>
              <span className="font-medium text-gray-900 dark:text-gray-100">{whatsappStatus?.provider ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">Instance ID:</span>
              <span className="font-mono text-xs text-gray-900 dark:text-gray-100">{whatsappStatus?.instanceId ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-500">Estado:</span>
              <Badge variant={connected ? 'success' : 'error'} size="sm">
                {connected ? 'Conectado' : 'Desconectado'}
              </Badge>
            </div>
            {whatsappStatus?.error && (
              <p className="mt-2 text-xs text-red-500">{whatsappStatus.error}</p>
            )}
          </div>
          <p className="mt-3 text-xs text-gray-500">
            Las conversaciones de WhatsApp aparecen en la sección de Conversaciones, filtradas por canal WhatsApp.
          </p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate('/conversations')}>
            <ExternalLink size={14} className="mr-1.5" />
            Ir a conversaciones
          </Button>
        </div>
      )}
    </div>
  )
}
