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
import { useAuthStore } from '@/core/auth/auth.store'
import { api } from '@/core/api/client'
import { settingService } from '@/services/setting.service'
import { useSettingsByCategory, useUpdateSettings } from '@/modules/settings/hooks/useSettings'
import type { ApiResponse } from '@/types/api'
import type { UserDto } from '@/types/auth'

const settingsTabs = [
  { id: 'profile', label: 'Perfil', icon: <User size={16} /> },
  { id: 'company', label: 'Empresa', icon: <Building2 size={16} /> },
  { id: 'ai', label: 'Inteligencia Artificial', icon: <Cpu size={16} /> },
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

function AISettings() {
  const { data: settings, isLoading } = useSettingsByCategory('ai')
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
      category: 'ai',
      settings: Object.entries(values).map(([key, value]) => ({ key, value })),
    })
  }

  if (isLoading) return <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950"><p className="text-gray-500">Cargando...</p></div>

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-950">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Inteligencia Artificial</h3>
        <p className="mt-1 text-sm text-gray-500">Configura los proveedores de IA y el modelo</p>
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
                  <option value="true">Activado</option>
                  <option value="false">Desactivado</option>
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
  ai: AISettings,
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
