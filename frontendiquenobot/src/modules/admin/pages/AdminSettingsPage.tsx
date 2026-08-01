import { useState, useEffect, useCallback, useMemo } from 'react'
import { Save, Mail, Bell, Shield, Database, Send, HardDriveDownload, Loader2, KeyRound, Server, CheckCircle2, Clock, FileText, AlertTriangle, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { adminService, type SettingDto, type BackupInfo } from '@/services/admin.service'
import { Button } from '@/shared/atoms/Button/Button'
import { Input } from '@/shared/atoms/Input/Input'
import { Toggle } from '@/shared/atoms/Toggle/Toggle'
import { Badge } from '@/shared/atoms/Badge/Badge'
import { Tabs } from '@/shared/molecules/Tabs'
import { cn } from '@/shared/utils'

type FieldType = 'text' | 'password' | 'number' | 'checkbox'

interface FieldDef {
  key: string
  label: string
  type: FieldType
  placeholder?: string
  helperText?: string
  min?: number
  step?: number
  suffix?: string
}

interface SectionDef {
  category: string
  title: string
  description: string
  icon: React.ComponentType<{ size?: number; className?: string }>
  fields: FieldDef[]
}

const SECTIONS: SectionDef[] = [
  {
    category: 'smtp',
    title: 'Correo Electrónico',
    description: 'Configura el servidor SMTP para enviar correos del sistema (verificación, recuperación de contraseña y alertas).',
    icon: Mail,
    fields: [
      { key: 'smtp_host', label: 'Servidor SMTP', type: 'text', placeholder: 'smtp.gmail.com', helperText: 'Host del servidor de correo saliente.' },
      { key: 'smtp_port', label: 'Puerto', type: 'number', placeholder: '587', helperText: 'Usualmente 587 (STARTTLS) o 465 (SSL).', min: 1, step: 1 },
      { key: 'smtp_user', label: 'Usuario', type: 'text', placeholder: 'cuenta@dominio.com', helperText: 'Cuenta de autenticación del servidor SMTP.' },
      { key: 'smtp_pass', label: 'Contraseña', type: 'password', placeholder: '••••••••••', helperText: 'Contraseña o clave de aplicación del SMTP.' },
      { key: 'smtp_from', label: 'Correo remitente', type: 'text', placeholder: 'noreply@iquenobot.com', helperText: 'Dirección que aparece como remitente de los correos.' },
    ],
  },
  {
    category: 'notifications',
    title: 'Notificaciones',
    description: 'Controla cómo el sistema envía notificaciones por correo electrónico.',
    icon: Bell,
    fields: [
      { key: 'email_notifications', label: 'Notificaciones por correo', type: 'checkbox', helperText: 'Activa o desactiva el envío de todos los correos del sistema.' },
      { key: 'system_alerts', label: 'Alertas del sistema', type: 'checkbox', helperText: 'Recibe alertas sobre eventos importantes (errores de respaldo, fallos críticos).' },
      { key: 'alert_email', label: 'Correo para alertas', type: 'text', placeholder: 'admin@iquenobot.com', helperText: 'Dirección que recibe las alertas del sistema.' },
    ],
  },
  {
    category: 'security',
    title: 'Seguridad',
    description: 'Políticas de acceso y protección de cuentas para toda la plataforma.',
    icon: Shield,
    fields: [
      { key: 'max_login_attempts', label: 'Intentos de login máximos', type: 'number', placeholder: '5', helperText: 'Intentos fallidos antes de bloquear la cuenta.', min: 1, step: 1 },
      { key: 'lockout_minutes', label: 'Bloqueo temporal (minutos)', type: 'number', placeholder: '15', helperText: 'Duración del bloqueo tras superar los intentos.', min: 1, step: 1 },
      { key: 'password_min_length', label: 'Longitud mínima contraseña', type: 'number', placeholder: '8', helperText: 'Mínimo de caracteres para nuevas contraseñas.', min: 6, step: 1 },
      { key: 'session_timeout', label: 'Timeout de sesión (minutos)', type: 'number', placeholder: '480', helperText: 'Minutos de inactividad antes de cerrar la sesión.', min: 5, step: 5 },
    ],
  },
  {
    category: 'backups',
    title: 'Respaldos',
    description: 'Programa respaldos automáticos de la base de datos y administra los archivos generados.',
    icon: Database,
    fields: [
      { key: 'auto_backup', label: 'Respaldos automáticos', type: 'checkbox', helperText: 'Genera respaldos de la base de datos automáticamente.' },
      { key: 'backup_frequency', label: 'Frecuencia (horas)', type: 'number', placeholder: '24', helperText: 'Cada cuántas horas se ejecuta un respaldo automático.', min: 1, step: 1 },
      { key: 'backup_retention', label: 'Retención (días)', type: 'number', placeholder: '7', helperText: 'Días que se conservan los respaldos antes de eliminarse.', min: 1, step: 1 },
    ],
  },
]

type SettingsMap = Record<string, Record<string, string>>

const DEFAULT_VALUES: Record<string, string> = {
  smtp_host: 'smtp.example.com',
  smtp_port: '587',
  smtp_user: '',
  smtp_pass: '',
  smtp_from: '',
  email_notifications: 'true',
  system_alerts: 'false',
  alert_email: '',
  max_login_attempts: '5',
  lockout_minutes: '15',
  password_min_length: '8',
  session_timeout: '480',
  auto_backup: 'false',
  backup_frequency: '24',
  backup_retention: '7',
}

function getErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null) {
    const err = error as { response?: { data?: { message?: string } } }
    return err.response?.data?.message ?? 'Ocurrió un error inesperado'
  }
  return String(error)
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return String(bytes) + ' B'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function formatDate(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  return d.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState<SettingsMap>({})
  const [original, setOriginal] = useState<SettingsMap>({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('smtp')
  const [dirty, setDirty] = useState(false)

  const loadAll = useCallback(async () => {
    setLoading(true)
    const result: SettingsMap = {}
    for (const section of SECTIONS) {
      const cat: Record<string, string> = {}
      try {
        const items: SettingDto[] = await adminService.getSettings(section.category)
        for (const item of items) cat[item.key] = item.value
      } catch {
        // keep defaults
      }
      for (const field of section.fields) {
        if (cat[field.key] === undefined) cat[field.key] = DEFAULT_VALUES[field.key] ?? ''
      }
      result[section.category] = cat
    }
    setSettings(result)
    setOriginal(structuredClone(result))
    setDirty(false)
    setLoading(false)
  }, [])

  useEffect(() => { void loadAll() }, [loadAll])

  const getValue = (cat: string, key: string) => settings[cat]?.[key] ?? ''
  const setValue = (cat: string, key: string, val: string) => {
    setSettings(prev => ({ ...prev, [cat]: { ...(prev[cat] || {}), [key]: val } }))
  }

  const isDirty = useMemo(() => JSON.stringify(settings) !== JSON.stringify(original), [settings, original])

  useEffect(() => {
    setDirty(isDirty)
  }, [isDirty])

  const validate = (): string | null => {
    const smtp = settings.smtp || {}
    const port = parseInt(smtp.smtp_port, 10)
    if (smtp.smtp_port && (isNaN(port) || port < 1 || port > 65535)) return 'El puerto SMTP debe ser un número entre 1 y 65535'
    const email = smtp.smtp_from
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'El correo remitente no es válido'
    const alertEmail = settings.notifications?.alert_email
    if (alertEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(alertEmail)) return 'El correo para alertas no es válido'
    for (const section of SECTIONS) {
      for (const field of section.fields) {
        if (field.type !== 'number') continue
        const val = getValue(section.category, field.key)
        if (val === '') continue
        const num = parseInt(val, 10)
        if (isNaN(num)) return `El campo "${field.label}" debe ser un número`
        if (field.min !== undefined && num < field.min) return `El campo "${field.label}" debe ser al menos ${String(field.min)}`
      }
    }
    return null
  }

  const handleSave = async () => {
    const error = validate()
    if (error) {
      toast.error(error)
      return
    }
    setSaving(true)
    try {
      for (const section of SECTIONS) {
        const catSettings = settings[section.category] || {}
        await adminService.updateSettings(section.category, catSettings)
      }
    setOriginal(structuredClone(settings))
    setDirty(false)
      toast.success('Configuración guardada correctamente')
    } catch (e) {
      toast.error(getErrorMessage(e))
    } finally {
      setSaving(false)
    }
  }

  const handleTestSmtp = async () => {
    const recipient = settings.smtp?.smtp_from || settings.notifications?.alert_email
    if (!recipient) {
      toast.error('Configura primero el correo remitente para poder enviar el correo de prueba')
      return
    }
    toast.loading('Enviando correo de prueba...', { id: 'smtp-test' })
    try {
      await adminService.testSmtp(recipient)
      toast.success(`Correo de prueba enviado a ${recipient}`, { id: 'smtp-test' })
    } catch (e) {
      toast.error(getErrorMessage(e), { id: 'smtp-test' })
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
      </div>
    )
  }

  const tabs = SECTIONS.map(s => ({ id: s.category, label: s.title, icon: <s.icon size={16} /> }))

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-brand-600 dark:text-brand-400">Panel de Super Admin</p>
          <h1 className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">Configuración Global</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Administra el correo, la seguridad, las notificaciones y los respaldos de toda la plataforma.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {dirty && <Badge variant="warning" dot>Cambios sin guardar</Badge>}
          <Button onClick={() => void handleSave()} loading={saving} disabled={!dirty}>
            <Save size={16} />
            Guardar configuración
          </Button>
        </div>
      </div>

      <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} variant="pills" />

      {SECTIONS.map(section => (
        <section
          key={section.category}
          className={cn('space-y-5 rounded-2xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-800 dark:bg-gray-950', activeTab !== section.category && 'hidden')}
        >
          <div className="flex items-start gap-3">
            <div className="rounded-xl bg-brand-50 p-2.5 text-brand-600 dark:bg-brand-900/20 dark:text-brand-400">
              <section.icon size={22} />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{section.title}</h2>
              <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{section.description}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            {section.fields.map(field => (
              <div key={field.key} className={cn(field.type === 'checkbox' && 'md:col-span-2')}>
                {field.type === 'checkbox' ? (
                  <div className="rounded-xl border border-gray-200 p-4 dark:border-gray-700">
                    <Toggle
                      checked={getValue(section.category, field.key) === 'true'}
                      onChange={(checked) => { setValue(section.category, field.key, String(checked)) }}
                      label={field.label}
                    />
                    {field.helperText && <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400">{field.helperText}</p>}
                  </div>
                ) : (
                  <div className="relative">
                    <Input
                      type={field.type === 'password' ? 'password' : field.type === 'number' ? 'text' : 'text'}
                      inputMode={field.type === 'number' ? 'numeric' : undefined}
                      label={field.label}
                      value={getValue(section.category, field.key)}
                      onChange={(e) => { setValue(section.category, field.key, e.target.value) }}
                      placeholder={field.placeholder}
                      helperText={field.helperText}
                      leftIcon={field.type === 'password' ? <KeyRound size={16} /> : field.type === 'number' ? <Server size={16} /> : undefined}
                    />
                    {field.suffix && (
                      <span className="absolute bottom-2.5 right-3 text-xs text-gray-400">{field.suffix}</span>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Section-specific actions */}
          {section.category === 'smtp' && (
            <div className="rounded-xl border border-dashed border-brand-200 bg-brand-50/50 p-4 dark:border-brand-900 dark:bg-brand-900/10">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-2">
                  <Send size={16} className="mt-0.5 text-brand-600 dark:text-brand-400" />
                  <div>
                    <p className="text-sm font-medium text-gray-900 dark:text-white">Probar conexión SMTP</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      Enviaremos un correo de prueba al remitente configurado para validar la conexión.
                    </p>
                  </div>
                </div>
                <Button variant="outline" size="sm" onClick={() => void handleTestSmtp()}>
                  <Send size={14} />
                  Enviar correo de prueba
                </Button>
              </div>
            </div>
          )}

          {section.category === 'backups' && <BackupsSection />}
        </section>
      ))}

      {/* Sticky save bar */}
      <div className="sticky bottom-4 z-10 flex items-center justify-between rounded-xl border border-gray-200 bg-white/90 px-5 py-3 shadow-lg backdrop-blur dark:border-gray-800 dark:bg-gray-950/90">
        <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          {dirty ? (
            <><AlertTriangle size={16} className="text-amber-500" /> Tienes cambios sin guardar</>
          ) : (
            <><CheckCircle2 size={16} className="text-green-500" /> Todos los cambios guardados</>
          )}
        </div>
        <Button onClick={() => void handleSave()} loading={saving} disabled={!dirty}>
          <Save size={16} />
          Guardar configuración
        </Button>
      </div>
    </div>
  )
}

function BackupsSection() {
  const [backups, setBackups] = useState<BackupInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)

  const load = useCallback(async () => {
    try {
      const data = await adminService.getBackups()
      setBackups(data)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])

  const handleCreate = async () => {
    setCreating(true)
    try {
      await adminService.createBackup()
      toast.success('Respaldo creado correctamente')
      await load()
    } catch (e) {
      toast.error(getErrorMessage(e))
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-700">
      <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3 dark:border-gray-700">
        <div className="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
          <HardDriveDownload size={16} className="text-brand-600 dark:text-brand-400" />
          Respaldos generados
        </div>
        <Button variant="outline" size="sm" onClick={() => void handleCreate()} loading={creating}>
          <RefreshCw size={14} />
          Crear respaldo ahora
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-10">
          <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
        </div>
      ) : backups.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-2 py-10 text-center">
          <Database size={28} className="text-gray-300 dark:text-gray-600" />
          <p className="text-sm text-gray-500 dark:text-gray-400">Aún no hay respaldos. Crea el primero o activa los respaldos automáticos.</p>
        </div>
      ) : (
        <ul className="divide-y divide-gray-100 dark:divide-gray-800">
          {backups.map(b => (
            <li key={b.fileName} className="flex items-center justify-between px-4 py-3">
              <div className="flex items-center gap-3">
                <FileText size={18} className="text-gray-400" />
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-white">{b.fileName}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{formatDate(b.createdAt)}</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant="neutral">{formatBytes(b.sizeBytes)}</Badge>
                <Clock size={14} className="text-gray-400" />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
