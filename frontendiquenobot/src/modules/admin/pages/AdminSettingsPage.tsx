import { useState, useEffect } from 'react'
import { Save, Mail, Bell, Shield, Database, Loader } from 'lucide-react'
import { adminService, type SettingDto } from '@/services/admin.service'

interface Section {
  category: string
  title: string
  icon: React.ComponentType<{ size?: number; className?: string }>
  fields: { key: string; label: string; type: string; placeholder?: string }[]
}

const SECTIONS: Section[] = [
  {
    category: 'smtp',
    title: 'Correo Electrónico',
    icon: Mail,
    fields: [
      { key: 'smtp_host', label: 'SMTP Host', type: 'text', placeholder: 'smtp.example.com' },
      { key: 'smtp_port', label: 'SMTP Puerto', type: 'text', placeholder: '587' },
      { key: 'smtp_user', label: 'SMTP Usuario', type: 'text' },
      { key: 'smtp_pass', label: 'SMTP Contraseña', type: 'password' },
      { key: 'smtp_from', label: 'Correo remitente', type: 'text', placeholder: 'noreply@iquenobot.com' },
    ],
  },
  {
    category: 'notifications',
    title: 'Notificaciones',
    icon: Bell,
    fields: [
      { key: 'email_notifications', label: 'Notificaciones por correo', type: 'checkbox' },
      { key: 'system_alerts', label: 'Alertas del sistema', type: 'checkbox' },
      { key: 'alert_email', label: 'Correo para alertas', type: 'text' },
    ],
  },
  {
    category: 'security',
    title: 'Seguridad',
    icon: Shield,
    fields: [
      { key: 'max_login_attempts', label: 'Intentos de login máximos', type: 'number' },
      { key: 'lockout_minutes', label: 'Bloqueo temporal (minutos)', type: 'number' },
      { key: 'password_min_length', label: 'Longitud mínima contraseña', type: 'number' },
      { key: 'session_timeout', label: 'Timeout de sesión (minutos)', type: 'number' },
    ],
  },
  {
    category: 'backups',
    title: 'Respaldos',
    icon: Database,
    fields: [
      { key: 'auto_backup', label: 'Respaldos automáticos', type: 'checkbox' },
      { key: 'backup_frequency', label: 'Frecuencia (horas)', type: 'number' },
      { key: 'backup_retention', label: 'Retención (días)', type: 'number' },
    ],
  },
]

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState<Record<string, Record<string, string>>>({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => { loadAll() }, [])

  const loadAll = async () => {
    setLoading(true)
    const result: Record<string, Record<string, string>> = {}
    for (const section of SECTIONS) {
      try {
        const items = await adminService.getSettings(section.category)
        result[section.category] = {}
        for (const item of items) result[section.category][item.key] = item.value
      } catch { result[section.category] = {} }
    }
    setSettings(result)
    setLoading(false)
  }

  const getValue = (cat: string, key: string): string => settings[cat]?.[key] ?? ''
  const setValue = (cat: string, key: string, val: string) => {
    setSettings({ ...settings, [cat]: { ...(settings[cat] || {}), [key]: val } })
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      for (const section of SECTIONS) {
        const catSettings = settings[section.category] || {}
        await adminService.updateSettings(section.category, catSettings)
      }
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch (e) { console.error(e) }
    finally { setSaving(false) }
  }

  if (loading) return (
    <div className="flex h-64 items-center justify-center">
      <Loader className="h-8 w-8 animate-spin text-brand-600" />
    </div>
  )

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Configuración Global</h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {SECTIONS.map((section) => {
          const Icon = section.icon
          return (
            <section key={section.category} className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-800 dark:bg-gray-950">
              <div className="mb-4 flex items-center gap-2">
                <Icon className="text-blue-600" size={20} />
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{section.title}</h2>
              </div>
              <div className="space-y-4">
                {section.fields.map((field) => (
                  <div key={field.key}>
                    {field.type === 'checkbox' ? (
                      <label className="flex items-center gap-2">
                        <input type="checkbox" checked={getValue(section.category, field.key) === 'true'}
                          onChange={(e) => setValue(section.category, field.key, String(e.target.checked))} className="rounded" />
                        <span className="text-sm text-gray-700 dark:text-gray-300">{field.label}</span>
                      </label>
                    ) : (
                      <>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">{field.label}</label>
                        <input type={field.type} value={getValue(section.category, field.key)}
                          onChange={(e) => setValue(section.category, field.key, e.target.value)}
                          placeholder={field.placeholder}
                          className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-white" />
                      </>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )
        })}
      </div>

      <div className="flex justify-end">
        <button onClick={handleSave} disabled={saving}
          className="flex items-center gap-2 rounded-lg bg-brand-600 px-6 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50">
          {saving ? <Loader className="h-4 w-4 animate-spin" /> : <Save size={18} />}
          {saved ? 'Guardado' : 'Guardar configuración'}
        </button>
      </div>
    </div>
  )
}
