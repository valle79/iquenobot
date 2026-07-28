import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Building2,
  Cpu,
  Bell,
  User,
  Shield,
  Globe,
  Bot,
  Clock,
  BookOpen,
  Save,
  Eye,
  EyeOff,
  CheckCircle2,
  XCircle,
} from 'lucide-react'
import { Tabs } from '@/shared/molecules/Tabs'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { Select } from '@/shared/atoms/Select/Select'
import { Toggle } from '@/shared/atoms/Toggle/Toggle'
import { WorkingHoursEditor } from '@/modules/settings/components/WorkingHoursEditor'
import { useAuthStore } from '@/core/auth/auth.store'
import { api } from '@/core/api/client'
import { useSettingsByCategory, useUpdateSettings } from '@/modules/settings/hooks/useSettings'
import { DEFAULT_WORKING_HOURS, DAY_ORDER } from '@/types/orchestrator'
import KnowledgeBasePage from '@/modules/knowledge/KnowledgeBasePage'
import type { ApiResponse } from '@/types/api'
import type { UserDto } from '@/types/auth'
import type { WorkingHoursSchedule } from '@/types/orchestrator'

const settingsTabs = [
  { id: 'profile', label: 'Perfil', icon: <User size={16} /> },
  { id: 'company', label: 'Empresa', icon: <Building2 size={16} /> },
  { id: 'bot', label: 'Bot', icon: <Bot size={16} /> },
  { id: 'ai', label: 'Inteligencia Artificial', icon: <Cpu size={16} /> },
  { id: 'working_hours', label: 'Horario', icon: <Clock size={16} /> },
  { id: 'knowledge', label: 'Conocimiento', icon: <BookOpen size={16} /> },
  { id: 'notifications', label: 'Notificaciones', icon: <Bell size={16} /> },
  { id: 'security', label: 'Seguridad', icon: <Shield size={16} /> },
  { id: 'general', label: 'General', icon: <Globe size={16} /> },
]

const profileSchema = z.object({
  firstName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  lastName: z.string().min(2, 'Mínimo 2 caracteres').max(100),
  phone: z.string().optional(),
})

type ProfileForm = z.infer<typeof profileSchema>

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'La contraseña actual es obligatoria'),
  newPassword: z.string().min(6, 'Mínimo 6 caracteres').max(100),
  confirmPassword: z.string().min(1, 'Confirma la nueva contraseña'),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Las contraseñas no coinciden',
  path: ['confirmPassword'],
})

type PasswordForm = z.infer<typeof passwordSchema>

function ProfileSettings() {
  const { user, setUser } = useAuthStore()
  const [saving, setSaving] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      phone: user?.phone ?? '',
    },
  })

  useEffect(() => {
    if (user) {
      reset({
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone || '',
      })
    }
  }, [user, reset])

  const onSubmit = async (data: ProfileForm) => {
    setSaving(true)
    try {
      const res = await api.put<ApiResponse<UserDto>>('/auth/users/me', {
        firstName: data.firstName,
        lastName: data.lastName,
        phone: data.phone || undefined,
      })
      setUser(res.data.data)
      reset({
        firstName: res.data.data.firstName,
        lastName: res.data.data.lastName,
        phone: res.data.data.phone || '',
      })
    } catch {
      // handled by api interceptor
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Información personal</h3>
        <p className="mt-1 text-sm text-gray-500">Actualiza tu información de perfil</p>
        <div className="mt-6 grid gap-6 md:grid-cols-2">
          <Input label="Nombre" error={errors.firstName?.message} {...register('firstName')} />
          <Input label="Apellido" error={errors.lastName?.message} {...register('lastName')} />
          <Input label="Email" value={user?.email ?? ''} disabled />
          <Input label="Teléfono" error={errors.phone?.message} {...register('phone')} />
        </div>
      </div>
      {isDirty && (
        <div className="flex justify-end">
          <Button type="submit" loading={saving}>
            <Save size={16} className="mr-2" />
            Guardar cambios
          </Button>
        </div>
      )}
    </form>
  )
}

function CompanySettings() {
  const { tenant } = useAuthStore()

  if (!tenant) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <p className="text-gray-500">No hay información de empresa disponible</p>
      </div>
    )
  }

  const fields = [
    { label: 'Nombre de la empresa', value: tenant.companyName },
    { label: 'Subdominio', value: tenant.subdomain },
    { label: 'Email de contacto', value: tenant.contactEmail },
    { label: 'Teléfono de contacto', value: tenant.contactPhone || '-' },
    { label: 'Sitio web', value: tenant.websiteUrl || '-' },
    { label: 'Dirección', value: tenant.address || '-' },
    { label: 'Ciudad', value: tenant.city || '-' },
    { label: 'País', value: tenant.country || '-' },
    { label: 'Zona horaria', value: tenant.timezone || '-' },
    { label: 'Moneda', value: tenant.currency || '-' },
    { label: 'Plan', value: tenant.subscriptionPlan || '-' },
    {
      label: 'Estado',
      value: tenant.status === 'ACTIVE'
        ? <span className="inline-flex items-center gap-1 text-green-600"><CheckCircle2 size={14} />Activo</span>
        : <span className="inline-flex items-center gap-1 text-red-600"><XCircle size={14} />Inactivo</span>,
    },
  ]

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Información de la empresa</h3>
      <p className="mt-1 text-sm text-gray-500">Datos registrados de tu empresa</p>
      <dl className="mt-6 divide-y divide-gray-100 dark:divide-gray-800">
        {fields.map((f) => (
          <div key={f.label} className="flex justify-between py-3">
            <dt className="text-sm font-medium text-gray-500">{f.label}</dt>
            <dd className="text-sm text-gray-900 dark:text-gray-100">{f.value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}

const botSchema = z.object({
  enabled: z.boolean(),
  autoReply: z.boolean(),
  humanHandoffEnabled: z.boolean(),
  fallbackMessage: z.string().min(1, 'El mensaje de respaldo es obligatorio'),
})

type BotForm = z.infer<typeof botSchema>

function BotSettings() {
  const { data: botSettings } = useSettingsByCategory('bot')
  const { data: aiSettings } = useSettingsByCategory('ai')
  const updateSettings = useUpdateSettings()

  const getBotVal = (key: string, fallback = '') =>
    botSettings?.find((s) => s.key === key)?.value ?? fallback
  const getAiVal = (key: string, fallback = '') =>
    aiSettings?.find((s) => s.key === key)?.value ?? fallback

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<BotForm>({
    resolver: zodResolver(botSchema),
    values: {
      enabled: getAiVal('enabled', 'false') === 'true',
      autoReply: getAiVal('auto_reply', 'false') === 'true',
      humanHandoffEnabled: getBotVal('human_handoff', 'true') === 'true',
      fallbackMessage: getBotVal('fallback_message', 'Lo siento, voy a conectarte con un agente humano.'),
    },
  })

  const handleSave = async (data: BotForm) => {
    await updateSettings.mutateAsync({
      category: 'ai',
      settings: [
        { key: 'enabled', value: String(data.enabled), type: 'boolean' },
        { key: 'auto_reply', value: String(data.autoReply), type: 'boolean' },
      ],
    })
    await updateSettings.mutateAsync({
      category: 'bot',
      settings: [
        { key: 'human_handoff', value: String(data.humanHandoffEnabled), type: 'boolean' },
        { key: 'fallback_message', value: data.fallbackMessage, type: 'text' },
      ],
    })
  }

  return (
    <form onSubmit={handleSubmit(handleSave)} className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Configuración del Bot</h3>
        <p className="mt-1 text-sm text-gray-500">Controla el comportamiento general del chatbot</p>

        <div className="mt-6 space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Bot habilitado</p>
              <p className="text-xs text-gray-500">Activa o desactiva el chatbot para responder mensajes</p>
            </div>
            <Toggle checked={watch('enabled')} onChange={(checked) => setValue('enabled', checked)} />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Auto-respuesta</p>
              <p className="text-xs text-gray-500">El bot responde automáticamente a los mensajes entrantes</p>
            </div>
            <Toggle checked={watch('autoReply')} onChange={(checked) => setValue('autoReply', checked)} />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Transferencia a agente humano</p>
              <p className="text-xs text-gray-500">Permitir que el bot transfiera conversaciones a un agente humano</p>
            </div>
            <Toggle
              checked={watch('humanHandoffEnabled')}
              onChange={(checked) => setValue('humanHandoffEnabled', checked)}
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Mensaje de respaldo
            </label>
            <p className="text-xs text-gray-500">Mensaje que se envía cuando el bot no puede responder</p>
            <textarea
              className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              {...register('fallbackMessage')}
            />
            {errors.fallbackMessage && (
              <p className="text-xs text-red-500">{errors.fallbackMessage.message}</p>
            )}
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" loading={updateSettings.isPending}>
          <Save size={16} className="mr-2" />
          Guardar cambios
        </Button>
      </div>
    </form>
  )
}

function AISettings() {
  const { data: settings, isLoading } = useSettingsByCategory('ai')
  const updateSettings = useUpdateSettings()

  const getVal = (key: string, fallback = '') =>
    settings?.find((s) => s.key === key)?.value ?? fallback

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(z.object({
      provider: z.string().min(1, 'Selecciona un proveedor'),
      systemPrompt: z.string().min(1, 'El prompt del sistema es obligatorio'),
      temperature: z.coerce.number().min(0).max(2),
      enabled: z.boolean(),
    })),
    values: {
      provider: getVal('provider', 'NONE'),
      systemPrompt: getVal('system_prompt', 'Eres un asistente virtual de atención al cliente amable y profesional.'),
      temperature: parseFloat(getVal('temperature', '0.7')),
      enabled: getVal('enabled', 'false') === 'true',
    },
  })

  const temperature = watch('temperature')

  const handleSave = async (data: { provider: string; systemPrompt: string; temperature: number; enabled: boolean }) => {
    await updateSettings.mutateAsync({
      category: 'ai',
      settings: [
        { key: 'provider', value: data.provider, type: 'text' },
        { key: 'system_prompt', value: data.systemPrompt, type: 'text' },
        { key: 'temperature', value: String(data.temperature), type: 'text' },
        { key: 'enabled', value: String(data.enabled), type: 'boolean' },
      ],
    })
  }

  if (isLoading) {
    return (
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <p className="text-gray-500">Cargando...</p>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit(handleSave)} className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Inteligencia Artificial</h3>
        <p className="mt-1 text-sm text-gray-500">Configura el proveedor de IA y el comportamiento del modelo</p>

        <div className="mt-6 space-y-5">
          <Select
            label="Proveedor de IA"
            options={[
              { value: 'NONE', label: 'Sin proveedor (deshabilitado)' },
              { value: 'OPENAI', label: 'OpenAI' },
              { value: 'GROQ', label: 'Groq' },
              { value: 'GEMINI', label: 'Google Gemini' },
              { value: 'CLAUDE', label: 'Anthropic Claude' },
            ]}
            error={errors.provider?.message}
            {...register('provider')}
          />

          <div className="space-y-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Prompt del sistema
            </label>
            <p className="text-xs text-gray-500">Instrucciones para el comportamiento del asistente de IA</p>
            <textarea
              className="h-32 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
              {...register('systemPrompt')}
            />
            {errors.systemPrompt && (
              <p className="text-xs text-red-500">{errors.systemPrompt.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Temperatura
              </label>
              <span className="text-sm text-gray-500">{temperature}</span>
            </div>
            <p className="text-xs text-gray-500">Controla la creatividad de las respuestas (0 = preciso, 2 = creativo)</p>
            <input
              type="range"
              min="0"
              max="2"
              step="0.1"
              className="w-full accent-brand-600"
              {...register('temperature')}
            />
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <Button type="submit" loading={updateSettings.isPending}>
          <Save size={16} className="mr-2" />
          Guardar cambios
        </Button>
      </div>
    </form>
  )
}

function WorkingHoursSettings() {
  const { data: generalSettings } = useSettingsByCategory('general')
  const { data: botSettings } = useSettingsByCategory('bot')
  const updateSettings = useUpdateSettings()

  const getGeneralVal = (key: string, fallback = '') =>
    generalSettings?.find((s) => s.key === key)?.value ?? fallback
  const getBotVal = (key: string, fallback = '') =>
    botSettings?.find((s) => s.key === key)?.value ?? fallback

  const [enabled, setEnabled] = useState(true)
  const [schedule, setSchedule] = useState<WorkingHoursSchedule>(DEFAULT_WORKING_HOURS)
  const [closedMessage, setClosedMessage] = useState(
    'Estamos fuera de horario laboral. Te atenderemos en nuestro horario de atención.'
  )
  const [timezone, setTimezone] = useState('America/Guayaquil')

  useEffect(() => {
    if (generalSettings) {
      setEnabled(getGeneralVal('business_hours_enabled', 'true') === 'true')
      try {
        const raw = getGeneralVal('business_hours')
        if (raw) setSchedule(JSON.parse(raw))
      } catch {}
      setTimezone(getGeneralVal('timezone', 'America/Guayaquil'))
    }
  }, [generalSettings])

  useEffect(() => {
    if (botSettings) {
      setClosedMessage(
        getBotVal('after_hours_message', 'Estamos fuera de horario laboral. Te atenderemos en nuestro horario de atención.')
      )
    }
  }, [botSettings])

  const handleSave = async () => {
    try {
      await updateSettings.mutateAsync({
        category: 'general',
        settings: [
          { key: 'business_hours_enabled', value: String(enabled), type: 'boolean' },
          { key: 'business_hours', value: JSON.stringify(schedule), type: 'json' },
          { key: 'timezone', value: timezone, type: 'text' },
        ],
      })
      await updateSettings.mutateAsync({
        category: 'bot',
        settings: [
          { key: 'after_hours_message', value: closedMessage, type: 'text' },
        ],
      })
    } catch (err) {
      console.error('Error al guardar horario laboral:', err)
      alert('Error al guardar: ' + (err instanceof Error ? err.message : String(err)))
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Horario Laboral</h3>
        <p className="mt-1 text-sm text-gray-500">Define el horario en el que el bot está disponible</p>

        <div className="mt-6 space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Horario habilitado</p>
              <p className="text-xs text-gray-500">Activa la restricción de horario laboral</p>
            </div>
            <Toggle checked={enabled} onChange={setEnabled} />
          </div>

          {enabled && (
            <>
              <Select
                label="Zona horaria"
                options={[
                  { value: 'America/Guayaquil', label: 'Guayaquil (GMT-5)' },
                  { value: 'America/Bogota', label: 'Bogotá (GMT-5)' },
                  { value: 'America/Mexico_City', label: 'Ciudad de México (GMT-6)' },
                  { value: 'America/Lima', label: 'Lima (GMT-5)' },
                  { value: 'America/Santiago', label: 'Santiago (GMT-4)' },
                  { value: 'America/Buenos_Aires', label: 'Buenos Aires (GMT-3)' },
                  { value: 'Europe/Madrid', label: 'Madrid (GMT+1)' },
                  { value: 'UTC', label: 'UTC (GMT+0)' },
                ]}
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
              />

              <WorkingHoursEditor schedule={schedule} onChange={setSchedule} />

              <div className="space-y-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  Mensaje fuera de horario
                </label>
                <p className="text-xs text-gray-500">Mensaje que se envía cuando el cliente escribe fuera del horario laboral</p>
                <textarea
                  className="h-20 w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
                  value={closedMessage}
                  onChange={(e) => setClosedMessage(e.target.value)}
                />
              </div>
            </>
          )}
        </div>
      </div>

      <div className="flex justify-end">
        <Button onClick={handleSave} loading={updateSettings.isPending}>
          <Save size={16} className="mr-2" />
          Guardar cambios
        </Button>
      </div>
    </div>
  )
}

function NotificationSettings() {
  const { data: settings, isLoading } = useSettingsByCategory('notifications')
  const updateSettings = useUpdateSettings()
  const [values, setValues] = useState<Record<string, string>>({})

  useEffect(() => {
    if (settings) {
      const v: Record<string, string> = {}
      settings.forEach((s) => { v[s.key] = s.value ?? '' })
      setValues(v)
    }
  }, [settings])

  const handleSave = () => {
    updateSettings.mutate({
      category: 'notifications',
      settings: Object.entries(values).map(([key, value]) => ({ key, value })),
    })
  }

  if (isLoading) return <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950"><p className="text-gray-500">Cargando...</p></div>

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Notificaciones</h3>
        <p className="mt-1 text-sm text-gray-500">Configura cómo y cuándo recibir notificaciones</p>
        <div className="mt-6 space-y-4">
          {settings?.map((s) => (
            <div key={s.key} className="flex items-center justify-between">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{s.description || s.key}</label>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  className="peer sr-only"
                  checked={values[s.key] === 'true'}
                  onChange={(e) => setValues((prev) => ({ ...prev, [s.key]: e.target.checked ? 'true' : 'false' }))}
                />
                <div className="h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white dark:border-gray-600 dark:bg-gray-700" />
              </label>
            </div>
          ))}
          {(!settings || settings.length === 0) && (
            <p className="text-sm text-gray-500">No hay configuraciones disponibles</p>
          )}
        </div>
      </div>
      <div className="flex justify-end">
        <Button onClick={handleSave} loading={updateSettings.isPending}>
          <Save size={16} className="mr-2" />
          Guardar cambios
        </Button>
      </div>
    </div>
  )
}

function SecuritySettings() {
  const [showCurrent, setShowCurrent] = useState(false)
  const [showNew, setShowNew] = useState(false)
  const [saving, setSaving] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  })

  const onSubmit = async (data: PasswordForm) => {
    setSaving(true)
    try {
      await api.put('/auth/users/me/password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      })
      reset()
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Seguridad</h3>
        <p className="mt-1 text-sm text-gray-500">Cambia tu contraseña y configura la autenticación</p>
        <div className="mt-6 space-y-4">
          <div className="relative">
            <Input
              label="Contraseña actual"
              type={showCurrent ? 'text' : 'password'}
              error={errors.currentPassword?.message}
              {...register('currentPassword')}
            />
            <button
              type="button"
              className="absolute right-3 top-9 text-gray-400 hover:text-gray-600"
              onClick={() => setShowCurrent(!showCurrent)}
            >
              {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          <div className="relative">
            <Input
              label="Nueva contraseña"
              type={showNew ? 'text' : 'password'}
              error={errors.newPassword?.message}
              {...register('newPassword')}
            />
            <button
              type="button"
              className="absolute right-3 top-9 text-gray-400 hover:text-gray-600"
              onClick={() => setShowNew(!showNew)}
            >
              {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          <Input
            label="Confirmar nueva contraseña"
            type="password"
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />
        </div>
      </div>
      <div className="flex justify-end">
        <Button type="submit" loading={saving}>
          <Save size={16} className="mr-2" />
          Cambiar contraseña
        </Button>
      </div>
    </form>
  )
}

function GeneralSettings() {
  const { data: settings, isLoading } = useSettingsByCategory('general')
  const updateSettings = useUpdateSettings()
  const [values, setValues] = useState<Record<string, string>>({})

  useEffect(() => {
    if (settings) {
      const v: Record<string, string> = {}
      settings.forEach((s) => { v[s.key] = s.value ?? '' })
      setValues(v)
    }
  }, [settings])

  const handleSave = () => {
    updateSettings.mutate({
      category: 'general',
      settings: Object.entries(values).map(([key, value]) => ({ key, value })),
    })
  }

  if (isLoading) return <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950"><p className="text-gray-500">Cargando...</p></div>

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Configuración general</h3>
        <p className="mt-1 text-sm text-gray-500">Idioma, zona horaria y preferencias</p>
        <div className="mt-6 space-y-4">
          {settings?.map((s) => (
            <div key={s.key} className="space-y-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{s.description || s.key}</label>
              {s.type === 'boolean' || s.type === 'select' ? (
                <select
                  className="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
                  value={values[s.key] ?? ''}
                  onChange={(e) => setValues((prev) => ({ ...prev, [s.key]: e.target.value }))}
                >
                  {s.type === 'boolean' ? (
                    <>
                      <option value="true">Activado</option>
                      <option value="false">Desactivado</option>
                    </>
                  ) : (
                    (values[s.key + '_options']?.split(',') ?? []).map((opt: string) => (
                      <option key={opt} value={opt}>{opt}</option>
                    ))
                  )}
                </select>
              ) : (
                <input
                  className="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100"
                  value={values[s.key] ?? ''}
                  onChange={(e) => setValues((prev) => ({ ...prev, [s.key]: e.target.value }))}
                />
              )}
            </div>
          ))}
          {(!settings || settings.length === 0) && (
            <p className="text-sm text-gray-500">No hay configuraciones disponibles</p>
          )}
        </div>
      </div>
      <div className="flex justify-end">
        <Button onClick={handleSave} loading={updateSettings.isPending}>
          <Save size={16} className="mr-2" />
          Guardar cambios
        </Button>
      </div>
    </div>
  )
}

const tabComponents: Record<string, React.FC> = {
  profile: ProfileSettings,
  company: CompanySettings,
  bot: BotSettings,
  ai: AISettings,
  working_hours: WorkingHoursSettings,
  knowledge: KnowledgeBasePage,
  notifications: NotificationSettings,
  security: SecuritySettings,
  general: GeneralSettings,
}

export default function SettingsPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState('profile')

  const ActiveComponent = tabComponents[activeTab] ?? ProfileSettings

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('navigation.settings')}</h1>
        <p className="mt-1 text-sm text-gray-500">Configuración del sistema</p>
      </div>

      <Tabs
        tabs={settingsTabs}
        activeTab={activeTab}
        onChange={setActiveTab}
        variant="pills"
      />

      <ActiveComponent />
    </div>
  )
}
